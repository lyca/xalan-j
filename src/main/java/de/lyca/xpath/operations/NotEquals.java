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

import de.lyca.xpath.objects.XBoolean;
import de.lyca.xpath.objects.XObject;

/**
 * The '!=' operation expression executer.
 */
public class NotEquals extends Operation {
  static final long serialVersionUID = -7869072863070586900L;

  /**
   * Apply the operation to two operands, and return the result.
   * 
   * 
   * @param left
   *          non-null reference to the evaluated left operand.
   * @param right
   *          non-null reference to the evaluated right operand.
   * 
   * @return non-null reference to the XObject that represents the result of the
   *         operation.
   * 
   * @throws TransformerException TODO
   */
  @Override
  public XObject operate(XObject left, XObject right) throws TransformerException {
    return left.notEquals(right) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
  }
}
