/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.lyca.xml.dtm.ref;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import de.lyca.xml.dtm.DTM;
import de.lyca.xml.dtm.DTMException;
import de.lyca.xml.dtm.DTMFilter;
import de.lyca.xml.dtm.DTMIterator;
import de.lyca.xml.dtm.DTMManager;
import de.lyca.xml.dtm.DTMWSFilter;
import de.lyca.xml.dtm.ref.dom2dtm.DOM2DTM;
import de.lyca.xml.dtm.ref.dom2dtm.DOM2DTMdefaultNamespaceDeclarationNode;
import de.lyca.xml.dtm.ref.sax2dtm.SAX2DTM;
import de.lyca.xml.dtm.ref.sax2dtm.SAX2RTFDTM;
import de.lyca.xml.res.Messages;
import de.lyca.xml.utils.PrefixResolver;
import de.lyca.xml.utils.SuballocatedIntVector;
import de.lyca.xml.utils.SystemIDResolver;
import de.lyca.xml.utils.WrappedRuntimeException;
import de.lyca.xml.utils.XMLReaderManager;
import de.lyca.xml.utils.XMLStringFactory;

/**
 * The default implementation for the DTMManager.
 * 
 * %REVIEW% There is currently a reentrancy issue, since the finalizer for XRTreeFrag (which runs in the GC thread)
 * wants to call DTMManager.release(), and may do so at the same time that the main transformation thread is accessing
 * the manager. Our current solution is to make most of the manager's methods <code>synchronized</code>. Early tests
 * suggest that doing so is not causing a significant performance hit in Xalan. However, it should be noted that there
 * is a possible alternative solution: rewrite release() so it merely posts a request for release onto a threadsafe
 * queue, and explicitly process that queue on an infrequent basis during main-thread activity (eg, when getDTM() is
 * invoked). The downside of that solution would be a greater delay before the DTM's storage is actually released for
 * reuse.
 */
public class DTMManagerDefault extends DTMManager {
  // static final boolean JKESS_XNI_EXPERIMENT=true;

  /** Set this to true if you want a dump of the DTM after creation. */
  private static final boolean DUMPTREE = false;

  /** Set this to true if you want a basic diagnostics. */
  private static final boolean DEBUG = false;

  /**
   * Map from DTM identifier numbers to DTM objects that this manager manages. One DTM may have several prefix numbers,
   * if extended node indexing is in use; in that case, m_dtm_offsets[] will used to control which prefix maps to which
   * section of the DTM.
   * 
   * This array grows as necessary; see addDTM().
   * 
   * This array grows as necessary; see addDTM(). Growth is uncommon... but access needs to be blindingly fast since
   * it's used in node addressing.
   */
  protected DTM m_dtms[] = new DTM[256];

  /**
   * Map from DTM identifier numbers to offsets. For small DTMs with a single identifier, this will always be 0. In
   * overflow addressing, where additional identifiers are allocated to access nodes beyond the range of a single Node
   * Handle, this table is used to map the handle's node field into the actual node identifier.
   * 
   * This array grows as necessary; see addDTM().
   * 
   * This array grows as necessary; see addDTM(). Growth is uncommon... but access needs to be blindingly fast since
   * it's used in node addressing. (And at the moment, that includes accessing it from DTMDefaultBase, which is why this
   * is not Protected or Private.)
   */
  int m_dtm_offsets[] = new int[256];

  /**
   * The cache for XMLReader objects to be used if the user did not supply an XMLReader for a SAXSource or supplied a
   * StreamSource.
   */
  protected XMLReaderManager m_readerManager = null;

  /**
   * The default implementation of ContentHandler, DTDHandler and ErrorHandler.
   */
  protected DefaultHandler m_defaultHandler = new DefaultHandler();

  /**
   * Add a DTM to the DTM table. This convenience call adds it as the "base DTM ID", with offset 0. The other version of
   * addDTM should be used if you want to add "extended" DTM IDs with nonzero offsets.
   * 
   * @param dtm Should be a valid reference to a DTM.
   * @param id Integer DTM ID to be bound to this DTM
   */
  synchronized public void addDTM(DTM dtm, int id) {
    addDTM(dtm, id, 0);
  }

