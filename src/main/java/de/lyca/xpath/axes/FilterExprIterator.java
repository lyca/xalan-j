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
package de.lyca.xpath.axes;

import java.util.List;

import de.lyca.xml.dtm.DTM;
import de.lyca.xml.utils.QName;
import de.lyca.xpath.Expression;
import de.lyca.xpath.ExpressionOwner;
import de.lyca.xpath.XPathVisitor;
import de.lyca.xpath.objects.XNodeSet;

public class FilterExprIterator extends BasicTestIterator {
  static final long serialVersionUID = 2552176105165737614L;
  /**
   * The contained expression. Should be non-null.
   * 
   * @serial
   */
  private Expression m_expr;

  /** The result of executing m_expr. Needs to be deep cloned on clone op. */
  transient private XNodeSet m_exprObj;

  /**
   * Create a FilterExprIterator object.
   * 
   */
  public FilterExprIterator() {
    super(null);
  }

  /**
   * Create a FilterExprIterator object.
   * @param expr TODO
   */
  public FilterExprIterator(Expression expr) {
    super(null);
    m_expr = expr;
  }

  /**
   * Initialize the context values for this expression after it is cloned.
   * 
   * @param context
   *          The XPath runtime context for this transformation.
   */
  @Override
  public void setRoot(int context, Object environment) {
    super.setRoot(context, environment);

    m_exprObj = FilterExprIteratorSimple.executeFilterExpr(context, m_execContext, getPrefixResolver(),
            getIsTopLevel(), m_stackFrame, m_expr);
  }

  /**
   * Get the next node via getNextXXX. Bottlenecked for derived class override.
   * 
   * @return The next node on the axis, or DTM.NULL.
   */
  @Override
  protected int getNextNode() {
    if (null != m_exprObj) {
      m_lastFetched = m_exprObj.nextNode();
    } else {
      m_lastFetched = DTM.NULL;
    }

    return m_lastFetched;
  }

  /**
   * Detaches the walker from the set which it iterated over, releasing any
   * computational resources and placing the iterator in the INVALID state.
   */
  @Override
  public void detach() {
    super.detach();
    m_exprObj.detach();
    m_exprObj = null;
  }

  /**
   * This function is used to fixup variables from QNames to stack frame indexes
   * at stylesheet build time.
   * 
   * @param vars
   *          List of QNames that correspond to variables. This list should be
   *          searched backwards for the first qualified name that corresponds
   *          to the variable reference qname. The position of the QName in the
   *          list from the start of the list will be its position in the stack
   *          frame (but variables above the globalsTop value will need to be
   *          offset to the current stack frame).
   */
  @Override
  public void fixupVariables(List<QName> vars, int globalsSize) {
    super.fixupVariables(vars, globalsSize);
    m_expr.fixupVariables(vars, globalsSize);
  }

  /**
   * Get the inner contained expression of this filter.
   * @return TODO
   */
  public Expression getInnerExpression() {
    return m_expr;
  }

  /**
   * Set the inner contained expression of this filter.
   * @param expr TODO
   */
  public void setInnerExpression(Expression expr) {
    expr.exprSetParent(this);
    m_expr = expr;
  }

  /**
   * Get the analysis bits for this walker, as defined in the WalkerFactory.
   * 
   * @return One of WalkerFactory#BIT_DESCENDANT, etc.
   */
  @Override
  public int getAnalysisBits() {
    if (null != m_expr && m_expr instanceof PathComponent)
      return ((PathComponent) m_expr).getAnalysisBits();
    return WalkerFactory.BIT_FILTER;
  }

  /**
   * Returns true if all the nodes in the iteration well be returned in document
   * order. Warning: This can only be called after setRoot has been called!
   * 
   * @return true as a default.
   */
  @Override
  public boolean isDocOrdered() {
    return m_exprObj.isDocOrdered();
  }

  class filterExprOwner implements ExpressionOwner {
    /**
     * @see ExpressionOwner#getExpression()
     */
    @Override
    public Expression getExpression() {
      return m_expr;
    }

    /**
     * @see ExpressionOwner#setExpression(Expression)
     */
    @Override
    public void setExpression(Expression exp) {
      exp.exprSetParent(FilterExprIterator.this);
      m_expr = exp;
    }

  }

  /**
   * This will traverse the heararchy, calling the visitor for each member. If
   * the called visitor method returns false, the subtree should not be called.
   * 
   * @param visitor
   *          The visitor whose appropriate method will be called.
   */
  @Override
  public void callPredicateVisitors(XPathVisitor visitor) {
    m_expr.callVisitors(new filterExprOwner(), visitor);

    super.callPredicateVisitors(visitor);
  }

  /**
   * @see Expression#deepEquals(Expression)
   */
  @Override
  public boolean deepEquals(Expression expr) {
    if (!super.deepEquals(expr))
      return false;

    final FilterExprIterator fet = (FilterExprIterator) expr;
    if (!m_expr.deepEquals(fet.m_expr))
      return false;

    return true;
  }

}
