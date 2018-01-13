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
package de.lyca.xpath.functions;

import java.util.List;

import javax.xml.transform.TransformerException;

import de.lyca.xml.dtm.DTM;
import de.lyca.xml.dtm.DTMIterator;
import de.lyca.xml.utils.QName;
import de.lyca.xpath.XPathContext;
import de.lyca.xpath.axes.SubContextList;
import de.lyca.xpath.compiler.Compiler;
import de.lyca.xpath.objects.XNumber;
import de.lyca.xpath.objects.XObject;

/**
 * Execute the Position() function.
 */
public class FuncPosition extends Function {
  static final long serialVersionUID = -9092846348197271582L;
  private boolean m_isTopLevel;

  /**
   * Figure out if we're executing a toplevel expression. If so, we can't be
   * inside of a predicate.
   */
  @Override
  public void postCompileStep(Compiler compiler) {
    m_isTopLevel = compiler.getLocationPathDepth() == -1;
  }

  /**
   * Get the position in the current context node list.
   * 
   * @param xctxt
   *          Runtime XPath context.
   * 
   * @return The current position of the itteration in the context node list, or
   *         -1 if there is no active context node list.
   */
  public int getPositionInContextNodeList(XPathContext xctxt) {

    // System.out.println("FuncPosition- entry");
    // If we're in a predicate, then this will return non-null.
    final SubContextList iter = m_isTopLevel ? null : xctxt.getSubContextList();

    if (null != iter) {
      final int prox = iter.getProximityPosition(xctxt);

      // System.out.println("FuncPosition- prox: "+prox);
      return prox;
    }

    DTMIterator cnl = xctxt.getContextNodeList();

    if (null != cnl) {
      int n = cnl.getCurrentNode();
      if (n == DTM.NULL) {
        if (cnl.getCurrentPos() == 0)
          return 0;

        // Then I think we're in a sort. See sort21.xsl. So the iterator has
        // already been spent, and is not on the node we're processing.
        // It's highly possible that this is an issue for other context-list
        // functions. Shouldn't be a problem for last(), and it shouldn't be
        // a problem for current().
        try {
          cnl = cnl.cloneWithReset();
        } catch (final CloneNotSupportedException cnse) {
          throw new de.lyca.xml.utils.WrappedRuntimeException(cnse);
        }
        final int currentNode = xctxt.getContextNode();
        // System.out.println("currentNode: "+currentNode);
        while (DTM.NULL != (n = cnl.nextNode())) {
          if (n == currentNode) {
            break;
          }
        }
      }
      // System.out.println("n: "+n);
      // System.out.println("FuncPosition- cnl.getCurrentPos(): "+cnl.getCurrentPos());
      return cnl.getCurrentPos();
    }

    // System.out.println("FuncPosition - out of guesses: -1");
    return -1;
  }

  /**
   * Execute the function. The function must return a valid object.
   * 
   * @param xctxt
   *          The current execution context.
   * @return A valid XObject.
   * 
   * @throws TransformerException TODO
   */
  @Override
  public XObject execute(XPathContext xctxt) throws TransformerException {
    final double pos = getPositionInContextNodeList(xctxt);

    return new XNumber(pos);
  }

  /**
   * No arguments to process, so this does nothing.
   */
  @Override
  public void fixupVariables(List<QName> vars, int globalsSize) {
    // no-op
  }
}
