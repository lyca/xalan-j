<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:baz1="http://xsl.lotus.com/ns1"
                xmlns:baz2="http://xsl.lotus.com/ns2"
                exclude-result-prefixes="baz1 baz2">

  <!-- FileName: namespace11 -->
  <!-- Document: http://www.w3.org/TR/xpath -->
  <!-- DocVersion: 19991116 -->
  <!-- Section: 4.1 Node Set Functions -->
  <!-- Creator: Paul Dick -->
  <!-- Purpose: Test of 'namespace-uri()' function, with hyphenated node name. -->

<xsl:template match="baz2:doc-one">
 <out>
	0 <xsl:value-of select="namespace-uri()"/>:
	1 <xsl:value-of select="namespace-uri(baz1:a-two)" xmlns:baz1="http://xsl.lotus.com/ns1" xmlns:baz2="http://xsl.lotus.com/ns2"/>:
	2 <xsl:value-of select="namespace-uri(baz1:a-two)"/>:
	3 <xsl:value-of select="namespace-uri(baz1:a-two/@attrib1)"/>:
	4 <xsl:value-of select="namespace-uri(baz2:b-three)"/>:
	5 <xsl:value-of select="namespace-uri(baz2:b-three/@baz1:attrib2)"/>:
	6 <xsl:value-of select="namespace-uri(baz2:b-three/c-four)"/>:
	7 <xsl:value-of select="namespace-uri(bogus)"/>:
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
