<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!-- FileName: namedtemplateerr11 -->
  <!-- Document: http://www.w3.org/TR/xslt -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 6 Named Templates -->
  <!-- Creator: David Marston -->
  <!-- Purpose: Try to use an AVT for the template name when invoking. -->
  <!-- ExpectedException: Could not find template named: {$tocall} -->
  <!-- ExpectedException: org.apache.xalan.xslt.XSLProcessorException: Could not find template named: {$tocall} -->
  <!-- ExpectedException: Illegal value: {$tocall} used for QNAME attribute: name -->

<xsl:template match="doc">
  <out>
    <xsl:variable name="tocall" select="'a'"/>
    <xsl:call-template name="{$tocall}"/>
  </out>
</xsl:template>

<xsl:template name="tocall">
  <xsl:text>This should NOT display!</xsl:text>
</xsl:template>

<xsl:template name="a">
  <xsl:text>We are in template a!</xsl:text>
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
