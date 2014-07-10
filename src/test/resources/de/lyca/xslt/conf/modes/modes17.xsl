<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!-- FileName: MODES17 -->
  <!-- Document: http://www.w3.org/TR/xslt -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 5.7 Modes -->
  <!-- Creator: David Marston -->
  <!-- Purpose: Check that underscores are allowed in names. -->

<xsl:template match="/">
  <out>
    <xsl:apply-templates select="doc" mode="_1st_mode"/>
  </out>
</xsl:template>

<xsl:template match="a" mode="a.mode">
  <xsl:text>mode-a:</xsl:text><xsl:value-of select="."/>
</xsl:template>

<xsl:template match="a">
  <xsl:text>no-mode:</xsl:text><xsl:value-of select="."/>
</xsl:template>

<xsl:template match="doc" mode="_1st_mode">
  <xsl:text>_1st_mode: </xsl:text><xsl:apply-templates select="a" mode="a.mode"/>
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