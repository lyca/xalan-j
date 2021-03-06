<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:ext="http://somebody.elses.extension"
    xmlns:java="http://xml.apache.org/xslt/java"
    xmlns:ped="http://tester.com"
    xmlns:bdd="http://buster.com"
    xmlns="www.lotus.com"
    exclude-result-prefixes="java jad #default"
    extension-element-prefixes="ext">

  <!-- FileName: lreerr01 -->
  <!-- Document: http://www.w3.org/TR/xslt -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 7.1.1 Literal Result Elements-->
  <!-- Creator: Paul Dick -->
  <!-- Purpose: It is an error if there is no namespace bound to the prefix named in
       the exclude-result-prefixes attribute of the stylesheet. -->
  <!-- Note: SCurcuru 28-Feb-00 added ExpectedException; seems like good error text to me. -->
  <!-- ExpectedException: org.apache.xalan.xslt.XSLProcessorException: Prefix in exclude-result-prefixes is not valid: jad -->
  <!-- ExpectedException: Prefix in exclude-result-prefixes is not valid: jad -->

<xsl:template match="doc">
  <out xsl:if= "my if" english="to leave"/>
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
