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
import de.lyca.xpath.res.Messages;

/**
 * Execute the Concat() function.
 */
public class FuncConcat extends FunctionMultiArgs {
  static final long serialVersionUID = 1737228885202314413L;

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

    final StringBuilder sb = new StringBuilder();

    // Compiler says we must have at least two arguments.
    sb.append(m_arg0.execute(xctxt).str());
    sb.append(m_arg1.execute(xctxt).str());

    if (null != m_arg2) {
      sb.append(m_arg2.execute(xctxt).str());
    }

    if (null != m_args) {
      for (int i = 0; i < m_args.length; i++) {
        sb.append(m_args[i].execute(xctxt).str());
      }
    }

    return new XString(sb.toString());
  }

  /**
   * Check that the number of arguments passed to this function is correct.
   * 
   * 
   * @param argNum
   *          The number of arguments that is being passed to the function.
   * 
   * @throws WrongNumberArgsException
   */
  @Override
  public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
    if (argNum < 2) {
      reportWrongNumberArgs();
    }
  }

  /**
   * Constructs and throws a WrongNumberArgException with the appropriate
   * message for this function object.
   * 
   * @throws WrongNumberArgsException
   */
  @Override
  protected void reportWrongNumberArgs() throws WrongNumberArgsException {
    throw new WrongNumberArgsException(Messages.get().gtone());
  }
}
