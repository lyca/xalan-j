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

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.ObjectType;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Santiago Pericas-Geertsen
 */
final class CastCall extends FunctionCall {

  /**
   * Name of the class that is the target of the cast. Must be a fully-qualified
   * Java class Name.
   */
  private String _className;

  /**
   * A reference to the expression being casted.
   */
  private Expression _right;

  /**
   * Constructor.
   */
  public CastCall(QName fname, List<Expression> arguments) {
    super(fname, arguments);
  }

  /**
   * Type check the two parameters for this function
   */
  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    // Check that the function was passed exactly two arguments
    if (argumentCount() != 2) // TODO illegalArg
      throw new TypeCheckError(new ErrorMsg(this, Messages.get().illegalArgErr()));

    // The first argument must be a literal String
    final Expression exp = argument(0);
    if (exp instanceof LiteralExpr) {
      _className = ((LiteralExpr) exp).getValue();
      _type = Type.newObjectType(_className);
    } else
      throw new TypeCheckError(new ErrorMsg(this,Messages.get().needLiteralErr(getName())));

    // Second argument must be of type reference or object
    _right = argument(1);
    final Type tright = _right.typeCheck(stable);
    if (tright != Type.Reference && tright instanceof ObjectType == false)
      throw new TypeCheckError(new ErrorMsg(this,Messages.get().dataConversionErr(tright, _type)));

    return _type;
  }

  @Override
  public void translate(CompilerContext ctx) {
    // FIXME
    // final ConstantPoolGen cpg = classGen.getConstantPool();
    // final InstructionList il = methodGen.getInstructionList();
    //
    // _right.translate(classGen, methodGen);
    // il.append(new CHECKCAST(cpg.addClass(_className)));
  }

}
