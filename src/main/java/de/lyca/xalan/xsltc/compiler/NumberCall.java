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

import java.util.List;

import com.sun.codemodel.JExpression;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class NumberCall extends FunctionCall {

  public NumberCall(QName fname, List<Expression> arguments) {
    super(fname, arguments);
  }

  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    switch (argumentCount()) {
    case 0:
      return _type = Type.Real;
    case 1:
      argument().typeCheck(stable);
      return _type = Type.Real;
    default:
      final ErrorMsg err = new ErrorMsg(Messages.get().illegalArgErr());
      throw new TypeCheckError(err);
    }
  }

  @Override
  public JExpression toJExpression(CompilerContext ctx) {
    JExpression expr;
    Type targ;
    if (argumentCount() == 0) {
      expr = ctx.currentNode();
      targ = Type.Node;
    } else {
      final Expression arg = argument();
      expr = arg.startIterator(ctx, arg.toJExpression(ctx));
      targ = arg.getType();
    }

    if (!targ.identicalTo(Type.Real)) {
      expr = targ.compileTo(ctx, expr, Type.Real);
    }
    return expr;
  }

}
