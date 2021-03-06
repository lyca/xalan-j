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
package de.lyca.xml.serializer.dom3;

import java.io.IOException;

import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSSerializerFilter;
import org.xml.sax.SAXException;

import de.lyca.xml.serializer.DOM3Serializer;
import de.lyca.xml.serializer.SerializationHandler;
import de.lyca.xml.serializer.utils.WrappedRuntimeException;

/**
 * This class implements the DOM3Serializer interface.
 */
public final class DOM3SerializerImpl implements DOM3Serializer {

  /**
   * Private class members
   */
  // The DOMErrorHandler
  private DOMErrorHandler fErrorHandler;

  // A LSSerializerFilter
  private LSSerializerFilter fSerializerFilter;

  // A LSSerializerFilter
  private String fNewLine;

  // A SerializationHandler ex. an instance of ToXMLStream
  private SerializationHandler fSerializationHandler;

  /**
   * Constructor
   * 
   * @param handler
   *          An instance of the SerializationHandler interface.
   */
  public DOM3SerializerImpl(SerializationHandler handler) {
    fSerializationHandler = handler;
  }

  // Public memebers

  /**
   * Returns a DOMErrorHandler set on the DOM Level 3 Serializer.
   * 
   * This interface is a public API.
   * 
   * @return A Level 3 DOMErrorHandler
   */
  @Override
  public DOMErrorHandler getErrorHandler() {
    return fErrorHandler;
  }

  /**
   * Returns a LSSerializerFilter set on the DOM Level 3 Serializer to filter
   * nodes during serialization.
   * 
   * This interface is a public API.
   * 
   * @return The Level 3 LSSerializerFilter
   */
  @Override
  public LSSerializerFilter getNodeFilter() {
    return fSerializerFilter;
  }

  /**
   * Gets the end-of-line sequence of characters to be used during
   * serialization.
   */
  public char[] getNewLine() {
    return fNewLine != null ? fNewLine.toCharArray() : null;
  }

  /**
   * Serializes the Level 3 DOM node by creating an instance of DOM3TreeWalker
   * which traverses the DOM tree and invokes handler events to serialize the
   * DOM NOde. Throws an exception only if an I/O exception occured while
   * serializing. This interface is a public API.
   * 
   * @param node
   *          the Level 3 DOM node to serialize
   * @throws IOException
   *           if an I/O exception occured while serializing
   */
  @Override
  public void serializeDOM3(Node node) throws IOException {
    try {
      final DOM3TreeWalker walker = new DOM3TreeWalker(fSerializationHandler, fErrorHandler, fSerializerFilter,
              fNewLine);

      walker.traverse(node);
    } catch (final SAXException se) {
      throw new WrappedRuntimeException(se);
    }
  }

  /**
   * Sets a DOMErrorHandler on the DOM Level 3 Serializer.
   * 
   * This interface is a public API.
   * 
   * @param handler
   *          the Level 3 DOMErrorHandler
   */
  @Override
  public void setErrorHandler(DOMErrorHandler handler) {
    fErrorHandler = handler;
  }

  /**
   * Sets a LSSerializerFilter on the DOM Level 3 Serializer to filter nodes
   * during serialization.
   * 
   * This interface is a public API.
   * 
   * @param filter
   *          the Level 3 LSSerializerFilter
   */
  @Override
  public void setNodeFilter(LSSerializerFilter filter) {
    fSerializerFilter = filter;
  }

  /**
   * Sets a SerializationHandler on the DOM Serializer.
   * 
   * This interface is a public API.
   * 
   * @param handler
   *          An instance of SerializationHandler
   */
  public void setSerializationHandler(SerializationHandler handler) {
    fSerializationHandler = handler;
  }

  /**
   * Sets the end-of-line sequence of characters to be used during
   * serialization.
   * 
   * @param newLine
   *          The end-of-line sequence of characters to be used during
   *          serialization.
   */
  @Override
  public void setNewLine(char[] newLine) {
    fNewLine = newLine != null ? new String(newLine) : null;
  }
}
