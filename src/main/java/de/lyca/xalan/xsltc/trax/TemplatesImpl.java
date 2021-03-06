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
package de.lyca.xalan.xsltc.trax;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;

import de.lyca.xalan.ObjectFactory;
import de.lyca.xalan.xsltc.DOM;
import de.lyca.xalan.xsltc.Translet;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.runtime.AbstractTranslet;

/**
 * @author Morten Jorgensen
 * @author G. Todd Millerj
 * @author Jochen Cordes {@literal <Jochen.Cordes@t-online.de>}
 * @author Santiago Pericas-Geertsen
 * 
 *         TODO Where is synchronization needed?
 */
public final class TemplatesImpl implements Templates, Serializable {
  static final long serialVersionUID = 673094361519270707L;
  /**
   * Name of the superclass of all translets. This is needed to determine which,
   * among all classes comprising a translet, is the main one.
   */
  private static String ABSTRACT_TRANSLET = "de.lyca.xalan.xsltc.runtime.AbstractTranslet";

  /**
   * Name of the main class or default name if unknown.
   */
  private String _name = null;

  /**
   * Contains the actual class definition for the translet class and any
   * auxiliary classes.
   */
  private byte[][] _bytecodes = null;

  /**
   * Contains the translet class definition(s). These are created when this
   * Templates is created or when it is read back from disk.
   */
  private Class<?>[] _class = null;

  /**
   * The index of the main translet class in the arrays _class[] and _bytecodes.
   */
  private int _transletIndex = -1;

  /**
   * Contains the list of auxiliary class definitions.
   */
  private Map<String, Class<?>> _auxClasses = null;

  /**
   * Output properties of this translet.
   */
  private Properties _outputProperties;

  /**
   * Number of spaces to add for output indentation.
   */
  private int _indentNumber;

  /**
   * Synchronization object to guard the compilation of the translet.
   */
  private final Object sync = new Object();

  /**
   * This latch guards against the case, where _class is not null anymore but
   * the _transletIndex is still not set correctly.
   */
  private final CountDownLatch latch = new CountDownLatch(1);

  /**
   * This URIResolver is passed to all Transformers. Declaring it transient to
   * fix bug 22438
   */
  private transient URIResolver _uriResolver = null;

  /**
   * Cache the DTM for the stylesheet in a thread local variable, which is used
   * by the document('') function. Use ThreadLocal because a DTM cannot be
   * shared between multiple threads. Declaring it transient to fix bug 22438
   */
  private transient ThreadLocal<DOM> _sdom = new ThreadLocal<>();

  /**
   * A reference to the transformer factory that this templates object belongs
   * to.
   */
  private transient TransformerFactoryImpl _tfactory = null;

  static final class TransletClassLoader extends ClassLoader {
    TransletClassLoader(ClassLoader parent) {
      super(parent);
    }

    /**
     * Access to final protected superclass member from outer class.
     */
    Class<?> defineClass(final byte[] b) {
      return defineClass(null, b, 0, b.length);
    }
  }

  /**
   * Create an XSLTC template object from the bytecodes. The bytecodes for the
   * translet and auxiliary classes, plus the name of the main translet class,
   * must be supplied.
   * 
   * @param bytecodes
   *          TODO
   * @param transletName
   *          TODO
   * @param outputProperties
   *          TODO
   * @param indentNumber
   *          TODO
   * @param tfactory
   *          TODO
   */
  protected TemplatesImpl(byte[][] bytecodes, String transletName, Properties outputProperties, int indentNumber,
      TransformerFactoryImpl tfactory) {
    _bytecodes = bytecodes;
    _name = transletName;
    _outputProperties = outputProperties;
    _indentNumber = indentNumber;
    _tfactory = tfactory;
  }

