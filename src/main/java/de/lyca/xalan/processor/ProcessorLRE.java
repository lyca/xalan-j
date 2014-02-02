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
package de.lyca.xalan.processor;

import java.util.List;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;

import de.lyca.xalan.res.XSLMessages;
import de.lyca.xalan.res.XSLTErrorResources;
import de.lyca.xalan.templates.ElemExtensionCall;
import de.lyca.xalan.templates.ElemLiteralResult;
import de.lyca.xalan.templates.ElemTemplate;
import de.lyca.xalan.templates.ElemTemplateElement;
import de.lyca.xalan.templates.Stylesheet;
import de.lyca.xalan.templates.StylesheetRoot;
import de.lyca.xalan.templates.XMLNSDecl;
import de.lyca.xml.utils.SAXSourceLocator;
import de.lyca.xpath.XPath;

/**
 * Processes an XSLT literal-result-element, or something that looks like one.
 * The actual {@link de.lyca.xalan.templates.ElemTemplateElement} produced may
 * be a {@link de.lyca.xalan.templates.ElemLiteralResult}, a
 * {@link de.lyca.xalan.templates.StylesheetRoot}, or a
 * {@link de.lyca.xalan.templates.ElemExtensionCall}.
 * 
 * @see <a
 *      href="http://www.w3.org/TR/xslt#literal-result-element">literal-result-element
 *      in XSLT Specification</a>
 * @see de.lyca.xalan.templates.ElemLiteralResult
 * @xsl.usage internal
 */
public class ProcessorLRE extends ProcessorTemplateElem {
  static final long serialVersionUID = -1490218021772101404L;

