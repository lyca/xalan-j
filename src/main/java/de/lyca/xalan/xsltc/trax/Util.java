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
package de.lyca.xalan.xsltc.trax;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.lyca.xalan.xsltc.compiler.XSLTC;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;

/**
 * @author Santiago Pericas-Geertsen
 */
public final class Util {

  public static String baseName(String name) {
    return de.lyca.xalan.xsltc.compiler.util.Util.baseName(name);
  }

  public static String noExtName(String name) {
    return de.lyca.xalan.xsltc.compiler.util.Util.noExtName(name);
  }

  public static String toJavaName(String name) {
    return de.lyca.xalan.xsltc.compiler.util.Util.toJavaName(name);
  }

  /**
   * Creates a SAX2 InputSource object from a TrAX Source object
   */
  public static InputSource getInputSource(XSLTC xsltc, Source source) throws TransformerConfigurationException {
    InputSource input = null;

    final String systemId = source.getSystemId();

    try {
      // Try to get InputSource from SAXSource input
      if (source instanceof SAXSource) {
        final SAXSource sax = (SAXSource) source;
        input = sax.getInputSource();
        // Pass the SAX parser to the compiler
        try {
          XMLReader reader = sax.getXMLReader();

          /*
           * Fix for bug 24695 According to JAXP 1.2 specification if a
           * SAXSource is created using a SAX InputSource the Transformer or
           * TransformerFactory creates a reader via the XMLReaderFactory if
           * setXMLReader is not used
           */

          if (reader == null) {
            try {
              reader = XMLReaderFactory.createXMLReader();
            } catch (final Exception e) {
              try {

                // Incase there is an exception thrown
                // resort to JAXP
                final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                parserFactory.setNamespaceAware(true);

                if (xsltc.isSecureProcessing()) {
                  try {
                    parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                  } catch (final org.xml.sax.SAXException se) {
                  }
                }

                reader = parserFactory.newSAXParser().getXMLReader();

              } catch (final ParserConfigurationException pce) {
                throw new TransformerConfigurationException("ParserConfigurationException", pce);
              }
            }
          }
          reader.setFeature("http://xml.org/sax/features/namespaces", true);
          reader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

          xsltc.setXMLReader(reader);
        } catch (final SAXNotRecognizedException snre) {
          throw new TransformerConfigurationException("SAXNotRecognizedException ", snre);
        } catch (final SAXNotSupportedException snse) {
          throw new TransformerConfigurationException("SAXNotSupportedException ", snse);
        } catch (final SAXException se) {
          throw new TransformerConfigurationException("SAXException ", se);
        }

      }
      // handle DOMSource
      else if (source instanceof DOMSource) {
        final DOMSource domsrc = (DOMSource) source;
        final Document dom = (Document) domsrc.getNode();
        final DOM2SAX dom2sax = new DOM2SAX(dom);
        xsltc.setXMLReader(dom2sax);

        // Try to get SAX InputSource from DOM Source.
        input = SAXSource.sourceToInputSource(source);
        if (input == null) {
          input = new InputSource(domsrc.getSystemId());
        }
      }
      // Try to get InputStream or Reader from StreamSource
      else if (source instanceof StreamSource) {
        final StreamSource stream = (StreamSource) source;
        final InputStream istream = stream.getInputStream();
        final Reader reader = stream.getReader();
        xsltc.setXMLReader(null); // Clear old XML reader

        // Create InputSource from Reader or InputStream in Source
        if (istream != null) {
          input = new InputSource(istream);
        } else if (reader != null) {
          input = new InputSource(reader);
        } else {
          input = new InputSource(systemId);
        }
      } else {
        final ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR);
        throw new TransformerConfigurationException(err.toString());
      }
      input.setSystemId(systemId);
    } catch (final NullPointerException e) {
      final ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_NO_SOURCE_ERR, "TransformerFactory.newTemplates()");
      throw new TransformerConfigurationException(err.toString());
    } catch (final SecurityException e) {
      final ErrorMsg err = new ErrorMsg(ErrorMsg.FILE_ACCESS_ERR, systemId);
      throw new TransformerConfigurationException(err.toString());
    }
    return input;
  }

}
