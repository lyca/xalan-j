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
package de.lyca.xpath.objects;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

import de.lyca.xml.dtm.DTMManager;
import de.lyca.xpath.NodeSetDTM;
import de.lyca.xpath.XPathContext;

/**
 * This class overrides the XNodeSet#object() method to provide the original
 * Node object, NodeList object, or NodeIterator.
 */
public class XNodeSetForDOM extends XNodeSet {
  static final long serialVersionUID = -8396190713754624640L;
  Object m_origObj;

  public XNodeSetForDOM(Node node, DTMManager dtmMgr) {
    m_dtmMgr = dtmMgr;
    m_origObj = node;
    final int dtmHandle = dtmMgr.getDTMHandleFromNode(node);
    setObject(new NodeSetDTM(dtmMgr));
    ((NodeSetDTM) m_obj).addNode(dtmHandle);
  }

  /**
   * Construct a XNodeSet object.
   * 
   * @param val
   *          Value of the XNodeSet object
   */
  public XNodeSetForDOM(XNodeSet val) {
    super(val);
    if (val instanceof XNodeSetForDOM) {
      m_origObj = ((XNodeSetForDOM) val).m_origObj;
    }
  }

  public XNodeSetForDOM(NodeList nodeList, XPathContext xctxt) {
    m_dtmMgr = xctxt.getDTMManager();
    m_origObj = nodeList;

    // JKESS 20020514: Longer-term solution is to force
    // folks to request length through an accessor, so we can defer this
    // retrieval... but that requires an API change.
    // m_obj=new de.lyca.xpath.NodeSetDTM(nodeList, xctxt);
    final de.lyca.xpath.NodeSetDTM nsdtm = new de.lyca.xpath.NodeSetDTM(nodeList, xctxt);
    m_last = nsdtm.getLength();
    setObject(nsdtm);
  }

  public XNodeSetForDOM(NodeIterator nodeIter, XPathContext xctxt) {
    m_dtmMgr = xctxt.getDTMManager();
    m_origObj = nodeIter;

    // JKESS 20020514: Longer-term solution is to force
    // folks to request length through an accessor, so we can defer this
    // retrieval... but that requires an API change.
    // m_obj = new de.lyca.xpath.NodeSetDTM(nodeIter, xctxt);
    final de.lyca.xpath.NodeSetDTM nsdtm = new de.lyca.xpath.NodeSetDTM(nodeIter, xctxt);
    m_last = nsdtm.getLength();
    setObject(nsdtm);
  }

  /**
   * Return the original DOM object that the user passed in. For use primarily
   * by the extension mechanism.
   * 
   * @return The object that this class wraps
   */
  @Override
  public Object object() {
    return m_origObj;
  }

  /**
   * Cast result object to a nodelist. Always issues an error.
   * 
   * @return null
   * 
   * @throws TransformerException TODO
   */
  @Override
  public NodeIterator nodeset() throws TransformerException {
    return m_origObj instanceof NodeIterator ? (NodeIterator) m_origObj : super.nodeset();
  }

  /**
   * Cast result object to a nodelist. Always issues an error.
   * 
   * @return null
   * 
   * @throws TransformerException TODO
   */
  @Override
  public NodeList nodelist() throws TransformerException {
    return m_origObj instanceof NodeList ? (NodeList) m_origObj : super.nodelist();
  }

}
