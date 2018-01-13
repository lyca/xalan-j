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
package de.lyca.xpath.patterns;

import javax.xml.transform.TransformerException;

import de.lyca.xml.dtm.Axis;
import de.lyca.xml.dtm.DTM;
import de.lyca.xml.dtm.DTMAxisTraverser;
import de.lyca.xml.dtm.DTMFilter;
import de.lyca.xpath.XPathContext;
import de.lyca.xpath.axes.WalkerFactory;
import de.lyca.xpath.objects.XObject;

/**
 * Special context node pattern matcher.
 */
public class ContextMatchStepPattern extends StepPattern {
  static final long serialVersionUID = -1888092779313211942L;

  /**
   * Construct a ContextMatchStepPattern.
   * 
   * @param axis
   *          TODO
   * @param paxis
   *          TODO
   */
  public ContextMatchStepPattern(Axis axis, Axis paxis) {
    super(DTMFilter.SHOW_ALL, axis, paxis);
  }

  /**
   * Execute this pattern step, including predicates.
   * 
   * 
   * @param xctxt
   *          XPath runtime context.
   * 
   * @return {@link de.lyca.xpath.patterns.NodeTest#SCORE_NODETEST},
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_NONE},
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_NSWILD},
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_OTHER}.
   * 
   * @throws TransformerException
   *           TODO
   */
  @Override
  public XObject execute(XPathContext xctxt) throws TransformerException {

    if (xctxt.getIteratorRoot() == xctxt.getCurrentNode())
      return getStaticScore();
    else
      return NodeTest.SCORE_NONE;
  }

  /**
   * Execute the match pattern step relative to another step.
   * 
   * 
   * @param xctxt
   *          The XPath runtime context. NEEDSDOC @param prevStep
   * @param prevStep
   *          TODO
   * @return {@link de.lyca.xpath.patterns.NodeTest#SCORE_NODETEST},
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_NONE},
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_NSWILD},
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link de.lyca.xpath.patterns.NodeTest#SCORE_OTHER}.
   * 
   * @throws TransformerException
   *           TODO
   */
  public XObject executeRelativePathPattern(XPathContext xctxt, StepPattern prevStep) throws TransformerException {

    XObject score = NodeTest.SCORE_NONE;
    final int context = xctxt.getCurrentNode();
    final DTM dtm = xctxt.getDTM(context);

    if (null != dtm) {
      DTMAxisTraverser traverser;

      Axis axis = m_axis;

      final boolean needToTraverseAttrs = WalkerFactory.isDownwardAxisOfMany(axis);
      final boolean iterRootIsAttr = dtm.getNodeType(xctxt.getIteratorRoot()) == DTM.ATTRIBUTE_NODE;

      if (Axis.PRECEDING == axis && iterRootIsAttr) {
        axis = Axis.PRECEDINGANDANCESTOR;
      }

      traverser = dtm.getAxisTraverser(axis);

      for (int relative = traverser.first(context); DTM.NULL != relative; relative = traverser.next(context,
          relative)) {
        try {
          xctxt.pushCurrentNode(relative);

          score = execute(xctxt);

          if (score != NodeTest.SCORE_NONE) {
            // score = executePredicates( xctxt, prevStep, SCORE_OTHER,
            // predContext, relative);
            if (executePredicates(xctxt, dtm, context))
              return score;

            score = NodeTest.SCORE_NONE;
          }

          if (needToTraverseAttrs && iterRootIsAttr && DTM.ELEMENT_NODE == dtm.getNodeType(relative)) {
            Axis xaxis = Axis.ATTRIBUTE;
            for (int i = 0; i < 2; i++) {
              final DTMAxisTraverser atraverser = dtm.getAxisTraverser(xaxis);

              for (int arelative = atraverser.first(relative); DTM.NULL != arelative; arelative = atraverser
                  .next(relative, arelative)) {
                try {
                  xctxt.pushCurrentNode(arelative);

                  score = execute(xctxt);

                  if (score != NodeTest.SCORE_NONE) {
                    // score = executePredicates( xctxt, prevStep, SCORE_OTHER,
                    // predContext, arelative);

                    if (score != NodeTest.SCORE_NONE)
                      return score;
                  }
                } finally {
                  xctxt.popCurrentNode();
                }
              }
              xaxis = Axis.NAMESPACE;
            }
          }

        } finally {
          xctxt.popCurrentNode();
        }
      }

    }

    return score;
  }

}