  /**
   * Add a DTM to the DTM table.
   * 
   * @param dtm Should be a valid reference to a DTM.
   * @param id Integer DTM ID to be bound to this DTM.
   * @param offset Integer addressing offset. The internal DTM Node ID is obtained by adding this offset to the
   *        node-number field of the public DTM Handle. For the first DTM ID accessing each DTM, this is 0; for overflow
   *        addressing it will be a multiple of {@literal 1<<IDENT_DTM_NODE_BITS}.
   */
  synchronized public void addDTM(DTM dtm, int id, int offset) {
    if (id >= IDENT_MAX_DTMS)
      // TODO: %REVIEW% Not really the right error message.
      // "No more DTM IDs are available!");
      throw new DTMException(Messages.get().noDtmidsAvail());

    // We used to just allocate the array size to IDENT_MAX_DTMS.
    // But we expect to increase that to 16 bits, and I'm not willing
    // to allocate that much space unless needed. We could use one of our
    // handy-dandy Fast*Vectors, but this will do for now.
    // %REVIEW%
    final int oldlen = m_dtms.length;
    if (oldlen <= id) {
      // Various growth strategies are possible. I think we don't want
      // to over-allocate excessively, and I'm willing to reallocate
      // more often to get that. See also Fast*Vector classes.
      //
      // %REVIEW% Should throw a more diagnostic error if we go over the max...
      final int newlen = Math.min(id + 256, IDENT_MAX_DTMS);

      final DTM new_m_dtms[] = new DTM[newlen];
      System.arraycopy(m_dtms, 0, new_m_dtms, 0, oldlen);
      m_dtms = new_m_dtms;
      final int new_m_dtm_offsets[] = new int[newlen];
      System.arraycopy(m_dtm_offsets, 0, new_m_dtm_offsets, 0, oldlen);
      m_dtm_offsets = new_m_dtm_offsets;
    }

    m_dtms[id] = dtm;
    m_dtm_offsets[id] = offset;
    dtm.documentRegistration();
    // The DTM should have been told who its manager was when we created it.
    // Do we need to allow for adopting DTMs _not_ created by this manager?
  }

  /**
   * Get the first free DTM ID available. %OPT% Linear search is inefficient!
   * 
   * @return TODO
   */
  synchronized public int getFirstFreeDTMID() {
    final int n = m_dtms.length;
    for (int i = 1; i < n; i++) {
      if (null == m_dtms[i])
        return i;
    }
    return n; // count on addDTM() to throw exception if out of range
  }

  /**
   * The default table for exandedNameID lookups.
   */
  private final ExpandedNameTable m_expandedNameTable = new ExpandedNameTable();

  /**
   * Constructor DTMManagerDefault
   * 
   */
  public DTMManagerDefault() {
  }

