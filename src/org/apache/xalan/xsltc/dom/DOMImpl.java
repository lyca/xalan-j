/*
 * @(#)$Id$
 *
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2001, Sun
 * Microsystems., http://www.sun.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 *
 */

/* Issues to resolve:
   o) All stacks in the DOMBuilder class have hardcoded length
   o) There are no namespace nodes (but namespace are handled correctly).
*/

package org.apache.xalan.xsltc.dom;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Stack;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Document;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.apache.xalan.xsltc.*;
import org.apache.xalan.xsltc.util.IntegerArray;
import org.apache.xalan.xsltc.runtime.BasisLibrary;
import org.apache.xalan.xsltc.runtime.SAXAdapter;
import org.apache.xalan.xsltc.runtime.Hashtable;

public final class DOMImpl implements DOM, Externalizable {

    // empty String for null attribute values
    private final static String EMPTYSTRING = "";

    // empty iterator to be returned when there are no children
    private final static NodeIterator EMPTYITERATOR = new NodeIterator() {
	    public NodeIterator reset() { return this; }
	    public NodeIterator setStartNode(int node) { return this; }
	    public int next() { return NULL; }
	    public void setMark() {}
	    public void gotoMark() {}
	    public int getLast() { return 0; }
	    public int getPosition() { return 0; }
	    public NodeIterator cloneIterator() { return this; }
	    public boolean isReverse() { return false; }
	    public NodeIterator resetOnce() { return this; }
	};

    // Contains the number of nodes and attribute nodes in the tree
    private int       _treeNodeLimit;
    private int       _firstAttributeNode;

    // Node-to-type, type-to-name, and name-to-type mappings
    private short[]   _type;
    private Hashtable _types = null;
    private String[]  _namesArray;

    // Tree navigation arrays
    private int[]     _parent;
    private int[]     _nextSibling;
    private int[]     _offsetOrChild; // Serves two purposes !!!
    private int[]     _lengthOrAttr;  // Serves two purposes !!!

    // Holds contents of text/comment nodes and attribute values
    private char[]    _text;

    // Namespace related stuff
    private String[]  _uriArray;
    private String[]  _prefixArray;
    private short[]   _namespace;
    private short[]   _prefix;
    private Hashtable _nsIndex = new Hashtable();

    // Tracks which textnodes are whitespaces and which are not
    private BitArray  _whitespace; // takes xml:space into acc.

    // The URI to this document
    private String    _documentURI;

    // Support for access/navigation through org.w3c.dom API
    private Node[] _nodes;
    private NodeList[] _nodeLists;
    private static NodeList EmptyNodeList;
    private static NamedNodeMap EmptyNamedNodeMap;

    /**
     * Define the origin of the document from which the tree was built
     */
    public void setDocumentURI(String uri) {
	_documentURI = uri;
    }

    /**
     * Returns the origin of the document from which the tree was built
     */
    public String getDocumentURI() {
	return(_documentURI);
    }

    public String getDocumentURI(int node) {
	return(_documentURI);
    }

    public void setupMapping(String[] names, String[] namespaces) {
	// This method only has a function in DOM adapters
    }

    /**
     * Returns 'true' if a specific node is an element (of any type)
     */
    private boolean isElement(final int node) {
	return ((node < _firstAttributeNode) && (_type[node] >= NTYPES));
    }

    /**
     * Returns the number of nodes in the tree (used for indexing)
     */
    public int getSize() {
	return(_type.length);
    }

    /**
     * Part of the DOM interface - no function here.
     */
    public void setFilter(StripFilter filter) { }


    /**
     * Returns true if node1 comes before node2 in document order
     */
    public boolean lessThan(int node1, int node2) {
	if ((node2 < _treeNodeLimit) && (node1 < node2))
	    return(true);
	else
	    return(false);
    }

    /**
     * Create an org.w3c.dom.Node from a node in the tree
     */
    public Node makeNode(int index) {
	if (_nodes == null) {
	    _nodes = new Node[_type.length];
	}
	return _nodes[index] != null
	    ? _nodes[index]
	    : (_nodes[index] = new NodeImpl(index));
    }

    /**
     * Create an org.w3c.dom.Node from a node in an iterator
     * The iterator most be started before this method is called
     */
    public Node makeNode(NodeIterator iter) {
	return makeNode(iter.next());
    }

    /**
     * Create an org.w3c.dom.NodeList from a node in the tree
     */
    public NodeList makeNodeList(int index) {
	if (_nodeLists == null) {
	    _nodeLists = new NodeList[_type.length];
	}
	return _nodeLists[index] != null
	    ? _nodeLists[index]
	    : (_nodeLists[index] = new NodeListImpl(index));
    }

    /**
     * Create an org.w3c.dom.NodeList from a node iterator
     * The iterator most be started before this method is called
     */
    public NodeList makeNodeList(NodeIterator iter) {
	return new NodeListImpl(iter);
    }

    /**
     * Create an empty org.w3c.dom.NodeList
     */
    private NodeList getEmptyNodeList() {
	return EmptyNodeList != null
	    ? EmptyNodeList
	    : (EmptyNodeList = new NodeListImpl(new int[0]));
    }

    /**
     * Create an empty org.w3c.dom.NamedNodeMap
     */
    private NamedNodeMap getEmptyNamedNodeMap() {
	return EmptyNamedNodeMap != null
	    ? EmptyNamedNodeMap
	    : (EmptyNamedNodeMap = new NamedNodeMapImpl(new int[0]));
    }

    /**
     * Exception thrown by methods in inner classes implementing
     * various org.w3c.dom interfaces (below)
     */
    private final class NotSupportedException extends DOMException {
	public NotSupportedException() {
	    super(NOT_SUPPORTED_ERR, "modification not supported");
	}
    }

    /**************************************************************
     * Implementation of org.w3c.dom.NodeList
     */
    private final class NodeListImpl implements NodeList {
	private final int[] _nodes;

	public NodeListImpl(int node) {
	    _nodes = new int[1];
	    _nodes[0] = node;
	}

	public NodeListImpl(int[] nodes) {
	    _nodes = nodes;
	}
                  
	public NodeListImpl(NodeIterator iter) {
	    final IntegerArray list = new IntegerArray();
	    int node;
	    while ((node = iter.next()) != NodeIterator.END) {
		list.add(node);
	    }         
	    _nodes = list.toIntArray();         
	}

	public int getLength() {
	    return _nodes.length;
	}
                  
	public Node item(int index) {
	    return makeNode(_nodes[index]);
	}
    }

                  
    /**************************************************************
     * Implementation of org.w3c.dom.NamedNodeMap
     */
    private final class NamedNodeMapImpl implements NamedNodeMap {

	private final int[] _nodes;
		
	public NamedNodeMapImpl(int[] nodes) {
	    _nodes = nodes;
	}
		
	public int getLength() {
	    return _nodes.length;
	}
		
	public Node getNamedItem(String name) {
	    for (int i = 0; i < _nodes.length; i++) {
		if (name.equals(getNodeName(_nodes[i]))) {
		    return makeNode(_nodes[i]);
		}
	    }
	    return null;
	}
		
	public Node item(int index) {
	    return makeNode(_nodes[index]);
	}
		
	public Node removeNamedItem(String name) {
	    throw new NotSupportedException();
	}
		
	public Node setNamedItem(Node node) {
	    throw new NotSupportedException();
	}

	public Node getNamedItemNS(String uri, String local) {
	    return(getNamedItem(uri+':'+local));
	}

	public Node setNamedItemNS(Node node) {
	    throw new NotSupportedException();
	}

	public Node removeNamedItemNS(String uri, String local) {
	    throw new NotSupportedException();
	}

    }


    /**************************************************************
     * Implementation of org.w3c.dom.Node
     */
    private final class NodeImpl implements Node {

	private final int _index;

	public NodeImpl(int index) {
	    _index = index;
	}

	public short getNodeType() {
	    switch (_type[_index]) {
	    case ROOT:
		return Node.DOCUMENT_NODE;
		
	    case TEXT:
		return Node.TEXT_NODE;
		
	    case PROCESSING_INSTRUCTION:
		return Node.PROCESSING_INSTRUCTION_NODE;
		
	    case COMMENT:
		return Node.COMMENT_NODE;
		
	    default:
		return _index < _firstAttributeNode
		    ? Node.ELEMENT_NODE : Node.ATTRIBUTE_NODE;
	    }
	}
		
	public Node getParentNode() {
	    final int parent = getParent(_index);
	    return parent > NULL ? makeNode(parent) : null;
	}
		
	public Node appendChild(Node node) throws DOMException {
	    throw new NotSupportedException();
	}
		
	public Node cloneNode(boolean deep) {
	    throw new NotSupportedException();
	}
		
	public NamedNodeMap getAttributes() {
	    if (getNodeType() == Node.ELEMENT_NODE) {
		int attribute = _lengthOrAttr[_index];
		if (attribute != NULL) {
		    final IntegerArray attributes = new IntegerArray(4);
		    do {
			attributes.add(attribute);
		    }
		    while ((attribute = _nextSibling[attribute]) != 0);
		    return new NamedNodeMapImpl(attributes.toIntArray());
		}
		else {
		    return getEmptyNamedNodeMap();
		}
	    }
	    else {
		return null;
	    }
	}

	public NodeList getChildNodes() {
	    if (hasChildNodes()) {
		final IntegerArray children = new IntegerArray(8);
		int child = _offsetOrChild[_index];
		do {
		    children.add(child);
		}
		while ((child = _nextSibling[child]) != 0);
		return new NodeListImpl(children.toIntArray());
	    }
	    else {
		return getEmptyNodeList();
	    }
	}
		
	public Node getFirstChild() {
	    return hasChildNodes()
		? makeNode(_offsetOrChild[_index])
		: null;
	}
		
	public Node getLastChild() {
	    return hasChildNodes()
		? makeNode(lastChild(_index))
		: null;
	}
		
	public Node getNextSibling() {
	    final int next = _nextSibling[_index];
	    return next != 0 ? makeNode(next) : null;
	}
		
	public String getNodeName() {
	    switch (_type[_index]) {
	    case ROOT:
		return "#document";
	    case TEXT:
		return "#text";
	    case PROCESSING_INSTRUCTION:
		return "#pi";
	    case COMMENT:
		return "#comment";
	    default:
		return DOMImpl.this.getNodeName(_index);
	    }
	}
		
	public String getNodeValue() throws DOMException {
	    return DOMImpl.this.getNodeValue(_index);
	}
		
	public Document getOwnerDocument() {
	    return null;
	}
		
