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

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;

import de.lyca.xalan.xsltc.DOM;
import de.lyca.xalan.xsltc.StripFilter;
import de.lyca.xalan.xsltc.compiler.util.ErrorMsg;
import de.lyca.xalan.xsltc.compiler.util.Messages;
import de.lyca.xalan.xsltc.dom.DOMWSFilter;
import de.lyca.xalan.xsltc.dom.SAXImpl;
import de.lyca.xalan.xsltc.dom.XSLTCDTMManager;
import de.lyca.xalan.xsltc.runtime.AbstractTranslet;

/**
 * @author Morten Jorgensen
 */
public final class XSLTCSource implements Source {

  private String _systemId = null;
  private Source _source = null;
  private final ThreadLocal<SAXImpl> _dom = new ThreadLocal<>();

  /**
   * Create a new XSLTC-specific source from a system ID
   * 
   * @param systemId
   *          TODO
   */
  public XSLTCSource(String systemId) {
    _systemId = systemId;
  }

  /**
   * Create a new XSLTC-specific source from a JAXP Source
   * 
   * @param source
   *          TODO
   */
  public XSLTCSource(Source source) {
    _source = source;
  }

  /**
   * Implements javax.xml.transform.Source.setSystemId() Set the system
   * identifier for this Source. This Source can get its input either directly
   * from a file (in this case it will instanciate and use a JAXP parser) or it
   * can receive it through ContentHandler/LexicalHandler interfaces.
   * 
   * @param systemId
   *          The system Id for this Source
   */
  @Override
  public void setSystemId(String systemId) {
    _systemId = systemId;
    if (_source != null) {
      _source.setSystemId(systemId);
    }
  }

  /**
   * Implements javax.xml.transform.Source.getSystemId() Get the system
   * identifier that was set with setSystemId.
   * 
   * @return The system identifier that was set with setSystemId, or null if
   *         setSystemId was not called.
   */
  @Override
  public String getSystemId() {
    if (_source != null)
      return _source.getSystemId();
    else
      return _systemId;
  }

  /**
   * Internal interface which returns a DOM for a given DTMManager and translet.
   * 
   * @param dtmManager
   *          TODO
   * @param translet
   *          TODO
   * @return TODO
   * @throws SAXException
   *           TODO
   */
  protected DOM getDOM(XSLTCDTMManager dtmManager, AbstractTranslet translet) throws SAXException {
    SAXImpl idom = _dom.get();

    if (idom != null) {
      if (dtmManager != null) {
        idom.migrateTo(dtmManager);
      }
    } else {
      Source source = _source;
      if (source == null) {
        if (_systemId != null && _systemId.length() > 0) {
          source = new StreamSource(_systemId);
        } else {
          final ErrorMsg err = new ErrorMsg(Messages.get().xsltcSourceErr());
          throw new SAXException(err.toString());
        }
      }

      DOMWSFilter wsfilter = null;
      if (translet != null && translet instanceof StripFilter) {
        wsfilter = new DOMWSFilter(translet);
      }

      final boolean hasIdCall = translet != null ? translet.hasIdCall() : false;

      if (dtmManager == null) {
        dtmManager = XSLTCDTMManager.newInstance();
      }

      idom = (SAXImpl) dtmManager.getDTM(source, true, wsfilter, false, false, hasIdCall);

      final String systemId = getSystemId();
      if (systemId != null) {
        idom.setDocumentURI(systemId);
      }
      _dom.set(idom);
    }
    return idom;
  }

}
