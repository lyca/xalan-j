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

import static com.sun.codemodel.JExpr.FALSE;
import static com.sun.codemodel.JExpr.direct;
import static com.sun.codemodel.JExpr.lit;
import static de.lyca.xalan.xsltc.compiler.Constants.ERROR;
import static de.lyca.xalan.xsltc.compiler.Constants.STATIC_NS_ANCESTORS_ARRAY_FIELD;
import static de.lyca.xalan.xsltc.compiler.Constants.STATIC_PREFIX_URIS_ARRAY_FIELD;
import static de.lyca.xalan.xsltc.compiler.Constants.STATIC_PREFIX_URIS_IDX_ARRAY_FIELD;
import static de.lyca.xalan.xsltc.compiler.Constants.WARNING;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JVar;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;
import de.lyca.xalan.xsltc.compiler.util.Util;
import de.lyca.xalan.xsltc.runtime.BasisLibrary;
import de.lyca.xml.utils.XML11Char;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 */
final class XslElement extends Instruction {

  private String _prefix;
  private boolean _ignore = false;
  private boolean _isLiteralName = true;
  private AttributeValueTemplate _name;
  private AttributeValueTemplate _namespace;
  private boolean namespaceUriEmpty = false;
  private boolean hasCalledStartPrefixMapping = false;

  /**
   * This method is now deprecated. The new implemation of this class never
   * declares the default NS.
   */
  public boolean declaresDefaultNS() {
    return false;
  }

  @Override
  public void parseContents(Parser parser) {
    final SymbolTable stable = parser.getSymbolTable();

    // Handle the 'name' attribute
    String name = getAttribute("name");
    if (hasAttribute("name") && name.isEmpty()) {
      final ErrorMsg msg = new ErrorMsg(this, Messages.get().illegalElemNameErr(name));
      parser.reportError(WARNING, msg);
      parseChildren(parser);
      _ignore = true; // Ignore the element if the QName is invalid
      return;

    } else if (!hasAttribute("name")) {
      // TODO Better error
      final ErrorMsg msg = new ErrorMsg(this, Messages.get().internalErr("xsl:element needs name attribute"));
      parser.reportError(ERROR, msg);
    }

    // Get namespace attribute
    String namespace = getAttribute("namespace");

    // Optimize compilation when name is known at compile time
    _isLiteralName = Util.isLiteral(name);
    if (_isLiteralName) {
      if (!XML11Char.isXML11ValidQName(name)) {
        final ErrorMsg msg = new ErrorMsg(this, Messages.get().illegalElemNameErr(name));
        parser.reportError(WARNING, msg);
        parseChildren(parser);
        _ignore = true; // Ignore the element if the QName is invalid
        return;
      }

      final QName qname = parser.getQNameSafe(name);
      String prefix = qname.getPrefix();
      if (prefix == null) {
        prefix = "";
      } else if (!prefix.isEmpty()) {
        String prefixAlias = stable.lookupPrefixAlias(prefix);
        if (prefixAlias != null) {
          prefix = prefixAlias;
        }
        final String local = qname.getLocalPart();
        name = prefix + ":" + local;
      }

      if (!hasAttribute("namespace")) {
        namespace = lookupNamespace(prefix);
        if (namespace == null) {
          final ErrorMsg err = new ErrorMsg(this, Messages.get().namespaceUndefErr(prefix));
          parser.reportError(WARNING, err);
          parseChildren(parser);
          _ignore = true; // Ignore the element if prefix is undeclared
          return;
        }
        _prefix = prefix;
        _namespace = new AttributeValueTemplate(namespace, parser, this);
      } else {
        // if (prefix == "") {
        // if (Util.isLiteral(namespace)) {
        // prefix = lookupPrefix(namespace);
        // if (prefix == null) {
        // prefix = stable.generateNamespacePrefix();
        // }
        // }
        //
        // // Prepend prefix to local name
        // final StringBuilder newName = new StringBuilder(prefix);
        // if (prefix != "") {
        // newName.append(':');
        // }
        // name = newName.append(local).toString();
        // }
        _prefix = prefix;
        _namespace = new AttributeValueTemplate(namespace, parser, this);
      }
    } else {
      // name attribute contains variable parts. If there is no namespace
      // attribute, the generated code needs to be prepared to look up
      // any prefix in the stylesheet at run-time.
      _namespace = namespace == "" ? null : new AttributeValueTemplate(namespace, parser, this);
    }
    namespaceUriEmpty = namespace.isEmpty();
    _name = new AttributeValueTemplate(name, parser, this);

    final String useSets = getAttribute("use-attribute-sets");
    if (useSets.length() > 0) {
      if (!Util.isValidQNames(useSets)) {
        final ErrorMsg err = new ErrorMsg(this, Messages.get().invalidQnameErr(useSets));
        parser.reportError(ERROR, err);
      }
      setFirstElement(new UseAttributeSets(useSets, parser));
    }

    parseChildren(parser);
  }

