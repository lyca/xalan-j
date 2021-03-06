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

import com.sun.codemodel.JExpression;
import com.sun.codemodel.JOp;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.MethodType;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class BinOpExpr extends Expression {
  public static final int PLUS = 0;
  public static final int MINUS = 1;
  public static final int TIMES = 2;
  public static final int DIV = 3;
  public static final int MOD = 4;

  private static final String[] Ops = { "+", "-", "*", "/", "%" };

  private final int _op;
  private Expression _left, _right;

  public BinOpExpr(int op, Expression left, Expression right) {
    _op = op;
    (_left = left).setParent(this);
    (_right = right).setParent(this);
  }

  /**
   * Returns true if this expressions contains a call to position(). This is
   * needed for context changes in node steps containing multiple predicates.
   */
  @Override
  public boolean hasPositionCall() {
    if (_left.hasPositionCall())
      return true;
    if (_right.hasPositionCall())
      return true;
    return false;
  }

  /**
   * Returns true if this expressions contains a call to last()
   */
  @Override
  public boolean hasLastCall() {
    return _left.hasLastCall() || _right.hasLastCall();
  }

  @Override
  public void setParser(Parser parser) {
    super.setParser(parser);
    _left.setParser(parser);
    _right.setParser(parser);
  }

  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    final Type tleft = _left.typeCheck(stable);
    final Type tright = _right.typeCheck(stable);
    final MethodType ptype = lookupPrimop(stable, Ops[_op], new MethodType(Type.Void, tleft, tright));
    if (ptype != null) {
      final Type arg1 = ptype.argsType().get(0);
      if (!arg1.identicalTo(tleft)) {
        _left = new CastExpr(_left, arg1);
      }
      final Type arg2 = ptype.argsType().get(1);
      if (!arg2.identicalTo(tright)) {
        _right = new CastExpr(_right, arg1);
      }
      return _type = ptype.resultType();
    }
    throw new TypeCheckError(this);
  }

  @Override
  public JExpression toJExpression(CompilerContext ctx) {
    JExpression leftExpr = _left.toJExpression(ctx);
    JExpression rightExpr = _right.toJExpression(ctx);

    switch (_op) {
    case PLUS:
      return JOp.plus(leftExpr, rightExpr);
    case MINUS:
      return JOp.minus(leftExpr, rightExpr);
    case TIMES:
      return JOp.mul(leftExpr, rightExpr);
    case DIV:
      return JOp.div(leftExpr, rightExpr);
    case MOD:
      return JOp.mod(leftExpr, rightExpr);
    default:
      final ErrorMsg msg = new ErrorMsg(this, Messages.get().illegalBinaryOpErr());
      getParser().reportError(Constants.ERROR, msg);
      return null;
    }
  }

  @Override
  public String toString() {
    return Ops[_op] + '(' + _left + ", " + _right + ')';
  }
}