  /**
   * Get an instance of a DTM, loaded with the content from the specified source. If the unique flag is true, a new
   * instance will always be returned. Otherwise it is up to the DTMManager to return a new instance or an instance that
   * it already created and may be being used by someone else.
   * 
   * A bit of magic in this implementation: If the source is null, unique is true, and incremental and doIndexing are
   * both false, we return an instance of SAX2RTFDTM, which see.
   * 
   * (I think more parameters will need to be added for error handling, and entity resolution, and more explicit control
   * of the RTF situation).
   * 
   * @param source the specification of the source object.
   * @param unique true if the returned DTM must be unique, probably because it is going to be mutated.
   * @param whiteSpaceFilter Enables filtering of whitespace nodes, and may be null.
   * @param incremental true if the DTM should be built incrementally, if possible.
   * @param doIndexing true if the caller considers it worth it to use indexing schemes.
   * 
   * @return a non-null DTM reference.
   */
  @Override
  synchronized public DTM getDTM(Source source, boolean unique, DTMWSFilter whiteSpaceFilter, boolean incremental,
      boolean doIndexing) {

    if (DEBUG && null != source) {
      System.out.println("Starting " + (unique ? "UNIQUE" : "shared") + " source: " + source.getSystemId());
    }

    final XMLStringFactory xstringFactory = m_xsf;
    final int dtmPos = getFirstFreeDTMID();
    final int documentID = dtmPos << IDENT_DTM_NODE_BITS;

    if (null != source && source instanceof DOMSource) {
      final DOM2DTM dtm = new DOM2DTM(this, (DOMSource) source, documentID, whiteSpaceFilter, xstringFactory,
          doIndexing);

      addDTM(dtm, dtmPos, 0);

      // if (DUMPTREE)
      // {
      // dtm.dumpDTM();
      // }

      return dtm;
    } else {
      final boolean isSAXSource = null != source ? source instanceof SAXSource : true;
      final boolean isStreamSource = null != source ? source instanceof StreamSource : false;

      if (isSAXSource || isStreamSource) {
        XMLReader reader = null;
        SAX2DTM dtm;

        try {
          InputSource xmlSource;

          if (null == source) {
            xmlSource = null;
          } else {
            reader = getXMLReader(source);
            xmlSource = SAXSource.sourceToInputSource(source);

            String urlOfSource = xmlSource.getSystemId();

            if (null != urlOfSource) {
              try {
                urlOfSource = SystemIDResolver.getAbsoluteURI(urlOfSource);
              } catch (final Exception e) {
                // %REVIEW% Is there a better way to send a warning?
                System.err.println("Can not absolutize URL: " + urlOfSource);
              }

              xmlSource.setSystemId(urlOfSource);
            }
          }

          if (source == null && unique && !incremental && !doIndexing) {
            // Special case to support RTF construction into shared DTM.
            // It should actually still work for other uses,
            // but may be slightly deoptimized relative to the base
            // to allow it to deal with carrying multiple documents.
            //
            // %REVIEW% This is a sloppy way to request this mode;
            // we need to consider architectural improvements.
            dtm = new SAX2RTFDTM(this, source, documentID, whiteSpaceFilter, xstringFactory, doIndexing);
          }
          /**************************************************************
           * // EXPERIMENTAL 3/22/02 else if(JKESS_XNI_EXPERIMENT && m_incremental) { dtm = new XNI2DTM(this, source,
           * documentID, whiteSpaceFilter, xstringFactory, doIndexing); }
           **************************************************************/
          // Create the basic SAX2DTM.
          else {
            dtm = new SAX2DTM(this, source, documentID, whiteSpaceFilter, xstringFactory, doIndexing);
          }

          // Go ahead and add the DTM to the lookup table. This needs to be
          // done before any parsing occurs. Note offset 0, since we've just
          // created a new DTM.
          addDTM(dtm, dtmPos, 0);

          final boolean haveXercesParser = null != reader
              && reader.getClass().getName().equals("org.apache.xerces.parsers.SAXParser");

          if (haveXercesParser) {
            incremental = true; // No matter what. %REVIEW%
          }

          // If the reader is null, but they still requested an incremental
          // build, then we still want to set up the IncrementalSAXSource stuff.
          if (m_incremental && incremental
          /* || ((null == reader) && incremental) */) {
            IncrementalSAXSource coParser = null;

            if (haveXercesParser) {
              // IncrementalSAXSource_Xerces to avoid threading.
              try {
                coParser = (IncrementalSAXSource) Class.forName("de.lyca.xml.dtm.ref.IncrementalSAXSource_Xerces")
                    .newInstance();
              } catch (final Exception ex) {
                ex.printStackTrace();
                coParser = null;
              }
            }

            if (coParser == null) {
              // Create a IncrementalSAXSource to run on the secondary thread.
              if (null == reader) {
                coParser = new IncrementalSAXSource_Filter();
              } else {
                final IncrementalSAXSource_Filter filter = new IncrementalSAXSource_Filter();
                filter.setXMLReader(reader);
                coParser = filter;
              }
            }

            /**************************************************************
             * // EXPERIMENTAL 3/22/02 if (JKESS_XNI_EXPERIMENT && m_incremental && dtm instanceof XNI2DTM && coParser
             * instanceof IncrementalSAXSource_Xerces) { org.apache.xerces.xni.parser.XMLPullParserConfiguration xpc=
             * ((IncrementalSAXSource_Xerces)coParser) .getXNIParserConfiguration(); if (xpc!=null) { // Bypass SAX;
             * listen to the XNI stream ((XNI2DTM)dtm).setIncrementalXNISource(xpc); } else { // Listen to the SAX
             * stream (will fail, diagnostically...) dtm.setIncrementalSAXSource(coParser); } } else
             ***************************************************************/

            // Have the DTM set itself up as IncrementalSAXSource's listener.
            dtm.setIncrementalSAXSource(coParser);

            if (null == xmlSource)
              // Then the user will construct it themselves.
              return dtm;

            if (null == reader.getErrorHandler()) {
              reader.setErrorHandler(dtm);
            }
            reader.setDTDHandler(dtm);

            try {
              // Launch parsing coroutine. Launches a second thread,
              // if we're using IncrementalSAXSource.filter().

              coParser.startParse(xmlSource);
            } catch (final RuntimeException re) {

              dtm.clearCoRoutine();

              throw re;
            } catch (final Exception e) {

              dtm.clearCoRoutine();

              throw new WrappedRuntimeException(e);
            }
          } else {
            if (null == reader)
              // Then the user will construct it themselves.
              return dtm;

            // not incremental
            reader.setContentHandler(dtm);
            reader.setDTDHandler(dtm);
            if (null == reader.getErrorHandler()) {
              reader.setErrorHandler(dtm);
            }

            try {
              reader.setProperty("http://xml.org/sax/properties/lexical-handler", dtm);
            } catch (final SAXNotRecognizedException e) {
            } catch (final SAXNotSupportedException e) {
            }

            try {
              reader.parse(xmlSource);
            } catch (final RuntimeException re) {
              dtm.clearCoRoutine();

              throw re;
            } catch (final Exception e) {
              dtm.clearCoRoutine();

              throw new WrappedRuntimeException(e);
            }
          }

          if (DUMPTREE) {
            System.out.println("Dumping SAX2DOM");
            dtm.dumpDTM(System.err);
          }

          return dtm;
        } finally {
          // Reset the ContentHandler, DTDHandler, ErrorHandler to the
          // DefaultHandler
          // after creating the DTM.
          if (reader != null && !(m_incremental && incremental)) {
            reader.setContentHandler(m_defaultHandler);
            reader.setDTDHandler(m_defaultHandler);
            reader.setErrorHandler(m_defaultHandler);

            // Reset the LexicalHandler to null after creating the DTM.
            try {
              reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
            } catch (final Exception e) {
            }
          }
          releaseXMLReader(reader);
        }
      } else
        // It should have been handled by a derived class or the caller
        // made a mistake.
        // "Not supported: " + source;
        throw new DTMException(Messages.get().notSupported(source));
    }
  }