  /**
   * Create an XSLTC template object from the translet class definition(s).
   * @param transletClasses TODO
   * @param transletName TODO
   * @param outputProperties TODO
   * @param indentNumber TODO
   * @param tfactory TODO
   */
  protected TemplatesImpl(Class<?>[] transletClasses, String transletName, Properties outputProperties,
      int indentNumber, TransformerFactoryImpl tfactory) {
    _class = transletClasses;
    _name = transletName;
    _transletIndex = 0;
    _outputProperties = outputProperties;
    _indentNumber = indentNumber;
    _tfactory = tfactory;
  }

  /**
   * Need for de-serialization, see readObject().
   */
  public TemplatesImpl() {
  }

  /**
   * Overrides the default readObject implementation since we decided it would
   * be cleaner not to serialize the entire tranformer factory. [ ref bugzilla
   * 12317 ] We need to check if the user defined class for URIResolver also
   * implemented Serializable if yes then we need to deserialize the URIResolver
   * Fix for bugzilla bug 22438
   */
  private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
    is.defaultReadObject();
    if (is.readBoolean()) {
      _uriResolver = (URIResolver) is.readObject();
    }

    _tfactory = new TransformerFactoryImpl();
  }

  /**
   * This is to fix bugzilla bug 22438 If the user defined class implements
   * URIResolver and Serializable then we want it to get serialized
   */
  private void writeObject(ObjectOutputStream os) throws IOException, ClassNotFoundException {
    os.defaultWriteObject();
    if (_uriResolver instanceof Serializable) {
      os.writeBoolean(true);
      os.writeObject(_uriResolver);
    } else {
      os.writeBoolean(false);
    }
  }

  /**
   * Store URIResolver needed for Transformers.
   * 
   * @param resolver
   *          TODO
   */
  public void setURIResolver(URIResolver resolver) {
    _uriResolver = resolver;
  }

  /**
   * The TransformerFactory must pass us the translet bytecodes using this
   * method before we can create any translet instances
   * 
   * @param bytecodes
   *          TODO
   */
  protected void setTransletBytecodes(byte[][] bytecodes) {
    _bytecodes = bytecodes;
  }

  /**
   * Returns the translet bytecodes stored in this template
   * 
   * @return TODO
   */
  public byte[][] getTransletBytecodes() {
    return _bytecodes;
  }

  /**
   * Returns the translet bytecodes stored in this template
   * 
   * @return TODO
   */
  public Class<?>[] getTransletClasses() {
    try {
      if (_class == null) {
        synchronized (sync) {
          if (_class == null) {
            defineTransletClasses();
          }
        }
      }
    } catch (final TransformerConfigurationException e) {
      // Falls through
    }
    return _class;
  }

  /**
   * Returns the index of the main class in array of bytecodes
   * 
   * @return TODO
   */
  public int getTransletIndex() {
    try {
      if (_class == null) {
        synchronized (sync) {
          if (_class == null) {
            defineTransletClasses();
          }
        }
      }
    } catch (final TransformerConfigurationException e) {
      // Falls through
    }
    return _transletIndex;
  }

  /**
   * The TransformerFactory should call this method to set the translet name
   * 
   * @param name
   *          TODO
   */
  protected void setTransletName(String name) {
    _name = name;
  }

  /**
   * Returns the name of the main translet class stored in this template
   * 
   * @return TODO
   */
  protected String getTransletName() {
    return _name;
  }

  /**
   * Defines the translet class and auxiliary classes. Returns a reference to
   * the Class object that defines the main class
   * 
   * @throws TransformerConfigurationException
   *           TODO
   */
  private void defineTransletClasses() throws TransformerConfigurationException {

    if (_bytecodes == null) {
      final ErrorMsg err = new ErrorMsg(Messages.get().noTransletClassErr());
      throw new TransformerConfigurationException(err.toString());
    }

    final TransletClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<TransletClassLoader>() {
      @Override
      public TransletClassLoader run() {
        return new TransletClassLoader(ObjectFactory.findClassLoader());
      }
    });

    try {
      final int classCount = _bytecodes.length;
      _class = new Class[classCount];

      if (classCount > 1) {
        _auxClasses = new HashMap<>();
      }

      for (int i = 0; i < classCount; i++) {
        _class[i] = loader.defineClass(_bytecodes[i]);
        final Class<?> superClass = _class[i].getSuperclass();

        // Check if this is the main class
        if (superClass.getName().equals(ABSTRACT_TRANSLET)) {
          _transletIndex = i;
        } else {
          _auxClasses.put(_class[i].getName(), _class[i]);
        }
      }

      if (_transletIndex < 0) {
        final ErrorMsg err = new ErrorMsg(Messages.get().noMainTransletErr(_name));
        throw new TransformerConfigurationException(err.toString());
      }

      latch.countDown();
    } catch (final ClassFormatError e) {
      final ErrorMsg err = new ErrorMsg(Messages.get().transletClassErr(_name));
      throw new TransformerConfigurationException(err.toString());
    } catch (final LinkageError e) {
      final ErrorMsg err = new ErrorMsg(Messages.get().transletClassErr(_name));
      throw new TransformerConfigurationException(err.toString());
    }
  }

  /**
   * This method generates an instance of the translet class that is wrapped
   * inside this Template. The translet instance will later be wrapped inside a
   * Transformer object.
   * 
   * @return TODO
   * @throws TransformerConfigurationException
   *           TODO
   */
  private Translet getTransletInstance() throws TransformerConfigurationException {
    try {
      if (_name == null)
        return null;

      if (_class == null) {
        synchronized (sync) {
          if (_class == null) {
            defineTransletClasses();
          }
        }
      }

      latch.await();

      // The translet needs to keep a reference to all its auxiliary
      // class to prevent the GC from collecting them
      final AbstractTranslet translet = (AbstractTranslet) _class[_transletIndex].newInstance();
      translet.postInitialization();
      translet.setTemplates(this);
      if (_auxClasses != null) {
        translet.setAuxiliaryClasses(_auxClasses);
      }

      return translet;
    } catch (final InstantiationException e) {
      final ErrorMsg err = new ErrorMsg(Messages.get().transletObjectErr());
      throw new TransformerConfigurationException(err.toString());
    } catch (final IllegalAccessException e) {
      final ErrorMsg err = new ErrorMsg(Messages.get().transletObjectErr());
      throw new TransformerConfigurationException(err.toString());
    } catch (final InterruptedException e) {
      final ErrorMsg err = new ErrorMsg(Messages.get().transletObjectErr());
      throw new TransformerConfigurationException(err.toString());
    }
  }

  /**
   * Implements JAXP's Templates.newTransformer()
   * 
   * @return TODO
   * @throws TransformerConfigurationException TODO
   */
  @Override
  public TransformerImpl newTransformer() throws TransformerConfigurationException {
    TransformerImpl transformer;

    transformer = new TransformerImpl(getTransletInstance(), _outputProperties, _indentNumber, _tfactory);

    if (_uriResolver != null) {
      transformer.setURIResolver(_uriResolver);
    }

    if (_tfactory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING)) {
      transformer.setSecureProcessing(true);
    }
    return transformer;
  }

  /**
   * Implements JAXP's Templates.getOutputProperties(). We need to instanciate a
   * translet to get the output settings, so we might as well just instanciate a
   * Transformer and use its implementation of this method.
   * 
   * @return TODO
   */
  @Override
  public Properties getOutputProperties() {
    try {
      return newTransformer().getStylesheetOutputProperties();
    } catch (final TransformerConfigurationException e) {
      return null;
    }
  }

  /**
   * Return the thread local copy of the stylesheet DOM.
   * 
   * @return TODO
   */
  public DOM getStylesheetDOM() {
    return _sdom.get();
  }

  /**
   * Set the thread local copy of the stylesheet DOM.
   * 
   * @param sdom
   *          TODO
   */
  public void setStylesheetDOM(DOM sdom) {
    _sdom.set(sdom);
  }
}
