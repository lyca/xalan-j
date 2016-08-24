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
/*
 * $Id$
 */

package de.lyca.xalan.xsltc.compiler;

import static com.sun.codemodel.JOp.not;

import java.util.List;

import com.sun.codemodel.JExpression;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class NotCall extends FunctionCall {
  public NotCall(QName fname, List<Expression> arguments) {
    super(fname, arguments);
  }

  @Override
  public JExpression toJExpression(CompilerContext ctx) {
    return not(argument().toJExpression(ctx));
  }

  @Override
  public void translate(CompilerContext ctx) {
 // FIXME
//    final InstructionList il = methodGen.getInstructionList();
//    argument().translate(classGen, methodGen);
//    il.append(ICONST_1);
//    il.append(IXOR);
  }

  @Override
  public void translateDesynthesized(CompilerContext ctx) {
 // FIXME
//    final InstructionList il = methodGen.getInstructionList();
//    final Expression exp = argument();
//    exp.translateDesynthesized(classGen, methodGen);
//    final BranchHandle gotoh = il.append(new GOTO(null));
//    _trueList = exp._falseList; // swap flow lists
//    _falseList = exp._trueList;
//    _falseList.add(gotoh);
  }
}
