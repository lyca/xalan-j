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
package de.lyca.xalan.xsltc.compiler;

import static com.sun.codemodel.JExpr.lit;

import com.sun.codemodel.JExpression;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class SimpleAttributeValue extends AttributeValue {

  private final String _value; // The attributes value (literate string).

  /**
   * Creates a new simple attribute value.
   * 
   * @param value
   *          the attribute value.
   */
  public SimpleAttributeValue(String value) {
    _value = value;
  }

  /**
   * Returns this attribute value's type (String).
   * 
   * @param stable
   *          The compiler/parser's symbol table
   */
  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    return _type = Type.String;
  }

  @Override
  public String toString() {
    return _value;
  }

  @Override
  protected boolean contextDependent() {
    return false;
  }

  @Override
  public JExpression toJExpression(CompilerContext ctx) {
    return lit(_value);
  }

}
