<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!-- FileName: select04 -->
  <!-- Document: http://www.w3.org/TR/xpath -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 2 -->
  <!-- Purpose: Test of unions, returned in document order. -->

<xsl:template match="/">
  <out>
    <xsl:apply-templates/>
  </out>
</xsl:template>

<xsl:template match="doc">
  This should come out fasolatido:
  <xsl:apply-templates select="fa"/>
  This should come out doremifasolatido:
  <xsl:apply-templates select="mi | do | fa | re"/>
  This should come out do-do-remi-mi1-mi2fasolatido-fa--so-:
  <xsl:apply-templates select="mi[@mi2='mi2'] | do | fa/so/@so | fa | mi/@* | re | fa/@fa | do/@do"/>
  This should come out solatidoG#:
  <xsl:apply-templates select=".//*[@so]"/>
  This should come out relatidoABb:
  <xsl:apply-templates select="*//la | //Bflat | re"/>
  This should come out domitiACD:
  <xsl:apply-templates select="fa/../mi | Aflat/natural/la | Csharp//* | /doc/do | *//ti"/>
</xsl:template>

<xsl:template match="@*">
  <xsl:value-of select="."/>
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
