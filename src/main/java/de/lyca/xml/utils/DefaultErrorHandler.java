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
package de.lyca.xml.utils;

import java.io.PrintStream;
import java.io.PrintWriter;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.lyca.xml.res.Messages;

/**
 * Implement SAX error handler for default reporting.
 */
public class DefaultErrorHandler implements ErrorHandler, ErrorListener {
  PrintWriter m_pw;

  /**
   * if this flag is set to true, we will rethrow the exception on the error() and fatalError() methods. If it is false,
   * the errors are reported to System.err.
   */
  boolean m_throwExceptionOnError = true;

  /**
   * Constructor DefaultErrorHandler
   */
  public DefaultErrorHandler(PrintWriter pw) {
    m_pw = pw;
  }

  /**
   * Constructor DefaultErrorHandler
   */
  public DefaultErrorHandler(PrintStream pw) {
    m_pw = new PrintWriter(pw, true);
  }

  /**
   * Constructor DefaultErrorHandler
   */
  public DefaultErrorHandler() {
    this(true);
  }

  /**
   * Constructor DefaultErrorHandler
   */
  public DefaultErrorHandler(boolean throwExceptionOnError) {
    // Defer creation of a PrintWriter until it's actually needed
    m_throwExceptionOnError = throwExceptionOnError;
  }

  /**
   * Retrieve <code>java.io.PrintWriter</code> to which errors are being directed.
   * 
   * @return The <code>PrintWriter</code> installed via the constructor or the default <code>PrintWriter</code>
   */
  public PrintWriter getErrorWriter() {
    // Defer creating the java.io.PrintWriter until an error needs to be
    // reported.
    if (m_pw == null) {
      m_pw = new PrintWriter(System.err, true);
    }
    return m_pw;
  }

  /**
   * Receive notification of a warning.
   * 
   * <p>
   * SAX parsers will use this method to report conditions that are not errors or fatal errors as defined by the XML 1.0
   * recommendation. The default behaviour is to take no action.
   * </p>
   * 
   * <p>
   * The SAX parser must continue to provide normal parsing events after invoking this method: it should still be
   * possible for the application to process the document through to the end.
   * </p>
   * 
   * @param exception The warning information encapsulated in a SAX parse exception.
   * @throws SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void warning(SAXParseException exception) throws SAXException {
    final PrintWriter pw = getErrorWriter();

    printLocation(pw, exception);
    pw.println("Parser warning: " + exception.getMessage());
  }

  /**
   * Receive notification of a recoverable error.
   * 
   * <p>
   * This corresponds to the definition of "error" in section 1.2 of the W3C XML 1.0 Recommendation. For example, a
   * validating parser would use this callback to report the violation of a validity constraint. The default behaviour
   * is to take no action.
   * </p>
   * 
   * <p>
   * The SAX parser must continue to provide normal parsing events after invoking this method: it should still be
   * possible for the application to process the document through to the end. If the application cannot do so, then the
   * parser should report a fatal error even if the XML 1.0 recommendation does not require it to do so.
   * </p>
   * 
   * @param exception The error information encapsulated in a SAX parse exception.
   * @throws SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void error(SAXParseException exception) throws SAXException {
    // printLocation(exception);
    // getErrorWriter().println(exception.getMessage());

    throw exception;
  }

  /**
   * Receive notification of a non-recoverable error.
   * 
   * <p>
   * This corresponds to the definition of "fatal error" in section 1.2 of the W3C XML 1.0 Recommendation. For example,
   * a parser would use this callback to report the violation of a well-formedness constraint.
   * </p>
   * 
   * <p>
   * The application must assume that the document is unusable after the parser has invoked this method, and should
   * continue (if at all) only for the sake of collecting addition error messages: in fact, SAX parsers are free to stop
   * reporting any other events once this method has been invoked.
   * </p>
   * 
   * @param exception The error information encapsulated in a SAX parse exception.
   * @throws SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    // printLocation(exception);
    // getErrorWriter().println(exception.getMessage());

    throw exception;
  }

  /**
   * Receive notification of a warning.
   * 
   * <p>
   * SAX parsers will use this method to report conditions that are not errors or fatal errors as defined by the XML 1.0
   * recommendation. The default behaviour is to take no action.
   * </p>
   * 
   * <p>
   * The SAX parser must continue to provide normal parsing events after invoking this method: it should still be
   * possible for the application to process the document through to the end.
   * </p>
   * 
   * @param exception The warning information encapsulated in a SAX parse exception.
   * @throws TransformerException Any SAX exception, possibly wrapping another exception.
   * @see TransformerException
   */
  @Override
  public void warning(TransformerException exception) throws TransformerException {
    final PrintWriter pw = getErrorWriter();

    printLocation(pw, exception);
    pw.println(exception.getMessage());
  }

