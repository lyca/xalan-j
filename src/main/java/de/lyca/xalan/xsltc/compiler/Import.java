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

import java.util.ListIterator;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.lyca.xalan.xsltc.compiler.util.CompilerContext;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.compiler.util.Type;
import de.lyca.xalan.xsltc.compiler.util.TypeCheckError;
import de.lyca.xml.utils.SystemIDResolver;

/**
 * @author Jacek Ambroziak
 * @author Morten Jorgensen
 * @author Erwin Bolwidt {@literal <ejb@klomp.org>}
 * @author Gunnlaugur Briem <gthb@dimon.is>
 */
final class Import extends TopLevelElement {

  private Stylesheet _imported = null;

  public Stylesheet getImportedStylesheet() {
    return _imported;
  }

  @Override
  public void parseContents(final Parser parser) {
    final XSLTC xsltc = parser.getXSLTC();
    final Stylesheet context = parser.getCurrentStylesheet();

    try {
      String docToLoad = getAttribute("href");

      InputSource input = null;
      XMLReader reader = null;
      final String currLoadedDoc = context.getSystemId();
      final SourceLoader loader = context.getSourceLoader();

      // Use SourceLoader if available
      if (loader != null) {
        input = loader.loadSource(docToLoad, currLoadedDoc, xsltc);
        if (input != null) {
          docToLoad = input.getSystemId();
          reader = xsltc.getXMLReader();
        }
      }

      // No SourceLoader or not resolved by SourceLoader
      if (input == null) {
        docToLoad = SystemIDResolver.getAbsoluteURI(docToLoad, currLoadedDoc);
        input = new InputSource(docToLoad);
      }

      if (context.checkForLoop(docToLoad)) {
        final ErrorMsg msg = new ErrorMsg(this, Messages.get().circularIncludeErr(docToLoad));
        parser.reportError(Constants.FATAL, msg);
        return;
      }

      // Return if we could not resolve the URL
      if (input == null) {
        final ErrorMsg msg = new ErrorMsg(this, Messages.get().fileNotFoundErr(docToLoad));
        parser.reportError(Constants.FATAL, msg);
        return;
      }

      final SyntaxTreeNode root;
      if (reader != null) {
        root = parser.parse(reader, input);
      } else {
        root = parser.parse(input);
      }

      if (root == null)
        return;
      _imported = parser.makeStylesheet(root);
      if (_imported == null)
        return;

      _imported.setSourceLoader(loader);
      _imported.setSystemId(docToLoad);
      _imported.setParentStylesheet(context);
      _imported.setImportingStylesheet(context);
      _imported.setTemplateInlining(context.getTemplateInlining());

      // precedence for the including stylesheet
      final int currPrecedence = parser.getCurrentImportPrecedence();
      final int nextPrecedence = parser.getNextImportPrecedence();
      _imported.setImportPrecedence(currPrecedence);
      context.setImportPrecedence(nextPrecedence);
      parser.setCurrentStylesheet(_imported);
      _imported.parseContents(parser);

      final ListIterator<SyntaxTreeNode> elements = _imported.elements();
      final Stylesheet topStylesheet = parser.getTopLevelStylesheet();
      while (elements.hasNext()) {
        final SyntaxTreeNode element = elements.next();
        if (element instanceof TopLevelElement) {
          if (element instanceof Variable) {
            topStylesheet.addVariable((Variable) element);
          } else if (element instanceof Param) {
            topStylesheet.addParam((Param) element);
          } else {
            topStylesheet.addElement(element);
          }
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      parser.setCurrentStylesheet(context);
    }
  }

  @Override
  public Type typeCheck(SymbolTable stable) throws TypeCheckError {
    if (!(getParent() instanceof Stylesheet)) {
      // TODO better error reporting ILLEGAL_PARENT
      final ErrorMsg err = new ErrorMsg(this, Messages.get().internalErr("Parent is not Stylesheet"));
      throw new TypeCheckError(err);
    }
    return Type.Void;
  }

  @Override
  public void translate(CompilerContext ctx) {
    // do nothing
  }
}
