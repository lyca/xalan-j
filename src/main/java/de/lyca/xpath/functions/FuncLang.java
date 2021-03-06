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

import de.lyca.xml.dtm.DTM;
import de.lyca.xpath.XPathContext;
import de.lyca.xpath.objects.XBoolean;
import de.lyca.xpath.objects.XObject;

/**
 * Execute the Lang() function.
 */
public class FuncLang extends FunctionOneArg {
  static final long serialVersionUID = -7868705139354872185L;

  /**
   * Execute the function. The function must return a valid object.
   * 
   * @param xctxt
   *          The current execution context.
   * @return A valid XObject.
   * 
   * @throws TransformerException TODO
   */
  @Override
  public XObject execute(XPathContext xctxt) throws TransformerException {

    final String lang = m_arg0.execute(xctxt).str();
    int parent = xctxt.getCurrentNode();
    boolean isLang = false;
    final DTM dtm = xctxt.getDTM(parent);

    while (DTM.NULL != parent) {
      if (DTM.ELEMENT_NODE == dtm.getNodeType(parent)) {
        final int langAttr = dtm.getAttributeNode(parent, "http://www.w3.org/XML/1998/namespace", "lang");

        if (DTM.NULL != langAttr) {
          final String langVal = dtm.getNodeValue(langAttr);
          // %OPT%
          if (langVal.toLowerCase().startsWith(lang.toLowerCase())) {
            final int valLen = lang.length();

            if (langVal.length() == valLen || langVal.charAt(valLen) == '-') {
              isLang = true;
            }
          }

          break;
        }
      }

      parent = dtm.getParent(parent);
    }

    return isLang ? XBoolean.S_TRUE : XBoolean.S_FALSE;
  }
}