  /**
   * Given a W3C DOM node, try and return a DTM handle. Note: calling this may be non-optimal, and there is no guarantee
   * that the node will be found in any particular DTM.
   * 
   * @param node Non-null reference to a DOM node.
   * 
   * @return a valid DTM handle.
   */
  @Override
  synchronized public int getDTMHandleFromNode(Node node) {
    if (null == node)
      // "node must be non-null for getDTMHandleFromNode!");
      throw new IllegalArgumentException(Messages.get().nodeNonNull());

    if (node instanceof DTMNodeProxy)
      return ((DTMNodeProxy) node).getDTMNodeNumber();

    else {
      // Find the DOM2DTMs wrapped around this Document (if any)
      // and check whether they contain the Node in question.
      //
      // NOTE that since a DOM2DTM may represent a subtree rather
      // than a full document, we have to be prepared to check more
      // than one -- and there is no guarantee that we will find
      // one that contains ancestors or siblings of the node we're
      // seeking.
      //
      // %REVIEW% We could search for the one which contains this
      // node at the deepest level, and thus covers the widest
      // subtree, but that's going to entail additional work
      // checking more DTMs... and getHandleOfNode is not a
      // cheap operation in most implementations.
      //
      // TODO: %REVIEW% If overflow addressing, we may recheck a DTM
      // already examined. Ouch. But with the increased number of DTMs,
      // scanning back to check this is painful.
      // POSSIBLE SOLUTIONS:
      // Generate a list of _unique_ DTM objects?
      // Have each DTM cache last DOM node search?
      final int max = m_dtms.length;
      for (int i = 0; i < max; i++) {
        final DTM thisDTM = m_dtms[i];
        if (null != thisDTM && thisDTM instanceof DOM2DTM) {
          final int handle = ((DOM2DTM) thisDTM).getHandleOfNode(node);
          if (handle != DTM.NULL)
            return handle;
        }
      }

      // Not found; generate a new DTM.
      //
      // %REVIEW% Is this really desirable, or should we return null
      // and make folks explicitly instantiate from a DOMSource? The
      // latter is more work but gives the caller the opportunity to
      // explicitly add the DTM to a DTMManager... and thus to know when
      // it can be discarded again, which is something we need to pay much
      // more attention to. (Especially since only DTMs which are assigned
      // to a manager can use the overflow addressing scheme.)
      //
      // %BUG% If the source node was a DOM2DTM$defaultNamespaceDeclarationNode
      // and the DTM wasn't registered with this DTMManager, we will create
      // a new DTM and _still_ not be able to find the node (since it will
      // be resynthesized). Another reason to push hard on making all DTMs
      // be managed DTMs.

      // Since the real root of our tree may be a DocumentFragment, we need to
      // use getParent to find the root, instead of getOwnerDocument. Otherwise
      // DOM2DTM#getHandleOfNode will be very unhappy.
      Node root = node;
      Node p = root.getNodeType() == Node.ATTRIBUTE_NODE ? ((Attr) root).getOwnerElement()
          : root.getParentNode();
      for (; p != null; p = p.getParentNode()) {
        root = p;
      }

      final DOM2DTM dtm = (DOM2DTM) getDTM(new DOMSource(root), false, null, true, true);

      int handle;

      if (node instanceof DOM2DTMdefaultNamespaceDeclarationNode) {
        // Can't return the same node since it's unique to a specific DTM,
        // but can return the equivalent node -- find the corresponding
        // Document Element, then ask it for the xml: namespace decl.
        handle = dtm.getHandleOfNode(((Attr) node).getOwnerElement());
        handle = dtm.getAttributeNode(handle, node.getNamespaceURI(), node.getLocalName());
      } else {
        handle = dtm.getHandleOfNode(node);
      }

      if (DTM.NULL == handle)
        // "Could not resolve the node to a handle!");
        throw new RuntimeException(Messages.get().couldNotResolveNode());

      return handle;
    }
  }

