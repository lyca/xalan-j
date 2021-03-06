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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import de.lyca.xalan.ObjectFactory;
import de.lyca.xml.res.Messages;
import de.lyca.xml.utils.WrappedRuntimeException;

/**
 * <p>
 * IncrementalSAXSource_Xerces takes advantage of the fact that Xerces1 incremental mode is already a coroutine of
 * sorts, and just wraps our IncrementalSAXSource API around it.
 * </p>
 * 
 * <p>
 * Usage example: See main().
 * </p>
 * 
 * <p>
 * Status: Passes simple main() unit-test. NEEDS JAVADOC.
 * </p>
 */
public class IncrementalSAXSource_Xerces implements IncrementalSAXSource {
  //
  // Reflection. To allow this to compile with both Xerces1 and Xerces2, which
  // require very different methods and objects, we need to avoid static
  // references to those APIs. So until Xerces2 is pervasive and we're willing
  // to make it a prerequisite, we will rely upon relection.
  //
  Method fParseSomeSetup = null; // Xerces1 method
  Method fParseSome = null; // Xerces1 method
  Object fPullParserConfig = null; // Xerces2 pull control object
  Method fConfigSetInput = null; // Xerces2 method
  Method fConfigParse = null; // Xerces2 method
  Method fSetInputSource = null; // Xerces2 pull control method
  Constructor<?> fConfigInputSourceCtor = null; // Xerces2 initialization method
  Method fConfigSetByteStream = null; // Xerces2 initialization method
  Method fConfigSetCharStream = null; // Xerces2 initialization method
  Method fConfigSetEncoding = null; // Xerces2 initialization method
  Method fReset = null; // Both Xerces1 and Xerces2, but diff. signatures

  //
  // Data
  //
  SAXParser fIncrementalParser;
  private boolean fParseInProgress = false;

  //
  // Constructors
  //

  /**
   * Create a IncrementalSAXSource_Xerces, and create a SAXParser to go with it. Xerces2 incremental parsing is only
   * supported if this constructor is used, due to limitations in the Xerces2 API (as of Beta 3). If you don't like that
   * restriction, tell the Xerces folks that there should be a simpler way to request incremental SAX parsing.
   * 
   * @throws NoSuchMethodException TODO
   */
  public IncrementalSAXSource_Xerces() throws NoSuchMethodException {
    try {
      // Xerces-2 incremental parsing support (as of Beta 3)
      // ContentHandlers still get set on fIncrementalParser (to get
      // conversion from XNI events to SAX events), but
      // _control_ for incremental parsing must be exercised via the config.
      //
      // At this time there's no way to read the existing config, only
      // to assert a new one... and only when creating a brand-new parser.
      //
      // Reflection is used to allow us to continue to compile against
      // Xerces1. If/when we can abandon the older versions of the parser,
      // this will simplify significantly.

      // If we can't get the magic constructor, no need to look further.
      final Class<?> xniConfigClass = ObjectFactory.findProviderClass(
          "org.apache.xerces.xni.parser.XMLParserConfiguration", ObjectFactory.findClassLoader(), true);
      final Class<?>[] args1 = { xniConfigClass };
      final Constructor<SAXParser> ctor = SAXParser.class.getConstructor(args1);

      // Build the parser configuration object. StandardParserConfiguration
      // happens to implement XMLPullParserConfiguration, which is the API
      // we're going to want to use.
      final Class<?> xniStdConfigClass = ObjectFactory.findProviderClass(
          "org.apache.xerces.parsers.StandardParserConfiguration", ObjectFactory.findClassLoader(), true);
      fPullParserConfig = xniStdConfigClass.newInstance();
      final Object[] args2 = { fPullParserConfig };
      fIncrementalParser = ctor.newInstance(args2);

      // Preload all the needed the configuration methods... I want to know
      // they're
      // all here before we commit to trying to use them, just in case the
      // API changes again.
      final Class<?> fXniInputSourceClass = ObjectFactory
          .findProviderClass("org.apache.xerces.xni.parser.XMLInputSource", ObjectFactory.findClassLoader(), true);
      final Class<?>[] args3 = { fXniInputSourceClass };
      fConfigSetInput = xniStdConfigClass.getMethod("setInputSource", args3);

      final Class<?>[] args4 = { String.class, String.class, String.class };
      fConfigInputSourceCtor = fXniInputSourceClass.getConstructor(args4);
      final Class<?>[] args5 = { java.io.InputStream.class };
      fConfigSetByteStream = fXniInputSourceClass.getMethod("setByteStream", args5);
      final Class<?>[] args6 = { java.io.Reader.class };
      fConfigSetCharStream = fXniInputSourceClass.getMethod("setCharacterStream", args6);
      final Class<?>[] args7 = { String.class };
      fConfigSetEncoding = fXniInputSourceClass.getMethod("setEncoding", args7);

      final Class<?>[] argsb = { Boolean.TYPE };
      fConfigParse = xniStdConfigClass.getMethod("parse", argsb);
      final Class<?>[] noargs = new Class[0];
      fReset = fIncrementalParser.getClass().getMethod("reset", noargs);
    } catch (final Exception e) {
      // Fallback if this fails (implemented in createIncrementalSAXSource) is
      // to attempt Xerces-1 incremental setup. Can't do tail-call in
      // constructor, so create new, copy Xerces-1 initialization,
      // then throw it away... Ugh.
      final IncrementalSAXSource_Xerces dummy = new IncrementalSAXSource_Xerces(new SAXParser());
      fParseSomeSetup = dummy.fParseSomeSetup;
      fParseSome = dummy.fParseSome;
      fIncrementalParser = dummy.fIncrementalParser;
    }
  }

