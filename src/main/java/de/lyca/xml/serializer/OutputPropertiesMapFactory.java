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
/*
 * $Id$
 */
package de.lyca.xml.serializer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import de.lyca.xml.serializer.utils.MsgKey;
import de.lyca.xml.serializer.utils.Utils;
import de.lyca.xml.serializer.utils.WrappedRuntimeException;

/**
 * This class is a factory to generate a set of default properties of key/value
 * pairs that are used to create a serializer through the factory
 * {@link SerializerFactory SerilizerFactory}. The properties generated by this
 * factory may be modified to non-default values before the SerializerFactory is
 * used to create a Serializer.
 * <p>
 * The given output types supported are "xml", "text", and "html". These type
 * strings can be obtained from the {@link Method Method} class in this package.
 * <p>
 * Other constants defined in this class are the non-standard property keys that
 * can be used to set non-standard property values on a java.util.Properties
 * object that is used to create or configure a serializer. Here are the
 * non-standard keys:
 * <ul>
 * <li><b>S_KEY_INDENT_AMOUNT </b> - The non-standard property key to use to set
 * the indentation amount. The "indent" key needs to have a value of "yes", and
 * this properties value is a the number of whitespaces to indent by per
 * indentation level.
 * 
 * <li><b>S_KEY_CONTENT_HANDLER </b> - This non-standard property key is used to
 * set the name of the fully qualified Java class that implements the
 * ContentHandler interface. The output of the serializer will be SAX events
 * sent to this an object of this class.
 * 
 * <li><b>S_KEY_ENTITIES </b> - This non-standard property key is used to
 * specify the name of the property file that specifies character to entity
 * reference mappings. A line in such a file is has the name of the entity and
 * the numeric (base 10) value of the corresponding character, like this one: <br>
 * quot=34 <br>
 * 
 * <li><b>S_USE_URL_ESCAPING </b> - This non-standard property key is used to
 * set a value of "yes" if the href values for HTML serialization should use %xx
 * escaping.
 * 
 * <li><b>S_OMIT_META_TAG </b> - This non-standard property key is used to set a
 * value of "yes" if the META tag should be omitted where it would otherwise be
 * supplied.
 * </ul>
 * 
 * @see SerializerFactory
 * @see Method
 * @see Serializer
 */
public final class OutputPropertiesMapFactory {
  /**
   * S_BUILTIN_EXTENSIONS_URL is a mnemonic for the XML Namespace
   * (http://xml.apache.org/xalan) predefined to signify Xalan's built-in XSLT
   * Extensions. When used in stylesheets, this is often bound to the "xalan:"
   * prefix.
   */
  private static final String S_BUILTIN_EXTENSIONS_URL = "http://xml.apache.org/xalan";

  /**
   * The old built-in extension url. It is still supported for backward
   * compatibility.
   */
  private static final String S_BUILTIN_OLD_EXTENSIONS_URL = "http://xml.apache.org/xslt";

  // ************************************************************
  // * PUBLIC CONSTANTS
  // ************************************************************
  /**
   * This is not a public API. This is the built-in extensions namespace,
   * reexpressed in {namespaceURI} syntax suitable for prepending to a localname
   * to produce a "universal name".
   */
  public static final String S_BUILTIN_EXTENSIONS_UNIVERSAL = "{" + S_BUILTIN_EXTENSIONS_URL + "}";

  // Some special Xalan keys.

  /**
   * The non-standard property key to use to set the number of whitepaces to
   * indent by, per indentation level, if indent="yes".
   */
  public static final String S_KEY_INDENT_AMOUNT = S_BUILTIN_EXTENSIONS_UNIVERSAL + "indent-amount";

  /**
   * The non-standard property key to use to set the characters to write out as
   * at the end of a line, rather than the default ones from the runtime.
   */
  public static final String S_KEY_LINE_SEPARATOR = S_BUILTIN_EXTENSIONS_UNIVERSAL + "line-separator";

  /**
   * This non-standard property key is used to set the name of the fully
   * qualified Java class that implements the ContentHandler interface. Fully
   * qualified name of class with a default constructor that implements the
   * ContentHandler interface, where the result tree events will be sent to.
   */

  public static final String S_KEY_CONTENT_HANDLER = S_BUILTIN_EXTENSIONS_UNIVERSAL + "content-handler";

  /**
   * This non-standard property key is used to specify the name of the property
   * file that specifies character to entity reference mappings.
   */
  public static final String S_KEY_ENTITIES = S_BUILTIN_EXTENSIONS_UNIVERSAL + "entities";