  /**
   * This method returns the SAX2 parser to use with the InputSource obtained from this URI. It may return null if any
   * SAX2-conformant XML parser can be used, or if getInputSource() will also return null. The parser must be free for
   * use (i.e., not currently in use for another parse(). After use of the parser is completed, the
   * releaseXMLReader(XMLReader) must be called.
   * 
   * @param inputSource The value returned from the URIResolver.
   * @return a SAX2 XMLReader to use to resolve the inputSource argument.
   * 
   * @return non-null XMLReader reference ready to parse.
   */
  synchronized public XMLReader getXMLReader(Source inputSource) {

    try {
      XMLReader reader = inputSource instanceof SAXSource ? ((SAXSource) inputSource).getXMLReader() : null;

      // If user did not supply a reader, ask for one from the reader manager
      if (null == reader) {
        if (m_readerManager == null) {
          m_readerManager = XMLReaderManager.getInstance();
        }

        reader = m_readerManager.getXMLReader();
      }

      return reader;

    } catch (final SAXException se) {
      throw new DTMException(se.getMessage(), se);
    }
  }

  /**
   * Indicates that the XMLReader object is no longer in use for the transform.
   * 
   * Note that the getXMLReader method may return an XMLReader that was specified on the SAXSource object by the
   * application code. Such a reader should still be passed to releaseXMLReader, but the reader manager will only re-use
   * XMLReaders that it created.
   * 
   * @param reader The XMLReader to be released.
   */
  synchronized public void releaseXMLReader(XMLReader reader) {
    if (m_readerManager != null) {
      m_readerManager.releaseXMLReader(reader);
    }
  }

  /**
   * Return the DTM object containing a representation of this node.
   * 
   * @param nodeHandle DTM Handle indicating which node to retrieve
   * 
   * @return a reference to the DTM object containing this node.
   */
  @Override
  synchronized public DTM getDTM(int nodeHandle) {
    try {
      // Performance critical function.
      return m_dtms[nodeHandle >>> IDENT_DTM_NODE_BITS];
    } catch (final ArrayIndexOutOfBoundsException e) {
      if (nodeHandle == DTM.NULL)
        return null; // Accept as a special case.
      else
        throw e; // Programming error; want to know about it.
    }
  }

  /**
   * Given a DTM, find the ID number in the DTM tables which addresses the start of the document. If overflow addressing
   * is in use, other DTM IDs may also be assigned to this DTM.
   * 
   * @param dtm The DTM which (hopefully) contains this node.
   * 
   * @return The DTM ID (as the high bits of a NodeHandle, not as our internal index), or -1 if the DTM doesn't belong
   *         to this manager.
   */
  @Override
  synchronized public int getDTMIdentity(DTM dtm) {
    // Shortcut using DTMDefaultBase's extension hooks
    // %REVIEW% Should the lookup be part of the basic DTM API?
    if (dtm instanceof DTMDefaultBase) {
      final DTMDefaultBase dtmdb = (DTMDefaultBase) dtm;
      if (dtmdb.getManager() == this)
        return dtmdb.getDTMIDs().elementAt(0);
      else
        return -1;
    }

    final int n = m_dtms.length;

    for (int i = 0; i < n; i++) {
      final DTM tdtm = m_dtms[i];

      if (tdtm == dtm && m_dtm_offsets[i] == 0)
        return i << IDENT_DTM_NODE_BITS;
    }

    return -1;
  }