	//??? how does it work with attributes
	public Node getPreviousSibling() {
	    int node = _parent[_index];
	    if (node > NULL) {
		int prev = -1;
		node = _offsetOrChild[node];
		while (node != _index) {
		    node = _nextSibling[prev = node];
		}
		if (prev != -1) {
		    return makeNode(prev);
		}
	    }
	    return null;
	}
		
	public boolean hasChildNodes() {
	    switch (getNodeType()) {
	    case Node.ELEMENT_NODE:
	    case Node.DOCUMENT_NODE:
		return _offsetOrChild[_index] != 0;

	    default:
		return false;
	    }
	}
		
	public Node insertBefore(Node n1, Node n2) throws DOMException {
	    throw new NotSupportedException();
	}
		
	public Node removeChild(Node n) throws DOMException {
	    throw new NotSupportedException();
	}
		
	public Node replaceChild(Node n1, Node n2) throws DOMException {
	    throw new NotSupportedException();
	}
		
	public void setNodeValue(String s) throws DOMException {
	    throw new NotSupportedException();
	}

	public void normalize() {
	    throw new NotSupportedException();
	}

	public boolean isSupported(String feature, String version) {
	    return false;
	}

	public String getNamespaceURI() {
	    return _uriArray[_namespace[_type[_index] - NTYPES]];
	}

	public String getPrefix() {
	    return _prefixArray[_prefix[_index]];
	}

	public void setPrefix(String prefix) {
	    throw new NotSupportedException();
	}

	public String getLocalName() {
	    return DOMImpl.this.getLocalName(_index);
	}

	public boolean hasAttributes() {
	    return (_lengthOrAttr[_index] != NULL);
	}

    }

    // A single copy (cache) of ElementFilter
    private Filter _elementFilter;

    /**
     * Returns a filter that lets only element nodes through
     */
    private Filter getElementFilter() {
	if (_elementFilter == null) {
	    _elementFilter = new Filter() {
		    public boolean test(int node) {
			return isElement(node);
		    }
		};
	}
	return _elementFilter;
    }

    /**
     * Implementation of a filter that only returns nodes of a
     * certain type (instanciate through getTypeFilter()).
     */
    private final class TypeFilter implements Filter {
	private final int _nodeType;
                  
	public TypeFilter(int type) {
	    _nodeType = type;
	}
                  
	public boolean test(int node) {
	    return _type[node] == _nodeType;
	}
    }

    /**
     * Returns a node type filter (implementation of Filter)
     */
    public Filter getTypeFilter(int type) {
	return new TypeFilter(type);
    }


    /**************************************************************
     * Iterator that returns all children of a given node
     */
    private final class ChildrenIterator extends NodeIteratorBase {
	// child to return next
	private int _currentChild;
	private int _last = -1;

	public NodeIterator setStartNode(final int node) {
	    _last = -1;
	    if (_isRestartable) {
		_currentChild = hasChildren(node)
		    ? _offsetOrChild[_startNode = node] : END;
		return resetPosition();
	    }
	    return this;
	}

	public int next() {
	    final int node = _currentChild;
	    _currentChild = _nextSibling[node];
	    return returnNode(node);
	}

	public void setMark() {
	    _markedNode = _currentChild;
	}

	public void gotoMark() {
	    _currentChild = _markedNode;
	}

	public int getLast() {
	    if (_last == -1) {
		_last = 1;
		int node = _offsetOrChild[_startNode];
		while ((node = _nextSibling[node]) != END) _last++;
	    }
	    return(_last);
	}

    } // end of ChildrenIterator


