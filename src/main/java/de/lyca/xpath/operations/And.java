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
package de.lyca.xpath.operations;

import javax.xml.transform.TransformerException;

import de.lyca.xpath.XPathContext;
import de.lyca.xpath.objects.XBoolean;
import de.lyca.xpath.objects.XObject;

/**
 * The 'and' operation expression executer.
 */
public class And extends Operation {
  static final long serialVersionUID = 392330077126534022L;

  /**
   * AND two expressions and return the boolean result. Override superclass
   * method for optimization purposes.
   * 
   * @param xctxt
   *          The runtime execution context.
   * 
   * @return {@link de.lyca.xpath.objects.XBoolean#S_TRUE} or
   *         {@link de.lyca.xpath.objects.XBoolean#S_FALSE}.
   * 
   * @throws TransformerException TODO
   */
  @Override
  public XObject execute(XPathContext xctxt) throws TransformerException {

    final XObject expr1 = m_left.execute(xctxt);

    if (expr1.bool()) {
      final XObject expr2 = m_right.execute(xctxt);

      return expr2.bool() ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    } else
      return XBoolean.S_FALSE;
  }

  /**
   * Evaluate this operation directly to a boolean.
   * 
   * @param xctxt
   *          The runtime execution context.
   * 
   * @return The result of the operation as a boolean.
   * 
   * @throws TransformerException TODO
   */
  @Override
  public boolean bool(XPathContext xctxt) throws TransformerException {
    return m_left.bool(xctxt) && m_right.bool(xctxt);
  }

}
