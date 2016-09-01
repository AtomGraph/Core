Lower-level Linked Data server application that serves as a base for [AtomGraph Processor](../../../Processor). It is implemented as a Java Web application (uses Maven).

Features
--------

AtomGraph Core provides features similar to that of [Pubby](http://wifo5-03.informatik.uni-mannheim.de/pubby/):
* serving Linked Data from a SPARQL endpoint
* HTTP Basic authentication for endpoints
 
For more advanced features and configuration, see [AtomGraph Processor](../../../Processor).

Configuration
-------------

The Core is configured in [web.xml](../../blob/master/src/main/webapp/WEB-INF/web.xml)

For developers
--------------
* low-level access to remote Linked Data resources and SPARQL endpoints
* JAX-RS interfaces and implementations of a Linked Data platform (so far read-only)
* JAX-RS providers for input and output of RDF data

Documentation
-------------
* [JavaDoc](http://atomgraph.github.io/atomgraph-core/apidocs/)

Dependencies
--------------

* [Apache Jena](http://jena.apache.org)
* [Jersey](http://jersey.java.net)
* [SL4J](http://www.slf4j.org)