    /**************************************************************
     * Iterator that returns the parent of a given node
     */
    private final class ParentIterator extends NodeIteratorBase {
	// candidate parent node
	private int _node;
	private int _nodeType = -1;
         
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_node = _parent[_startNode = node];
		return resetPosition();
	    }
	    return this;
	}
             
	public NodeIterator setNodeType(final int type) {
	    _nodeType = type;
	    return this;
	}

	public int next() {
	    int result = _node;
	    if ((_nodeType != -1) && (_type[_node] != _nodeType))
		result = END;
	    else
		result = _node;
	    _node = END;
	    return returnNode(result);
	}

	public void setMark() {
	    _markedNode = _node;
	}

	public void gotoMark() {
	    _node = _markedNode;
	}
    } // end of ParentIterator


    /**************************************************************
     * Iterator that returns children of a given type for a given node.
     * The functionality chould be achieved by putting a filter on top
     * of a basic child iterator, but a specialised iterator is used
     * for efficiency (both speed and size of translet).
     */
    private final class TypedChildrenIterator extends NodeIteratorBase {
	private final int _nodeType;
	// node to consider next
	private int _currentChild;
         
	public TypedChildrenIterator(int nodeType) {
	    _nodeType = nodeType;
	}

	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_currentChild = hasChildren(node)
		    ? _offsetOrChild[_startNode = node] : END;
		return resetPosition();
	    }
	    return this;
	}

	public NodeIterator reset() {
	    if (hasChildren(_startNode))
		_currentChild = _offsetOrChild[_startNode];
	    else
		_currentChild = END;
	    return this;
	}

	public int next() {
	    for (int node = _currentChild; node != END;
		 node = _nextSibling[node]) {
		if (_type[node] == _nodeType) {
		    _currentChild = _nextSibling[node];
		    return returnNode(node);
		}
	    }
	    return END;
	}

	public void setMark() {
	    _markedNode = _currentChild;
	}

	public void gotoMark() {
	    _currentChild = _markedNode;
	}
    } // end of TypedChildrenIterator


    /**************************************************************
     * Iterator that returns children within a given namespace for a
     * given node. The functionality chould be achieved by putting a
     * filter on top of a basic child iterator, but a specialised
     * iterator is used for efficiency (both speed and size of translet).
     */
    private final class NamespaceChildrenIterator extends NodeIteratorBase {
	private final int _nsType;
	private int _currentChild;
         
	public NamespaceChildrenIterator(final int type) {
	    _nsType = type;
	}

	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_currentChild = hasChildren(node)
		    ? _offsetOrChild[_startNode = node] : END;
		return resetPosition();
	    }
	    return this;
	}

	public int next() {
	    for (int node = _currentChild; node != END; 
		 node = _nextSibling[node]) {
		if (getNamespaceType(node) == _nsType) {
		    _currentChild = _nextSibling[node];
		    return returnNode(node);
		}
	    }
	    return END;
	}

	public void setMark() {
	    _markedNode = _currentChild;
	}

	public void gotoMark() {
	    _currentChild = _markedNode;
	}

    } // end of TypedChildrenIterator


    /**************************************************************
     * Iterator that returns attributes within a given namespace for a node.
     */
    private final class NamespaceAttributeIterator extends NodeIteratorBase {

	private final int _nsType;
	private int _attribute;
         
	public NamespaceAttributeIterator(int nsType) {
	    super();
	    _nsType = nsType;
	}
                  
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		for (node = _lengthOrAttr[_startNode = node];
		     node != NULL && getNamespaceType(node) != _nsType;
		     node = _nextSibling[node]);
		_attribute = node;
		return resetPosition();
	    }
	    return this;
	}
                  
	public int next() {
	    final int save = _attribute;
	    int node = save;
	    _attribute = _nextSibling[_attribute];
	    for (node = _lengthOrAttr[_startNode = node];
		 node != NULL && getNamespaceType(node) != _nsType;
		 node = _nextSibling[node]);
	    _attribute = node;

	    return returnNode(save);
	}

	public void setMark() {
	    _markedNode = _attribute;
	}

	public void gotoMark() {
	    _attribute = _markedNode;
	}
         
    } // end of TypedChildrenIterator


    /**************************************************************
     * Iterator that returns all siblings of a given node.
     */
    private class FollowingSiblingIterator extends NodeIteratorBase {
	private int _node;
         
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_node = _startNode = node;
		return resetPosition();
	    }
	    return this;
	}
                  
	public int next() {
	    return returnNode(_node = _nextSibling[_node]);
	}

	public void setMark() {
	    _markedNode = _node;
	}

	public void gotoMark() {
	    _node = _markedNode;
	}
    } // end of FollowingSiblingIterator


    /**************************************************************
     * Iterator that returns all following siblings of a given node.
     */
    private final class TypedFollowingSiblingIterator
	extends FollowingSiblingIterator {
	private final int _nodeType;

	public TypedFollowingSiblingIterator(int type) {
	    _nodeType = type;
	}
         
	public int next() {
	    int node;
	    while ((node = super.next()) != NULL) {
		if (_type[node] == _nodeType) return node;
	    }
	    return END;
	}

    } // end of TypedFollowingSiblingIterator


    /**************************************************************
     * Iterator that returns attribute nodes (of what nodes?)
     */
    private final class AttributeIterator extends NodeIteratorBase {
	private int _attribute;
         
	// assumes caller will pass element nodes
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_attribute = isElement(node)
		    ? _lengthOrAttr[_startNode = node]
		    : NULL;
		return resetPosition();
	    }
	    return this;
	}
                  
	public int next() {
	    final int node = _attribute;
	    _attribute = _nextSibling[_attribute];
	    return returnNode(node);
	}

	public void setMark() {
	    _markedNode = _attribute;
	}

	public void gotoMark() {
	    _attribute = _markedNode;
	}
    } // end of AttributeIterator


    /**************************************************************
     * Iterator that returns attribute nodes of a given type
     */
    private final class TypedAttributeIterator extends NodeIteratorBase {
	private final int _nodeType;
	private int _attribute;
         
	public TypedAttributeIterator(int nodeType) {
	    _nodeType = nodeType;
	}
                  
	// assumes caller will pass element nodes
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		for (node = _lengthOrAttr[_startNode = node];
		     node != NULL && _type[node] != _nodeType;
		     node = _nextSibling[node]);
		_attribute = node;
		return resetPosition();
	    }
	    return this;
	}
                  
	public int next() {
	    final int node = _attribute;
	    _attribute = NULL;         // singleton iterator
	    return returnNode(node);
	}

	public void setMark() {
	    _markedNode = _attribute;
	}

	public void gotoMark() {
	    _attribute = _markedNode;
	}
    } // end of TypedAttributeIterator


    /**************************************************************
     * Iterator that returns preceding siblings of a given node
     */
    private class PrecedingSiblingIterator extends NodeIteratorBase {
	private int _start;
	private int _node;
         
	public boolean isReverse() {
	    return true;
	}
         
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_node = _offsetOrChild[_parent[_startNode = _start = node]];
		return resetPosition();
	    }
	    return this;
	}
                  
	public int next() {
	    if (_node == _start) {
		return NULL;
	    }
	    else {
		final int node = _node;
		_node = _nextSibling[node];
		return returnNode(node);
	    }
	}

	public void setMark() {
	    _markedNode = _node;
	}

	public void gotoMark() {
	    _node = _markedNode;
	}

    } // end of PrecedingSiblingIterator


    /**************************************************************
     * Iterator that returns preceding siblings of a given type for
     * a given node
     */
    private final class TypedPrecedingSiblingIterator
	extends PrecedingSiblingIterator {
	private final int _nodeType;

	public TypedPrecedingSiblingIterator(int type) {
	    _nodeType = type;
	}
         
	public int next() {
	    int node;
	    while ((node = super.next()) != NULL && _type[node] != _nodeType) {
	    }
	    return returnNode(node);
	}

    } // end of PrecedingSiblingIterator


    /**************************************************************
     * Iterator that returns preceding nodes of a given node.
     * This includes the node set {root+1, start-1}, but excludes
     * all ancestors.
     */
    private class PrecedingIterator extends NodeIteratorBase {

	private int _node = 0;
	private int[] _stack = new int[8];
	private int _sp = 0;
	private int _spStart = 0;
	private int _spMark = 0;

	public boolean isReverse() {
	    return true;
	}
         
	public NodeIterator cloneIterator() {
	    try {
		_isRestartable = false;
		final PrecedingIterator clone = 
		    (PrecedingIterator)super.clone();
		return clone.reset();
	    }
	    catch (CloneNotSupportedException e) {
		BasisLibrary.runTimeError("Iterator clone not supported.");
		return null;
	    }
	}
         
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_startNode = node;
		_node = ROOTNODE;
		int parent = node;
		while ((parent = _parent[parent]) > ROOTNODE) {
		    if (_sp == _stack.length) {
			final int[] stack = new int[_sp + 4];
			System.arraycopy(_stack, 0, stack, 0, _sp);
			_stack = stack;
		    }
		    _stack[_sp++] = parent;
		}
		_sp--;
		_spStart = _sp;
		return resetPosition();
	    }
	    return this;
	}
                  
	public int next() {
	    // Advance node index and check if all nodes have been returned.
	    while (++_node < _startNode) {
		// Check if we reached one of the base node's ancestors
		if ((_sp < 0) || (_node < _stack[_sp])) return(_node);
		// Anvance past the next ancestor node
		_sp--;
	    }
	    return(NULL);
	}

	// redefine NodeIteratorBase's reset
	public NodeIterator reset() {
	    _node = ROOTNODE;
	    _spStart = _sp;
	    return resetPosition();
	}

	public void setMark() {
	    _markedNode = _node;
	    _spMark = _sp;
	}

	public void gotoMark() {
	    _node = _markedNode;
	    _sp = _spMark;
	}
    } // end of PrecedingIterator


    /**************************************************************
     * Iterator that returns preceding nodes of agiven type for a
     * given node. This includes the node set {root+1, start-1}, but
     * excludes all ancestors.
     */
    private final class TypedPrecedingIterator extends PrecedingIterator {
	private final int _nodeType;

	public TypedPrecedingIterator(int type) {
	    _nodeType = type;
	}
         
	public int next() {
	    int node;
	    while ((node = super.next()) != NULL && _type[node] != _nodeType) {
	    }
	    return node;
	}

    } // end of TypedPrecedingIterator


    /**************************************************************
     * Iterator that returns following nodes of for a given node.
     */
    private class FollowingIterator extends NodeIteratorBase {
	//  _node precedes search for next
	protected int _node;
                  
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_startNode = node;
		// find rightmost descendant (or self)
		int current;
		while ((node = lastChild(current = node)) != NULL) {
		}
		_node = current;
		// _node precedes possible following(node) nodes
		return resetPosition();
	    }
	    return this;
	}
                      
	public int next() {
	    final int node = _node + 1;
	    return node < _treeNodeLimit ? returnNode(_node = node) : NULL;
	}

	public void setMark() {
	    _markedNode = _node;
	}

	public void gotoMark() {
	    _node = _markedNode;
	}
    } // end of FollowingIterator


    /**************************************************************
     * Iterator that returns following nodes of a given type for a given node.
     */
    private final class TypedFollowingIterator extends FollowingIterator {
	private final int _nodeType;

	public TypedFollowingIterator(int type) {
	    _nodeType = type;
	}
                  
	public int next() {
	    int node;
	    while ((node = super.next()) != NULL) {
		if (_type[node] == _nodeType) return node;
	    }
	    return END;
	}
    } // end of TypedFollowingIterator


    /**************************************************************
     * Iterator that returns the ancestors of a given node.
     * The nodes are returned in reverse document order, so you
     * get the context node (or its parent node) first, and the
     * root node in the very, very end.
     */         
    private class AncestorIterator extends NodeIteratorBase {

	protected int _index;

	public final boolean isReverse() {
	    return true;
	}

	public int getLast() {
	    return(ROOT);
	}
         
	public NodeIterator cloneIterator() {
	    _isRestartable = false;         // must set to false for any clone
	    try {
		final AncestorIterator clone = (AncestorIterator)super.clone();
		clone._startNode = _startNode;
		return clone.reset();
	    } catch (CloneNotSupportedException e) {
		BasisLibrary.runTimeError("Iterator clone not supported.");
		return null;
	    }
	}
                  
	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		if (_includeSelf)
		    _startNode = node;
		else
		    _startNode = _parent[node];
		_index = _startNode;
		return resetPosition();
	    }
	    return this;
	}

	public NodeIterator reset() {
	    _index = _startNode;
	    return resetPosition();
	}
                  
	public int next() {
	    if (_index >= 0) {
		int bob = _index;
		if (_index == 0)
		    _index = -1;
		else
		    _index = _parent[_index];
		return returnNode(bob);
	    }
	    return(NULL);
	}

	public void setMark() {
	    _markedNode = _index;
	}

	public void gotoMark() {
	    _index = _markedNode;
	}
    } // end of AncestorIterator


    /**************************************************************
     * Typed iterator that returns the ancestors of a given node.
     */             
    private final class TypedAncestorIterator extends AncestorIterator {

	private final int _nodeType;
                  
	public TypedAncestorIterator(int type) {
	    _nodeType = type;
	}

	public int next() {
	    int node;
	    while ((node = super.next()) != NULL) {
		if (_type[node] == _nodeType)
		    return returnNode(node);
	    }
	    return(NULL);
	}

	public int getLast() {
	    int last = _index;
	    int curr = _index;

	    while (curr >= 0) {
		if (curr == 0)
		    curr = -1;
		else {
		    curr = _parent[curr];
		    if (_type[curr] == _nodeType)
			last = curr;
		}
	    }
	    return(last);
	}

    } // end of TypedAncestorIterator


    /**************************************************************
     * Iterator that returns the descendants of a given node.
     */             
    private class DescendantIterator extends NodeIteratorBase {
	//  _node precedes search for next
	protected int _node;
	// first node outside descendant range
	protected int _limit;

	public NodeIterator setStartNode(int node) {
	    _startNode = node;
	    if (_isRestartable) {
		_node = _startNode = _includeSelf ? node - 1 : node;
		// no descendents if no children
		if (hasChildren(node) == false) {
		    // set _limit to match next()'s criteria for end
		    _limit = node + 1;
		}
		// find leftmost descendant of next sibling
		else if ((node = _nextSibling[node]) == 0) {
		    // no next sibling, array end is the limit
		    _limit = _treeNodeLimit;
		}
		else {
		    _limit = leftmostDescendant(node);
		}
		return resetPosition();
	    }
	    return this;
	}

	public int next() {
	    if (++_node >= (_limit))
		return(NULL);
	    else
		return(returnNode(_node));
	}

	public void setMark() {
	    _markedNode = _node;
	}

	public void gotoMark() {
	    _node = _markedNode;
	}

    } // end of DescendantIterator


    /**************************************************************
     * Typed iterator that returns the descendants of a given node.
     */             
    private final class TypedDescendantIterator extends DescendantIterator {
	private final int _nodeType;
                  
	public TypedDescendantIterator(int nodeType) {
	    _nodeType = nodeType;
	}
                  
	public int next() {
	    final int limit = _limit;
	    final int type = _nodeType;
	    int node = _node + 1; // start search w/ next
	    // while condition == which nodes to skip
	    // iteration stops when at end or node w/ desired type
	    while (node < limit && _type[node] != type) {
		++node;
	    }
	    return node < limit ? returnNode(_node = node) : NULL;
	}

    } // end of TypedDescendantIterator


    /**************************************************************
     * Iterator that returns the descendants of a given node.
     */             
    private class NthDescendantIterator extends DescendantIterator {

	int _pos;

	public NthDescendantIterator(int pos) {
	    _pos = pos;
	    _limit = _treeNodeLimit;
	}

	// The start node of this iterator is always the root!!!
	public NodeIterator setStartNode(int node) {
	    NodeIterator iterator = super.setStartNode(1);
	    _limit = _treeNodeLimit;
	    return iterator;
	}

	public int next() {
	    int node;
	    while ((node = super.next()) != END) {
		int parent = _parent[node];
		int child = _offsetOrChild[parent];
		int pos = 0;

		do {
		    if (isElement(child)) pos++;
		} while ((pos < _pos) && (child = _nextSibling[child]) != 0);
		if (node == child) return node;
	    }
	    return(END);
	}

    } // end of NthDescendantIterator


    /**************************************************************
     * Iterator that returns a given node only if it is of a given type.
     */             
    private final class TypedSingletonIterator extends SingletonIterator {
	private final int _nodeType;

	public TypedSingletonIterator(int nodeType) {
	    _nodeType = nodeType;
	}
         
	public int next() {
	    final int result = super.next();
	    return _type[result] == _nodeType ? result : NULL;
	}
    } // end of TypedSingletonIterator


    /**************************************************************
     * Iterator to put on top of other iterators. It will take the
     * nodes from the underlaying iterator and return all but
     * whitespace text nodes. The iterator needs to be a supplied
     * with a filter that tells it what nodes are WS text.
     */             
    private final class StrippingIterator extends NodeIteratorBase {

	private static final int USE_PREDICATE  = 0;
	private static final int STRIP_SPACE    = 1;
	private static final int PRESERVE_SPACE = 2;

	private StripFilter _filter = null;
	private short[] _mapping = null;
	private final NodeIterator _source;
	private boolean _children = false;
	private int _action = USE_PREDICATE;
	private int _last = -1;

	public StrippingIterator(NodeIterator source,
				 short[] mapping,
				 StripFilter filter) {

	    _filter = filter;
	    _mapping = mapping;
	    _source = source;

	    if (_source instanceof ChildrenIterator ||
		_source instanceof TypedChildrenIterator) {
		_children = true;
	    }
	}

	public NodeIterator setStartNode(int node) {
	    if (_children) {
		if (_filter.stripSpace((DOM)DOMImpl.this, node,
				       _mapping[_type[node]]))
		    _action = STRIP_SPACE;
		else
		    _action = PRESERVE_SPACE;
	    }

	    _source.setStartNode(node);
	    //return resetPosition();
	    return(this);
	}
    
	public int next() {
	    int node;
	    while ((node = _source.next()) != END) {
		switch(_action) {
		case STRIP_SPACE:
		    if (_whitespace.getBit(node)) continue;
		    // fall through...
		case PRESERVE_SPACE:
		    return returnNode(node);
		case USE_PREDICATE:
		default:
		    if (_whitespace.getBit(node) &&
			_filter.stripSpace((DOM)DOMImpl.this, node,
					   _mapping[_type[_parent[node]]]))
			continue;
		    return returnNode(node);
		}
	    }
	    return END;
	}

	public NodeIterator reset() {
	    _source.reset();
	    return this;
	}

	public void setMark() {
	    _source.setMark();
	}

	public void gotoMark() {
	    _source.gotoMark();
	}

	public int getLast() {
	    // Return chached value (if we have it)
	    if (_last != -1) return _last;

	    int count = getPosition();
	    int node;

	    _source.setMark();
	    while ((node = _source.next()) != END) {
		switch(_action) {
		case STRIP_SPACE:
		    if (_whitespace.getBit(node))
			continue;
		    // fall through...
		case PRESERVE_SPACE:
		    count++;
		    break;
		case USE_PREDICATE:
		default:
		    if (_whitespace.getBit(node) &&
			_filter.stripSpace((DOM)DOMImpl.this, node,
					   _mapping[_type[_parent[node]]]))
			continue;
		    else
			count++;
		}
	    }
	    _source.gotoMark();
	    _last = count;
	    return(count);
	}

    } // end of StrippingIterator

    public NodeIterator strippingIterator(NodeIterator iterator,
					  short[] mapping,
					  StripFilter filter) {
	return(new StrippingIterator(iterator, mapping, filter));
    }

    /**************************************************************
     * This is a specialised iterator for predicates comparing node or
     * attribute values to variable or parameter values.
     */
    private final class NodeValueIterator extends NodeIteratorBase {

	private NodeIterator _source;
	private String _value;
	private boolean _op;
	private final boolean _isReverse;
	private int _returnType = RETURN_PARENT;

	public NodeValueIterator(NodeIterator source, int returnType,
				 String value, boolean op) {
	    _source = source;
	    _returnType = returnType;
	    _value = value;
	    _op = op;
	    _isReverse = source.isReverse();
	}

	public boolean isReverse() {
	    return _isReverse;
	}
    
	public NodeIterator cloneIterator() {
	    try {
		NodeValueIterator clone = (NodeValueIterator)super.clone();
		clone._isRestartable = false;
		clone._source = _source.cloneIterator();
		clone._value = _value;
		clone._op = _op;
		return clone.reset();
	    }
	    catch (CloneNotSupportedException e) {
		BasisLibrary.runTimeError("Iterator clone not supported."); 
		return null;
	    }
	}
    
	public NodeIterator reset() {
	    _source.reset();
	    return resetPosition();
	}

	public int next() {

	    int node;
	    while ((node = _source.next()) != END) {
		String val = getNodeValue(node);
		if (_value.equals(val) == _op) {
		    if (_returnType == RETURN_CURRENT)
			return returnNode(node);
		    else
			return returnNode(_parent[node]);
		}
	    }
	    return END;
	}

	public NodeIterator setStartNode(int node) {
	    if (_isRestartable) {
		_source.setStartNode(_startNode = node); 
		return resetPosition();
	    }
	    return this;
	}

	public void setMark() {
	    _source.setMark();
	}

	public void gotoMark() {
	    _source.gotoMark();
	}
    }                       

    public NodeIterator getNodeValueIterator(NodeIterator iterator, int type,
					     String value, boolean op) {
	return(new NodeValueIterator(iterator, type, value, op));
    }

    /**************************************************************
     * Iterator that assured that a single node is only returned once
     * and that the nodes are returned in document order.
     */             
    private final class OrderedIterator extends NodeIteratorBase {

	private BitArray  _nodes = null;
	private int       _save = 0;
	private int       _mark = 0;
	private int       _start = 0;
	private int       _node = -1;
	private int       _last = 0;

	public OrderedIterator(NodeIterator source, int node) {
	    _nodes = new BitArray(_treeNodeLimit);
	    source.setStartNode(node);
	    while ((_node = source.next()) != END) {
		if (_start == -1) _start = _node;
		_last = _node;
		_nodes.setBit(_node);
	    }
	    _node = -1;
	}
 
	public int next() {
	    while ((_node < _treeNodeLimit) && (!_nodes.getBit(++_node))) ;
	    if (_node >= _treeNodeLimit) return(END);
	    return returnNode(_node);
	}

	public NodeIterator reset() {
	    _node = _start - 1;
	    return(this);
	}

	public int getLast() {
	    return(_last);
	}
         
	public void setMark() {
	    _save = _node;
	}

	public void gotoMark() {
	    _node = _save;
	}

	public NodeIterator setStartNode(int start) {
	    _start = start;
	    return((NodeIterator)this);
	}

	public boolean isReverse() {
	    return(false);
	}

	public NodeIterator cloneIterator() {
	    return((NodeIterator)this);
	}

    } // end of OrderedIterator


    /**
     * Encapsulates an iterator in an OrderedIterator to ensure node order
     */
    public NodeIterator orderNodes(NodeIterator source, int node) {
	return new OrderedIterator(source, node);
    }

    /**
     * Returns the leftmost descendant of a node (bottom left in sub-tree)
     */
    private int leftmostDescendant(int node) {
	int current;
	while (_type[current = node] >= NTYPES 
	       && (node = _offsetOrChild[node]) != NULL) {
	}
	return current;
    }

    /**
     * Returns index of last child or 0 if no children
     */
    private int lastChild(int node) {
	if (isElement(node) || node == ROOTNODE) {
	    int child;
	    if ((child = _offsetOrChild[node]) != NULL) {
		while ((child = _nextSibling[node = child]) != NULL) {
		}
		return node;
	    }
	}
	return 0;
    }

    /**
     * Returns the parent of a node
     */
    public int getParent(final int node) {
	return _parent[node];
    }

    /**
     * Returns a node's position amongst other nodes of the same type
     */
    public int getTypedPosition(int type, int node) {
	// Just return the basic position if no type is specified
	if (type == -1) type = _type[node];

	// Initialize with the first sbiling of the current node
	int match = 0;
	int curr  = _offsetOrChild[_parent[node]];
	if (_type[curr] == type) match++;

	// Then traverse all other siblings up until the current node
	while (curr != node) {
	    curr = _nextSibling[curr];
	    if (_type[curr] == type) match++;
	}

	// And finally return number of matches
	return match;         
    }

    /**
     * Returns an iterator's last node of a given type
     */
    public int getTypedLast(int type, int node) {
	// Just return the basic position if no type is specified
	if (type == -1) type = _type[node];

	// Initialize with the first sbiling of the current node
	int match = 0;
	int curr  = _offsetOrChild[_parent[node]];
	if (_type[curr] == type) match++;

	// Then traverse all other siblings up until the very last one
	while (curr != NULL) {
	    curr = _nextSibling[curr];
	    if (_type[curr] == type) match++;
	}

	return match;         
    }

    /**
     * Returns singleton iterator containg the document root
     * Works for them main document (mark == 0)
     */
    public NodeIterator getIterator() {
	return new SingletonIterator(ROOTNODE);
    }

    /**
     * Returns the type of a specific node
     */
    public int getType(final int node) {
	if (node >= _type.length)
	    return(0);
	else
	    return _type[node];
    }
    
    /**
     * Returns the namespace type of a specific node
     */
    public int getNamespaceType(final int node) {
	final int type = _type[node];
	if (type >= NTYPES)
	    return(_namespace[type-NTYPES]);
	else
	    return(0); // default namespace
    }

    /**
     * Returns the node-to-type mapping array
     */
    public short[] getTypeArray() {
	return _type;
    }

    /**
     * Returns the (String) value of any node in the tree
     */
    public String getNodeValue(final int node) {
	if ((node == NULL) || (node > _treeNodeLimit)) return EMPTYSTRING;
	switch(_type[node]) {
	case ROOT:
	    return getNodeValue(_offsetOrChild[node]);
	case TEXT:
	case COMMENT:
	case PROCESSING_INSTRUCTION:
	    return makeStringValue(node);
	default:
	    if (node < _firstAttributeNode)
		return getElementValue(node); // element string value
	    else
		return makeStringValue(node); // attribute value
	}
    }

    private String getLocalName(int node) {
	final int type = _type[node] - NTYPES;
	final String qname = _namesArray[type];
	final String uri = _uriArray[_namespace[type]];

	if (uri != null) {
	    final int len = uri.length();
	    if (len > 0) return qname.substring(len+1);
	}
	return qname;
    }

    /**
     * Sets up a translet-to-dom type mapping table
     */
    private Hashtable setupMapping(String[] namesArray) {
	final int nNames = namesArray.length;
	final Hashtable types = new Hashtable(nNames);
	for (int i = 0; i < nNames; i++) {
	    types.put(namesArray[i], new Integer(i + NTYPES));
	}
	return types;
    }

    /**
     * Returns the internal type associated with an expaneded QName
     */
    public int getGeneralizedType(final String name) {
	final Integer type = (Integer)_types.get(name);
	if (type == null) {
	    // memorize default type
	    final int code = name.charAt(0) == '@' ? ATTRIBUTE : ELEMENT;
	    _types.put(name, new Integer(code));
	    return code;
	}
	else {
	    return type.intValue();
	}
    }

    /**
     * Get mapping from DOM element/attribute types to external types
     */
    public short[] getMapping(String[] names) {
	int i;
	final int namesLength = names.length;
	final int mappingLength = _namesArray.length + NTYPES;
	final short[] result = new short[mappingLength];

	// primitive types map to themselves
	for (i = 0; i < NTYPES; i++)
	    result[i] = (short)i;

	// extended types initialized to "beyond caller's types"
	// unknown element or Attr
	for (i = NTYPES; i < mappingLength; i++) {
	    final int type = i - NTYPES;
	    final String name = _namesArray[type];
	    final String uri = _uriArray[_namespace[type]];
	    int len = 0;
	    if (uri != null) {
		len = uri.length();
		if (len > 0) len++;
	    }
	    if (name.charAt(len) == '@')
		result[i] = (short)ATTRIBUTE;
	    else
		result[i] = (short)ELEMENT;
	}

	// actual mapping of caller requested names
	for (i = 0; i < namesLength; i++) {
	    result[getGeneralizedType(names[i])] = (short)(i + NTYPES);
	}
             
	return(result);

    }

    /**
     * Get mapping from external element/attribute types to DOM types
     */
    public short[] getReverseMapping(String[] names) {
	int i;
	final short[] result = new short[names.length + NTYPES];
	// primitive types map to themselves
	for (i = 0; i < NTYPES; i++) {
	    result[i] = (short)i;
	}
	// caller's types map into appropriate dom types
	for (i = 0; i < names.length; i++) {
	    result[i + NTYPES] = (short)getGeneralizedType(names[i]);
	    if (result[i + NTYPES] == ELEMENT)
		result[i + NTYPES] = NO_TYPE;
	}
	return(result);
    }

    /**
     * Get mapping from DOM namespace types to external namespace types
     */
    public short[] getNamespaceMapping(String[] namespaces) {
	int i;
	final int nsLength = namespaces.length;
	final int mappingLength = _uriArray.length;
	final short[] result = new short[mappingLength];

	// Initialize all entries to -1
	for (i=0; i<mappingLength; i++)
	    result[i] = (-1);

	for (i=0; i<nsLength; i++) {
	    Integer type = (Integer)_nsIndex.get(namespaces[i]);
	    if (type != null) {
		result[type.intValue()] = (short)i;
	    }
	}

	return(result);
    }

    /**
     * Get mapping from external namespace types to DOM namespace types
     */
    public short[] getReverseNamespaceMapping(String[] namespaces) {
	int i;
	final int length = namespaces.length;
	final short[] result = new short[length];

	for (i=0; i<length; i++) {
	    Integer type = (Integer)_nsIndex.get(namespaces[i]);
	    if (type == null)
		result[i] = -1;
	    else
		result[i] = type.shortValue();
	}

	return(result);
    }

    /**
     * Dump the whole tree to a file (serialized)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
	out.writeInt(_treeNodeLimit);
	out.writeObject(_type);
	out.writeObject(_namespace);
	out.writeObject(_parent);
	out.writeObject(_nextSibling);
	out.writeObject(_offsetOrChild);
	out.writeObject(_lengthOrAttr);
	out.writeObject(_text);
	out.writeObject(_namesArray);
	out.writeObject(_uriArray);
	out.writeObject(_whitespace);
	out.writeObject(_prefix);
	out.flush();
    }

    /**
     * Read the whole tree from a file (serialized)
     */
    public void readExternal(ObjectInput in)
	throws IOException, ClassNotFoundException {
	_treeNodeLimit = in.readInt();
	_type          = (short[])in.readObject();
	_namespace     = (short[])in.readObject();
	_parent        = (int[])in.readObject();
	_nextSibling   = (int[])in.readObject();
	_offsetOrChild = (int[])in.readObject();
	_lengthOrAttr  = (int[])in.readObject();
	_text          = (char[])in.readObject();
	_namesArray    = (String[])in.readObject();
	_uriArray      = (String[])in.readObject();
	_whitespace    = (BitArray)in.readObject();
	_prefix        = (short[])in.readObject();
	_types         = setupMapping(_namesArray);
    }

    /**
     * Constructor - defaults to 32K nodes
     */
    public DOMImpl() {
	this(32*1024);
    }
         
    /**
     * Constructor - defines initial size
     */
    public DOMImpl(int size) {
	_type          = new short[size];
	_parent        = new int[size];
	_nextSibling   = new int[size];
	_offsetOrChild = new int[size];
	_lengthOrAttr  = new int[size];
	_text          = new char[size * 10];
	_whitespace    = new BitArray(size);
	_prefix        = new short[size];
	// _namesArray[] and _uriArray are allocated in endDocument
    }

    /**
     * Prints the whole tree to standard output
     */
    public void print(int node, int level) {
	switch(_type[node]) {
	case ROOT:
	    print(_offsetOrChild[node], level);
	    break;
	case TEXT:
	case COMMENT:
	case PROCESSING_INSTRUCTION:
	    System.out.print(makeStringValue(node));
	    break;
	default:                  // element
	    final String name = getNodeName(node);
	    System.out.print("<" + name);
	    for (int a = _lengthOrAttr[node]; a != NULL; a = _nextSibling[a]) {
		System.out.print("\n" + getNodeName(a) +
				 "=\"" + makeStringValue(a) + "\"");
	    }
	    System.out.print('>');
	    for (int child = _offsetOrChild[node]; child != NULL;
		 child = _nextSibling[child]) {
		print(child, level + 1);
	    }
	    System.out.println("</" + name + '>');
	    break;
	}
    }

    /**
     * Returns the name of a node (attribute or element).
     */
    public String getNodeName(final int node) {
	// Get the node type and make sure that it is within limits
	final short type = _type[node];
	switch(type) {
	case DOM.ROOT:
	case DOM.TEXT:
	case DOM.UNUSED:
	case DOM.ELEMENT:
	case DOM.ATTRIBUTE:
	case DOM.COMMENT:
	    return EMPTYSTRING;
	case DOM.PROCESSING_INSTRUCTION:
	    return "a-pi";
	default:
	    // Construct the local part (omit '@' for attributes)
	    String name  = getLocalName(node);
	    if (node >= _firstAttributeNode)
		name = name.substring(1);

	    final int pi = _prefix[node];
	    if (pi > 0) {
		final String prefix = _prefixArray[pi];
		if (prefix != EMPTYSTRING)
		    name = prefix+':'+name;
	    }
	    return name;
	}
    }

    /**
     * Returns the namespace URI to which a node belongs
     */
    public String getNamespaceName(final int node) {
	final int type = getNamespaceType(node);
	final String name = _uriArray[type];
	if (name == null)
	    return(EMPTYSTRING);
	else
	    return(name);
    }

    /**
     * Returns the string value of a single text/comment node or
     * attribute value (they are all stored in the same array).
     */
    private String makeStringValue(final int node) {
	return new String(_text, _offsetOrChild[node], _lengthOrAttr[node]);
    }

    /**
     * Returns the attribute node of a given type (if any) for an element
     */
    public int getAttributeNode(final int type, final int element) {
	for (int attr = _lengthOrAttr[element];
	     attr != NULL;
	     attr = _nextSibling[attr]) {
	    if (_type[attr] == type) return attr;
	}
	return NULL;
    }

    /**
     * Returns the value of a given attribute type of a given element
     */
    public String getAttributeValue(final int type, final int element) {
	final int attr = getAttributeNode(type, element);
	if (attr != NULL)
	    return makeStringValue(attr);
	else
	    return EMPTYSTRING;
    }

    /**
     * Returns true if a given element has an attribute of a given type
     */
    public boolean hasAttribute(final int type, final int node) {
	if (getAttributeNode(type, node) != NULL)
	    return true;
	else
	    return false;
    }

    /**
     * This method is for testing/debugging only
     */
    public String getAttributeValue(final String name, final int element) {
	return getAttributeValue(getGeneralizedType(name), element);
    }
    
    /**
     * Returns true if the given element has any children
     */
    private boolean hasChildren(final int node) {
	if (node < _firstAttributeNode) {
	    final int type = _type[node];
	    return(((type >= NTYPES) || (type == ROOT)) &&
		   (_offsetOrChild[node] != 0));
	}
	return(false);
    }

    /**
     * Returns an iterator with all the children of a given node
     */
    public NodeIterator getChildren(final int node) {
	if (hasChildren(node))
	    return(new ChildrenIterator());
	else
	    return(EMPTYITERATOR);
    }

    /**
     * Returns an iterator with all children of a specific type
     * for a given node (element)
     */
    public NodeIterator getTypedChildren(final int type) {
	return(new TypedChildrenIterator(type));
    }

    /**
     * This is a shortcut to the iterators that implement the
     * supported XPath axes (only namespace::) is not supported.
     * Returns a bare-bones iterator that must be initialized
     * with a start node (using iterator.setStartNode()).
     */
    public NodeIterator getAxisIterator(final int axis) {
	NodeIterator iterator = null;

	switch (axis) {
	case Axis.SELF:
	    iterator = new SingletonIterator();
	    break;
	case Axis.CHILD:
	    iterator = new ChildrenIterator();
	    break;
	case Axis.PARENT:
	    return(new ParentIterator());
	case Axis.ANCESTOR:
	    return(new AncestorIterator());
	case Axis.ANCESTORORSELF:
	    return((new AncestorIterator()).includeSelf());
	case Axis.ATTRIBUTE:
	    return(new AttributeIterator());
	case Axis.DESCENDANT:
	    iterator = new DescendantIterator();
	    break;
	case Axis.DESCENDANTORSELF:
	    iterator = (new DescendantIterator()).includeSelf();
	    break;
	case Axis.FOLLOWING:
	    iterator = new FollowingIterator();
	    break;
	case Axis.PRECEDING:
	    iterator = new PrecedingIterator();
	    break;
	case Axis.FOLLOWINGSIBLING:
	    iterator = new FollowingSiblingIterator();
	    break;
	case Axis.PRECEDINGSIBLING:
	    iterator = new PrecedingSiblingIterator();
	    break;
	case Axis.NAMESPACE:
	    return(new TypedAttributeIterator(getGeneralizedType("xmlns")));
	default:
	    BasisLibrary.runTimeError("Error: iterator for axis '" + 
				      Axis.names[axis] + "' not implemented");
	    System.exit(1);
	}
	return(iterator);
    }

    /**
     * Similar to getAxisIterator, but this one returns an iterator
     * containing nodes of a typed axis (ex.: child::foo)
     */
    public NodeIterator getTypedAxisIterator(int axis, int type) {
	NodeIterator iterator = null;

	/* This causes an error when using patterns for elements that
	   do not exist in the DOM (translet types which do not correspond
	   to a DOM type are mapped to the DOM.ELEMENT type).
	*/

	if (type == NO_TYPE) {
	    return(EMPTYITERATOR);
	}
        else if (type == ELEMENT) {
	    iterator = new FilterIterator(getAxisIterator(axis),
					  getElementFilter());
	}
	else {
	    switch (axis) {
	    case Axis.SELF:
		iterator = new TypedSingletonIterator(type);
		break;
	    case Axis.CHILD:
		iterator = new TypedChildrenIterator(type);
		break;
	    case Axis.PARENT:
		return(new ParentIterator().setNodeType(type));
	    case Axis.ANCESTOR:
		return(new TypedAncestorIterator(type));
	    case Axis.ANCESTORORSELF:
		return((new TypedAncestorIterator(type)).includeSelf());
	    case Axis.ATTRIBUTE:
		return(new TypedAttributeIterator(type));
	    case Axis.DESCENDANT:
		iterator = new TypedDescendantIterator(type);
		break;
	    case Axis.DESCENDANTORSELF:
		iterator = (new TypedDescendantIterator(type)).includeSelf();
		break;
	    case Axis.FOLLOWING:
		iterator = new TypedFollowingIterator(type);
		break;
	    case Axis.PRECEDING:
		iterator = new TypedPrecedingIterator(type);
		break;
	    case Axis.FOLLOWINGSIBLING:
		iterator = new TypedFollowingSiblingIterator(type);
		break;
	    case Axis.PRECEDINGSIBLING:
		iterator = new TypedPrecedingSiblingIterator(type);
		break;
	    default:
		BasisLibrary.runTimeError("Error: typed iterator for axis " + 
					  Axis.names[axis]+"not implemented");
	    }
	}
	    return(iterator);
    }

    /**
     * Do not thing that this returns an iterator for the namespace axis.
     * It returns an iterator with nodes that belong in a certain namespace,
     * such as with <xsl:apply-templates select="blob/foo:*"/>
     * The 'axis' specifies the axis for the base iterator from which the
     * nodes are taken, while 'ns' specifies the namespace URI type.
     */
    public NodeIterator getNamespaceAxisIterator(int axis, int ns) {

	NodeIterator iterator = null;

	if (ns == NO_TYPE) {
	    return(EMPTYITERATOR);
	}
	else {
	    switch (axis) {
	    case Axis.CHILD:
		iterator = new NamespaceChildrenIterator(ns);
		break;
	    case Axis.ATTRIBUTE:
		iterator = new NamespaceAttributeIterator(ns);
		break;
	    default:
		BasisLibrary.runTimeError("Error: typed iterator for axis " + 
					  Axis.names[axis]+"not implemented");
	    }
	}
	return(iterator);
    }

    /**
     * Returns an iterator with all descendants of a node that are of
     * a given type.
     */
    public NodeIterator getTypedDescendantIterator(int type) {
	NodeIterator iterator = new TypedDescendantIterator(type);
	iterator.setStartNode(1);
	return(iterator);
    }

    /**
     * Returns the nth descendant of a node (1 = parent, 2 = gramps)
     */
    public NodeIterator getNthDescendant(int node, int n) {
	return (new NthDescendantIterator(n));
    }

    /**
     * Copy the contents of a text-node to an output handler
     */
    public void characters(final int textNode, TransletOutputHandler handler)
	throws TransletException {
	handler.characters(_text,
			   _offsetOrChild[textNode],
			   _lengthOrAttr[textNode]);
    }

    /**
     * Copy a node-set to an output handler
     */
    public void copy(NodeIterator nodes, TransletOutputHandler handler)
	throws TransletException {
	int node;
	while ((node = nodes.next()) != NULL) {
	    copy(node, handler);
	}
    }

    /**
     * Copy the whole tree to an output handler
     */
    public void copy(TransletOutputHandler handler) throws TransletException {
	copy(ROOTNODE, handler);
    }

    /** 
     * Performs a deep copy (ref. XSLs copy-of())
     *
     * TODO: Copy namespace declarations. Can't be done until we
     *       add namespace nodes and keep track of NS prefixes
     * TODO: Copy comment nodes
     */
    public void copy(final int node, TransletOutputHandler handler)
	throws TransletException {

	final int type = _type[node];

	switch(type) {
	case ROOT:
	    for (int c=_offsetOrChild[node]; c!=NULL; c=_nextSibling[c])
		copy(c, handler);
	    break;
	case PROCESSING_INSTRUCTION:
	    copyPI(node, handler);
	    break;
	case COMMENT:
	case TEXT:
	    handler.characters(_text,
			       _offsetOrChild[node],
			       _lengthOrAttr[node]);
	    break;
	case ATTRIBUTE:
	    shallowCopy(node, handler);
	    break;
	default:
	    if (isElement(node)) {
		// Start element definition
		final String name = copyElement(node, type, handler);
		// Copy element attribute
		for (int a=_lengthOrAttr[node]; a!=NULL; a=_nextSibling[a]) {
		    final String uri = getNamespaceName(a);
		    if (uri != EMPTYSTRING) {
			final String prefix = _prefixArray[_prefix[a]];
			handler.namespace(prefix, uri);
		    }
		    handler.attribute(getNodeName(a), makeStringValue(a));
		}
		// Copy element children
		for (int c=_offsetOrChild[node]; c!=NULL; c=_nextSibling[c])
		    copy(c, handler);
		// Close element definition
		handler.endElement(name);
	    }
	    // Shallow copy of attribute to output handler
	    else {
		final String uri = getNamespaceName(node);
		if (uri != EMPTYSTRING) {
		    final String prefix = _prefixArray[_prefix[node]];
		    handler.namespace(prefix, uri);
		}
		handler.attribute(getNodeName(node), makeStringValue(node));
	    }
	    break;
	}
    }

    /**
     * Copies a processing instruction node to an output handler
     */ 
    private void copyPI(final int node, TransletOutputHandler handler)
	throws TransletException {
	final char[] text = _text;
	final int start = _offsetOrChild[node];
	final int length = _lengthOrAttr[node];

	// Target and Value are separated by a whitespace - find it!
	int i = start;
	while (text[i] != ' ') i++;

	final int len = i - start;
	final String target = new String(text, start, len);
	final String value  = new String(text, i + 1, length - len);

	handler.processingInstruction(target, value);
    }

    /**
     * Performs a shallow copy (ref. XSLs copy())
     *
     * TODO: Copy namespace declarations. Can't be done until we
     *       add namespace nodes and keep track of NS prefixes
     * TODO: Copy comment nodes
     */
    public String shallowCopy(final int node, TransletOutputHandler handler)
	throws TransletException {

	final int type = _type[node];

	switch(type) {
	case ROOT: // do nothing
	    return EMPTYSTRING;
	case TEXT:
	    handler.characters(_text,
			       _offsetOrChild[node],
			       _lengthOrAttr[node]);
	    return null;
	case PROCESSING_INSTRUCTION:
	    copyPI(node, handler);
	    return null;
	case COMMENT:
	    return null;
	default:
	    if (isElement(node)) {
		return(copyElement(node, type, handler));
	    }
	    else {
		final String uri = getNamespaceName(node);
		if (uri != EMPTYSTRING) {
		    final String prefix = _prefixArray[_prefix[node]];
		    handler.namespace(prefix, uri);
		}
		handler.attribute(getNodeName(node), makeStringValue(node));
		return null;
	    }
	}
    }

    private String copyElement(int node, int type,
			       TransletOutputHandler handler)
	throws TransletException {

	type = type - NTYPES;
	String name = _namesArray[type];
	final int pi = _prefix[node];
	if (pi > 0) {
	    final String prefix = _prefixArray[pi];
	    final String uri = _uriArray[_namespace[type]];
	    final String local = getLocalName(node);
	    if (prefix.equals(EMPTYSTRING))
		name = local;
	    else
		name = prefix+':'+local;
	    handler.startElement(name);
	    handler.namespace(prefix, uri);
	}
	else {
	    handler.startElement(name);
	}
	return name;
    }

    /**
     * Returns the string value of the entire tree
     */
    public String getStringValue() {
	return getElementValue(ROOTNODE);
    }

    /**
     * Returns the string value of any element
     */
    public String getElementValue(final int element) {
	// optimization: only create StringBuffer if > 1 child
	final int child = _offsetOrChild[element];
	return child != NULL
	    ? (_type[child] == TEXT && _nextSibling[child] == NULL
	       ? makeStringValue(child)
	       : stringValueAux(new StringBuffer(), element).toString())
	    : EMPTYSTRING;
    }

    /**
     * Helper to getElementValue() above
     */
    private StringBuffer stringValueAux(StringBuffer buffer, final int element) {
	for (int child = _offsetOrChild[element];
	     child != NULL;
	     child = _nextSibling[child]) {
	    switch (_type[child]) {
	    case COMMENT:
		break;
	    case TEXT:
		buffer.append(_text,
			      _offsetOrChild[child],
			      _lengthOrAttr[child]);
		break;
	    case PROCESSING_INSTRUCTION:
		break;
		// !!! at the moment default can only be an element???
	    default:
		stringValueAux(buffer, child);
	    }
	}
	return buffer;
    }

    /**
     * Returns a node' defined language for a node (if any)
     */
    public String getLanguage(int node) {
	final Integer langType = (Integer)_types.get("xml:@lang");
	if (langType != null) {
	    final int type = langType.intValue();
	    while (node != DOM.NULL) {
		int attr = _lengthOrAttr[node];
		while (attr != DOM.NULL) {
		    if (_type[attr] == type)
			return(getNodeValue(attr));
		    attr = _nextSibling[attr];
		}
		node = getParent(node);
	    }
	}
	return(null);
    }

    /**
     * Returns an instance of the DOMBuilder inner class
     * This class will consume the input document through a SAX2
     * interface and populate the tree.
     */
    public DOMBuilder getBuilder() {
	return new DOMBuilderImpl();
    }

    /**
     * Returns a DOMBuilder class wrapped in a SAX adapter.
     * I am not sure if we need this one anymore now that the
     * DOM builder's interface is pure SAX2 (must investigate)
     */
    public TransletOutputHandler getOutputDomBuilder() {
	return new SAXAdapter(getBuilder());
    }

    /**
     * Returns true if a character is an XML whitespace character.
     * Order of tests is important for performance ([space] first).
     */
    private static final boolean isWhitespaceChar(char c) {
	return c == 0x20 || c == 0x0A || c == 0x0D || c == 0x09;
    }


    /****************************************************************/
    /*               DOM builder class definition                   */
    /****************************************************************/
    private final class DOMBuilderImpl implements DOMBuilder {

	private final static int ATTR_ARRAY_SIZE = 32;
	private final static int REUSABLE_TEXT_SIZE = 32;
	private Hashtable _shortTexts           = null;

	private Hashtable _names                = null;
	private int       _nextNameCode         = NTYPES;
	private int[]     _parentStack          = new int[64];
	private int[]     _previousSiblingStack = new int[64];
	private int       _sp;
	private int       _baseOffset           = 0;
	private int       _currentOffset        = 0;
	private int       _currentNode          = 0;

	// Temporary structures for attribute nodes
	private int       _currentAttributeNode = 0;
	private short[]   _type2        = new short[ATTR_ARRAY_SIZE];
	private short[]   _prefix2      = new short[ATTR_ARRAY_SIZE];
	private int[]     _parent2      = new int[ATTR_ARRAY_SIZE];
	private int[]     _nextSibling2 = new int[ATTR_ARRAY_SIZE];
	private int[]     _offset       = new int[ATTR_ARRAY_SIZE];
	private int[]     _length       = new int[ATTR_ARRAY_SIZE];

	// Namespace prefix-to-uri mapping stuff
	private Hashtable _nsPrefixes   = new Hashtable();
	private int       _uriCount     = 0;
	private int       _prefixCount  = 0;
	
	// Stack used to keep track of what whitespace text nodes are protected
	// by xml:space="preserve" attributes and which nodes that are not.
	private int[]   _xmlSpaceStack = new int[64];
	private int     _idx = 1;
	private boolean _preserve = false;

	private static final String XML_STRING = "xml:";
	private static final String XMLSPACE_STRING = "xml:space";
	private static final String PRESERVE_STRING = "preserve";

	/**
	 * Default constructor for the DOMBuiler class
	 */
	public DOMBuilderImpl() {
	    _xmlSpaceStack[0] = DOM.ROOTNODE;
	}

	/**
	 * Returns the namespace URI that a prefix currentl maps to
	 */
	private String getNamespaceURI(String prefix) {
	    // Get the stack associated with this namespace prefix
	    final Stack stack = (Stack)_nsPrefixes.get(prefix);
	    if ((stack != null) && (!stack.empty())) {
		return((String)stack.peek());
	    }
	    else
		return(EMPTYSTRING);
	}

	/**
	 * Call this when an xml:space attribute is encountered to
	 * define the whitespace strip/preserve settings.
	 */
	private void xmlSpaceDefine(String val, final int node) {
	    final boolean setting = val.equals(PRESERVE_STRING);
	    if (setting != _preserve) {
		_xmlSpaceStack[_idx++] = node;
		_preserve = setting;
	    }
	}

	/**
	 * Call this from endElement() to revert strip/preserve setting
	 * to whatever it was before the corresponding startElement()
	 */
	private void xmlSpaceRevert(final int node) {
	    if (node == _xmlSpaceStack[_idx - 1]) {
		_idx--;
		_preserve = !_preserve;
	    }
	}

	/**
	 * Returns the next available node. Increases the various arrays
	 * that constitute the node if necessary.
	 */
	private int nextNode() {
	    final int index = _currentNode++;
	    if (index == _type.length) {
		resizeArrays(_type.length * 2, index);
	    }
	    return index;
	}

	/**
	 * Returns the next available attribute node. Increases the
	 * various arrays that constitute the attribute if necessary
	 */
	private int nextAttributeNode() {
	    final int index = _currentAttributeNode++;
	    if (index == _type2.length) {
		resizeArrays2(_type2.length * 2, index);
	    }
	    return index;
	}

	/**
	 * Resize the character array that holds the contents of
	 * all text nodes, comments and attribute values
	 */
	private void resizeTextArray(final int newSize) {
	    final char[] newText = new char[newSize];
	    System.arraycopy(_text, 0, newText, 0, _currentOffset);
	    _text = newText;
	}
	
	/**
	 * Links together the children of a node. Child nodes are linked
	 * through the _nextSibling array
	 */
	private void linkChildren(final int node) {
	    _parent[node] = _parentStack[_sp];
	    if (_previousSiblingStack[_sp] != 0) { // current not first child
		_nextSibling[_previousSiblingStack[_sp]] = node;
	    }
	    else {
		_offsetOrChild[_parentStack[_sp]] = node;
	    }
	    _previousSiblingStack[_sp] = node;
	}

	/**
	 * Generate the internal type for an element's expanded QName
	 */
	private short makeElementNode(String uri, String localname)
	    throws SAXException {
	    
	    final String name;
	    if (uri != EMPTYSTRING)
		name = uri + ':' + localname;
	    else
		name = localname;

	    // Stuff the QName into the names vector & hashtable
	    Integer obj = (Integer)_names.get(name);
	    if (obj == null) {
		_names.put(name, obj = new Integer(_nextNameCode++));
	    }
	    return (short)obj.intValue();
	}

	/**
	 * Generate the internal type for an element's expanded QName
	 */
	private short makeElementNode(String name, int col)
	    throws SAXException {
	    // Expand prefix:localname to full QName
	    if (col > -1) {
		final String uri = getNamespaceURI(name.substring(0, col));
		name = uri + name.substring(col);
	    }
	    // Append default namespace with the name has no prefix
	    else {
		final String uri = getNamespaceURI(EMPTYSTRING);
		if (!uri.equals(EMPTYSTRING)) name = uri + ':' + name;
	    }

	    // Stuff the QName into the names vector & hashtable
	    Integer obj = (Integer)_names.get(name);
	    if (obj == null) {
		_names.put(name, obj = new Integer(_nextNameCode++));
	    }
	    return (short)obj.intValue();
	}

	/**
	 *
	 */
	private short registerPrefix(String prefix) {
	    Stack stack = (Stack)_nsPrefixes.get(prefix);
	    if (stack != null) {
		Integer obj = (Integer)stack.elementAt(0);
		return (short)obj.intValue();
	    }
	    return 0;
	}

	/*
	 * This method will check if the current text node contains text that
	 * is already in the text array. If the text is found in the array
	 * then this method returns the offset of the previous instance of the
	 * string. Otherwise the text is inserted in the array, and the
	 * offset of the new instance is inserted.
	 * Updates the globals _baseOffset and _currentOffset
	 */
	private int maybeReuseText(final int length) {
	    final int base = _baseOffset;
	    if (length <= REUSABLE_TEXT_SIZE) {
		// Use a char array instead of string for performance benefit
		char[] chars = new char[length];
		System.arraycopy(_text, base, chars, 0, length);
		final Integer offsetObj = (Integer)_shortTexts.get(chars);

		if (offsetObj != null) {
		    _currentOffset = base;       // step back current
		    return offsetObj.intValue(); // reuse previous string
		}
		else {
		    _shortTexts.put(chars, new Integer(base));
		}
	    }
	    _baseOffset = _currentOffset; // advance base to current
	    return base;
	}

	/**
	 * Links a text reference (an occurance of a sequence of characters
	 * in the _text[] array) to a specific node index.
	 */
	private void storeTextRef(final int node) {
	    final int length = _currentOffset - _baseOffset;
	    _offsetOrChild[node] = maybeReuseText(length);
	    _lengthOrAttr[node]  = length;
	}
	
	/**
	 * Creates a text-node and checks if it is a whitespace node.
	 */
	private int makeTextNode(boolean isWhitespace) {
	    if (_currentOffset > _baseOffset) {
		final int node = nextNode();
		final int limit = _currentOffset;
		// Tag as whitespace node if the parser tells us that it is...
		if (isWhitespace) {
		    _whitespace.setBit(node);
		}
		// ...otherwise we check if this is a whitespace node, unless
		// the node is protected by an xml:space="preserve" attribute.
		else if (!_preserve) {
		    int i = _baseOffset;
		    while (isWhitespaceChar(_text[i++]) && i < limit) ;
		    if ((i == limit) && isWhitespaceChar(_text[i-1])) {
			_whitespace.setBit(node);
		    }
		}

		_type[node] = TEXT;
		linkChildren(node);
		storeTextRef(node);
		return node;
	    }
	    return -1;
	}

	/**
	 * Links an attribute value (an occurance of a sequence of characters
	 * in the _text[] array) to a specific attribute node index.
	 */
	private void storeAttrValRef(final int attributeNode) {
	    final int length = _currentOffset - _baseOffset;
	    _offset[attributeNode] = maybeReuseText(length);
	    _length[attributeNode] = length;
	}

	/**
	 * Creates an attribute node
	 */
	private int makeAttributeNode(int parent, Attributes attList, int i)
	    throws SAXException {

    	    final int node = nextAttributeNode();

	    final String qname = attList.getQName(i);
	    final String localname = attList.getLocalName(i);
	    final String value = attList.getValue(i);
	    StringBuffer namebuf = new StringBuffer(EMPTYSTRING);
	    
	    // Create the internal attribute node name (uri+@+localname)
	    if (qname.startsWith(XML_STRING)) {
		if (qname.startsWith(XMLSPACE_STRING))
		    xmlSpaceDefine(attList.getValue(i), parent);
		namebuf.append("xml:");
	    }
	    else {
		final String uri = attList.getURI(i);
		if ((uri != null) && (!uri.equals(EMPTYSTRING))) {
		    namebuf.append(uri);
		    namebuf.append(':');
		}
	    }
	    namebuf.append('@');
	    namebuf.append(localname);

	    String name = namebuf.toString();
	    
	    // Get the index of the attribute node name (create new if non-ex).
	    Integer obj = (Integer)_names.get(name);
	    if (obj == null) {
		_type2[node] = (short)_nextNameCode;
		_names.put(name, obj = new Integer(_nextNameCode++));
	    }
	    else {
		_type2[node] = (short)obj.intValue();
	    }

	    _parent2[node] = parent;

	    final int col = qname.lastIndexOf(':');
	    if (col > 0) {
		_prefix2[node] = registerPrefix(qname.substring(0, col));
	    }

	    characters(attList.getValue(i));
	    storeAttrValRef(node);
	    return node;
	}
	
	/****************************************************************/
	/*               SAX Interface Starts Here                      */
	/****************************************************************/

	/**
	 * SAX2: Receive notification of character data.
	 */
	public void characters(char[] ch, int start, int length) {
	    if (_currentOffset + length > _text.length) {
		resizeTextArray(_text.length * 2);
	    }
	    System.arraycopy(ch, start, _text, _currentOffset, length);
	    _currentOffset += length;
	}

	/**
	 * SAX2: Receive notification of the beginning of a document.
	 */
	public void startDocument() {
	    _shortTexts     = new Hashtable();
	    _names          = new Hashtable();
	    _sp             = 0;
	    _parentStack[0] = ROOTNODE;	// root
	    _currentNode    = ROOTNODE + 1;
	    _currentAttributeNode = 0;
	    startPrefixMapping(EMPTYSTRING, EMPTYSTRING);
	}

	/**
	 * SAX2: Receive notification of the end of a document.
	 */
	public void endDocument() {
	    makeTextNode(false);

	    _shortTexts = null;
	    final int namesSize = _nextNameCode - NTYPES;

	    // Fill the _namesArray[] array
	    _namesArray = new String[namesSize];
	    Enumeration keys = _names.keys();
	    while (keys.hasMoreElements()) {
		final String name = (String)keys.nextElement();
		final Integer idx = (Integer)_names.get(name);
		_namesArray[idx.intValue() - NTYPES] = name;
	    }

	    _names = null;
	    _types = setupMapping(_namesArray);

	    // trim arrays' sizes
	    resizeTextArray(_currentOffset);
	    
	    _firstAttributeNode = _currentNode;
	    shiftAttributes(_currentNode);
	    resizeArrays(_currentNode + _currentAttributeNode, _currentNode);
	    appendAttributes();
	    _treeNodeLimit = _currentNode + _currentAttributeNode;

	    // Fill the _namespace[] and _uriArray[] array
	    _namespace = new short[namesSize];
	    _uriArray = new String[_uriCount];
	    for (int i = 0; i<namesSize; i++) {
		final String qname = _namesArray[i];
		final int col = _namesArray[i].lastIndexOf(':');
		// Elements/attributes with the xml prefix are not in a NS
		if ((!qname.startsWith(XML_STRING)) && (col > -1)) {
		    final String uri = _namesArray[i].substring(0, col);
		    final Integer idx = (Integer)_nsIndex.get(uri);
		    _namespace[i] = idx.shortValue();
		    _uriArray[idx.intValue()] = uri;
		}
	    }

	    _prefixArray = new String[_prefixCount];
	    Enumeration p = _nsPrefixes.keys();
	    while (p.hasMoreElements()) {
		final String prefix = (String)p.nextElement();
		final Stack stack = (Stack)_nsPrefixes.get(prefix);
		final Integer I = (Integer)stack.elementAt(0);
		_prefixArray[I.shortValue()] = prefix;
	    }
	}
	
	/**
	 * SAX2: Receive notification of the beginning of an element.
	 */
	public void startElement(String uri, String localName,
				 String qname, Attributes attributes)
	    throws SAXException {

	    makeTextNode(false);

	    // Get node index and setup parent/child references
	    final int node = nextNode();
	    linkChildren(node);
	    _parentStack[++_sp] = node;

	    final int count = attributes.getLength();

	    // Process attribute list and create attr nodes
	    if (count > 0) {
		int attr = _currentAttributeNode + 1;
		_lengthOrAttr[node] = attr;
		for (int i = 0; i<count; i++) {
		    attr = makeAttributeNode(node, attributes, i);
		    _nextSibling2[attr] = attr + 1;
		}
		_nextSibling2[attr] = DOM.NULL;
	    }
	    // The element has no attributes
	    else {
		_lengthOrAttr[node] = DOM.NULL;
	    }	    
	    
	    final int col = qname.lastIndexOf(':');

	    // Assign an internal type to this element (may exist)
	    if ((uri != null) && (localName.length() > 0))
		_type[node] = makeElementNode(uri, localName);
	    else
		_type[node] = makeElementNode(qname, col);

	    // Assign an internal type to the element's prefix (may exist)
	    if (col > -1) {
		_prefix[node] = registerPrefix(qname.substring(0, col));
	    }
	}
	
	/**
	 * SAX2: Receive notification of the end of an element.
	 */
	public void endElement(String namespaceURI, String localName,
			       String qname) {
	    makeTextNode(false);

	    // Revert to strip/preserve-space setting from before this element
	    xmlSpaceRevert(_parentStack[_sp]);
	    _previousSiblingStack[_sp--] = 0;
	}

	/**
	 * SAX2: Receive notification of a processing instruction.
	 */
	public void processingInstruction(String target, String data)
	    throws SAXException {

	    makeTextNode(false);

	    final int node = nextNode();
	    _type[node] = PROCESSING_INSTRUCTION;
	    linkChildren(node);
	    characters(target);
	    characters(" ");
	    characters(data);
	    storeTextRef(node);
	}

	/**
	 * SAX2: Receive notification of ignorable whitespace in element
	 * content. Similar to characters(char[], int, int).
	 */
	public void ignorableWhitespace(char[] ch, int start, int length) {
	    if (_currentOffset + length > _text.length) {
		resizeTextArray(_text.length * 2);
	    }
	    System.arraycopy(ch, start, _text, _currentOffset, length);
	    _currentOffset += length;
	    makeTextNode(true);
	}

	/**
	 * SAX2: Receive an object for locating the origin of SAX document
	 * events. 
	 */
	public void setDocumentLocator(Locator locator) {
	    // Not handled
	}

	/**
	 * SAX2: Receive notification of a skipped entity.
	 */
	public void skippedEntity(String name) {
	    // Not handled 
	}

	/**
	 * SAX2: Begin the scope of a prefix-URI Namespace mapping.
	 */
	public void startPrefixMapping(String prefix, String uri) {
	    // Get the stack associated with this namespace prefix
	    Stack stack = (Stack)_nsPrefixes.get(prefix);
	    if (stack == null) {
		stack = new Stack();
		stack.push(new Integer(_prefixCount++));
		_nsPrefixes.put(prefix, stack);
	    }

	    // Check if the URI already exists before pushing on stack
	    if (_nsIndex.get(uri) == null) {
		_nsIndex.put(uri, new Integer(_uriCount++));
	    }
	    stack.push(uri);
	}

	/**
	 * SAX2: End the scope of a prefix-URI Namespace mapping.
	 */
	public void endPrefixMapping(String prefix) {
	    // Get the stack associated with this namespace prefix
	    final Stack stack = (Stack)_nsPrefixes.get(prefix);
	    if ((stack != null) && (!stack.empty())) stack.pop();
	}

	/**
	 * SAX2: Report an XML comment anywhere in the document.
	 */
	public void comment(char[] ch, int start, int length) {
	    makeTextNode(false);
	    if (_currentOffset + length > _text.length) {
		resizeTextArray(_text.length * 2);
	    }
	    System.arraycopy(ch, start, _text, _currentOffset, length);
	    _currentOffset += length;
	    final int node = makeTextNode(false);
	    _type[node] = COMMENT;
	}

	/**
	 * SAX2: Ignored events
	 */
	public void startCDATA() {}
	public void endCDATA() {}
	public void startDTD(String name, String publicId, String systemId) {}
	public void endDTD() {}
	public void startEntity(String name) {}
	public void endEntity(String name) {}

	/**
	 * Similar to the SAX2 method character(char[], int, int), but this
	 * method takes a string as its only parameter. The effect is the same.
	 */
	private void characters(final String string) {
	    final int length = string.length();
	    if (_currentOffset + length > _text.length) {
		resizeTextArray(_text.length * 2);
	    }
	    string.getChars(0, length, _text, _currentOffset);
	    _currentOffset += length;
	}

	private void resizeArrays(final int newSize, final int length) {
	    if (newSize > length) {
		// Resize the '_type' array
		final short[] newType = new short[newSize];
		System.arraycopy(_type, 0, newType, 0, length);
		_type = newType;

		// Resize the '_parent' array
		final int[] newParent = new int[newSize];
		System.arraycopy(_parent, 0, newParent, 0, length);
		_parent = newParent;

		// Resize the '_nextSibling' array
		final int[] newNextSibling = new int[newSize];
		System.arraycopy(_nextSibling, 0, newNextSibling, 0, length);
		_nextSibling = newNextSibling;

		// Resize the '_offsetOrChild' array
		final int[] newOffsetOrChild = new int[newSize];
		System.arraycopy(_offsetOrChild, 0, newOffsetOrChild, 0,length);
		_offsetOrChild = newOffsetOrChild;

		// Resize the '_lengthOrAttr' array
		final int[] newLengthOrAttr = new int[newSize];
		System.arraycopy(_lengthOrAttr, 0, newLengthOrAttr, 0, length);
		_lengthOrAttr = newLengthOrAttr;

		// Resize the '_whitespace' array (a BitArray instance)
		_whitespace.resize(newSize);

		// Resize the '_prefix' array
		final short[] newPrefix = new short[newSize];
		System.arraycopy(_prefix, 0, newPrefix, 0, length);
		_prefix = newPrefix;
	    }
	}
	
	private void resizeArrays2(final int newSize, final int length) {
	    if (newSize > length) {
		// Resize the '_type2' array (attribute types)
		final short[] newType = new short[newSize];
		System.arraycopy(_type2, 0, newType, 0, length);
		_type2 = newType;

		// Resize the '_parent2' array (attribute parent elements)
		final int[] newParent = new int[newSize];
		System.arraycopy(_parent2, 0, newParent, 0, length);
		_parent2 = newParent;

		// Resize the '_nextSibling2' array (you get the idea...)
		final int[] newNextSibling = new int[newSize];
		System.arraycopy(_nextSibling2, 0, newNextSibling, 0, length);
		_nextSibling2 = newNextSibling;

		// Resize the '_offset' array (attribute value start)
		final int[] newOffset = new int[newSize];
		System.arraycopy(_offset, 0, newOffset, 0, length);
		_offset = newOffset;

		// Resize the 'length' array (attribute value length)
		final int[] newLength = new int[newSize];
		System.arraycopy(_length, 0, newLength, 0, length);
		_length = newLength;

		// Resize the '_prefix2' array
		final short[] newPrefix = new short[newSize];
		System.arraycopy(_prefix2, 0, newPrefix, 0, length);
		_prefix2 = newPrefix;
	    }
	}
	
	private void shiftAttributes(final int shift) {
	    int i = 0;
	    int next = 0;
	    final int limit = _currentAttributeNode;
	    int lastParent = -1;

	    for (i = 0; i < limit; i++) {
		if (_parent2[i] != lastParent) {
		    lastParent = _parent2[i];
		    _lengthOrAttr[lastParent] = i + shift;
		}
		next = _nextSibling2[i];
		_nextSibling2[i] = next != 0 ? next + shift : 0;
	    }
	}
	
	private void appendAttributes() {
	    final int len = _currentAttributeNode;
	    if (len > 0) {
		final int dst = _currentNode;
		System.arraycopy(_type2,         0, _type,          dst, len);
		System.arraycopy(_prefix2,       0, _prefix,        dst, len);
		System.arraycopy(_parent2,       0, _parent,        dst, len);
		System.arraycopy(_nextSibling2,  0, _nextSibling,   dst, len);
		System.arraycopy(_offset,        0, _offsetOrChild, dst, len);
		System.arraycopy(_length,        0, _lengthOrAttr,  dst, len);
	    }
	}

    } // end of DOMBuilder
}
