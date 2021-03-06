<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!-- FileName: select67 -->
  <!-- Document: http://www.w3.org/TR/xpath -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 12.1 -->
  <!-- Creator: David Marston -->
  <!-- Purpose: Test that document('') refers to this stylesheet, and exploit
     that fact to choose a template dynamically. Idea from Mike Kay. -->

<xsl:template match="/document-element">
  <xsl:variable name="whichtmplt" select="'this'"/>
  <out>
    <xsl:apply-templates select="document('')/*/xsl:template[@name=$whichtmplt]"/>
    <xsl:apply-templates/>
  </out>
</xsl:template>

<xsl:template name="this" match="xsl:template[@name='this']">We are inside.
  <xsl:value-of select="name(.)"/>
</xsl:template>

<xsl:template name="that" match="xsl:template[@name='that']">We are offside.
  <xsl:value-of select="name(.)"/>
</xsl:template>

<xsl:template name="the_other" match="*">We are generic.
  <xsl:value-of select="name(.)"/>
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
