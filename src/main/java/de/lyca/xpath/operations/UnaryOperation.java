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
package de.lyca.xpath.operations;

import java.util.List;

import javax.xml.transform.TransformerException;

import de.lyca.xml.utils.QName;
import de.lyca.xpath.Expression;
import de.lyca.xpath.ExpressionOwner;
import de.lyca.xpath.XPathContext;
import de.lyca.xpath.XPathVisitor;
import de.lyca.xpath.objects.XObject;

/**
 * The unary operation base class.
 */
public abstract class UnaryOperation extends Expression implements ExpressionOwner {
  static final long serialVersionUID = 6536083808424286166L;

  /**
   * The operand for the operation.
   * 
   * @serial
   */
  protected Expression m_right;

  /**
   * This function is used to fixup variables from QNames to stack frame indexes at stylesheet build time.
   * 
   * @param vars List of QNames that correspond to variables. This list should be searched backwards for the first
   *        qualified name that corresponds to the variable reference qname. The position of the QName in the list from
   *        the start of the list will be its position in the stack frame (but variables above the globalsTop value will
   *        need to be offset to the current stack frame).
   */
  @Override
  public void fixupVariables(List<QName> vars, int globalsSize) {
    m_right.fixupVariables(vars, globalsSize);
  }

  /**
   * Tell if this expression or it's subexpressions can traverse outside the current subtree.
   * 
   * @return true if traversal outside the context node's subtree can occur.
   */
  @Override
  public boolean canTraverseOutsideSubtree() {

    if (null != m_right && m_right.canTraverseOutsideSubtree())
      return true;

    return false;
  }

  /**
   * Set the expression operand for the operation.
   * 
   * @param r The expression operand to which the unary operation will be applied.
   */
  public void setRight(Expression r) {
    m_right = r;
    r.exprSetParent(this);
  }

  /**
   * Execute the operand and apply the unary operation to the result.
   * 
   * @param xctxt The runtime execution context.
   * @return An XObject that represents the result of applying the unary operation to the evaluated operand.
   * @throws TransformerException TODO
   */
  @Override
  public XObject execute(XPathContext xctxt) throws TransformerException {

    return operate(m_right.execute(xctxt));
  }

  /**
   * Apply the operation to two operands, and return the result.
   * 
   * @param right non-null reference to the evaluated right operand.
   * @return non-null reference to the XObject that represents the result of the operation.
   * @throws TransformerException TODO
   */
  public abstract XObject operate(XObject right) throws TransformerException;

  /**
   * @return the operand of unary operation, as an Expression.
   */
  public Expression getOperand() {
    return m_right;
  }

  /**
   * @see de.lyca.xpath.XPathVisitable#callVisitors(ExpressionOwner, XPathVisitor)
   */
  @Override
  public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
    if (visitor.visitUnaryOperation(owner, this)) {
      m_right.callVisitors(this, visitor);
    }
  }

  /**
   * @see ExpressionOwner#getExpression()
   */
  @Override
  public Expression getExpression() {
    return m_right;
  }

  /**
   * @see ExpressionOwner#setExpression(Expression)
   */
  @Override
  public void setExpression(Expression exp) {
    exp.exprSetParent(this);
    m_right = exp;
  }

  /**
   * @see Expression#deepEquals(Expression)
   */
  @Override
  public boolean deepEquals(Expression expr) {
    return isSameClass(expr) && m_right.deepEquals(((UnaryOperation) expr).m_right);
  }

}
