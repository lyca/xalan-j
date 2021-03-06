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

import javax.xml.transform.Result;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import de.lyca.xml.dtm.DTM;
import de.lyca.xml.utils.NodeConsumer;
import de.lyca.xml.utils.XMLString;

/**
 * This class does a pre-order walk of the DTM tree, calling a ContentHandler
 * interface as it goes. As such, it's more like the Visitor design pattern than
 * like the DOM's TreeWalker.
 * 
 * I think normally this class should not be needed, because of
 * DTM#dispatchToEvents.
 */
public class DTMTreeWalker {

  /** Local reference to a ContentHandler */
  private ContentHandler m_contentHandler = null;

  /** DomHelper for this TreeWalker */
  protected DTM m_dtm;

  /**
   * Set the DTM to be traversed.
   * 
   * @param dtm
   *          The Document Table Model to be used.
   */
  public void setDTM(DTM dtm) {
    m_dtm = dtm;
  }

  /**
   * Get the ContentHandler used for the tree walk.
   * 
   * @return the ContentHandler used for the tree walk
   */
  public ContentHandler getcontentHandler() {
    return m_contentHandler;
  }

  /**
   * Set the ContentHandler used for the tree walk.
   * 
   * @param ch
   *          the ContentHandler to be the result of the tree walk.
   */
  public void setcontentHandler(ContentHandler ch) {
    m_contentHandler = ch;
  }

  /**
   * Constructor.
   */
  public DTMTreeWalker() {
  }

  /**
   * Constructor.
   * 
   * @param contentHandler
   *          The implemention of the contentHandler operation (toXMLString,
   *          digest, ...)
   * @param dtm
   *          TODO
   */
  public DTMTreeWalker(ContentHandler contentHandler, DTM dtm) {
    m_contentHandler = contentHandler;
    m_dtm = dtm;
  }

  /**
   * Perform a non-recursive pre-order/post-order traversal, operating as a
   * Visitor. startNode (preorder) and endNode (postorder) are invoked for each
   * node as we traverse over them, with the result that the node is written out
   * to m_contentHandler.
   * 
   * @param pos
   *          Node in the tree at which to start (and end) traversal -- in other
   *          words, the root of the subtree to traverse over.
   * 
   * @throws SAXException
   *           TODO
   */
  public void traverse(int pos) throws SAXException {
    // %REVIEW% Why isn't this just traverse(pos,pos)?

    final int top = pos; // Remember the root of this subtree

    while (DTM.NULL != pos) {
      startNode(pos);
      int nextNode = m_dtm.getFirstChild(pos);
      while (DTM.NULL == nextNode) {
        endNode(pos);

        if (top == pos) {
          break;
        }

        nextNode = m_dtm.getNextSibling(pos);

        if (DTM.NULL == nextNode) {
          pos = m_dtm.getParent(pos);

          if (DTM.NULL == pos || top == pos) {
            // %REVIEW% This condition isn't tested in traverse(pos,top)
            // -- bug?
            if (DTM.NULL != pos) {
              endNode(pos);
            }

            nextNode = DTM.NULL;

            break;
          }
        }
      }

      pos = nextNode;
    }
  }

  /**
   * Perform a non-recursive pre-order/post-order traversal, operating as a
   * Visitor. startNode (preorder) and endNode (postorder) are invoked for each
   * node as we traverse over them, with the result that the node is written out
   * to m_contentHandler.
   * 
   * @param pos
   *          Node in the tree where to start traversal
   * @param top
   *          Node in the tree where to end traversal. If top==DTM.NULL, run
   *          through end of document.
   * 
   * @throws SAXException
   *           TODO
   */
  public void traverse(int pos, int top) throws SAXException {
    // %OPT% Can we simplify the loop conditionals by adding:
    // if(top==DTM.NULL) top=0
    // -- or by simply ignoring this case and relying on the fact that
    // pos will never equal DTM.NULL until we're ready to exit?

    while (DTM.NULL != pos) {
      startNode(pos);
      int nextNode = m_dtm.getFirstChild(pos);
      while (DTM.NULL == nextNode) {
        endNode(pos);

        if (DTM.NULL != top && top == pos) {
          break;
        }

        nextNode = m_dtm.getNextSibling(pos);

        if (DTM.NULL == nextNode) {
          pos = m_dtm.getParent(pos);

          if (DTM.NULL == pos || DTM.NULL != top && top == pos) {
            nextNode = DTM.NULL;

            break;
          }
        }
      }

      pos = nextNode;
    }
  }

  /** Flag indicating whether following text to be processed is raw text */
  boolean nextIsRaw = false;

  /**
   * Optimized dispatch of characters.
   */
  private final void dispatachChars(int node) throws SAXException {
    m_dtm.dispatchCharactersEvents(node, m_contentHandler, false);
  }