  /**
   * Release the DTMManager's reference(s) to a DTM, making it unmanaged. This is typically done as part of returning
   * the DTM to the heap after we're done with it.
   * 
   * @param dtm the DTM to be released.
   * 
   * @param shouldHardDelete If false, this call is a suggestion rather than an order, and we may not actually release
   *        the DTM. This is intended to support intelligent caching of documents... which is not implemented in this
   *        version of the DTM manager.
   * 
   * @return true if the DTM was released, false if shouldHardDelete was set and we decided not to.
   */
  @Override
  synchronized public boolean release(DTM dtm, boolean shouldHardDelete) {
    if (DEBUG) {
      System.out.println("Releasing " + (shouldHardDelete ? "HARD" : "soft") + " dtm=" +
      // Following shouldn't need a nodeHandle, but does...
      // and doesn't seem to report the intended value
          dtm.getDocumentBaseURI());
    }

    if (dtm instanceof SAX2DTM) {
      ((SAX2DTM) dtm).clearCoRoutine();
    }

    // Multiple DTM IDs may be assigned to a single DTM.
    // The Right Answer is to ask which (if it supports
    // extension, the DTM will need a list anyway). The
    // Wrong Answer, applied if the DTM can't help us,
    // is to linearly search them all; this may be very
    // painful.
    //
    // %REVIEW% Should the lookup move up into the basic DTM API?
    if (dtm instanceof DTMDefaultBase) {
      final SuballocatedIntVector ids = ((DTMDefaultBase) dtm).getDTMIDs();
      for (int i = ids.size() - 1; i >= 0; --i) {
        m_dtms[ids.elementAt(i) >>> DTMManager.IDENT_DTM_NODE_BITS] = null;
      }
    } else {
      final int i = getDTMIdentity(dtm);
      if (i >= 0) {
        m_dtms[i >>> DTMManager.IDENT_DTM_NODE_BITS] = null;
      }
    }

    dtm.documentRelease();
    return true;
  }

  /**
   * Method createDocumentFragment
   * 
   * 
   * NEEDSDOC (createDocumentFragment) @return
   */
  @Override
  synchronized public DTM createDocumentFragment() {

    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

      dbf.setNamespaceAware(true);

      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document doc = db.newDocument();
      final Node df = doc.createDocumentFragment();

      return getDTM(new DOMSource(df), true, null, false, false);
    } catch (final Exception e) {
      throw new DTMException(e);
    }
  }

  /**
   * NEEDSDOC Method createDTMIterator
   * 
   * 
   * NEEDSDOC @param whatToShow NEEDSDOC @param filter NEEDSDOC @param entityReferenceExpansion
   * 
   * NEEDSDOC (createDTMIterator) @return
   */
  @Override
  synchronized public DTMIterator createDTMIterator(int whatToShow, DTMFilter filter,
      boolean entityReferenceExpansion) {

    /** @todo: implement this de.lyca.xml.dtm.DTMManager abstract method */
    return null;
  }

  /**
   * NEEDSDOC Method createDTMIterator
   * 
   * 
   * NEEDSDOC @param xpathString NEEDSDOC @param presolver
   * 
   * NEEDSDOC (createDTMIterator) @return
   */
  @Override
  synchronized public DTMIterator createDTMIterator(String xpathString, PrefixResolver presolver) {

    /** @todo: implement this de.lyca.xml.dtm.DTMManager abstract method */
    return null;
  }

  /**
   * NEEDSDOC Method createDTMIterator
   * 
   * 
   * NEEDSDOC @param node
   * 
   * NEEDSDOC (createDTMIterator) @return
   */
  @Override
  synchronized public DTMIterator createDTMIterator(int node) {

    /** @todo: implement this de.lyca.xml.dtm.DTMManager abstract method */
    return null;
  }

  /**
   * NEEDSDOC Method createDTMIterator
   * 
   * 
   * NEEDSDOC @param xpathCompiler NEEDSDOC @param pos
   * 
   * NEEDSDOC (createDTMIterator) @return
   */
  @Override
  synchronized public DTMIterator createDTMIterator(Object xpathCompiler, int pos) {

    /** @todo: implement this de.lyca.xml.dtm.DTMManager abstract method */
    return null;
  }

  /**
   * return the expanded name table.
   * 
   * @param dtm TODO
   * 
   * @return TODO
   */
  public ExpandedNameTable getExpandedNameTable(DTM dtm) {
    return m_expandedNameTable;
  }
}
