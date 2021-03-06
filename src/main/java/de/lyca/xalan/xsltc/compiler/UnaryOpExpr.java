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

import static com.sun.codemodel.JOp.minus;

import com.sun.codemodel.JExpression;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.MethodType;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class UnaryOpExpr extends Expression {
  private Expression _left;

  public UnaryOpExpr(Expression left) {
    (_left = left).setParent(this);
  }

  /**
   * Returns true if this expressions contains a call to position(). This is
   * needed for context changes in node steps containing multiple predicates.
   */
  @Override
  public boolean hasPositionCall() {
    return _left.hasPositionCall();
  }

  /**
   * Returns true if this expressions contains a call to last()
   */
  @Override
  public boolean hasLastCall() {
    return _left.hasLastCall();
  }

  @Override
  public void setParser(Parser parser) {
    super.setParser(parser);
    _left.setParser(parser);
  }

  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    final Type tleft = _left.typeCheck(stable);
    final MethodType ptype = lookupPrimop(stable, "u-", new MethodType(Type.Void, tleft));

    if (ptype != null) {
      final Type arg1 = ptype.argsType().get(0);
      if (!arg1.identicalTo(tleft)) {
        _left = new CastExpr(_left, arg1);
      }
      return _type = ptype.resultType();
    }

    throw new TypeCheckError(this);
  }

  @Override
  public String toString() {
    return "u-" + '(' + _left + ')';
  }

  @Override
  public JExpression toJExpression(CompilerContext ctx) {
    return minus(_left.toJExpression(ctx));
  }

}
