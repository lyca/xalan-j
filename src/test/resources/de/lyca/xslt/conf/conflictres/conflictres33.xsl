<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!-- FileName: conflictres33 -->
  <!-- Document: http://www.w3.org/TR/xslt -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 5.5 -->
  <!-- Creator: Tom Amiro -->
  <!-- Purpose: Test system allocated priorities for "node()[]" -->

<xsl:output method="xml" indent="no" encoding="UTF-8"/>

<xsl:template match="/doc">
  <out>
    <xsl:apply-templates select="a"/>
  </out>
</xsl:template>

<!-- system allocated priority  0.5 -->
<xsl:template match="node()[@x1]" >
  <xsl:text>Correct - Matched "node()[@x1]" </xsl:text>
</xsl:template>

<!-- system allocated priority 0 -->
<xsl:template match="a">
  <xsl:text>Matched "a" </xsl:text>
</xsl:template>

<!-- system allocated priority -.5 -->
<xsl:template match="node()" >
  <xsl:text>Matched "node()" </xsl:text>
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
