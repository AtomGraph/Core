<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->
<!DOCTYPE xsl:stylesheet [
    <!ENTITY java "http://xml.apache.org/xalan/java/">
    <!ENTITY g "http://graphity.org/ontology/">
    <!ENTITY rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <!ENTITY rdfs "http://www.w3.org/2000/01/rdf-schema#">
    <!ENTITY xsd "http://www.w3.org/2001/XMLSchema#">
    <!ENTITY sparql "http://www.w3.org/2005/sparql-results#">
]>
<xsl:stylesheet version="2.0"
xmlns="http://www.w3.org/1999/xhtml"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xhtml="http://www.w3.org/1999/xhtml"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:g="&g;"
xmlns:rdf="&rdf;"
xmlns:rdfs="&rdfs;"
xmlns:xsd="&xsd;"
xmlns:sparql="&sparql;"
xmlns:url="&java;java.net.URLDecoder"
exclude-result-prefixes="#all">

    <!-- subject/object resource -->
    <xsl:template match="@rdf:about">
	<a href="{.}" title="{.}">
	    <xsl:apply-templates select="." mode="g:LabelMode"/>
	</a>
    </xsl:template>

    <xsl:template match="@rdf:nodeID">
	<span id="{.}" title="{.}">
	    <xsl:apply-templates select="." mode="g:LabelMode"/>
	</span>
    </xsl:template>

    <!-- object resource -->
    <xsl:template match="@rdf:resource | sparql:uri">
	<a href="{.}" title="{.}">
	    <xsl:variable name="doc" select="document(g:document-uri(.))"/>
	    <xsl:choose>
		<xsl:when test="key('resources', ., $doc)/@rdf:about">
		    <xsl:apply-templates select="key('resources', ., $doc)/@rdf:about" mode="g:LabelMode"/>
		</xsl:when>
		<xsl:when test="key('resources', ., $doc)/@rdf:nodeID">
		    <xsl:apply-templates select="key('resources', ., $doc)/@rdf:nodeID" mode="g:LabelMode"/>
		</xsl:when>
		<xsl:when test="key('resources', ., $ont-model)/@rdf:about">
		    <xsl:apply-templates select="key('resources', ., $ont-model)/@rdf:about" mode="g:LabelMode"/>
		</xsl:when>
		<xsl:when test="key('resources', ., $ont-model)/@rdf:nodeID">
		    <xsl:apply-templates select="key('resources', ., $ont-model)/@rdf:nodeID" mode="g:LabelMode"/>
		</xsl:when>
		<xsl:when test="contains(., '#') and not(ends-with(., '#'))">
		    <xsl:value-of select="substring-after(., '#')"/>
		</xsl:when>
		<xsl:when test="string-length(tokenize(., '/')[last()]) &gt; 0">
		    <xsl:value-of select="translate(url:decode(tokenize(., '/')[last()], 'UTF-8'), '_', ' ')"/>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:value-of select="."/>
		</xsl:otherwise>
	    </xsl:choose>
	</a>
    </xsl:template>

    <!-- property -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*">
	<xsl:variable name="this" select="xs:anyURI(concat(namespace-uri(.), local-name(.)))" as="xs:anyURI"/>
	<span title="{$this}">
	    <xsl:apply-templates select="." mode="g:LabelMode"/>
	</span>
    </xsl:template>

    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*" mode="g:LabelMode">
	<xsl:variable name="this" select="xs:anyURI(concat(namespace-uri(.), local-name(.)))" as="xs:anyURI"/>
	<span title="{$this}">
	    <xsl:variable name="doc" select="document(g:document-uri($this))"/>
	    <xsl:choose>
		<xsl:when test="key('resources', $this, $doc)/@rdf:about">
		    <xsl:apply-templates select="key('resources', $this, $doc)/@rdf:about" mode="g:LabelMode"/>
		</xsl:when>
		<xsl:when test="starts-with($this, $base-uri)">
		    <xsl:apply-templates select="key('resources', $this, $ont-model)/@rdf:about" mode="g:LabelMode"/>
		</xsl:when>
		<xsl:when test="contains($this, '#') and not(ends-with($this, '#'))">
		    <xsl:value-of select="substring-after($this, '#')"/>
		</xsl:when>
		<xsl:when test="string-length(tokenize($this, '/')[last()]) &gt; 0">
		    <xsl:value-of select="translate(url:decode(tokenize($this, '/')[last()], 'UTF-8'), '_', ' ')"/>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:value-of select="."/>
		</xsl:otherwise>
	    </xsl:choose>
	</span>
    </xsl:template>

    <!-- object blank node (avoid infinite loop) -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*/@rdf:nodeID">
	<xsl:apply-templates select="key('resources', .)[not(@rdf:nodeID = current()/../../@rdf:nodeID)]"/>
    </xsl:template>

    <!-- object literal -->
    <xsl:template match="text()">
	<xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="text()[../@rdf:datatype] | sparql:literal[@datatype]">
	<span title="{../@rdf:datatype | @datatype}">
	    <xsl:value-of select="."/>
	</span>
    </xsl:template>

    <xsl:template match="text()[../@rdf:datatype = '&xsd;float'] | text()[../@rdf:datatype = '&xsd;double'] | sparql:literal[@datatype = '&xsd;float'] | sparql:literal[@datatype = '&xsd;double']" priority="1">
	<span title="{../@rdf:datatype}">
	    <xsl:value-of select="format-number(., '#####.00')"/>
	</span>
    </xsl:template>

    <xsl:template match="text()[../@rdf:datatype = '&xsd;date'] | sparql:literal[@datatype = '&xsd;date']" priority="1">
	<span title="{../@rdf:datatype}">
	    <xsl:value-of select="format-date(., '[D] [MNn] [Y]', $lang, (), ())"/>
	</span>
    </xsl:template>

    <xsl:template match="text()[../@rdf:datatype = '&xsd;dateTime'] | sparql:literal[@datatype = '&xsd;dateTime']" priority="1">
	<!-- http://www.w3.org/TR/xslt20/#date-time-examples -->
	<!-- http://en.wikipedia.org/wiki/Date_format_by_country -->
	<span title="{../@rdf:datatype}">
	    <xsl:value-of select="format-dateTime(., '[D] [MNn] [Y] [H01]:[m01]', $lang, (), ())"/>
	</span>
    </xsl:template>

</xsl:stylesheet>