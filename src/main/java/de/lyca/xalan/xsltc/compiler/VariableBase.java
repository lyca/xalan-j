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

import static com.sun.codemodel.JExpr._new;
import static com.sun.codemodel.JExpr.lit;

import java.util.ArrayList;
import java.util.List;

import com.sun.codemodel.JExpression;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.NodeSetType;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.Util;
import de.lyca.xalan.xsltc.dom.CachedNodeListIterator;
import de.lyca.xml.utils.XML11Char;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @author Erwin Bolwidt {@literal <ejb@klomp.org>}
 * @author John Howard {@literal <JohnH@schemasoft.com>}
 */
class VariableBase extends TopLevelElement {

  protected QName _name; // The name of the variable.
  protected String _escapedName; // The escaped qname of the variable.
  protected Type _type; // The type of this variable.
  protected boolean _isLocal; // True if the variable is local.
  protected JVar _param; // Reference to JVM variable
  protected Expression _select; // Reference to variable expression
  protected String select; // Textual repr. of variable expr.

  // References to this variable (when local)
  protected List<VariableRefBase> _refs = new ArrayList<>(2);

  // Used to make sure parameter field is not added twice
  protected boolean _ignore = false;

  /**
   * Disable this variable/parameter
   */
  public void disable() {
    _ignore = true;
  }

  /**
   * Add a reference to this variable. Called by VariableRef when an expression
   * contains a reference to this variable.
   */
  public void addReference(VariableRefBase vref) {
    _refs.add(vref);
  }

  /**
   * Create the variable in the current block
   */
  public void mapRegister(CompilerContext ctx) {
    if (_param == null) {
      final String name = getEscapedName(); // TODO: namespace ?
      final JType varType = _type.toJCType();
      _param = ctx.currentBlock().decl(varType, name);
    }
  }

  /**
   * Returns an instruction for loading the value of this variable onto the JVM
   * stack.
   */
  public JVar loadParam() {
    return _param;
  }

  /**
   * Returns an instruction for storing a value from the JVM stack into this
   * variable.
   */
  public void storeParam(JVar param) {
    _param = param;
  }

  /**
   * Returns the expression from this variable's select attribute (if any)
   */
  public Expression getExpression() {
    return _select;
  }

  /**
   * Display variable as single string
   */
  @Override
  public String toString() {
    return "variable(" + _name + ")";
  }

  /**
   * Returns the type of the variable
   */
  public Type getType() {
    return _type;
  }

  /**
   * Returns the name of the variable or parameter as it will occur in the
   * compiled translet.
   */
  public QName getName() {
    return _name;
  }

  /**
   * Returns the escaped qname of the variable or parameter
   */
  public String getEscapedName() {
    return _escapedName;
  }

  /**
   * Set the name of the variable or paremeter. Escape all special chars.
   */
  public void setName(QName name) {
    _name = name;
    _escapedName = Util.escape(name.getStringRep());
  }

  /**
   * Returns the true if the variable is local
   */
  public boolean isLocal() {
    return _isLocal;
  }

  /**
   * Parse the contents of the <xsl:decimal-format> element.
   */
  @Override
  public void parseContents(Parser parser) {
    // Get the 'name attribute
    final String name = getAttribute("name");

    if (name.length() > 0) {
      if (!XML11Char.isXML11ValidQName(name)) {
        final ErrorMsg err = new ErrorMsg(this, Messages.get().invalidQnameErr(name));
        parser.reportError(Constants.ERROR, err);
      }
      setName(parser.getQNameIgnoreDefaultNs(name));
    } else {
      reportError(this, parser, Messages.get().requiredAttrErr("name"));
    }

    // Check whether variable/param of the same name is already in scope
    final VariableBase other = parser.lookupVariable(_name);
    if (other != null && other.getParent() == getParent()) {
      reportError(this, parser, Messages.get().variableRedefErr(name));
    }

    select = getAttribute("select");
    if (select.length() > 0) {
      _select = getParser().parseExpression(this, "select", null);
      if (_select.isDummy()) {
        reportError(this, parser, Messages.get().requiredAttrErr("select"));
        return;
      }
    }

    // Children must be parsed first -> static scoping
    parseChildren(parser);
  }

  /**
   * Compile the value of the variable, which is either in an expression in a
   * 'select' attribute, or in the variable elements body
   */
  public JExpression compileValue(CompilerContext ctx) {
    // Compile expression is 'select' attribute if present
    if (_select != null) {
      JExpression select = _select.toJExpression(ctx);
      // Create a CachedNodeListIterator for select expressions in a variable or
      // parameter.
      if (_select.getType() instanceof NodeSetType) {
        select = _new(ctx.ref(CachedNodeListIterator.class)).arg(select);
      }
      return _select.startIterator(ctx, select);
    }
    // If not, compile result tree from parameter body if present.
    else if (hasContents()) {
      return compileResultTree(ctx);
    }
    // If neither are present then store empty string in variable
    else {
      return lit("");
    }
  }

}