  /**
   * Receive notification of a recoverable error.
   * 
   * <p>
   * This corresponds to the definition of "error" in section 1.2 of the W3C XML 1.0 Recommendation. For example, a
   * validating parser would use this callback to report the violation of a validity constraint. The default behaviour
   * is to take no action.
   * </p>
   * 
   * <p>
   * The SAX parser must continue to provide normal parsing events after invoking this method: it should still be
   * possible for the application to process the document through to the end. If the application cannot do so, then the
   * parser should report a fatal error even if the XML 1.0 recommendation does not require it to do so.
   * </p>
   * 
   * @param exception The error information encapsulated in a SAX parse exception.
   * @throws TransformerException Any SAX exception, possibly wrapping another exception.
   * @see TransformerException
   */
  @Override
  public void error(TransformerException exception) throws TransformerException {
    // If the m_throwExceptionOnError flag is true, rethrow the exception.
    // Otherwise report the error to System.err.
    if (m_throwExceptionOnError)
      throw exception;
    else {
      final PrintWriter pw = getErrorWriter();

      printLocation(pw, exception);
      pw.println(exception.getMessage());
    }
  }

  /**
   * Receive notification of a non-recoverable error.
   * 
   * <p>
   * This corresponds to the definition of "fatal error" in section 1.2 of the W3C XML 1.0 Recommendation. For example,
   * a parser would use this callback to report the violation of a well-formedness constraint.
   * </p>
   * 
   * <p>
   * The application must assume that the document is unusable after the parser has invoked this method, and should
   * continue (if at all) only for the sake of collecting addition error messages: in fact, SAX parsers are free to stop
   * reporting any other events once this method has been invoked.
   * </p>
   * 
   * @param exception The error information encapsulated in a SAX parse exception.
   * @throws TransformerException Any SAX exception, possibly wrapping another exception.
   * @see TransformerException
   */
  @Override
  public void fatalError(TransformerException exception) throws TransformerException {
    // If the m_throwExceptionOnError flag is true, rethrow the exception.
    // Otherwise report the error to System.err.
    if (m_throwExceptionOnError)
      throw exception;
    else {
      final PrintWriter pw = getErrorWriter();

      printLocation(pw, exception);
      pw.println(exception.getMessage());
    }
  }

  public static void ensureLocationSet(TransformerException exception) {
    // SourceLocator locator = exception.getLocator();
    SourceLocator locator = null;
    Throwable cause = exception;

    // Try to find the locator closest to the cause.
    do {
      if (cause instanceof SAXParseException) {
        locator = new SAXSourceLocator((SAXParseException) cause);
      } else if (cause instanceof TransformerException) {
        final SourceLocator causeLocator = ((TransformerException) cause).getLocator();
        if (null != causeLocator) {
          locator = causeLocator;
        }
      }

      if (cause instanceof TransformerException) {
        cause = ((TransformerException) cause).getCause();
      } else if (cause instanceof SAXException) {
        cause = ((SAXException) cause).getException();
      } else {
        cause = null;
      }
    } while (null != cause);

    exception.setLocator(locator);
  }

  public static void printLocation(PrintStream pw, TransformerException exception) {
    printLocation(new PrintWriter(pw), exception);
  }

  public static void printLocation(PrintStream pw, SAXParseException exception) {
    printLocation(new PrintWriter(pw), exception);
  }

  public static void printLocation(PrintWriter pw, Throwable exception) {
    SourceLocator locator = null;
    Throwable cause = exception;

    // Try to find the locator closest to the cause.
    do {
      if (cause instanceof SAXParseException) {
        locator = new SAXSourceLocator((SAXParseException) cause);
      } else if (cause instanceof TransformerException) {
        final SourceLocator causeLocator = ((TransformerException) cause).getLocator();
        if (null != causeLocator) {
          locator = causeLocator;
        }
      }
      if (cause instanceof TransformerException) {
        cause = ((TransformerException) cause).getCause();
      } else if (cause instanceof WrappedRuntimeException) {
        cause = ((WrappedRuntimeException) cause).getException();
      } else if (cause instanceof SAXException) {
        cause = ((SAXException) cause).getException();
      } else {
        cause = null;
      }
    } while (null != cause);

    if (null != locator) {
      // getErrorWriter().println("Parser fatal error: "+exception.getMessage());
      // "SystemId Unknown";
      final String id = null != locator.getPublicId() ? locator.getPublicId()
          : null != locator.getSystemId() ? locator.getSystemId() : Messages.get().systemidUnknown();

      pw.print(id + "; " + Messages.get().line() + locator.getLineNumber() + "; " + Messages.get().column()
          + locator.getColumnNumber() + "; ");
    } else {
      pw.print("(" + Messages.get().locationUnknown() + ")");
    }
  }
}
