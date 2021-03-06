<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!-- FileName: variable62 -->
  <!-- Document: http://www.w3.org/TR/xslt -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 11.6 -->
  <!-- Creator: David Marston -->
  <!-- Purpose: Test for passing numeric param via apply-templates, with sorting -->

<xsl:output method="xml" encoding="UTF-8"/>

<xsl:template match="doc">
  <out>
    <xsl:apply-templates select="a-set">
      <xsl:sort select="@s" data-type="number" order="ascending"/>
      <xsl:with-param name="total" select="sum(a-set/a)"/>
    </xsl:apply-templates>
  </out>
</xsl:template>

<xsl:template match="a-set">
  <xsl:param name="total">Default text in param 1</xsl:param>
  <xsl:text>&#10;</xsl:text>
  <list from="{@g}">
    <xsl:attribute name="proportion">
      <xsl:value-of select="concat(sum(./a/text()),'/',$total)"/>
    </xsl:attribute>
    <xsl:for-each select="./a/text()">
      <xsl:value-of select="."/>
      <xsl:text>,</xsl:text>
    </xsl:for-each>
  </list>
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