  /**
   * Run type check on element name & contents
   */
  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    if (!_ignore) {
      _name.typeCheck(stable);
      if (_namespace != null) {
        _namespace.typeCheck(stable);
      }
    }
    typeCheckContents(stable);
    return Type.Void;
  }

  private boolean hasElementContent() {
    if (hasContents()) {
      for (final SyntaxTreeNode item : getContents()) {
        if (item instanceof XslAttribute || item instanceof Text) {
          continue;
        } else {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * This method is called when the name of the element is known at compile
   * time. In this case, there is no need to inspect the element name at runtime
   * to determine if a prefix exists, needs to be generated, etc.
   */
  public void translateLiteral(CompilerContext ctx) {
    JExpression handler = ctx.currentHandler();
    if (!_ignore) {
      if (_name.elementCount() == 1) {
        ctx.currentBlock().invoke(handler, "startElement").arg(_name.toJExpression(ctx));
      } else {
        ctx.currentBlock().invoke(handler, "startElement").arg(_name.toJExpression(ctx));
      }
      if (_namespace != null) {
        ctx.currentBlock().add(
            ctx.currentHandler().invoke("namespaceAfterStartElement").arg(_prefix).arg(_namespace.toJExpression(ctx)));
        // if (hasElementContent() && _namespace.elementCount() > 0) {
        // ctx.currentBlock().add(ctx.currentHandler().invoke("startPrefixMapping").arg("").arg("").arg(JExpr.TRUE));
        // }
        // il.append(methodGen.loadHandler());
        // il.append(new PUSH(cpg, _prefix));
        // _namespace.translate(classGen, methodGen);
        // il.append(methodGen.namespace());
      }
    }

    translateContents(ctx);

    if (!_ignore) {
      if (_name.elementCount() == 1) {
        ctx.currentBlock().invoke(handler, "endElement").arg(_name.toJExpression(ctx));
      } else {
        ctx.currentBlock().invoke(handler, "endElement").arg(_name.toJExpression(ctx));
      }
    }
  }

  /**
   * At runtime the compilation of xsl:element results in code that: (i)
   * evaluates the avt for the name, (ii) checks for a prefix in the name (iii)
   * generates a new prefix and create a new qname when necessary (iv) calls
   * startElement() on the handler (v) looks up a uri in the XML when the prefix
   * is not known at compile time (vi) calls namespace() on the handler (vii)
   * evaluates the contents (viii) calls endElement().
   */
  @Override
  public void translate(CompilerContext ctx) {
    // FIXME
    // final ConstantPoolGen cpg = classGen.getConstantPool();
    // final InstructionList il = methodGen.getInstructionList();
    //
    // // Optimize translation if element name is a literal
    if (_isLiteralName) {
      translateLiteral(ctx);
      return;
    }

    JVar elementValue = null;
    if (!_ignore) {

      // if the qname is an AVT, then the qname has to be checked at runtime if
      // it is a valid qname
      // final LocalVariableGen nameValue =
      // methodGen.addLocalVariable2("nameValue", Util.getJCRefType(STRING_SIG),
      // null);

      // store the name into a variable first so _name.translate only needs to
      // be called once

      JVar nameValue = ctx.currentBlock().decl(ctx.ref(String.class), ctx.nextNameValue(), _name.toJExpression(ctx));

      // nameValue.setStart(il.append(new ASTORE(nameValue.getIndex())));
      // il.append(new ALOAD(nameValue.getIndex()));

      // call checkQName if the name is an AVT
      // final int check = cpg.addMethodref(BASIS_LIBRARY_CLASS, "checkQName",
      // "(" + STRING_SIG + ")V");
      // il.append(new INVOKESTATIC(check));

      ctx.currentBlock().add(ctx.ref(BasisLibrary.class).staticInvoke("checkQName").arg(nameValue));

      // Push handler for call to endElement()
      // il.append(methodGen.loadHandler());

      // load name value again
      // nameValue.setEnd(il.append(new ALOAD(nameValue.getIndex())));

      JExpression namespace;
      if (_namespace != null) {
        namespace = _namespace.toJExpression(ctx);
      } else {
        // If name is an AVT and namespace is not specified, need to
        // look up any prefix in the stylesheet by calling
        // BasisLibrary.lookupStylesheetQNameNamespace(
        // name, stylesheetNode, ancestorsArray,
        // prefixURIsIndexArray, prefixURIPairsArray,
        // !ignoreDefaultNamespace)
        namespace = ctx.ref(BasisLibrary.class).staticInvoke("lookupStylesheetQNameNamespace").arg(nameValue)
            .arg(lit(getNodeIDForStylesheetNSLookup())).arg(direct(STATIC_NS_ANCESTORS_ARRAY_FIELD))
            .arg(direct(STATIC_PREFIX_URIS_IDX_ARRAY_FIELD)).arg(direct(STATIC_PREFIX_URIS_ARRAY_FIELD)).arg(FALSE);
        // final String transletClassName = getXSLTC().getClassName();
        // il.append(DUP);
        // il.append(new PUSH(cpg, getNodeIDForStylesheetNSLookup()));
        // il.append(new GETSTATIC(cpg.addFieldref(transletClassName,
        // STATIC_NS_ANCESTORS_ARRAY_FIELD,
        // NS_ANCESTORS_INDEX_SIG)));
        // il.append(new GETSTATIC(cpg.addFieldref(transletClassName,
        // STATIC_PREFIX_URIS_IDX_ARRAY_FIELD,
        // PREFIX_URIS_IDX_SIG)));
        // il.append(new GETSTATIC(cpg.addFieldref(transletClassName,
        // STATIC_PREFIX_URIS_ARRAY_FIELD,
        // PREFIX_URIS_ARRAY_SIG)));
        // // Default namespace is significant
        // il.append(ICONST_0);
        // il.append(new INVOKESTATIC(cpg.addMethodref(BASIS_LIBRARY_CLASS,
        // LOOKUP_STYLESHEET_QNAME_NS_REF,
        // LOOKUP_STYLESHEET_QNAME_NS_SIG)));
      }

      // Push additional arguments
      // il.append(methodGen.loadHandler());
      // il.append(methodGen.loadDOM());
      // il.append(methodGen.loadCurrentNode());

      elementValue = ctx.currentBlock().decl(ctx.ref(String.class), ctx.nextElementName(),
          ctx.ref(BasisLibrary.class).staticInvoke("startXslElement").arg(nameValue).arg(namespace)
              .arg(ctx.currentHandler()).arg(ctx.currentDom()).arg(ctx.currentNode()));

      // Invoke BasisLibrary.startXslElemCheckQName()
      // il.append(new INVOKESTATIC(cpg.addMethodref(BASIS_LIBRARY_CLASS,
      // "startXslElement", "(" + STRING_SIG + STRING_SIG
      // + TRANSLET_OUTPUT_SIG + DOM_INTF_SIG + "I)" + STRING_SIG)));

    }

    translateContents(ctx);

    if (!_ignore) {
      ctx.currentBlock().invoke(ctx.currentHandler(), "endElement").arg(elementValue);
      // il.append(methodGen.endElement());
    }
  }

  /**
   * Override this method to make sure that xsl:attributes are not copied to
   * output if this xsl:element is to be ignored
   */
  @Override
  public void translateContents(CompilerContext ctx) {
    for (final SyntaxTreeNode item : getContents()) {
      if (_ignore && item instanceof XslAttribute) {
        continue;
      }
      if (!namespaceUriEmpty) {
        if (!hasCalledStartPrefixMapping && item instanceof LiteralElement) {
          QName qName = item.getQName();
          String prefix = qName.getPrefix() == null ? "" : qName.getPrefix();
          String namespace = qName.getNamespace() == null ? "" : qName.getNamespace();
          ctx.currentBlock()
              .add(ctx.currentHandler().invoke("startPrefixMapping").arg(prefix).arg(namespace).arg(JExpr.TRUE));
          hasCalledStartPrefixMapping = true;
        }
        if (!hasCalledStartPrefixMapping && item instanceof CopyOf) {
          ctx.currentBlock().add(ctx.currentHandler().invoke("startPrefixMapping").arg("").arg("").arg(JExpr.TRUE));
          hasCalledStartPrefixMapping = true;
        }
      }
      item.translate(ctx);
    }
  }

}