  /**
   * Start processing given node
   * 
   * 
   * @param node
   *          Node to process
   * 
   * @throws SAXException
   *           TODO
   */
  protected void startNode(int node) throws SAXException {

    if (m_contentHandler instanceof NodeConsumer) {
      // %TBD%
      // ((NodeConsumer) m_contentHandler).setOriginatingNode(node);
    }

    switch (m_dtm.getNodeType(node)) {
      case DTM.COMMENT_NODE: {
        final XMLString data = m_dtm.getStringValue(node);

        if (m_contentHandler instanceof LexicalHandler) {
          final LexicalHandler lh = (LexicalHandler) m_contentHandler;
          data.dispatchAsComment(lh);
        }
      }
        break;
      case DTM.DOCUMENT_FRAGMENT_NODE:

        // ??;
        break;
      case DTM.DOCUMENT_NODE:
        m_contentHandler.startDocument();
        break;
      case DTM.ELEMENT_NODE:
        final DTM dtm = m_dtm;

        for (int nsn = dtm.getFirstNamespaceNode(node, true); DTM.NULL != nsn; nsn = dtm.getNextNamespaceNode(node, nsn,
            true)) {
          // String prefix = dtm.getPrefix(nsn);
          final String prefix = dtm.getNodeNameX(nsn);

          m_contentHandler.startPrefixMapping(prefix, dtm.getNodeValue(nsn));

        }

        // System.out.println("m_dh.getNamespaceOfNode(node):
        // "+m_dh.getNamespaceOfNode(node));
        // System.out.println("m_dh.getLocalNameOfNode(node):
        // "+m_dh.getLocalNameOfNode(node));
        String ns = dtm.getNamespaceURI(node);
        if (null == ns) {
          ns = "";
        }

        // %OPT% !!
        final AttributesImpl attrs = new AttributesImpl();

        for (int i = dtm.getFirstAttribute(node); i != DTM.NULL; i = dtm.getNextAttribute(i)) {
          attrs.addAttribute(dtm.getNamespaceURI(i), dtm.getLocalName(i), dtm.getNodeName(i), "CDATA",
              dtm.getNodeValue(i));
        }

        m_contentHandler.startElement(ns, m_dtm.getLocalName(node), m_dtm.getNodeName(node), attrs);
        break;
      case DTM.PROCESSING_INSTRUCTION_NODE: {
        final String name = m_dtm.getNodeName(node);

        // String data = pi.getData();
        if (name.equals("xslt-next-is-raw")) {
          nextIsRaw = true;
        } else {
          m_contentHandler.processingInstruction(name, m_dtm.getNodeValue(node));
        }
      }
        break;
      case DTM.CDATA_SECTION_NODE: {
        final boolean isLexH = m_contentHandler instanceof LexicalHandler;
        final LexicalHandler lh = isLexH ? (LexicalHandler) m_contentHandler : null;

        if (isLexH) {
          lh.startCDATA();
        }

        dispatachChars(node);

        {
          if (isLexH) {
            lh.endCDATA();
          }
        }
      }
        break;
      case DTM.TEXT_NODE: {
        if (nextIsRaw) {
          nextIsRaw = false;

          m_contentHandler.processingInstruction(Result.PI_DISABLE_OUTPUT_ESCAPING, "");
          dispatachChars(node);
          m_contentHandler.processingInstruction(Result.PI_ENABLE_OUTPUT_ESCAPING, "");
        } else {
          dispatachChars(node);
        }
      }
        break;
      case DTM.ENTITY_REFERENCE_NODE: {
        if (m_contentHandler instanceof LexicalHandler) {
          ((LexicalHandler) m_contentHandler).startEntity(m_dtm.getNodeName(node));
        } else {

          // warning("Can not output entity to a pure SAX ContentHandler");
        }
      }
        break;
      default:
    }
  }

  /**
   * End processing of given node
   * 
   * 
   * @param node
   *          Node we just finished processing
   * 
   * @throws SAXException
   *           TODO
   */
  protected void endNode(int node) throws SAXException {

    switch (m_dtm.getNodeType(node)) {
      case DTM.DOCUMENT_NODE:
        m_contentHandler.endDocument();
        break;
      case DTM.ELEMENT_NODE:
        String ns = m_dtm.getNamespaceURI(node);
        if (null == ns) {
          ns = "";
        }
        m_contentHandler.endElement(ns, m_dtm.getLocalName(node), m_dtm.getNodeName(node));

        for (int nsn = m_dtm.getFirstNamespaceNode(node, true); DTM.NULL != nsn; nsn = m_dtm.getNextNamespaceNode(node,
            nsn, true)) {
          // String prefix = m_dtm.getPrefix(nsn);
          final String prefix = m_dtm.getNodeNameX(nsn);

          m_contentHandler.endPrefixMapping(prefix);
        }
        break;
      case DTM.CDATA_SECTION_NODE:
        break;
      case DTM.ENTITY_REFERENCE_NODE: {
        if (m_contentHandler instanceof LexicalHandler) {
          final LexicalHandler lh = (LexicalHandler) m_contentHandler;

          lh.endEntity(m_dtm.getNodeName(node));
        }
      }
        break;
      default:
    }
  }
} // TreeWalker