  /**
   * This non-standard property key is used to set a value of "yes" if the href
   * values for HTML serialization should use %xx escaping.
   */
  public static final String S_USE_URL_ESCAPING = S_BUILTIN_EXTENSIONS_UNIVERSAL + "use-url-escaping";

  /**
   * This non-standard property key is used to set a value of "yes" if the META
   * tag should be omitted where it would otherwise be supplied.
   */
  public static final String S_OMIT_META_TAG = S_BUILTIN_EXTENSIONS_UNIVERSAL + "omit-meta-tag";

  /**
   * The old built-in extension namespace, this is not a public API.
   */
  public static final String S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL = "{" + S_BUILTIN_OLD_EXTENSIONS_URL + "}";

  /**
   * This is not a public API, it is only public because it is used by outside
   * of this package, it is the length of the old built-in extension namespace.
   */
  public static final int S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL_LEN = S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL.length();

  // ************************************************************
  // * PRIVATE CONSTANTS
  // ************************************************************

  private static final String S_XSLT_PREFIX = "xslt.output.";
  private static final int S_XSLT_PREFIX_LEN = S_XSLT_PREFIX.length();
  private static final String S_XALAN_PREFIX = "org.apache.xslt.";
  private static final int S_XALAN_PREFIX_LEN = S_XALAN_PREFIX.length();

  /** Synchronization object for lazy initialization of the above tables. */
  private static Integer m_synch_object = new Integer(1);

  /** the directory in which the various method property files are located */
  private static final String PROP_DIR = SerializerBase.PKG_PATH + '/';
  /** property file for default XML properties */
  private static final String PROP_FILE_XML = "output_xml.properties";
  /** property file for default TEXT properties */
  private static final String PROP_FILE_TEXT = "output_text.properties";
  /** property file for default HTML properties */
  private static final String PROP_FILE_HTML = "output_html.properties";
  /**
   * property file for default UNKNOWN (Either XML or HTML, to be determined
   * later) properties
   */
  private static final String PROP_FILE_UNKNOWN = "output_unknown.properties";

  // ************************************************************
  // * PRIVATE STATIC FIELDS
  // ************************************************************

  /** The default properties of all output files. */
  private static Map<String, String> m_xml_map = null;

  /** The default properties when method="html". */
  private static Map<String, String> m_html_map = null;

  /** The default properties when method="text". */
  private static Map<String, String> m_text_map = null;

  /** The properties when method="" for the "unknown" wrapper */
  private static Map<String, String> m_unknown_map = null;

  /**
   * Creates an empty OutputProperties with the property key/value defaults
   * specified by a property file. The method argument is used to construct a
   * string of the form output_[method].properties (for instance,
   * output_html.properties). The output_xml.properties file is always used as
   * the base.
   * 
   * <p>
   * Anything other than 'text', 'xml', and 'html', will use the
   * output_xml.properties file.
   * </p>
   * 
   * @param method
   *          non-null reference to method name.
   * 
   * @return Properties object that holds the defaults for the given method.
   */
  public static final Map<String, String> getDefaultMethodProperties(String method) {
    String fileName = null;
    // According to this article : Double-check locking does not work
    // http://www.javaworld.com/javaworld/jw-02-2001/jw-0209-toolbox.html
    // Init with double checked locking
    try {
      if (m_xml_map == null) {
        synchronized (m_synch_object) {
          if (m_xml_map == null) {
            m_xml_map = loadPropertiesFile(fileName = PROP_FILE_XML, Collections.<String, String> emptyMap());
          }
        }
      }

      if (method.equals(Method.XML))
        return Collections.unmodifiableMap(m_xml_map);

      if (method.equals(Method.HTML)) {
        if (m_html_map == null) {
          synchronized (m_synch_object) {
            if (m_html_map == null) {
              m_html_map = loadPropertiesFile(fileName = PROP_FILE_HTML, m_xml_map);
            }
          }
        }
        return m_html_map;
      }

      if (method.equals(Method.TEXT)) {
        if (m_text_map == null) {
          synchronized (m_synch_object) {
            if (m_text_map == null) {
              m_text_map = loadPropertiesFile(fileName = PROP_FILE_TEXT, m_xml_map);
              if (m_text_map.get(OutputKeys.ENCODING) == null) {
                final String mimeEncoding = Encodings.getMimeEncoding(null);
                m_text_map.put(OutputKeys.ENCODING, mimeEncoding);
              }

            }
          }
        }
        return m_text_map;
      }

      if (method.equals(Method.UNKNOWN)) {
        if (m_unknown_map == null) {
          synchronized (m_synch_object) {
            if (m_unknown_map == null) {
              m_unknown_map = loadPropertiesFile(fileName = PROP_FILE_UNKNOWN, m_xml_map);
            }
          }
        }
        return m_unknown_map;
      }
      // TODO: Calculate res file from name.
      return Collections.unmodifiableMap(m_xml_map);
    } catch (final IOException ioe) {
      throw new WrappedRuntimeException(Utils.messages.createMessage(MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
              new Object[] { fileName, method }), ioe);
    }
  }

