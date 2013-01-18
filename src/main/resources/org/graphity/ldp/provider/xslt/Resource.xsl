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
    <!ENTITY gldp "http://ldp.graphity.org/ontology/">
    <!ENTITY rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <!ENTITY rdfs "http://www.w3.org/2000/01/rdf-schema#">
    <!ENTITY owl "http://www.w3.org/2002/07/owl#">
    <!ENTITY xsd "http://www.w3.org/2001/XMLSchema#">
    <!ENTITY sparql "http://www.w3.org/2005/sparql-results#">
    <!ENTITY ldp "http://www.w3.org/ns/ldp#">
    <!ENTITY geo "http://www.w3.org/2003/01/geo/wgs84_pos#">
    <!ENTITY dbpedia-owl "http://dbpedia.org/ontology/">
    <!ENTITY dc "http://purl.org/dc/elements/1.1/">
    <!ENTITY dct "http://purl.org/dc/terms/">
    <!ENTITY foaf "http://xmlns.com/foaf/0.1/">
    <!ENTITY sioc "http://rdfs.org/sioc/ns#">
    <!ENTITY skos "http://www.w3.org/2004/02/skos/core#">
    <!ENTITY sp "http://spinrdf.org/sp#">
    <!ENTITY spin "http://spinrdf.org/spin#">
    <!ENTITY sd "http://www.w3.org/ns/sparql-service-description#">
    <!ENTITY list "http://jena.hpl.hp.com/ARQ/list#">
    <!ENTITY xhv "http://www.w3.org/1999/xhtml/vocab#">
]>
<xsl:stylesheet version="2.0"
xmlns="http://www.w3.org/1999/xhtml"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xhtml="http://www.w3.org/1999/xhtml"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:g="&g;"
xmlns:gldp="&gldp;"
xmlns:rdf="&rdf;"
xmlns:rdfs="&rdfs;"
xmlns:owl="&owl;"
xmlns:sparql="&sparql;"
xmlns:ldp="&ldp;"
xmlns:geo="&geo;"
xmlns:dbpedia-owl="&dbpedia-owl;"
xmlns:dc="&dc;"
xmlns:dct="&dct;"
xmlns:foaf="&foaf;"
xmlns:sioc="&sioc;"
xmlns:skos="&skos;"
xmlns:sp="&sp;"
xmlns:spin="&spin;"
xmlns:sd="&sd;"
xmlns:list="&list;"
xmlns:xhv="&xhv;"
xmlns:url="&java;java.net.URLDecoder"
exclude-result-prefixes="#all">

    <xsl:import href="imports/default.xsl"/>
    <xsl:import href="group-sort-triples.xsl"/>
    
    <xsl:include href="imports/foaf.xsl"/>
    <xsl:include href="imports/dbpedia-owl.xsl"/>
    <xsl:include href="functions.xsl"/>

    <xsl:output method="xhtml" encoding="UTF-8" indent="yes" omit-xml-declaration="yes" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN" media-type="application/xhtml+xml"/>
    
    <xsl:preserve-space elements="pre"/>

    <xsl:param name="base-uri" as="xs:anyURI"/>
    <xsl:param name="absolute-path" as="xs:anyURI"/>
    <xsl:param name="request-uri" as="xs:anyURI"/>
    <xsl:param name="http-headers" as="xs:string"/>

    <xsl:param name="lang" select="'en'" as="xs:string"/>

    <xsl:param name="mode" select="$resource/g:mode/@rdf:resource" as="xs:anyURI?"/>
    <xsl:param name="ont-model" as="document-node()"/> <!-- select="document($base-uri)"  -->
    <xsl:param name="offset" select="$select-res/sp:offset" as="xs:integer?"/>
    <xsl:param name="limit" select="$select-res/sp:limit" as="xs:integer?"/>
    <xsl:param name="order-by" select="key('resources', $orderBy/sp:expression/@rdf:resource)/sp:varName" as="xs:string?"/>
    <xsl:param name="desc" select="$orderBy[1]/rdf:type/@rdf:resource = '&sp;Desc'" as="xs:boolean"/>

    <xsl:param name="query" as="xs:string?"/>

    <xsl:variable name="ont-uri" select="resolve-uri('ontology/', $base-uri)" as="xs:anyURI"/>
    <xsl:variable name="resource" select="key('resources', $request-uri, $ont-model)" as="element()?"/>
    <xsl:variable name="ont-resource" select="key('resources', $absolute-path, $ont-model)" as="element()?"/>
    <xsl:variable name="query-res" select="key('resources', $resource/spin:query/@rdf:resource | $resource/spin:query/@rdf:nodeID, $ont-model)" as="element()?"/>
    <xsl:variable name="where-res" select="list:member(key('resources', $query-res/sp:where/@rdf:nodeID, $ont-model), $ont-model)"/>
    <xsl:variable name="select-res" select="key('resources', $where-res/sp:query/@rdf:resource | $where-res/sp:query/@rdf:nodeID, $ont-model)" as="element()?"/>
    <xsl:variable name="orderBy" select="if ($select-res/sp:orderBy) then list:member(key('resources', $select-res/sp:orderBy/@rdf:nodeID), /) else ()"/>
    
    <xsl:key name="resources" match="*[*][@rdf:about] | *[*][@rdf:nodeID]" use="@rdf:about | @rdf:nodeID"/>
    <xsl:key name="predicates" match="*[@rdf:about]/* | *[@rdf:nodeID]/*" use="concat(namespace-uri(.), local-name(.))"/>
    <xsl:key name="predicates-by-object" match="*[@rdf:about]/* | *[@rdf:nodeID]/*" use="@rdf:about | @rdf:nodeID"/>
    <xsl:key name="resources-by-host" match="*[@rdf:about]" use="sioc:has_host/@rdf:resource"/>
    <xsl:key name="resources-by-page-of" match="*[@rdf:about]" use="ldp:pageOf/@rdf:resource"/>
 
    <rdf:Description rdf:nodeID="previous">
	<rdfs:label xml:lang="en">Previous</rdfs:label>
    </rdf:Description>

    <rdf:Description rdf:nodeID="next">
	<rdfs:label xml:lang="en">Next</rdfs:label>
    </rdf:Description>

    <xsl:template match="/">
	<html>
	    <head>
		<title>
		    <xsl:apply-templates mode="gldp:TitleMode"/>
		</title>
		<base href="{$base-uri}" />
		
		<xsl:for-each select="key('resources', $base-uri, $ont-model)">
		    <meta name="author" content="{dct:creator/@rdf:resource}"/>
		    <meta name="description" content="{dct:description}" xml:lang="{dct:description/@xml:lang}" lang="{dct:description/@xml:lang}"/>
		</xsl:for-each>
		<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
		
		<link href="static/css/bootstrap.css" rel="stylesheet"/>
		<link href="static/css/bootstrap-responsive.css" rel="stylesheet"/>
		
		<style type="text/css">
		    <![CDATA[
			body { padding-top: 60px; padding-bottom: 40px; }
			form.form-inline { margin: 0; }
			ul.inline { margin-left: 0; }
			.inline li { display: inline; }
			.well-small { background-color: #FAFAFA ; }
			textarea#query-string { font-family: monospace; }
		    ]]>
		</style>
		
		<xsl:apply-templates mode="gldp:ScriptMode"/>
      	    </head>
	    <body>
		<div class="navbar navbar-fixed-top">
		    <div class="navbar-inner">
			<div class="container-fluid">    
			    <a class="brand" href="{$base-uri}">
				<xsl:apply-templates select="key('resources', $base-uri, $ont-model)" mode="g:LabelMode"/>
			    </a>

			    <div class="nav-collapse">
				<ul class="nav">
				    <!-- make menu links for all resources in the ontology, except base URI -->
				    <xsl:for-each select="key('resources-by-host', $base-uri, $ont-model)/@rdf:about[not(. = $base-uri)]">
					<xsl:sort select="g:label(., /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
					<li>
					    <xsl:if test=". = $absolute-path">
						<xsl:attribute name="class">active</xsl:attribute>
					    </xsl:if>
					    <xsl:apply-templates select="."/>
					</li>
				    </xsl:for-each>
				</ul>

				<!--
				<form class="navbar-search pull-left" action="search" method="get">
				    <input class="search-query span2" name="query" type="text" placeholder="Search"/>
				</form>
				-->
			    </div>
			</div>
		    </div>
		</div>

		<div class="container-fluid">
		    <div class="row-fluid">
			<xsl:variable name="grouped-rdf">
			    <xsl:apply-templates mode="g:GroupTriples"/>
			</xsl:variable>
			<xsl:apply-templates select="$grouped-rdf/rdf:RDF"/>
		    </div>
		    
		    <div class="footer">
			<p>
			    <xsl:value-of select="format-date(current-date(), '[Y]', $lang, (), ())"/>
			</p>
		    </div>
		</div>
	    </body>
	</html>
    </xsl:template>

    <xsl:template match="rdf:RDF" mode="gldp:TitleMode">
	<xsl:apply-templates select="key('resources', $base-uri, $ont-model)" mode="g:LabelMode"/>
	<xsl:text> - </xsl:text>
	<xsl:apply-templates select="key('resources', $absolute-path, $ont-model)" mode="gldp:TitleMode"/>
    </xsl:template>

    <xsl:template match="*[@rdf:about]" mode="gldp:TitleMode">
	<xsl:apply-templates select="." mode="g:LabelMode"/>
    </xsl:template>

    <xsl:template match="rdf:RDF" mode="gldp:ScriptMode">
    </xsl:template>

    <xsl:template match="rdf:RDF">
	<div class="span8">
	    <xsl:choose>
		<xsl:when test="$mode = '&g;ListMode'">
		    <xsl:apply-templates select="." mode="g:ListMode"/>
		</xsl:when>
		<xsl:when test="$mode = '&g;TableMode'">
		    <xsl:apply-templates select="." mode="g:TableMode"/>
		</xsl:when>
		<xsl:when test="$mode = '&g;InputMode'">
		    <xsl:apply-templates select="." mode="g:InputMode"/>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:apply-templates select="key('resources', $absolute-path)"/>
		    <!-- apply all other URI resources -->
		    <xsl:apply-templates select="*[not(@rdf:about = $absolute-path)][not(key('predicates-by-object', @rdf:nodeID))]"/>
		</xsl:otherwise>
	    </xsl:choose>
	</div>
	
	<div class="span4">
	    <xsl:for-each-group select="*/*" group-by="concat(namespace-uri(.), local-name(.))">
		<xsl:sort select="g:label(xs:anyURI(concat(namespace-uri(.), local-name(.))), /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
		<xsl:apply-templates select="current-group()[1]" mode="gldp:SidebarNavMode"/>
	    </xsl:for-each-group>
	</div>
    </xsl:template>

    <xsl:template match="*" mode="gldp:ModeSelectMode"/>
	
    <xsl:template match="sioc:Container | *[rdf:type/@rdf:resource = '&sioc;Container']" mode="gldp:ModeSelectMode" priority="1">
	<ul class="nav nav-tabs">
	    <li>
		<xsl:if test="$mode = '&g;ListMode'">
		    <xsl:attribute name="class">active</xsl:attribute>
		</xsl:if>

		<a href="{@rdf:about}{g:query-string($offset, $limit, $order-by, $desc, (), '&g;ListMode')}">
		    <xsl:apply-templates select="key('resources', '&g;ListMode', document('&g;'))" mode="g:LabelMode"/>
		</a>
	    </li>
	    <li>
		<xsl:if test="$mode = '&g;TableMode'">
		    <xsl:attribute name="class">active</xsl:attribute>
		</xsl:if>

		<a href="{@rdf:about}{g:query-string($offset, $limit, $order-by, $desc, (), '&g;TableMode')}">
		    <xsl:apply-templates select="key('resources', '&g;TableMode', document('&g;'))" mode="g:LabelMode"/>
		</a>
	    </li>
	    <li>
		<xsl:if test="$mode = '&g;InputMode'">
		    <xsl:attribute name="class">active</xsl:attribute>
		</xsl:if>

		<a href="{@rdf:about}{g:query-string($offset, $limit, $order-by, $desc, (), '&g;InputMode')}">
		    <xsl:apply-templates select="key('resources', '&g;InputMode', document('&g;'))" mode="g:LabelMode"/>
		</a>
	    </li>
	</ul>
    </xsl:template>

    <!-- TO-DO: make reusable with match="@rdf:about" - same as in gldp:HeaderMode -->
    <xsl:template match="rdf:RDF" mode="gldp:MediaTypeSelectMode">
	<div class="btn-group pull-right">
	    <!--
	    <xsl:if test="@rdf:about != $absolute-path">
		<a href="{@rdf:about}" class="btn">Source</a>
	    </xsl:if>
	    -->
	    <a href="{resolve-uri('sparql', $base-uri)}?query={encode-for-uri($query-res/sp:text)}" class="btn">SPARQL</a>
	    <a href="{$absolute-path}?accept={encode-for-uri('application/rdf+xml')}" class="btn">RDF/XML</a>
	    <a href="{$absolute-path}?accept={encode-for-uri('text/turtle')}" class="btn">Turtle</a>
	</div>
    </xsl:template>
    
    <!-- subject -->
    <xsl:template match="*[*][@rdf:about] | *[*][@rdf:nodeID]">
	<xsl:apply-templates select="." mode="gldp:HeaderMode"/>

	<div class="row-fluid">
	    <div class="span6">
		<xsl:apply-templates select="." mode="gldp:PropertyListMode"/>
	    </div>

	    <div class="span6">
		<xsl:apply-templates select="." mode="gldp:TypeMode"/>
	    </div>
	</div>
    </xsl:template>

    <xsl:template match="rdf:type/@rdf:resource">
	<span title="{.}" class="btn">
	    <xsl:apply-imports/>
	</span>
    </xsl:template>

    <!-- HEADER MODE -->
	
    <xsl:template match="*[*][@rdf:about] | *[*][@rdf:nodeID]" mode="gldp:HeaderMode" priority="1">
	<div class="well">
	    <xsl:apply-templates mode="gldp:HeaderImageMode"/>

	    <xsl:apply-templates select="@rdf:about | @rdf:nodeID" mode="gldp:HeaderMode"/>

	    <xsl:if test="rdfs:comment[lang($lang) or not(@xml:lang)] or dc:description[lang($lang) or not(@xml:lang)] or dct:description[lang($lang) or not(@xml:lang)] or dbpedia-owl:abstract[lang($lang) or not(@xml:lang)] or sioc:content[lang($lang) or not(@xml:lang)]">
		<p>
		    <xsl:choose>
			<xsl:when test="rdfs:comment[lang($lang) or not(@xml:lang)]">
			    <xsl:value-of select="rdfs:comment[lang($lang) or not(@xml:lang)][1]"/>
			</xsl:when>
			<xsl:when test="dc:description[lang($lang) or not(@xml:lang)]">
			    <xsl:value-of select="dc:description[lang($lang) or not(@xml:lang)][1]"/>
			</xsl:when>
			<xsl:when test="dct:description[lang($lang) or not(@xml:lang)]">
			    <xsl:value-of select="dct:description[lang($lang) or not(@xml:lang)][1]"/>
			</xsl:when>
			<xsl:when test="dbpedia-owl:abstract[lang($lang) or not(@xml:lang)][1]">
			    <xsl:value-of select="dbpedia-owl:abstract[lang($lang) or not(@xml:lang)][1]"/>
			</xsl:when>
			<xsl:when test="sioc:content[lang($lang) or not(@xml:lang)]">
			    <xsl:value-of select="substring(sioc:content[lang($lang) or not(@xml:lang)][1], 1, 300)"/>
			</xsl:when>
		    </xsl:choose>
		</p>
	    </xsl:if>

	    <!-- xsl:apply-templates? -->
	    <xsl:if test="rdf:type">
		<ul class="inline">
		    <xsl:apply-templates select="rdf:type" mode="gldp:HeaderMode">
			<xsl:sort select="g:label(@rdf:resource | @rdf:nodeID, /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
		    </xsl:apply-templates>
		</ul>
	    </xsl:if>
	</div>
    </xsl:template>

    <xsl:template match="@rdf:about[. = $absolute-path]" mode="gldp:HeaderMode">
	<h1 class="page-header">
	    <xsl:apply-templates select="."/>
	</h1>
    </xsl:template>

    <xsl:template match="@rdf:about | @rdf:nodeID" mode="gldp:HeaderMode">
	<h2>
	    <xsl:apply-templates select="."/>
	</h2>
    </xsl:template>

    <xsl:template match="rdf:type" mode="gldp:HeaderMode">
	<li>
	    <xsl:apply-templates select="@rdf:resource | @rdf:nodeID"/>
	</li>
    </xsl:template>

    <!-- HEADER IMAGE MODE -->

    <!-- ignore all other properties -->
    <xsl:template match="*" mode="gldp:HeaderImageMode"/>
	
    <xsl:template match="foaf:img | foaf:depiction | foaf:thumbnail | foaf:logo" mode="gldp:HeaderImageMode" priority="1">
	<p>
	    <xsl:apply-templates select="@rdf:resource"/>
	</p>
    </xsl:template>

    <!-- PROPERTY LIST MODE -->

    <xsl:template match="*[*][@rdf:about] | *[*][@rdf:nodeID]" mode="gldp:PropertyListMode" priority="1">
	<xsl:variable name="no-domain-properties" select="*[not(rdfs:domain(xs:anyURI(concat(namespace-uri(.), local-name(.)))) = current()/rdf:type/@rdf:resource)]"/>

	<xsl:if test="$no-domain-properties">
	    <div class="well well-small">
		<dl>
		    <xsl:apply-templates select="$no-domain-properties" mode="gldp:PropertyListMode">
			<xsl:sort select="g:label(xs:anyURI(concat(namespace-uri(.), local-name(.))), /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
			<xsl:sort select="if (@rdf:resource) then (g:label(@rdf:resource, /, $lang)) else text()" data-type="text" order="ascending" lang="{$lang}"/> <!-- g:label(@rdf:nodeID, /, $lang) -->
		    </xsl:apply-templates>
		</dl>
	    </div>
	</xsl:if>
    </xsl:template>

    <!-- has to match the previous template pattern - can this be made smarter? -->
    <xsl:template match="rdf:type | foaf:img | foaf:depiction | foaf:logo | owl:sameAs | rdfs:label | rdfs:comment | rdfs:seeAlso | dc:title | dct:title | dc:description | dct:description | dct:subject | dbpedia-owl:abstract | sioc:content" mode="gldp:PropertyListMode" priority="1"/>

    <!-- property -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*" mode="gldp:PropertyListMode">
	<dt>
	    <xsl:apply-templates select="."/>
	</dt>
	<dd>
	    <xsl:apply-templates select="node() | @rdf:resource | @rdf:nodeID" mode="gldp:PropertyListMode"/>
	</dd>
    </xsl:template>

    <xsl:template match="node() | @rdf:resource" mode="gldp:PropertyListMode">
	<xsl:apply-templates select="."/>
    </xsl:template>

    <!-- include blank nodes recursively but avoid infinite loop -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*/@rdf:nodeID" mode="gldp:PropertyListMode">
	<xsl:apply-templates select="key('resources', .)[not(@rdf:nodeID = current()/../../@rdf:nodeID)]"/>
    </xsl:template>

    <!-- TYPE MODE -->

    <xsl:template match="*[*][@rdf:about] | *[*][@rdf:nodeID]" mode="gldp:TypeMode">
	<xsl:variable name="domain-types" select="rdf:type/@rdf:resource[../../*/xs:anyURI(concat(namespace-uri(.), local-name(.))) = g:inDomainOf(.)]"/>

	<xsl:apply-templates select="$domain-types" mode="gldp:TypeMode">
	    <xsl:sort select="g:label(., /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
	</xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="rdf:type/@rdf:resource" mode="gldp:TypeMode">
	<xsl:variable name="in-domain-properties" select="../../*[xs:anyURI(concat(namespace-uri(.), local-name(.))) = g:inDomainOf(current()) or rdfs:domain(xs:anyURI(concat(namespace-uri(.), local-name(.)))) = xs:anyURI(current())][not(self::rdf:type)][not(self::foaf:img)][not(self::foaf:depiction)][not(self::foaf:logo)][not(self::owl:sameAs)][not(self::rdfs:label)][not(self::rdfs:comment)][not(self::rdfs:seeAlso)][not(self::dc:title)][not(self::dct:title)][not(self::dc:description)][not(self::dct:description)][not(self::dct:subject)][not(self::dbpedia-owl:abstract)][not(self::sioc:content)]"/>

	<xsl:if test="$in-domain-properties">
	    <div class="well well-small">
		<h3>
		    <!-- <xsl:apply-imports/> -->
		    <xsl:apply-templates select="."/>
		</h3>
		<dl class="well-small">
		    <xsl:apply-templates select="$in-domain-properties" mode="gldp:PropertyListMode">
			<xsl:sort select="g:label(xs:anyURI(concat(namespace-uri(.), local-name(.))), /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
			<xsl:sort select="if (@rdf:resource) then (g:label(@rdf:resource, /, $lang)) else text()" data-type="text" order="ascending" lang="{$lang}"/> <!-- g:label(@rdf:nodeID, /, $lang) -->
		    </xsl:apply-templates>
		</dl>
	    </div>
	</xsl:if>
    </xsl:template>

    <xsl:template match="@rdf:about | @rdf:resource" mode="gldp:TypeMode">
	<xsl:apply-templates select="."/>
    </xsl:template>

    <!-- SIDEBAR NAV MODE -->
    
    <xsl:template match="*[@rdf:about]" mode="gldp:SidebarNavMode">
	<xsl:apply-templates mode="gldp:SidebarNavMode">
	    <xsl:sort select="g:label(xs:anyURI(concat(namespace-uri(.), local-name(.))), /, $lang)" data-type="text" order="ascending"/>
	</xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="rdfs:seeAlso | owl:sameAs | dc:subject | dct:subject" mode="gldp:SidebarNavMode" priority="1">
	<xsl:variable name="this" select="xs:anyURI(concat(namespace-uri(.), local-name(.)))" as="xs:anyURI"/>
	
	<div class="well sidebar-nav">
	    <h2 class="nav-header">
		<a href="{$base-uri}" title="{$this}">
		    <xsl:apply-templates select="key('resources', $this, document(namespace-uri()))" mode="g:LabelMode"/>
		</a>
	    </h2>
		
	    <!-- TO-DO: fix for a single resource! -->
	    <ul class="nav nav-pills nav-stacked">
		<xsl:for-each-group select="key('predicates', $this)" group-by="@rdf:resource">
		    <xsl:sort select="g:label(@rdf:resource, /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
		    <xsl:apply-templates select="current-group()[1]/@rdf:resource" mode="gldp:SidebarNavMode"/>
		</xsl:for-each-group>
	    </ul>
	</div>
    </xsl:template>

    <!-- ignore all other properties -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*" mode="gldp:SidebarNavMode"/>

    <xsl:template match="rdfs:seeAlso/@rdf:resource | owl:sameAs/@rdf:resource | dc:subject/@rdf:resource | dct:subject/@rdf:resource" mode="gldp:SidebarNavMode">
	<li>
	    <xsl:apply-templates select="."/>
	</li>
    </xsl:template>

    <!-- PAGINATION MODE -->

    <xsl:template match="*[xhv:prev] | *[xhv:next]" mode="gldp:PaginationMode">
	<ul class="pager">
	    <li class="previous">
		<xsl:choose>
		    <xsl:when test="xhv:prev">
			<a href="{xhv:prev/@rdf:resource}" class="active">
			    &#8592; <xsl:apply-templates select="key('resources', 'previous', document(''))" mode="g:LabelMode"/>
			</a>
		    </xsl:when>
		    <xsl:otherwise>
			<xsl:attribute name="class">previous disabled</xsl:attribute>
			<a>
			    &#8592; <xsl:apply-templates select="key('resources', 'previous', document(''))" mode="g:LabelMode"/>
			</a>
		    </xsl:otherwise>
		</xsl:choose>
	    </li>
	    <li class="next">
		<xsl:choose>
		    <xsl:when test="xhv:next">
			<!-- possible to add arrows by overriding -->
			<a href="{xhv:next/@rdf:resource}">
			    <xsl:apply-templates select="key('resources', 'next', document(''))" mode="g:LabelMode"/> &#8594;
			</a>
		    </xsl:when>
		    <xsl:otherwise>
			<xsl:attribute name="class">next disabled</xsl:attribute>
			<a>
			    <xsl:apply-templates select="key('resources', 'next', document(''))" mode="g:LabelMode"/> &#8594;
			</a>
		    </xsl:otherwise>
		</xsl:choose>
	    </li>
	</ul>
    </xsl:template>

    <!-- LIST MODE -->

    <xsl:template match="rdf:RDF" mode="g:ListMode">
	<xsl:apply-templates select="key('resources', $absolute-path)" mode="gldp:HeaderMode"/>

	<xsl:apply-templates select="key('resources', $absolute-path)" mode="gldp:ModeSelectMode"/>

	<!-- page resource -->
	<xsl:apply-templates select="key('resources', $request-uri)" mode="gldp:PaginationMode"/>

	<!-- all resources that are not recursive blank nodes, except page -->
	<xsl:apply-templates select="*[not(@rdf:about = $absolute-path)][not(@rdf:about = $request-uri)][not(key('predicates-by-object', @rdf:nodeID))]" mode="g:ListMode"/>
	
	<!-- page resource -->
	<xsl:apply-templates select="key('resources', $request-uri)" mode="gldp:PaginationMode"/>
    </xsl:template>

    <xsl:template match="*[*][@rdf:about] | *[*][@rdf:nodeID]" mode="g:ListMode">
	<div class="well">
	    <xsl:apply-templates mode="gldp:ListImageMode"/>

	    <xsl:apply-templates select="@rdf:about | @rdf:nodeID" mode="gldp:HeaderMode"/>
	    
	    <xsl:if test="rdf:type">
		<ul class="inline">
		    <xsl:apply-templates select="rdf:type" mode="gldp:HeaderMode">
			<xsl:sort select="g:label(@rdf:resource | @rdf:nodeID, /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
		    </xsl:apply-templates>
		</ul>
	    </xsl:if>

	    <xsl:apply-templates select="." mode="g:DescriptionMode"/>

	    <xsl:if test="@rdf:nodeID">
		<xsl:apply-templates select="." mode="gldp:PropertyListMode"/>
	    </xsl:if>
	</div>
    </xsl:template>

    <!-- ignore all other properties -->
    <xsl:template match="*" mode="gldp:ListImageMode"/>
	
    <xsl:template match="foaf:img | foaf:depiction | foaf:thumbnail | foaf:logo" mode="gldp:ListImageMode" priority="1">
	<p>
	    <xsl:apply-templates select="@rdf:resource"/>
	</p>
    </xsl:template>

    <!-- TABLE HEADER MODE -->

    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*" mode="gldp:TableHeaderMode">
	<xsl:variable name="this" select="xs:anyURI(concat(namespace-uri(.), local-name(.)))" as="xs:anyURI"/>

	<th>
	    <a href="{$absolute-path}{g:query-string($offset, $limit, $this, $desc, (), $mode)}" title="{$this}">
		<xsl:variable name="doc" select="document(g:document-uri($this))"/>		
		<xsl:apply-templates select="key('resources', $this, $doc)" mode="g:LabelMode"/>
	    </a>
	</th>
    </xsl:template>

    <!-- TABLE MODE -->

    <xsl:template match="rdf:RDF" mode="g:TableMode">
	<xsl:apply-templates select="key('resources', $absolute-path)" mode="gldp:HeaderMode"/>

	<xsl:apply-templates select="key('resources', $absolute-path)" mode="gldp:ModeSelectMode"/>

	<!-- page resource -->
	<xsl:apply-templates select="key('resources', $request-uri)" mode="gldp:PaginationMode"/>

	<!-- loaded resources = everything except container, page, and non-root blank nodes -->
	<xsl:variable name="loaded-resources" select="*[not(@rdf:about = $absolute-path)][not(@rdf:about = $request-uri)][not(key('predicates-by-object', @rdf:nodeID))]"/>
	<xsl:variable name="predicates" as="element()*">
	    <xsl:for-each-group select="$loaded-resources/*" group-by="concat(namespace-uri(.), local-name(.))">
		<xsl:sort select="g:label(xs:anyURI(concat(namespace-uri(.), local-name(.))), /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
		<xsl:sequence select="current-group()[1]"/>
	    </xsl:for-each-group>
	</xsl:variable>

	<table class="table table-bordered table-striped">
	    <thead>
		<tr>
		    <th>
			<a href="{$absolute-path}{g:query-string($offset, $limit, (), $desc, (), $mode)}">
			    <xsl:apply-templates select="key('resources', '&rdf;Resource', document('&rdf;'))" mode="g:LabelMode"/>
			</a>
		    </th>

		    <xsl:apply-templates select="$predicates" mode="gldp:TableHeaderMode"/>
		</tr>
	    </thead>
	    <tbody>
		<xsl:apply-templates select="$loaded-resources" mode="g:TableMode"/>
	    </tbody>
	</table>
	
	<!-- page resource -->
	<xsl:apply-templates select="key('resources', $request-uri)" mode="gldp:PaginationMode"/>
    </xsl:template>

    <xsl:template match="*[*][@rdf:about] | *[*][@rdf:nodeID]" mode="g:TableMode">
	<!-- loaded resources = everything except container, page, and non-root blank nodes -->
	<xsl:variable name="loaded-resources" select="../*[not(@rdf:about = $absolute-path)][not(@rdf:about = $request-uri)][not(key('predicates-by-object', @rdf:nodeID))]"/>
	<xsl:variable name="predicates" as="element()*">
	    <xsl:for-each-group select="$loaded-resources/*" group-by="concat(namespace-uri(.), local-name(.))">
		<xsl:sort select="g:label(xs:anyURI(concat(namespace-uri(.), local-name(.))), /, $lang)" data-type="text" order="ascending" lang="{$lang}"/>
		<xsl:sequence select="current-group()[1]"/>
	    </xsl:for-each-group>
	</xsl:variable>
	
	<tr>
	    <xsl:apply-templates select="@rdf:about | @rdf:nodeID" mode="g:TableMode"/>

	    <xsl:variable name="subject" select="."/>
	    <xsl:for-each select="$predicates">
		<xsl:variable name="this" select="xs:anyURI(concat(namespace-uri(.), local-name(.)))" as="xs:anyURI"/>
		<xsl:variable name="predicate" select="$subject/*[concat(namespace-uri(.), local-name(.)) = $this]"/>
		<xsl:choose>
		    <xsl:when test="$predicate">
			<xsl:apply-templates select="$predicate" mode="g:TableMode"/>
		    </xsl:when>
		    <xsl:otherwise>
			<td></td>
		    </xsl:otherwise>
		</xsl:choose>
	    </xsl:for-each>
	</tr>
    </xsl:template>

    <xsl:template match="@rdf:about | @rdf:nodeID" mode="g:TableMode">
	<td>
	    <xsl:apply-templates select="."/>
	</td>
    </xsl:template>

    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*" mode="g:TableMode"/>

    <!-- apply properties that match lang() -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*[lang($lang)]" mode="g:TableMode" priority="1">
	<td>
	    <xsl:apply-templates select="node() | @rdf:resource | @rdf:nodeID"/>
	</td>
    </xsl:template>
    
    <!-- apply the first one in the group if there's no lang() match -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*[not(../*[concat(namespace-uri(.), local-name(.)) = concat(namespace-uri(current()), local-name(current()))][lang($lang)])][not(preceding-sibling::*[concat(namespace-uri(.), local-name(.)) = concat(namespace-uri(current()), local-name(current()))])]" mode="g:TableMode" priority="1">
	<td>
	    <xsl:apply-templates select="node() | @rdf:resource | @rdf:nodeID"/>
	</td>
    </xsl:template>

    <!-- INPUT MODE -->
    
    <xsl:template match="rdf:RDF" mode="g:InputMode">
	<xsl:apply-templates select="key('resources', $absolute-path)" mode="gldp:HeaderMode"/>

	<xsl:apply-templates select="key('resources', $absolute-path)" mode="gldp:ModeSelectMode"/>

	<form class="form-horizontal">
	    <xsl:apply-templates mode="g:InputMode"/>
	    
	    <div class="form-actions">
		<button type="submit" class="btn btn-primary">Save</button>
	    </div>
	</form>
    </xsl:template>

    <xsl:template match="*[@rdf:about] | *[@rdf:nodeID]" mode="g:InputMode">
	<fieldset>
	    <xsl:apply-templates select="@rdf:about | @rdf:nodeID" mode="g:InputMode"/>

	    <xsl:apply-templates mode="g:InputMode"/>
	</fieldset>
    </xsl:template>

    <!-- subject resource -->
    <xsl:template match="@rdf:about" mode="g:InputMode">
	<legend>
	    <xsl:apply-templates select="."/>
	</legend>

	<input type="hidden" name="su" value="{.}"/>
	<!-- <xsl:apply-templates select="."/> -->
    </xsl:template>

    <!-- subject blank node -->
    <xsl:template match="@rdf:nodeID" mode="g:InputMode">
	<legend>
	    <xsl:apply-templates select="."/>
	</legend>

	<input type="hidden" name="sb" value="{.}"/>	
    </xsl:template>

    <!-- property -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/* | *[@rdf:resource]" mode="g:InputMode">
	<xsl:variable name="this" select="xs:anyURI(concat(namespace-uri(.), local-name(.)))" as="xs:anyURI"/>
	<xsl:variable name="property" select="key('resources', $this, document(g:document-uri($this)))"/>

	<div class="control-group">
	    <label for="what" class="control-label" title="{$property/rdfs:comment}">
		<xsl:apply-templates select="$property/@rdf:about"/>
	    </label>

	    <!-- <xsl:value-of select="rdfs:range/@rdf:resource"/>!! -->

	    <div class="controls">
		<input type="hidden" name="pu" value="{$this}"/>

		<xsl:choose>
		    <xsl:when test="$property/rdf:type/@rdf:resource = '&owl;ObjectProperty'">
			<select name="ou">
			    <xsl:apply-templates select="@rdf:resource | @rdf:nodeID" mode="g:InputMode"/>
			</select>
		    </xsl:when>
		    <xsl:when test="$property/rdf:type/@rdf:resource = '&owl;DatatypeProperty'">
			<xsl:apply-templates select="text()" mode="g:InputMode"/>
		    </xsl:when>
		    <xsl:otherwise>
			<xsl:for-each select="text() | @rdf:resource | @rdf:nodeID"> <!-- node() -->
			    <input type="text" name="ol" value="{.}"/>
			</xsl:for-each>
		    </xsl:otherwise>
		</xsl:choose>
	    </div>
	</div>
    </xsl:template>

    <!-- skip <dt> for properties that are not first in the sorted group -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*[preceding-sibling::*[concat(namespace-uri(.), local-name(.)) = concat(namespace-uri(current()), local-name(current()))]]" mode="g:InputMode" priority="1">
	<xsl:variable name="this" select="xs:anyURI(concat(namespace-uri(.), local-name(.)))" as="xs:anyURI"/>
	<xsl:variable name="property" select="key('resources', $this, document(g:document-uri($this)))"/>

	<div class="control-group">
	    <div class="controls">
		<input type="hidden" name="pu" value="{$this}"/>

		<xsl:choose>
		    <xsl:when test="$property/rdf:type/@rdf:resource = '&owl;ObjectProperty'">
			<select name="ou">
			    <xsl:apply-templates select="@rdf:resource | @rdf:nodeID" mode="g:InputMode"/>
			</select>
		    </xsl:when>
		    <xsl:when test="$property/rdf:type/@rdf:resource = '&owl;DatatypeProperty'">
			<xsl:apply-templates select="text()" mode="g:InputMode"/>
		    </xsl:when>
		    <xsl:otherwise>
			<xsl:for-each select="text() | @rdf:resource | @rdf:nodeID"> <!-- node() -->
			    <input type="text" name="ol" value="{.}"/>
			</xsl:for-each>
		    </xsl:otherwise>
		</xsl:choose>
		
		<xsl:if test="@rdf:datatype | @xml:lang">
		    <span class="help-inline">
			<xsl:apply-templates select="@rdf:datatype | @xml:lang"/> <!-- datatype xor language -->
		    </span>
		</xsl:if>
	    </div>
	</div>
    </xsl:template>

    <!-- object resource -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*/@rdf:resource" mode="g:InputMode">
	<!-- <input type="hidden" name="ou" value="{.}"/> -->
	<!-- <xsl:apply-templates select="."/> -->
	<option value="{.}">
	    <xsl:apply-templates select="." mode="g:LabelMode"/>
	</option>
	
	<!--
	<select>
	    <option>New</option>
	</select>
	<button class="btn btn-primary">Add</button>
	-->
    </xsl:template>

    <!-- object blank node -->
    <xsl:template match="*[@rdf:about or @rdf:nodeID]/*/@rdf:nodeID" mode="g:InputMode">
	<input type="hidden" name="ob" value="{.}"/>
	<!-- <xsl:apply-templates select="."/> -->
	
	<!-- <xsl:apply-templates select="key('resources', .)" mode="g:InputMode"/> -->
    </xsl:template>

    <!-- object literal -->
    <xsl:template match="text()" mode="g:InputMode">
	<input type="text" name="ol" value="{.}">
	    <xsl:if test="not(../@rdf:datatype) or ../@rdf:datatype = '&xsd;string'">
		<xsl:attribute name="class">input-xxlarge</xsl:attribute>
	    </xsl:if>
	</input>
    </xsl:template>

    <xsl:template match="@rdf:datatype" mode="g:InputMode">
	<input type="text" name="lt" value="{.}"/>

	<xsl:apply-templates select="." mode="g:LabelMode"/>
    </xsl:template>

    <xsl:template match="@xml:lang" mode="g:InputMode">
	<input type="text" name="ll" value="{.}"/>

	<xsl:apply-templates select="." mode="g:LabelMode"/>
    </xsl:template>

    <xsl:function name="rdfs:range" as="xs:anyURI*">
	<xsl:param name="property-uri" as="xs:anyURI+"/>
	<!-- <xsl:message>$property-uri: <xsl:value-of select="$property-uri"/></xsl:message> -->
	<xsl:for-each select="$property-uri">
	    <xsl:for-each select="document(g:document-uri($property-uri))">
		<xsl:sequence select="key('resources', $property-uri)/rdfs:range/@rdf:resource"/>
	    </xsl:for-each>
	</xsl:for-each>
    </xsl:function>

    <!-- LABEL MODE -->
    <xsl:template match="*[@rdf:about] | *[@rdf:nodeID]" mode="g:LabelMode">
	<xsl:choose>
	    <xsl:when test="rdfs:label[lang($lang)]">
		<xsl:value-of select="rdfs:label[lang($lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="foaf:nick[lang($lang)]">
		<xsl:value-of select="foaf:nick[lang($lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="foaf:name[lang($lang)]">
		<xsl:value-of select="foaf:name[lang($lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="dc:title[lang($lang)]">
		<xsl:value-of select="dc:title[lang($lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="dct:title[lang($lang)]">
		<xsl:value-of select="dct:title[lang($lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="skos:prefLabel[lang($lang)]">
		<xsl:value-of select="skos:prefLabel[lang($lang)][1]"/>
	    </xsl:when>
	    
	    <xsl:when test="rdfs:label[not(@xml:lang)]">
		<xsl:value-of select="rdfs:label[not(@xml:lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="foaf:nick[not(@xml:lang)]">
		<xsl:value-of select="foaf:nick[not(@xml:lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="foaf:name[not(@xml:lang)]">
		<xsl:value-of select="foaf:name[not(@xml:lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="dc:title[not(@xml:lang)]">
		<xsl:value-of select="dc:title[not(@xml:lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="dct:title[not(@xml:lang)]">
		<xsl:value-of select="dct:title[not(@xml:lang)][1]"/>
	    </xsl:when>
	    <xsl:when test="skos:prefLabel[not(@xml:lang)]">
		<xsl:value-of select="skos:prefLabel[not(@xml:lang)][1]"/>
	    </xsl:when>

	    <xsl:when test="rdfs:label | @rdfs:label">
		<xsl:value-of select="(rdfs:label | @rdfs:label)[1]"/>
	    </xsl:when>
	    <xsl:when test="foaf:nick | @foaf:nick">
		<xsl:value-of select="(foaf:nick | @foaf:nick)[1]"/>
	    </xsl:when>
	    <xsl:when test="foaf:firstName and foaf:lastName">
		<xsl:value-of select="concat(foaf:firstName[1], ' ', foaf:lastName[1])"/>
	    </xsl:when>
	    <xsl:when test="foaf:name | @foaf:name">
		<xsl:value-of select="(foaf:name | @foaf:name)[1]"/>
	    </xsl:when>
	    <xsl:when test="dc:title | @dc:title">
		<xsl:value-of select="(dc:title | @dc:title)[1]"/>
	    </xsl:when>
	    <xsl:when test="dct:title | @dct:title">
		<xsl:value-of select="(dct:title | @dct:title)[1]"/>
	    </xsl:when>
	    <xsl:when test="skos:prefLabel | @skos:prefLabel">
		<xsl:value-of select="(skos:prefLabel | @skos:prefLabel)[1]"/>
	    </xsl:when>
	    <xsl:when test="contains(@rdf:about, '#') and not(ends-with(@rdf:about, '#'))">
		<xsl:value-of select="substring-after(@rdf:about, '#')"/>
	    </xsl:when>
	    <xsl:when test="string-length(tokenize(@rdf:about, '/')[last()]) &gt; 0">
		<xsl:value-of select="translate(url:decode(tokenize(@rdf:about, '/')[last()], 'UTF-8'), '_', ' ')"/>
	    </xsl:when>
	    <xsl:otherwise>
		<xsl:value-of select="@rdf:about | @rdf:nodeID"/>
	    </xsl:otherwise>
	</xsl:choose>
    </xsl:template>

    <xsl:template match="*[@rdf:about] | *[@rdf:nodeID]" mode="g:DescriptionMode">
	<xsl:if test="rdfs:comment[lang($lang) or not(@xml:lang)] or dc:description[lang($lang) or not(@xml:lang)] or dct:description[lang($lang) or not(@xml:lang)] or dbpedia-owl:abstract[lang($lang) or not(@xml:lang)] or sioc:content[lang($lang) or not(@xml:lang)]">
	    <p>
		<xsl:choose>
		    <xsl:when test="rdfs:comment[lang($lang)]">
			<xsl:value-of select="substring(rdfs:comment[lang($lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="dc:description[lang($lang)]">
			<xsl:value-of select="substring(dc:description[lang($lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="dct:description[lang($lang)]">
			<xsl:value-of select="substring(dct:description[lang($lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="dbpedia-owl:abstract[lang($lang)]">
			<xsl:value-of select="substring(dbpedia-owl:abstract[lang($lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="sioc:content[lang($lang)]">
			<xsl:value-of select="substring(sioc:content[lang($lang)][1], 1, 300)"/>
		    </xsl:when>
		    
		    <xsl:when test="rdfs:comment[not(@xml:lang)]">
			<xsl:value-of select="substring(rdfs:comment[not(@xml:lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="dc:description[not(@xml:lang)]">
			<xsl:value-of select="substring(dc:description[not(@xml:lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="dct:description[not(@xml:lang)]">
			<xsl:value-of select="substring(dct:description[not(@xml:lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="dbpedia-owl:abstract[not(@xml:lang)]">
			<xsl:value-of select="substring(dbpedia-owl:abstract[not(@xml:lang)][1], 1, 300)"/>
		    </xsl:when>
		    <xsl:when test="sioc:content[not(@xml:lang)]">
			<xsl:value-of select="substring(sioc:content[not(@xml:lang)][1], 1, 300)"/>
		    </xsl:when>
		</xsl:choose>
	    </p>
	</xsl:if>
    </xsl:template>

</xsl:stylesheet>