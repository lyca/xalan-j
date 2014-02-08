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
/*
 * $Id$
 */

package de.lyca.xalan.xsltc.cmdline;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.lyca.xalan.xsltc.DOMEnhancedForDTM;
import de.lyca.xalan.xsltc.StripFilter;
import de.lyca.xalan.xsltc.TransletException;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.dom.DOMWSFilter;
import de.lyca.xalan.xsltc.dom.XSLTCDTMManager;
import de.lyca.xalan.xsltc.runtime.AbstractTranslet;
import de.lyca.xalan.xsltc.runtime.Constants;
import de.lyca.xalan.xsltc.runtime.Parameter;
import de.lyca.xalan.xsltc.runtime.output.TransletOutputHandlerFactory;
import de.lyca.xml.dtm.DTMWSFilter;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author G. Todd Miller
 * @author Morten Jorgensen
 */
final public class Transform {

  private final String _fileName;
  private final String _className;
  private String _jarFileSrc;
  private boolean _isJarFileSpecified = false;
  private List<Parameter> _params = null;
  private final boolean _uri, _debug;
  private final int _iterations;

  public Transform(String className, String fileName, boolean uri, boolean debug, int iterations) {
    _fileName = fileName;
    _className = className;
    _uri = uri;
    _debug = debug;
    _iterations = iterations;
  }

  public String getFileName() {
    return _fileName;
  }

  public String getClassName() {
    return _className;
  }

  public void setParameters(List<Parameter> params) {
    _params = params;
  }

  private void setJarFileInputSrc(boolean flag, String jarFile) {
    // TODO: at this time we do not do anything with this
    // information, attempts to add the jarfile to the CLASSPATH
    // were successful via System.setProperty, but the effects
    // were not visible to the running JVM. For now we add jarfile
    // to CLASSPATH in the wrapper script that calls this program.
    _isJarFileSpecified = flag;
    // TODO verify jarFile exists...
    _jarFileSrc = jarFile;
  }