  /**
   * Load the properties file from a resource stream. If a key name such as
   * "org.apache.xslt.xxx", fix up the start of string to be a curly namespace.
   * If a key name starts with "xslt.output.xxx", clip off "xslt.output.". If a
   * key name *or* a key value is discovered, check for \u003a in the text, and
   * fix it up to be ":", since earlier versions of the JDK do not handle the
   * escape sequence (at least in key names).
   * 
   * @param resourceName
   *          non-null reference to resource name.
   * @param defaults
   *          Default properties, which may be null.
   */
  static private Map<String, String> loadPropertiesFile(final String resourceName, Map<String, String> defaults)
          throws IOException {

    // This static method should eventually be moved to a thread-specific class
    // so that we can cache the ContextClassLoader and bottleneck all properties
    // file
    // loading throughout Xalan.

    final Map<String, String> result = new HashMap<>(defaults);
    // For loading the properties
    final Properties props = new Properties();

    InputStream is = null;
    BufferedInputStream bis = null;

    try {
      is = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
        @Override
        public InputStream run() {
          return OutputPropertiesMapFactory.class.getResourceAsStream(resourceName);
        }
      });
      bis = new BufferedInputStream(is);
      props.load(bis);
    } finally {
      if (bis != null) {
        bis.close();
      }
      if (is != null) {
        is.close();
      }
    }

    // Note that we're working at the HashTable level here,
    // and not at the Properties level! This is important
    // because we don't want to modify the default properties.
    // NB: If fixupPropertyString ends up changing the property
    // name or value, we need to remove the old key and re-add
    // with the new key and value. However, then our Enumeration
    // could lose its place in the HashTable. So, we first
    // clone the HashTable and enumerate over that since the
    // clone will not change. When we migrate to Collections,
    // this code should be revisited and cleaned up to use
    // an Iterator which may (or may not) alleviate the need for
    // the clone. Many thanks to Padraig O'hIceadha
    // <padraig@gradient.ie> for finding this problem. Bugzilla 2000.

    final Enumeration keys = props.keys();
    while (keys.hasMoreElements()) {
      final String key = (String) keys.nextElement();
      // Now check if the given key was specified as a
      // System property. If so, the system property
      // overides the default value in the propery file.
      String value = null;
      try {
        value = System.getProperty(key);
      } catch (final SecurityException se) {
        // No-op for sandbox/applet case, leave null -sc
      }
      if (value == null) {
        value = (String) props.get(key);
      }

      final String newKey = fixupPropertyString(key, true);
      String newValue = null;
      try {
        newValue = System.getProperty(newKey);
      } catch (final SecurityException se) {
        // No-op for sandbox/applet case, leave null -sc
      }
      if (newValue == null) {
        newValue = fixupPropertyString(value, false);
      } else {
        newValue = fixupPropertyString(newValue, false);
      }

      result.put(newKey, newValue);
    }

    return result;
  }

  /**
   * Fix up a string in an output properties file according to the rules of
   * {@link #loadPropertiesFile}.
   * 
   * @param s
   *          non-null reference to string that may need to be fixed up.
   * @return A new string if fixup occured, otherwise the s argument.
   */
  private static String fixupPropertyString(String s, boolean doClipping) {
    int index;
    if (doClipping && s.startsWith(S_XSLT_PREFIX)) {
      s = s.substring(S_XSLT_PREFIX_LEN);
    }
    if (s.startsWith(S_XALAN_PREFIX)) {
      s = S_BUILTIN_EXTENSIONS_UNIVERSAL + s.substring(S_XALAN_PREFIX_LEN);
    }
    if ((index = s.indexOf("\\u003a")) > 0) {
      final String temp = s.substring(index + 6);
      s = s.substring(0, index) + ":" + temp;

    }
    return s;
  }

  public static Map<String, String> unmodifiableMapFromProperties(Properties properties) {
    final Map<String, String> result = new HashMap<>();
    for (final String key : properties.stringPropertyNames()) {
      result.put(key, properties.getProperty(key));
    }
    return Collections.unmodifiableMap(result);
  }

}
