Description
===========

Graphity LDP is a fully extensible generic Linked Data platform for building Web applications.
It can be used for publishing and analysis of open data, as well as import and integration of private user data.

The platform supports standard RDF access methods such as Linked Data and SPARQL endpoints, and includes plugin mechanisms for importing file formats and APIs as RDF.

Features
--------

* interfaces and base classes for rapid building of Linked Data webapps
* high- and low-level access to remote Linked Data resources and SPARQL endpoints
* providers for input and output of RDF data, either raw or via XSLT transformations
* behind-the-scenes access of non-Linked Data resources via GRDDL
* mapping and resolution of URIs to known schemas/ontologies
* HTTP caching & authentication
* easy XSLT transformation and SPARQL query building

Installation
============

Graphity LDP is a [Maven Web application](http://maven.apache.org/guides/mini/guide-webapp.html).

You have the following options to install Graphity:
* checkout the source code from the Git repository and build it as a Maven webapp
* [download](https://github.com/Graphity/graphity-ldp/downloads) the project as a `.jar` library and include it in your Java project (Maven repository is not available yet)

Java code
=========

Linked Data Platform
--------------------

It contains JAX-RS-compatible convenience classes for handling Linked Data requests; writing out Linked Data and SPARQL responses either raw or via XSLT; and more.

* [`org.graphity.ldp`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp)
    * [`org.graphity.ldp.Application`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/Application.java): Base class for all LDP applications -- use for subclassing
    * [`org.graphity.ldp.model`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/model): LDP interfaces
        * [`org.graphity.ldp.model.ContainerResource`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/model/ContainerResource.java): Base interface for Linked Data container resources
        * [`org.graphity.ldp.model.Resource`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/model/Resource.java): Base interface for all Linked Data-serving JAX-RS resources
        * [`org.graphity.ldp.model.impl`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/model/impl): Abstract LDP interface implementing base classes -- use for subclassing
    * [`org.graphity.ldp.provider`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/provider): Generic `Provider` classes for reading request/writing `Response`
        * [`org.graphity.ldp.provider.ModelProvider`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/provider/ModelProvider.java): Reads `Model` from request body/writes `Model` to `Response` body
        * [`org.graphity.provider.ldp.RDFPostReader`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/provider/RDFPostReader.java): Reads `Model` from [RDF/POST](http://www.lsrn.org/semweb/rdfpost.html) requests
        * [`org.graphity.ldp.provider.ResultSetWriter`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/provider/ResultSetWriter.java): Writes [`ResultSet`](http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSet.html) with SPARQL results into `Response`
        * [`org.graphity.ldp.provider.xslt`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/ldp/provider/xslt): Abstract base classes for XSLT transformation-based `Response` writers

Core package
------------

They contain Jena-compatible convenience classes for reading Linked Data resources from URIs, SPARQL endpoints, and HTML forms; writing out Linked Data and SPARQL responses either raw or via XSLT; building SPARQL queries and XSLT transformations, and more.

*Basic version of Graphity core is also available in a [PHP version](https://github.com/Graphity/graphity-core).*

* [`org.graphity`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity): Classes shared by all Graphity applications
    * [`org.graphity.adapter`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/adapter): [`DatasetAdapter`](http://jena.apache.org/documentation/javadoc/fuseki/org/apache/jena/fuseki/http/DatasetAdapter.html)-related wrappers for Model caching via Graph store protocol
    * [`org.graphity.model`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/model): Graphity model interfaces
        * [`org.graphity.model.LinkedDataResource`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/java/org/graphity/model/LinkedDataResource.java): Prototypical Linked Data Resource interface
        * [`org.graphity.model.ResourceFactory`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/java/org/graphity/model/ResourceFactory.java): Factory creating client resource instances for remote Linked Data or SPARQL resources
        * [`org.graphity.model.impl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/java/org/graphity/model/impl): Implementations of Graphity model interfaces
            * [`org.graphity.model.impl.LinkedDataResourceImpl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/java/org/graphity/model/impl/LinkedDataResourceImpl.java): Base class implementation of `LinkedDataResource`
    * [`org.graphity.util`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/java/org/graphity/util): Utility classes
        * [`org.graphity.util.QueryBuilder`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/QueryBuilder.java): Builds Jena [`Query`](http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html) or SPIN [`Query`](www.topquadrant.com/topbraid/spin/api/javadoc/org/topbraid/spin/model/class-use/Query.html) from components (e.g. `LIMIT`/`OFFSET` parameters; RDF resources specifying `OPTIONAL` or a subquery)
        * [`org.graphity.util.XSLTBuilder`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/XSLTBuilder.java): Builds XSLT transformation out of components. Chaining is possible.
        * [`org.graphity.util.locator`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/locator): Pluggable classes for retrieving RDF from URIs. Implement Jena's [`Locator`](http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/Locator.html) interface.
            * [`org.graphity.util.locator.grddl`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/locator): Pluggable classes for [GRDDL](http://www.w3.org/TR/grddl/) import of 3rd party REST APIs and XML formats. Implement Jena's [`Locator`](http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/Locator.html) interface. Need to be added to `DataManager` to take effect.
            * [`org.graphity.util.locator.LocatorGRDDL`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/locator/LocatorGRDDL.java): Generic base class for GRDDL XSLT transformation-based `Locator`s. Also see stylesheets [`here`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/org/graphity/util/locator/grddl).
            * [`org.graphity.util.locator.LocatorLinkedData`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/locator/LocatorLinkedData.java): General-purpose class for loading RDF from Linked Data URIs using content negotiation
            * [`org.graphity.util.locator.LocatorLinkedDataOAuth2`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/locator/LocatorLinkedDataOAuth2.java): General-purpose class for loading RDF from Linked Data URIs using content negotiation and [OAuth2](http://oauth.net/2/) authentication (_unfinished_)
            * [`org.graphity.util.locator.PrefixMapper`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/locator/PrefixMapper.java): Subclass of [`LocationMapper`](http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/LocationMapper.html) for mapping resource (class, property etc.) URIs into local copies of known ontologies. Also see [`resources/location-mapping.ttl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/location-mapping.ttl); ontologies are cached [`here`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/org/graphity/browser/vocabulary).
        * [`org.graphity.util.manager`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/manager): RDF data management classes
            * [`org.graphity.util.manager.DataManager`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/manager/DataManager.java): Subclass of Jena's [`FileManager`](http://jena.sourceforge.net/how-to/filemanager.html) for loading `Model`s and `ResultSet`s from the Web. All code making requests for RDF data or SPARQL endpoints should use this class. Implements [`URIResolver`](http://docs.oracle.com/javase/6/docs/api/javax/xml/transform/URIResolver.html) and resolves URIs when [`document()`](http://www.w3.org/TR/xslt20/#function-document) function is called in XSLT.
            * [`org.graphity.util.manager.SPARQLService`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/manager/SPARQLService.java): Represent individual SPARQL endpoints, should only be used in case authentication or other custom features are needed. Need to be added to `DataManager` to take effect.
        * [`org.graphity.util.oauth`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/util/oauth): Classes related to JAX-RS implementation of OAuth
    * [`org.graphity.vocabulary`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/java/org/graphity/vocabulary): Graphity ontologies as classes with Jena `Resource`s


Used libraries
--------------

* [Apache Jena](http://jena.apache.org)
* [TopBraid SPIN API](http://topbraid.org/spin/api/)
* [Jersey](http://jersey.java.net)
* [SL4J](http://www.slf4j.org)

Maven dependencies are discovered automatically from `pom.xml`, others (such as SPIN API) are included as `.jar` files in the `/lib` folder (and can be "installed locally" using Maven).

XSLT stylesheets
================

GRDDL
-----

* [`resources/org/graphity/util/locator/grddl`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/org/graphity/util/locator/grddl): XSLT stylesheets for use with `LocatorGRDDL` and its subclasses
    * [`atom-grddl.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/util/locator/grddl/atom-grddl.xsl): Atom to RDF transformation (_unfinished_)
    * [`twitter-grddl.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/util/locator/grddl/twitter-grddl.xsl): Twitter API to RDF transformation (_unfinished_)

Papers & presentations
======================

* [Linked Data Success Stories](http://www.slideshare.net/graphity/linked-data-success-stories)
* [European Data Forum 2012 poster](http://semantic-web.dk/posters/Graphity%20EDF2012.pdf)

W3C ["Linked Enterprise Data Patterns" workshop](http://www.w3.org/2011/09/LinkedData/)

* [Graphity position paper](http://www.w3.org/2011/09/LinkedData/ledp2011_submission_1.pdf)
* [Graphity presentation](http://semantic-web.dk/presentations/LEDP2011.pdf)

Tools
=====

* [RDF/XML and Turtle validator](http://www.rdfabout.com/demo/validator/)
* [SPARQL query validator](http://sparql.org/query-validator.html)
* [SPIN query converter] (http://spinservices.org/spinrdfconverter.html)