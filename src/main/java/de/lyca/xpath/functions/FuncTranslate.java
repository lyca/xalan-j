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

import de.lyca.xpath.XPathContext;
import de.lyca.xpath.objects.XObject;
import de.lyca.xpath.objects.XString;

/**
 * Execute the Translate() function.
 */
public class FuncTranslate extends Function3Args {
  static final long serialVersionUID = -1672834340026116482L;

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

    final String theFirstString = m_arg0.execute(xctxt).str();
    final String theSecondString = m_arg1.execute(xctxt).str();
    final String theThirdString = m_arg2.execute(xctxt).str();
    final int theFirstStringLength = theFirstString.length();
    final int theThirdStringLength = theThirdString.length();

    // A vector to contain the new characters. We'll use it to construct
    // the result string.
    final StringBuilder sbuffer = new StringBuilder();

    for (int i = 0; i < theFirstStringLength; i++) {
      final char theCurrentChar = theFirstString.charAt(i);
      final int theIndex = theSecondString.indexOf(theCurrentChar);

      if (theIndex < 0) {

        // Didn't find the character in the second string, so it
        // is not translated.
        sbuffer.append(theCurrentChar);
      } else if (theIndex < theThirdStringLength) {

        // OK, there's a corresponding character in the
        // third string, so do the translation...
        sbuffer.append(theThirdString.charAt(theIndex));
      } else {

        // There's no corresponding character in the
        // third string, since it's shorter than the
        // second string. In this case, the character
        // is removed from the output string, so don't
        // do anything.
      }
    }

    return new XString(sbuffer.toString());
  }
}
