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

import static de.lyca.xalan.xsltc.compiler.Constants.FATAL;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
abstract class Instruction extends SyntaxTreeNode {

  /**
   * Type check all the children of this node.
   */
  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    return typeCheckContents(stable);
  }

  /**
   * Translate this node into JVM bytecodes.
   */
  @Override
  public void translate(CompilerContext ctx) {
    final ErrorMsg msg = new ErrorMsg(this, Messages.get().notImplementedErr(getClass()));
    getParser().reportError(FATAL, msg);
  }
}
