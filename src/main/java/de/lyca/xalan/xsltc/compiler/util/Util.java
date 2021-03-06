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
package de.lyca.xalan.xsltc.compiler.util;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import de.lyca.xml.utils.XML11Char;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public final class Util {
  private static char filesep;

  private static final Map<String, String> KEYWORDS = new HashMap<>();
  static {
    KEYWORDS.put("abstract", "$abstract");
    KEYWORDS.put("assert", "$assert");
    KEYWORDS.put("boolean", "$boolean");
    KEYWORDS.put("break", "$break");
    KEYWORDS.put("byte", "$byte");
    KEYWORDS.put("case", "$case");
    KEYWORDS.put("catch", "$catch");
    KEYWORDS.put("char", "$char");
    KEYWORDS.put("class", "$class");
    KEYWORDS.put("const", "$const");
    KEYWORDS.put("continue", "$continue");
    KEYWORDS.put("default", "$default");
    KEYWORDS.put("do", "$do");
    KEYWORDS.put("double", "$double");
    KEYWORDS.put("else", "$else");
    KEYWORDS.put("enum", "$enum");
    KEYWORDS.put("extends", "$extends");
    KEYWORDS.put("final", "$final");
    KEYWORDS.put("finally", "$finally");
    KEYWORDS.put("float", "$float");
    KEYWORDS.put("for", "$for");
    KEYWORDS.put("goto", "$goto");
    KEYWORDS.put("if", "$if");
    KEYWORDS.put("implements", "$implements");
    KEYWORDS.put("import", "$import");
    KEYWORDS.put("instanceof", "$instanceof");
    KEYWORDS.put("int", "$int");
    KEYWORDS.put("interface", "$interface");
    KEYWORDS.put("long", "$long");
    KEYWORDS.put("native", "$native");
    KEYWORDS.put("new", "$new");
    KEYWORDS.put("package", "$package");
    KEYWORDS.put("private", "$private");
    KEYWORDS.put("protected", "$protected");
    KEYWORDS.put("public", "$public");
    KEYWORDS.put("return", "$return");
    KEYWORDS.put("short", "$short");
    KEYWORDS.put("static", "$static");
    KEYWORDS.put("strictfp", "$strictfp");
    KEYWORDS.put("super", "$super");
    KEYWORDS.put("switch", "$switch");
    KEYWORDS.put("synchronized", "$synchronized");
    KEYWORDS.put("this", "$this");
    KEYWORDS.put("throw", "$throw");
    KEYWORDS.put("throws", "$throws");
    KEYWORDS.put("transient", "$transient");
    KEYWORDS.put("try", "$try");
    KEYWORDS.put("void", "$void");
    KEYWORDS.put("volatile", "$volatile");
    KEYWORDS.put("while", "$while");

    KEYWORDS.put("true", "$true");
    KEYWORDS.put("false", "$false");
    KEYWORDS.put("null", "$null");

    KEYWORDS.put("node", "$node");
    KEYWORDS.put("translet", "$null");
    KEYWORDS.put("iterator", "$iterator");
    KEYWORDS.put("document", "$document");

    final String temp = System.getProperty("file.separator", "/");
    filesep = temp.charAt(0);
  }

  public static String noExtName(String name) {
    final int index = name.lastIndexOf('.');
    return name.substring(0, index >= 0 ? index : name.length());
  }

  /**
   * Search for both slashes in order to support URLs and files.
   * @param name TODO
   * @return TODO
   */
  public static String baseName(String name) {
    int index = name.lastIndexOf('\\');
    if (index < 0) {
      index = name.lastIndexOf('/');
    }

    if (index >= 0)
      return name.substring(index + 1);
    else {
      final int lastColonIndex = name.lastIndexOf(':');
      if (lastColonIndex > 0)
        return name.substring(lastColonIndex + 1);
      else
        return name;
    }
  }

  /**
   * Search for both slashes in order to support URLs and files.
   * @param name TODO
   * @return TODO
   */
  public static String pathName(String name) {
    int index = name.lastIndexOf('/');
    if (index < 0) {
      index = name.lastIndexOf('\\');
    }
    return name.substring(0, index + 1);
  }

  /**
   * Replace all illegal Java chars by '_'.
   * @param name TODO
   * @return TODO
   */
  public static String toJavaName(String name) {
    if (name.length() > 0) {
      final StringBuilder result = new StringBuilder();

      char ch = name.charAt(0);
      result.append(Character.isJavaIdentifierStart(ch) ? ch : '_');

      final int n = name.length();
      for (int i = 1; i < n; i++) {
        ch = name.charAt(i);
        result.append(Character.isJavaIdentifierPart(ch) ? ch : '_');
      }
      return result.toString();
    }
    return name;
  }

  public static String internalName(String cname) {
    return cname.replace('.', filesep);
  }

  /**
   * Replace a certain character in a string with a new substring.
   * @param base TODO
   * @param ch TODO
   * @param str TODO
   * @return TODO
   */
  public static String replace(String base, char ch, String str) {
    return base.indexOf(ch) < 0 ? base : replace(base, String.valueOf(ch), new String[] { str });
  }

  public static String replace(String base, String delim, String[] str) {
    final int len = base.length();
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < len; i++) {
      final char ch = base.charAt(i);
      final int k = delim.indexOf(ch);

      if (k >= 0) {
        result.append(str[k]);
      } else {
        result.append(ch);
      }
    }
    return result.toString();
  }

  /**
   * Replace occurances of '.', '-', '/' and ':'
   * @param input TODO
   * @return TODO
   */
  public static String escape(String input) {
    return KEYWORDS.containsKey(input) ? KEYWORDS.get(input) : '$' + replace(input, ".-/:", new String[] { "$dot$",
        "$dash$", "$slash$", "$colon$" });
  }

  public static String getLocalName(String qname) {
    final int index = qname.lastIndexOf(':');
    return index > 0 ? qname.substring(index + 1) : qname;
  }

  public static String getPrefix(String qname) {
    final int index = qname.lastIndexOf(':');
    return index > 0 ? qname.substring(0, index) : "";
  }

  /**
   * Checks if the string is a literal (i.e. not an AVT) or not.
   * @param str TODO
   * @return TODO
   */
  public static boolean isLiteral(String str) {
    final int length = str.length();
    for (int i = 0; i < length - 1; i++) {
      if (str.charAt(i) == '{' && str.charAt(i + 1) != '{')
        return false;
    }
    return true;
  }

  /**
   * Checks if the string is valid list of qnames
   * @param str TODO
   * @return TODO
   */
  public static boolean isValidQNames(String str) {
    if (str != null && !str.isEmpty()) {
      final StringTokenizer tokens = new StringTokenizer(str);
      while (tokens.hasMoreTokens()) {
        if (!XML11Char.isXML11ValidQName(tokens.nextToken()))
          return false;
      }
    }
    return true;
  }

}