  /**
   * Create a IncrementalSAXSource_Xerces wrapped around an existing SAXParser. Currently this works only for recent
   * releases of Xerces-1. Xerces-2 incremental is currently possible only if we are allowed to create the parser
   * instance, due to limitations in the API exposed by Xerces-2 Beta 3; see the no-args constructor for that code.
   * 
   * @param parser TODO
   * @throws NoSuchMethodException if the SAXParser class doesn't support the Xerces incremental parse operations. In
   *         that case, caller should fall back upon the IncrementalSAXSource_Filter approach.
   */
  public IncrementalSAXSource_Xerces(SAXParser parser) throws NoSuchMethodException {
    // Reflection is used to allow us to compile against
    // Xerces2. If/when we can abandon the older versions of the parser,
    // this constructor will simply have to fail until/unless the
    // Xerces2 incremental support is made available on previously
    // constructed SAXParser instances.
    fIncrementalParser = parser;
    final Class<?> me = parser.getClass();
    Class<?>[] parms = { InputSource.class };
    fParseSomeSetup = me.getMethod("parseSomeSetup", parms);
    parms = new Class<?>[0];
    fParseSome = me.getMethod("parseSome", parms);
    // Fallback if this fails (implemented in createIncrementalSAXSource) is
    // to use IncrementalSAXSource_Filter rather than Xerces-specific code.
  }

  //
  // Factories
  //
  static public IncrementalSAXSource createIncrementalSAXSource() {
    try {
      return new IncrementalSAXSource_Xerces();
    } catch (final NoSuchMethodException e) {
      // Xerces version mismatch; neither Xerces1 nor Xerces2 succeeded.
      // Fall back on filtering solution.
      final IncrementalSAXSource_Filter iss = new IncrementalSAXSource_Filter();
      iss.setXMLReader(new SAXParser());
      return iss;
    }
  }

  static public IncrementalSAXSource createIncrementalSAXSource(SAXParser parser) {
    try {
      return new IncrementalSAXSource_Xerces(parser);
    } catch (final NoSuchMethodException e) {
      // Xerces version mismatch; neither Xerces1 nor Xerces2 succeeded.
      // Fall back on filtering solution.
      final IncrementalSAXSource_Filter iss = new IncrementalSAXSource_Filter();
      iss.setXMLReader(parser);
      return iss;
    }
  }

  //
  // Public methods
  //

  // Register handler directly with the incremental parser
  @Override
  public void setContentHandler(ContentHandler handler) {
    // Typecast required in Xerces2; SAXParser doesn't inheret XMLReader
    // %OPT% Cast at asignment?
    ((XMLReader) fIncrementalParser).setContentHandler(handler);
  }

  // Register handler directly with the incremental parser
  @Override
  public void setLexicalHandler(LexicalHandler handler) {
    // Not supported by all SAX2 parsers but should work in Xerces:
    try {
      // Typecast required in Xerces2; SAXParser doesn't inheret XMLReader
      // %OPT% Cast at asignment?
      ((XMLReader) fIncrementalParser).setProperty("http://xml.org/sax/properties/lexical-handler", handler);
    } catch (final SAXNotRecognizedException e) {
      // Nothing we can do about it
    } catch (final SAXNotSupportedException e) {
      // Nothing we can do about it
    }
  }

  // Register handler directly with the incremental parser
  @Override
  public void setDTDHandler(DTDHandler handler) {
    // Typecast required in Xerces2; SAXParser doesn't inheret XMLReader
    // %OPT% Cast at asignment?
    ((XMLReader) fIncrementalParser).setDTDHandler(handler);
  }

  // ================================================================
  /**
   * startParse() is a simple API which tells the IncrementalSAXSource to begin reading a document.
   * 
   * @throws SAXException is parse thread is already in progress or parsing can not be started.
   */
  @Override
  public void startParse(InputSource source) throws SAXException {
    if (fIncrementalParser == null)
      // "startParse needs a non-null SAXParser.");
      throw new SAXException(Messages.get().startparseNeedsSaxparser());
    if (fParseInProgress)
      // "startParse may not be called while parsing.");
      throw new SAXException(Messages.get().startparseWhileParsing());

    boolean ok = false;

    try {
      ok = parseSomeSetup(source);
    } catch (final Exception ex) {
      throw new SAXException(ex);
    }

    if (!ok)
      // "could not initialize parser with");
      throw new SAXException(Messages.get().couldNotInitParser());
  }