  /**
   * Receive notification of the start of an element.
   * 
   * @param handler
   *          non-null reference to current StylesheetHandler that is
   *          constructing the Templates.
   * @param uri
   *          The Namespace URI, or an empty string.
   * @param localName
   *          The local name (without prefix), or empty string if not namespace
   *          processing.
   * @param rawName
   *          The qualified name (with prefix).
   * @param attributes
   *          The specified or defaulted attributes.
   */
  @Override
  public void startElement(StylesheetHandler handler, String uri, String localName, String rawName,
          Attributes attributes) throws org.xml.sax.SAXException {

    try {
      ElemTemplateElement p = handler.getElemTemplateElement();
      boolean excludeXSLDecl = false;
      boolean isLREAsStyleSheet = false;

      if (null == p) {

        // Literal Result Template as stylesheet.
        final XSLTElementProcessor lreProcessor = handler.popProcessor();
        final XSLTElementProcessor stylesheetProcessor = handler.getProcessorFor(
                de.lyca.xml.utils.Constants.S_XSLNAMESPACEURL, "stylesheet", "xsl:stylesheet");

        handler.pushProcessor(lreProcessor);

        Stylesheet stylesheet;
        try {
          stylesheet = getStylesheetRoot(handler);
        } catch (final TransformerConfigurationException tfe) {
          throw new TransformerException(tfe);
        }

        // stylesheet.setDOMBackPointer(handler.getOriginatingNode());
        // ***** Note that we're assigning an empty locator. Is this necessary?
        final SAXSourceLocator slocator = new SAXSourceLocator();
        final Locator locator = handler.getLocator();
        if (null != locator) {
          slocator.setLineNumber(locator.getLineNumber());
          slocator.setColumnNumber(locator.getColumnNumber());
          slocator.setPublicId(locator.getPublicId());
          slocator.setSystemId(locator.getSystemId());
        }
        stylesheet.setLocaterInfo(slocator);
        stylesheet.setPrefixes(handler.getNamespaceSupport());
        handler.pushStylesheet(stylesheet);

        isLREAsStyleSheet = true;

        final AttributesImpl stylesheetAttrs = new AttributesImpl();
        final AttributesImpl lreAttrs = new AttributesImpl();
        final int n = attributes.getLength();

        for (int i = 0; i < n; i++) {
          final String attrLocalName = attributes.getLocalName(i);
          final String attrUri = attributes.getURI(i);
          final String value = attributes.getValue(i);

          if (null != attrUri && attrUri.equals(de.lyca.xml.utils.Constants.S_XSLNAMESPACEURL)) {
            stylesheetAttrs.addAttribute(null, attrLocalName, attrLocalName, attributes.getType(i),
                    attributes.getValue(i));
          } else if ((attrLocalName.startsWith("xmlns:") || attrLocalName.equals("xmlns"))
                  && value.equals(de.lyca.xml.utils.Constants.S_XSLNAMESPACEURL)) {

            // ignore
          } else {
            lreAttrs.addAttribute(attrUri, attrLocalName, attributes.getQName(i), attributes.getType(i),
                    attributes.getValue(i));
          }
        }

        attributes = lreAttrs;

        // Set properties from the attributes, but don't throw
        // an error if there is an attribute defined that is not
        // allowed on a stylesheet.
        try {
          stylesheetProcessor.setPropertiesFromAttributes(handler, "stylesheet", stylesheetAttrs, stylesheet);
        } catch (final Exception e) {
          // This is pretty ugly, but it will have to do for now.
          // This is just trying to append some text specifying that
          // this error came from a missing or invalid XSLT namespace
          // declaration.
          // If someone comes up with a better solution, please feel
          // free to contribute it. -mm

          if (stylesheet.getDeclaredPrefixes() == null || !declaredXSLNS(stylesheet))
            throw new org.xml.sax.SAXException(XSLMessages.createWarning(XSLTErrorResources.WG_OLD_XSLT_NS, null));
          else
            throw new org.xml.sax.SAXException(e);
        }
        handler.pushElemTemplateElement(stylesheet);

        final ElemTemplate template = new ElemTemplate();
        if (slocator != null) {
          template.setLocaterInfo(slocator);
        }

        appendAndPush(handler, template);

        final XPath rootMatch = new XPath("/", stylesheet, stylesheet, XPath.MATCH, handler.getStylesheetProcessor()
                .getErrorListener());

        template.setMatch(rootMatch);

        // template.setDOMBackPointer(handler.getOriginatingNode());
        stylesheet.setTemplate(template);

        p = handler.getElemTemplateElement();
        excludeXSLDecl = true;
      }

      final XSLTElementDef def = getElemDef();
      final Class<?> classObject = def.getClassObject();
      boolean isExtension = false;
      boolean isComponentDecl = false;
      boolean isUnknownTopLevel = false;

      while (null != p) {

        // System.out.println("Checking: "+p);
        if (p instanceof ElemLiteralResult) {
          final ElemLiteralResult parentElem = (ElemLiteralResult) p;

          isExtension = parentElem.containsExtensionElementURI(uri);
        } else if (p instanceof Stylesheet) {
          final Stylesheet parentElem = (Stylesheet) p;

          isExtension = parentElem.containsExtensionElementURI(uri);

          if (false == isExtension
                  && null != uri
                  && (uri.equals(de.lyca.xml.utils.Constants.S_BUILTIN_EXTENSIONS_URL) || uri
                          .equals(de.lyca.xml.utils.Constants.S_BUILTIN_OLD_EXTENSIONS_URL))) {
            isComponentDecl = true;
          } else {
            isUnknownTopLevel = true;
          }
        }

        if (isExtension) {
          break;
        }

        p = p.getParentElem();
      }

      ElemTemplateElement elem = null;

      try {
        if (isExtension) {

          // System.out.println("Creating extension(1): "+uri);
          elem = new ElemExtensionCall();
        } else if (isComponentDecl) {
          elem = (ElemTemplateElement) classObject.newInstance();
        } else if (isUnknownTopLevel) {

          // TBD: Investigate, not sure about this. -sb
          elem = (ElemTemplateElement) classObject.newInstance();
        } else {
          elem = (ElemTemplateElement) classObject.newInstance();
        }

        elem.setDOMBackPointer(handler.getOriginatingNode());
        elem.setLocaterInfo(handler.getLocator());
        elem.setPrefixes(handler.getNamespaceSupport(), excludeXSLDecl);

        if (elem instanceof ElemLiteralResult) {
          ((ElemLiteralResult) elem).setNamespace(uri);
          ((ElemLiteralResult) elem).setLocalName(localName);
          ((ElemLiteralResult) elem).setRawName(rawName);
          ((ElemLiteralResult) elem).setIsLiteralResultAsStylesheet(isLREAsStyleSheet);
        }
      } catch (final InstantiationException ie) {
        handler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, ie);// "Failed creating ElemLiteralResult instance!",
                                                                                   // ie);
      } catch (final IllegalAccessException iae) {
        handler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, iae);// "Failed creating ElemLiteralResult instance!",
                                                                                    // iae);
      }

