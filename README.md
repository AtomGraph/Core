Reinventing Web applications
============================

The year is 2013, and you are still writing source code to build Web applications? We at Graphity want to show
you that the current Web application architectural approaches are obsolete, ineffective, and cost more than you
probably realize. We want to help you on the way to the [Semantic Web](http://www.w3.org/standards/semanticweb/),
which is already imminent. But more on that later.

Before you jump on the next Web application framework or another key-value store bandwagon, let's take a step
back and look at what the Web and its applications are made of.

The Web
-------

The Web as we know it consists of network of webpages, or more generally of human-readable documents and services.
Web 2.0 also brought a network of APIs, or machine-readable documents and (mostly RESTful) services.
Unfortunately, the human- and machine-readable networks exist largely in parallel. First of all, not all websites
provide an API access. There is a bigger problem however -- the one of standardization.

We all know what standards the webpages are made of -- HTML, CSS, and JavaScript are the technologies that
enable any web browser to talk to almost any website on the human-readable Web. It does not matter what domain
the website represents -- an online bookshop, a dating site, a netbank, or a failblog for the lulz -- thanks to
the standards, we can communicate with them through the same browser software and uniform components such as
hyperlinks and form controls.

The APIs
--------

What about the machine-readable Web 2.0? Sure, we have HTTP and REST, JSON and XML -- they make up the majority
of modern APIs. Does that mean that any Web 2.0 API can talk to any other API? Sadly, the answer is _no_. The
APIs use different data models and serialization formats, and even if they use the same ones, the data cannot be
directly mapped from one domain model to the other.

As a result, you cannot have your faiblog entries reposted on Twitter or vica versa, unless there is a built-in
connector or 3rd party software that integrates this specific pair of services. If you run your own site or
application, you need a connector for each of the services you want to integrate, and as it likely has its own
API, it provides yet another custom-made format and/or specification.

What we have here, is a fundamental problem of standardization: Web 2.0 APIs are machine-readable, but they
cannot _natively_ communicate with each other as the is no standard communication protocol. Surely, a
standardization problem never stopped developers and businesses from getting things done, and Web services and
APIs get integrated all the time. From glue-code hacked in 10 minutes, to specialized connector services, to
full-blown industries built around data integration (see [IPaaS](http://www.gartner.com/it-glossary/information-platform-as-a-service-ipaas/)),
massive amounts of software are developed worldwide just to get the this ever-growing network of machine-readable
nodes to intercommunicate.

The costs
---------

You probably already knew all of the above. By looking at the state of the machine-readable Web, it is safe to
predict that the demand for data integration is rapidly increasing, internally in organizations as well as
globally. Consider that for a moment with the following factors:
* pair-wise integration with N service nodes requires N connectors, the total sum approaching N^2 in a global increasingly interconnected network
* implementation of service connectors consume expensive developer hours and deal with data conversions code
* source code is prone to bugs: more source code means more bugs
* proprietary integration platforms make the process easier and more user-friendly, but still provide the old many-to-many solution and also put the customer in a lock-in with switching-costs
* data conversion code is also prone to poor performance, as there are theoretical and practical mismatches between data models (such as object-oriented, relational, XML, JSON, RDF etc.) that eventually lead to leaky abstractions
* imperative languages suffer from poor concurrency, while declarative languages are becoming popular (see [The Downfall of Imperative Programming](http://fpcomplete.com/the-downfall-of-imperative-programming/))

We think the many-to-many data integration approach does not scale, and the general "(Web) application is
domain-specific source code" paradigm has become obsolete. Software cannot be un-written and billions of dollars
are already spent on systems that were outdated before they even got installed.
But how can we avoid this in the future? We need to take a fresh look at Web applications. And we think Graphity
provides the first solution of this kind. It consists of the following:

* uniform data model: a cornerstone of universal [pivotal data conversion](http://en.wikipedia.org/wiki/Data_conversion#Pivotal_conversion)
* generic Web applications: implemented in a declarative fashion, they work natively on the uniform data model
* quality data

The above points are no less than a recipe for the implementation of the bottom layer of Semantic Web: data
integration on a global scale. We will show how Graphity helps you with all of the options, so you can focus on
building great apps and enriching your data.

Linked Data
-----------

We do not need to invent the uniform data model, as it has been around for almost 10 years and is very
well-standardized. It is the W3C [Resource Description Framework](http://www.w3.org/standards/techs/rdf), or RDF.

The shape of RDF data model is a graph, or a network of nodes (by comparison, XML data model is tree-shaped).
The graph is formed from statements that have a stable triple form: subject/property/object.
The nodes in the graph are either
* resources identified by [URI](http://en.wikipedia.org/wiki/Uniform_resource_identifier) references
* anonymous resources (blank nodes)
* predicates
* literal values (that can only be in object position but can have a datatype or language type attached)

RDF has its own standard query language called [SPARQL](http://www.w3.org/standards/techs/sparql) (just like the
relational model has SQL), which operates on graph pattern matching. RDF is schema-less, and it's graph-shaped
nature makes merging datasources natural and effortless (when was the last time you effortlessly merged
relational databases?) Related reusable RDF terms are however often grouped into [ontologies](http://www.w3.org/standards/semanticweb/ontology)
(semantic vocabularies).

When published on the Web, RDF data (usually in conjunction with SPARQL endpoints) is called [Linked Data](http://www.w3.org/standards/semanticweb/data).
RDF and Linked Data have the following advantages:
* one
* two
* three

URIs can identify both informational and real-world resources, which means that RDF can be used to encode
metadata about documents, systems, other data, as well as the real world (although there is a [special case](http://www.w3.org/TR/cooluris/#semweb)
in Linked Data access, as real-world objects obviously cannot be retrieved as information).

In other words, Linked Data is the native fabric of the machine-readable Web. It is currently the only standard
approach to global integration for which the demand is increasing. In fact, Google and Facebook have both
developed proprietary closed-wall platforms as alternatives to Linked Data: [Knowledge Graph](http://www.google.com/insidesearch/features/search/knowledge.html)
and [Open Graph](http://developers.facebook.com/docs/concepts/opengraph/), respectively. If you're not using
Linked Data yet, we strongly encourage you to do yourself a favor and start now!

Generic Web applications
------------------------

Resources
State
Graph

Templates / processors
Finite state machines
XSLT analogy
Context resource





Graphity
========

not reinvent, but leverage standards: SPIN, RDF/POST

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

Usage
=====

Installation
------------

Graphity LDP is a [Maven Web application](http://maven.apache.org/guides/mini/guide-webapp.html).
It requires [Java 1.7](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (due to the improvements in the [Locale](http://docs.oracle.com/javase/7/docs/api/java/util/Locale.html) class).

You have the following options to install Graphity:
* checkout the source code from the Git repository and build it as a Maven webapp
* [download](https://github.com/Graphity/graphity-ldp/downloads) the project as a `.jar` library and include it in your Java project (Maven repository is not available yet)

Maven dependencies are discovered automatically from `pom.xml`.

From Java
---------

Graphity LDP is meant to be used as a library for Linked Data Web applications. Follow these simple steps to get started:
* create a new Maven Web application
* add Graphity LDP as dependency
* extend one of the `Resource` base class implementations, for example [`ResourceBase`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/java/org/graphity/ldp/model/impl/ResourceBase.java) - it will serve as the root JAX-RS resource
* extend `Application` class if necessary
* register the `Application` class (either your own or from LDP) in your project's `webapp/WEB-INF/web.xml` like this:

    <servlet>
        <servlet-name>index</servlet-name>
        <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
        <init-param>
            <param-name>javax.ws.rs.Application</param-name>
            <param-value>org.graphity.ldp.Application</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>index</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

For a complete example of a Web application built on Graphity LDP, take a look at [Graphity Browser](https://github.com/Graphity/graphity-browser).


Used libraries
--------------

* [Apache Jena](http://jena.apache.org)
* [TopBraid SPIN API](http://topbraid.org/spin/api/)
* [Jersey](http://jersey.java.net)
* [SL4J](http://www.slf4j.org)

Resources
=========

Utility stylesheets
-------------------

* [`resources/org/graphity/ldp/provider/xslt`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/org/graphity/ldp/provider/xslt)
    * [`functions.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/ldp/provider/xslt/functions.xsl): Graphity utility functions
    * [`group-sort-triples.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/ldp/provider/xslt/group-sort-triples.xsl): Groups and sorts RDF/XML statements
    * [`rdfxml2google-wire.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/ldp/provider/xslt/rdfxml2google-wire.xsl): Generic RDF/XML to Google [DataTable](https://developers.google.com/chart/interactive/docs/reference#DataTable) transformation
    * [`sparql2google-wire.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/ldp/provider/xslt/sparql2google-wire.xsl): Generic RDF/XML to Google DataTable transformation

GRDDL stylesheets
-----------------

* [`resources/org/graphity/util/locator/grddl`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/org/graphity/util/locator/grddl): XSLT stylesheets for use with `LocatorGRDDL` and its subclasses
    * [`atom-grddl.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/util/locator/grddl/atom-grddl.xsl): Atom to RDF transformation (_unfinished_)
    * [`twitter-grddl.xsl`](https://github.com/Graphity/graphity-ldp/blob/master/src/main/resources/org/graphity/util/locator/grddl/twitter-grddl.xsl): Twitter API to RDF transformation (_unfinished_)


Ontologies
----------

* [`resources/org/graphity/vocabulary`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/org/graphity/vocabulary): Contains cached local copies of popular ontologies
    * [`graphity.ttl`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/org/graphity/vocabulary/graphity.ttl): Ontology reused by all Graphity applications

Mappings
--------

* [`resources/location-mapping.n3`](https://github.com/Graphity/graphity-ldp/tree/master/src/main/resources/location-mapping.n3): Jena's [`LocationMapper`](http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/LocationMapper.html) [configuration file](http://jena.sourceforge.net/how-to/filemanager.html) mapping the locally cached ontologies

Using resources in your project
-------------------------------

In order to include the above resources into your own Maven project, you can add the following execution for `maven-dependency-plugin` to your `pom.xml`:

    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-dependency-plugin</artifactId>
      <version>2.1</version>
      <executions>
        <execution>
          <id>resource-dependencies</id>
          <phase>generate-resources</phase>
          <goals>
            <goal>unpack-dependencies</goal>
          </goals>
          <configuration>
            <includeGroupIds>org.graphity</includeGroupIds>
            <includes>**\/*.xsl,**\/*.ttl,**\/*.rdf,**\/*.owl</includes>
            <outputDirectory>${project.build.directory}/classes</outputDirectory>
          </configuration>
        </execution>
      </executions>
    </plugin>

This will copy all reusable `.xsl`, `.ttl`, `.rdf`, and `.owl` files from LDP's `resource` folder.

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