<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"> 

  <!-- FileName: NumberFormat43 -->
  <!-- Document: http://www.w3.org/TR/xslt -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 12.3 -->
  <!-- Creator: David Marston -->
  <!-- Purpose: Have two decimal-format elements with identical names, one in an import.
    This is allowed as long as all attributes are identical (including defaults). -->

<xsl:import href="x43import.xsl"/>

<xsl:decimal-format name="decimal3" NaN='not a number' decimal-separator='.'/>

<xsl:template match="doc">
  <out>
    <xsl:text>&#10;</xsl:text>
    <one>
      <xsl:value-of select="format-number('NaN','###','decimal3')"/>
      <xsl:text>, </xsl:text>
      <xsl:value-of select="format-number(-13.2,'###.0','decimal3')"/>
    </one>
    <xsl:text>&#10;</xsl:text>
    <xsl:call-template name="sub"/>
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