      setPropertiesFromAttributes(handler, rawName, attributes, elem);

      // bit of a hack here...
      if (!isExtension && elem instanceof ElemLiteralResult) {
        isExtension = ((ElemLiteralResult) elem).containsExtensionElementURI(uri);

        if (isExtension) {

          // System.out.println("Creating extension(2): "+uri);
          elem = new ElemExtensionCall();

          elem.setLocaterInfo(handler.getLocator());
          elem.setPrefixes(handler.getNamespaceSupport());
          ((ElemLiteralResult) elem).setNamespace(uri);
          ((ElemLiteralResult) elem).setLocalName(localName);
          ((ElemLiteralResult) elem).setRawName(rawName);
          setPropertiesFromAttributes(handler, rawName, attributes, elem);
        }
      }

      appendAndPush(handler, elem);
    } catch (final TransformerException te) {
      throw new org.xml.sax.SAXException(te);
    }
  }

  /**
   * This method could be over-ridden by a class that extends this class.
   * 
   * @param handler
   *          non-null reference to current StylesheetHandler that is
   *          constructing the Templates.
   * @return an object that represents the stylesheet element.
   */
  protected Stylesheet getStylesheetRoot(StylesheetHandler handler) throws TransformerConfigurationException {
    StylesheetRoot stylesheet;
    stylesheet = new StylesheetRoot(handler.getSchema(), handler.getStylesheetProcessor().getErrorListener());
    if (handler.getStylesheetProcessor().isSecureProcessing()) {
      stylesheet.setSecureProcessing(true);
    }

    return stylesheet;
  }

  /**
   * Receive notification of the end of an element.
   * 
   * @param handler
   *          non-null reference to current StylesheetHandler that is
   *          constructing the Templates.
   * @param uri
   *          The Namespace URI, or an empty string.
   * @param localName
   *          The local name (without prefix), or empty string if not namespace
   *          processing.
   * @param rawName
   *          The qualified name (with prefix).
   */
  @Override
  public void endElement(StylesheetHandler handler, String uri, String localName, String rawName)
          throws org.xml.sax.SAXException {

    final ElemTemplateElement elem = handler.getElemTemplateElement();

    if (elem instanceof ElemLiteralResult) {
      if (((ElemLiteralResult) elem).getIsLiteralResultAsStylesheet()) {
        handler.popStylesheet();
      }
    }

    super.endElement(handler, uri, localName, rawName);
  }

  private boolean declaredXSLNS(Stylesheet stylesheet) {
    final List<XMLNSDecl> declaredPrefixes = stylesheet.getDeclaredPrefixes();
    final int n = declaredPrefixes.size();

    for (int i = 0; i < n; i++) {
      final XMLNSDecl decl = declaredPrefixes.get(i);
      if (decl.getURI().equals(de.lyca.xml.utils.Constants.S_XSLNAMESPACEURL))
        return true;
    }
    return false;
  }
}
