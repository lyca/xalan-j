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

import static com.sun.codemodel.JExpr._this;
import static com.sun.codemodel.JExpr.direct;
import static com.sun.codemodel.JExpr.lit;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JStatement;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;
import de.lyca.xalan.xsltc.dom.NodeCounter;
import de.lyca.xalan.xsltc.dom.NodeSortRecord;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
abstract class IdKeyPattern extends LocationPathPattern {

  protected RelativePathPattern _left = null;;
  private String _index = null;
  private String _value = null;;

  public IdKeyPattern(String index, String value) {
    _index = index;
    _value = value;
  }

  public String getIndexName() {
    return _index;
  }

  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    return Type.NodeSet;
  }

  @Override
  public boolean isWildcard() {
    return false;
  }

  public void setLeft(RelativePathPattern left) {
    _left = left;
  }

  @Override
  public StepPattern getKernelPattern() {
    return null;
  }

  @Override
  public void reduceKernelPattern() {
  }

  @Override
  public String toString() {
    return "id/keyPattern(" + _index + ", " + _value + ')';
  }

  @Override
  public void compilePattern(CompilerContext ctx, JStatement fail) {
    JBlock _then = ctx.currentBlock()._if(toJExpression(ctx))._then();
    _then.add(getTemplate().compile(ctx));
    _then._continue();
  }

  /**
   * This method is called when the constructor is compiled in
   * Stylesheet.compileConstructor() and not as the syntax tree is traversed.
   */
  @Override
  public JExpression toJExpression(CompilerContext ctx) {
    // Call getKeyIndex in AbstractTranslet with the name of the key
    // to get the index for this key (which is also a node iterator).
    JExpression clazzCtx = _this();

    if (ctx.ref(NodeCounter.class).isAssignableFrom(ctx.clazz())) {
      clazzCtx = direct("_translet");
    }
    if (ctx.ref(NodeSortRecord.class).isAssignableFrom(ctx.clazz())) {
      clazzCtx = direct("_translet");
    }
    JInvocation getKeyIndex = clazzCtx.invoke("getKeyIndex").arg(_index);

    // Now use the value in the second argument to determine what nodes
    // the iterator should return.
    JExpression result;
    if (this instanceof IdPattern) {
      result = getKeyIndex.invoke("containsID").arg(ctx.currentNode()).arg(_value).ne(lit(0));
    } else {
      result = getKeyIndex.invoke("containsKey").arg(ctx.currentNode()).arg(_value).ne(lit(0));
    }
    return result;
  }

}
