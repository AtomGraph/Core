Core is a Linked Data framework. It crosses Apache Jena RDF API with the JAX-RS REST API (Eclipse Jersey implementation), providing in a uniform Linked Data API.

Core serves as a base for [AtomGraph Processor](../../../Processor). It is implemented as a Java Web application (uses Maven).

Features
--------

AtomGraph Core server provides features similar to that of [Pubby](http://wifo5-03.informatik.uni-mannheim.de/pubby/):
* serving Linked Data from a SPARQL endpoint
* HTTP Basic authentication for endpoints
 
For more advanced features and configuration, see [AtomGraph Processor](../../../Processor).

Configuration
-------------

The Core is configured in [web.xml](../../blob/master/src/main/webapp/WEB-INF/web.xml)

Uncomment `http://www.w3.org/ns/sparql-service-description#endpoint` and `https://w3id.org/atomgraph/core#graphStore` init parameters and provide their values. Otherwise the server will not start.

Linked Data API
--------------
* low-level access to remote Linked Data resources and SPARQL endpoints
* JAX-RS interfaces and implementations of a Linked Data resources
* JAX-RS providers for input and output of RDF data

Documentation
-------------
* [JavaDoc](https://atomgraph.github.io/Core/apidocs/)

Dependencies
--------------

* [Apache Jena](http://jena.apache.org)
* [Jersey](https://eclipse-ee4j.github.io/jersey/)
* [SL4J](http://www.slf4j.org)
