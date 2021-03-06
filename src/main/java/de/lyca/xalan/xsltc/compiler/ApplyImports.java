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

import static de.lyca.xalan.xsltc.compiler.Constants.ITERATOR_PNAME;
import static de.lyca.xalan.xsltc.compiler.Constants.POP_PARAM_FRAME;
import static de.lyca.xalan.xsltc.compiler.Constants.PUSH_PARAM_FRAME;

import de.lyca.xalan.res.XSLTErrorResources;
import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;

final class ApplyImports extends Instruction {

  private QName _modeName;
  private int _precedence;

  /**
   * Determine the lowest import precedence for any stylesheet imported or
   * included by the stylesheet in which this <xsl:apply-imports/> element
   * occured. The templates that are imported by the stylesheet in which this
   * element occured will all have higher import precedence than the integer
   * returned by this method.
   */
  private int getMinPrecedence(int max) {
    // Move to root of include tree
    Stylesheet includeRoot = getStylesheet();
    while (includeRoot._includedFrom != null) {
      includeRoot = includeRoot._includedFrom;
    }

    return includeRoot.getMinimumDescendantPrecedence();
  }

  /**
   * Parse the attributes and contents of an <xsl:apply-imports/> element.
   */
  @Override
  public void parseContents(Parser parser) {
    // Indicate to the top-level stylesheet that all templates must be
    // compiled into separate methods.
    final Stylesheet stylesheet = getStylesheet();
    stylesheet.setTemplateInlining(false);

    // Get the mode we are currently in (might not be any)
    final Template template = getTemplate();
    _modeName = template.getModeName();
    _precedence = template.getImportPrecedence();

    parseChildren(parser); // with-params
  }

  /**
   * Type-check the attributes/contents of an <xsl:apply-imports/> element.
   */
  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    SyntaxTreeNode parent = getParent();
    while (parent != null) {
      if (parent instanceof ForEach) {
        // TODO better error reporting: XSLTErrorResources.ER_NO_APPLY_IMPORT_IN_FOR_EACH
        final ErrorMsg err = new ErrorMsg(this,
            Messages.get().internalErr("xsl:apply-imports not allowed in a xsl:for-each"));
        throw new TypeCheckError(err);
      }
      parent = parent.getParent();
    }
    typeCheckContents(stable); // with-params
    return Type.Void;
  }

  /**
   * Translate call-template. A parameter frame is pushed only if some template
   * in the stylesheet uses parameters.
   */
  @Override
  public void translate(CompilerContext ctx) {
    // Push a new parameter frame in case imported template might expect
    // parameters. The apply-imports has nothing that it can pass.
    if (ctx.stylesheet().hasLocalParams()) {
      ctx.currentBlock().invoke(PUSH_PARAM_FRAME);
    }

    // Get the [min,max> precedence of all templates imported under the
    // current stylesheet
    final int maxPrecedence = _precedence;
    final int minPrecedence = getMinPrecedence(maxPrecedence);
    final Mode mode = ctx.stylesheet().getMode(_modeName);

    // Get name of appropriate apply-templates function for this
    // xsl:apply-imports instruction
    final String functionName = mode.functionName(minPrecedence, maxPrecedence);

    // Construct the translet class-name and the signature of the method
    ctx.currentBlock().invoke(functionName).arg(ctx.currentDom()).arg(ctx.param(ITERATOR_PNAME))
        .arg(ctx.currentHandler()).arg(ctx.currentNode());

    // Pop any parameter frame that was pushed above.
    if (ctx.stylesheet().hasLocalParams()) {
      ctx.currentBlock().invoke(POP_PARAM_FRAME);
    }
  }

}
