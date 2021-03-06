<?xml version="1.0"?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:lxslt="http://xml.apache.org/xslt"
                xmlns:extension1288="MyCounter"
                extension-element-prefixes="extension1288"
                version="1.0">

  <lxslt:component prefix="extension1288"
                   elements="" functions="read">
    <lxslt:script lang="javaclass" src="Bugzilla1288"/>
  </lxslt:component>

  <!-- XSL variable declaration -->
  <xsl:variable name="var">
  <history/>
  </xsl:variable>

  <!-- Extension function call -->
  <xsl:template match="/">
    <out>
      <p>Extension output below:</p>
      <xsl:variable name="result" select="extension1288:run($var)"/>
      <xsl:copy-of select="$var"/>
      <p>Extension output above:</p>
    </out>
  </xsl:template>
 

  <!--
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
  -->

</xsl:stylesheet>
