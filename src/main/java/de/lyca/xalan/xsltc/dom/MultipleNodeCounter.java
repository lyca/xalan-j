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
package de.lyca.xalan.xsltc.dom;

import de.lyca.xalan.xsltc.DOM;
import de.lyca.xalan.xsltc.runtime.AbstractTranslet;
import de.lyca.xalan.xsltc.util.IntegerArray;
import de.lyca.xml.dtm.Axis;
import de.lyca.xml.dtm.DTMAxisIterator;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public abstract class MultipleNodeCounter extends NodeCounter {
  private DTMAxisIterator _precSiblings = null;

  public MultipleNodeCounter(AbstractTranslet translet, DOM document, DTMAxisIterator iterator) {
    super(translet, document, iterator);
  }

  @Override
  public NodeCounter setStartNode(int node) {
    _node = node;
    _nodeType = _document.getExpandedTypeID(node);
    _precSiblings = _document.getAxisIterator(Axis.PRECEDINGSIBLING);
    return this;
  }

  @Override
  public String getCounter() {
    if (_value != Integer.MIN_VALUE) {
      // See Errata E24
      if (_value == 0)
        return "0";
      else if (Double.isNaN(_value))
        return "NaN";
      else if (_value < 0 && Double.isInfinite(_value))
        return "-Infinity";
      else if (Double.isInfinite(_value))
        return "Infinity";
      else
        return formatNumbers((int) _value);
    }

    final IntegerArray ancestors = new IntegerArray();

    // Gather all ancestors that do not match from pattern
    int next = _node;
    ancestors.add(next); // include self
    while ((next = _document.getParent(next)) > END && !matchesFrom(next)) {
      ancestors.add(next);
    }

    // Create an array of counters
    final int nAncestors = ancestors.cardinality();
    final int[] counters = new int[nAncestors];
    for (int i = 0; i < nAncestors; i++) {
      counters[i] = Integer.MIN_VALUE;
    }

    // Increment array of counters according to semantics
    for (int j = 0, i = nAncestors - 1; i >= 0; i--, j++) {
      final int ancestor = ancestors.at(i);

      if (matchesCount(ancestor)) {
        _precSiblings.setStartNode(ancestor);
        while ((next = _precSiblings.next()) != END) {
          if (matchesCount(next)) {
            counters[j] = counters[j] == Integer.MIN_VALUE ? 1 : counters[j] + 1;
          }
        }
        // Count the node itself
        counters[j] = counters[j] == Integer.MIN_VALUE ? 1 : counters[j] + 1;
      }
    }
    return formatNumbers(counters);
  }

  public static NodeCounter getDefaultNodeCounter(AbstractTranslet translet, DOM document, DTMAxisIterator iterator) {
    return new DefaultMultipleNodeCounter(translet, document, iterator);
  }

  static class DefaultMultipleNodeCounter extends MultipleNodeCounter {
    public DefaultMultipleNodeCounter(AbstractTranslet translet, DOM document, DTMAxisIterator iterator) {
      super(translet, document, iterator);
    }
  }
}
