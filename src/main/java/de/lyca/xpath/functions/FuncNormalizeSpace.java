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
package de.lyca.xpath.functions;

import javax.xml.transform.TransformerException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.lyca.xml.dtm.DTM;
import de.lyca.xml.utils.XMLString;
import de.lyca.xpath.XPathContext;
import de.lyca.xpath.objects.XObject;
import de.lyca.xpath.objects.XString;

/**
 * Execute the normalize-space() function.
 */
public class FuncNormalizeSpace extends FunctionDef1Arg {
  static final long serialVersionUID = -3377956872032190880L;

  /**
   * Execute the function. The function must return a valid object.
   * 
   * @param xctxt The current execution context.
   * @return A valid XObject.
   * @throws TransformerException TODO
   */
  @Override
  public XObject execute(XPathContext xctxt) throws TransformerException {
    final XMLString s1 = getArg0AsString(xctxt);

    return (XString) s1.fixWhiteSpace(true, true, false);
  }

  /**
   * Execute an expression in the XPath runtime context, and return the result of the expression.
   * 
   * @param xctxt The XPath runtime context.
   * @throws TransformerException if a runtime exception occurs.
   * @throws SAXException TODO
   */
  @Override
  public void executeCharsToContentHandler(XPathContext xctxt, ContentHandler handler)
      throws TransformerException, SAXException {
    if (Arg0IsNodesetExpr()) {
      final int node = getArg0AsNode(xctxt);
      if (DTM.NULL != node) {
        final DTM dtm = xctxt.getDTM(node);
        dtm.dispatchCharactersEvents(node, handler, true);
      }
    } else {
      final XObject obj = execute(xctxt);
      obj.dispatchCharactersEvents(handler);
    }
  }

}