  private void doTransform() {
    try {
      final Class<?> clazz = ObjectFactory.findProviderClass(_className, ObjectFactory.findClassLoader(), true);
      final AbstractTranslet translet = (AbstractTranslet) clazz.newInstance();
      translet.postInitialization();

      // Create a SAX parser and get the XMLReader object it uses
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      try {
        factory.setFeature(Constants.NAMESPACE_FEATURE, true);
      } catch (final Exception e) {
        factory.setNamespaceAware(true);
      }
      final SAXParser parser = factory.newSAXParser();
      final XMLReader reader = parser.getXMLReader();

      // Set the DOM's DOM builder as the XMLReader's SAX2 content handler
      final XSLTCDTMManager dtmManager = (XSLTCDTMManager) XSLTCDTMManager.getDTMManagerClass().newInstance();

      DTMWSFilter wsfilter;
      if (translet != null && translet instanceof StripFilter) {
        wsfilter = new DOMWSFilter(translet);
      } else {
        wsfilter = null;
      }

      final DOMEnhancedForDTM dom = (DOMEnhancedForDTM) dtmManager.getDTM(new SAXSource(reader, new InputSource(
              _fileName)), false, wsfilter, true, false, translet.hasIdCall());

      dom.setDocumentURI(_fileName);
      translet.prepassDocument(dom);

      // Pass global parameters
      final int n = _params.size();
      for (int i = 0; i < n; i++) {
        final Parameter param = _params.get(i);
        translet.addParameter(param._name, param._value);
      }

      // Transform the document
      final TransletOutputHandlerFactory tohFactory = TransletOutputHandlerFactory.newInstance();
      tohFactory.setOutputType(TransletOutputHandlerFactory.STREAM);
      tohFactory.setEncoding(translet._encoding);
      tohFactory.setOutputMethod(translet._method);

      if (_iterations == -1) {
        // TODO transformer
        translet.transform(dom, tohFactory.getSerializationHandler(null));
      } else if (_iterations > 0) {
        long mm = System.currentTimeMillis();
        for (int i = 0; i < _iterations; i++) {
          // TODO transformer
          translet.transform(dom, tohFactory.getSerializationHandler(null));
        }
        mm = System.currentTimeMillis() - mm;

        System.err.println("\n<!--");
        System.err.println("  transform  = " + (double) mm / (double) _iterations + " ms");
        System.err.println("  throughput = " + 1000.0 / ((double) mm / (double) _iterations) + " tps");
        System.err.println("-->");
      }
    } catch (final TransletException e) {
      if (_debug) {
        e.printStackTrace();
      }
      System.err.println(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY) + e.getMessage());
    } catch (final RuntimeException e) {
      if (_debug) {
        e.printStackTrace();
      }
      System.err.println(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY) + e.getMessage());
    } catch (final FileNotFoundException e) {
      if (_debug) {
        e.printStackTrace();
      }
      final ErrorMsg err = new ErrorMsg(ErrorMsg.FILE_NOT_FOUND_ERR, _fileName);
      System.err.println(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY) + err.toString());
    } catch (final MalformedURLException e) {
      if (_debug) {
        e.printStackTrace();
      }
      final ErrorMsg err = new ErrorMsg(ErrorMsg.INVALID_URI_ERR, _fileName);
      System.err.println(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY) + err.toString());
    } catch (final ClassNotFoundException e) {
      if (_debug) {
        e.printStackTrace();
      }
      final ErrorMsg err = new ErrorMsg(ErrorMsg.CLASS_NOT_FOUND_ERR, _className);
      System.err.println(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY) + err.toString());
    } catch (final UnknownHostException e) {
      if (_debug) {
        e.printStackTrace();
      }
      final ErrorMsg err = new ErrorMsg(ErrorMsg.INVALID_URI_ERR, _fileName);
      System.err.println(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY) + err.toString());
    } catch (final SAXException e) {
      final Exception ex = e.getException();
      if (_debug) {
        if (ex != null) {
          ex.printStackTrace();
        }
        e.printStackTrace();
      }
      System.err.print(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY));
      if (ex != null) {
        System.err.println(ex.getMessage());
      } else {
        System.err.println(e.getMessage());
      }
    } catch (final Exception e) {
      if (_debug) {
        e.printStackTrace();
      }
      System.err.println(new ErrorMsg(ErrorMsg.RUNTIME_ERROR_KEY) + e.getMessage());
    }
  }

  public static void printUsage() {
    System.err.println(new ErrorMsg(ErrorMsg.TRANSFORM_USAGE_STR));
  }

  public static void main(String[] args) {
    try {
      if (args.length > 0) {
        int i;
        int iterations = -1;
        boolean uri = false, debug = false;
        boolean isJarFileSpecified = false;
        String jarFile = null;

        // Parse options starting with '-'
        for (i = 0; i < args.length && args[i].charAt(0) == '-'; i++) {
          if (args[i].equals("-u")) {
            uri = true;
          } else if (args[i].equals("-x")) {
            debug = true;
          } else if (args[i].equals("-j")) {
            isJarFileSpecified = true;
            jarFile = args[++i];
          } else if (args[i].equals("-n")) {
            try {
              iterations = Integer.parseInt(args[++i]);
            } catch (final NumberFormatException e) {
              // ignore
            }
          } else {
            printUsage();
          }
        }

        // Enough arguments left ?
        if (args.length - i < 2) {
          printUsage();
        }

        // Get document file and class name
        final Transform handler = new Transform(args[i + 1], args[i], uri, debug, iterations);
        handler.setJarFileInputSrc(isJarFileSpecified, jarFile);

        // Parse stylesheet parameters
        final List<Parameter> params = new ArrayList<>();
        for (i += 2; i < args.length; i++) {
          final int equal = args[i].indexOf('=');
          if (equal > 0) {
            final String name = args[i].substring(0, equal);
            final String value = args[i].substring(equal + 1);
            params.add(new Parameter(name, value));
          } else {
            printUsage();
          }
        }

        if (i == args.length) {
          handler.setParameters(params);
          handler.doTransform();
        }
      } else {
        printUsage();
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