  /**
   * deliverMoreNodes() is a simple API which tells the coroutine parser that we need more nodes. This is intended to be
   * called from one of our partner routines, and serves to encapsulate the details of how incremental parsing has been
   * achieved.
   * 
   * @param parsemore If true, tells the incremental parser to generate another chunk of output. If false, tells the
   *        parser that we're satisfied and it can terminate parsing of this document.
   * @return Boolean.TRUE if the CoroutineParser believes more data may be available for further parsing. Boolean.FALSE
   *         if parsing ran to completion. Exception if the parser objected for some reason.
   */
  @Override
  public Object deliverMoreNodes(boolean parsemore) {
    if (!parsemore) {
      fParseInProgress = false;
      return Boolean.FALSE;
    }

    Object arg;
    try {
      final boolean keepgoing = parseSome();
      arg = keepgoing ? Boolean.TRUE : Boolean.FALSE;
    } catch (final SAXException ex) {
      arg = ex;
    } catch (final IOException ex) {
      arg = ex;
    } catch (final Exception ex) {
      arg = new SAXException(ex);
    }
    return arg;
  }

  // Private methods -- conveniences to hide the reflection details
  private boolean parseSomeSetup(InputSource source) throws SAXException, IOException, IllegalAccessException,
      java.lang.reflect.InvocationTargetException, java.lang.InstantiationException {
    if (fConfigSetInput != null) {
      // Obtain input from SAX inputSource object, construct XNI version of
      // that object. Logic adapted from Xerces2.
      final Object[] parms1 = { source.getPublicId(), source.getSystemId(), null };
      final Object xmlsource = fConfigInputSourceCtor.newInstance(parms1);
      final Object[] parmsa = { source.getByteStream() };
      fConfigSetByteStream.invoke(xmlsource, parmsa);
      parmsa[0] = source.getCharacterStream();
      fConfigSetCharStream.invoke(xmlsource, parmsa);
      parmsa[0] = source.getEncoding();
      fConfigSetEncoding.invoke(xmlsource, parmsa);

      // Bugzilla5272 patch suggested by Sandy Gao.
      // Has to be reflection to run with Xerces2
      // after compilation against Xerces1. or vice
      // versa, due to return type mismatches.
      final Object[] noparms = new Object[0];
      fReset.invoke(fIncrementalParser, noparms);

      parmsa[0] = xmlsource;
      fConfigSetInput.invoke(fPullParserConfig, parmsa);

      // %REVIEW% Do first pull. Should we instead just return true?
      return parseSome();
    } else {
      final Object[] parm = { source };
      final Object ret = fParseSomeSetup.invoke(fIncrementalParser, parm);
      return ((Boolean) ret).booleanValue();
    }
  }

  // Would null work???
  private static final Object[] noparms = new Object[0];
  private static final Object[] parmsfalse = { Boolean.FALSE };

  private boolean parseSome()
      throws SAXException, IOException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
    // Take next parsing step, return false iff parsing complete:
    if (fConfigSetInput != null) {
      final Object ret = fConfigParse.invoke(fPullParserConfig, parmsfalse);
      return ((Boolean) ret).booleanValue();
    } else {
      final Object ret = fParseSome.invoke(fIncrementalParser, noparms);
      return ((Boolean) ret).booleanValue();
    }
  }

  // ================================================================
  /**
   * Simple unit test. Attempt coroutine parsing of document indicated by first argument (as a URI), report progress.
   * 
   * @param args TODO
   */
  public static void main(String args[]) {
    System.out.println("Starting...");

    final CoroutineManager co = new CoroutineManager();
    final int appCoroutineID = co.co_joinCoroutineSet(-1);
    if (appCoroutineID == -1) {
      System.out.println("ERROR: Couldn't allocate coroutine number.\n");
      return;
    }
    final IncrementalSAXSource parser = createIncrementalSAXSource();

    // Use a serializer as our sample output
    org.apache.xml.serialize.XMLSerializer trace;
    trace = new org.apache.xml.serialize.XMLSerializer(System.out, null);
    parser.setContentHandler(trace);
    parser.setLexicalHandler(trace);

    // Tell coroutine to begin parsing, run while parsing is in progress

    for (int arg = 0; arg < args.length; ++arg) {
      try {
        final InputSource source = new InputSource(args[arg]);
        Object result = null;
        boolean more = true;
        parser.startParse(source);
        for (result = parser.deliverMoreNodes(more); result == Boolean.TRUE; result = parser.deliverMoreNodes(more)) {
          System.out.println("\nSome parsing successful, trying more.\n");

          // Special test: Terminate parsing early.
          if (arg + 1 < args.length && "!".equals(args[arg + 1])) {
            ++arg;
            more = false;
          }

        }

        if (result instanceof Boolean && (Boolean) result == Boolean.FALSE) {
          System.out.println("\nParser ended (EOF or on request).\n");
        } else if (result == null) {
          System.out.println("\nUNEXPECTED: Parser says shut down prematurely.\n");
        } else if (result instanceof Exception)
          throw new WrappedRuntimeException((Exception) result);
        // System.out.println("\nParser threw exception:");
        // ((Exception)result).printStackTrace();

      }

      catch (final SAXException e) {
        e.printStackTrace();
      }
    }

  }

}
