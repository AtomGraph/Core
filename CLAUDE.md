# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AtomGraph Core is a Java framework that bridges [Apache Jena](https://jena.apache.org/) (RDF/SPARQL) with [JAX-RS](https://jakarta.ee/specifications/restful-ws/) (Jakarta REST via Eclipse Jersey). It provides a Linked Data API for SPARQL endpoints and Graph Stores, with support for content negotiation across all standard RDF serialization formats.

## Build & Test Commands

```bash
# Build as library JAR (default)
mvn clean package

# Build as standalone WAR for Tomcat deployment
mvn clean package -Pstandalone

# Run all tests
mvn clean test

# Run a specific test class
mvn test -Dtest=DirectGraphStoreTest

# Run a specific test method
mvn test -Dtest=DirectGraphStoreTest#testGet

# Generate JavaDoc
mvn javadoc:javadoc
```

Requires Java 17.

## Architecture

### Key Abstractions

- **`Service`** — central interface abstracting a SPARQL 1.1 backend (endpoint + graph store). Two implementations: `dataset.ServiceImpl` (local in-memory Jena Dataset) and `remote.ServiceImpl` (HTTP remote endpoint).
- **`Application`** (`com.atomgraph.core.Application`) — JAX-RS `ResourceConfig` subclass; extend this to configure your own Linked Data application.
- **`MediaTypes`** — manages all supported MIME types with q-values for content negotiation. Dynamically discovers formats from Jena RIOT at startup.

### Package Map

| Package | Role |
|---------|------|
| `com.atomgraph.core` | Application bootstrap, MediaTypes |
| `com.atomgraph.core.model` | `Service`, `EndpointAccessor`, `DatasetAccessor` interfaces and `impl/` sub-packages (`dataset.*` for local, `remote.*` for HTTP) |
| `com.atomgraph.core.client` | `SPARQLClient`, `GraphStoreClient`, `QuadStoreClient` — HTTP clients for remote SPARQL/Graph Store endpoints |
| `com.atomgraph.core.io` | JAX-RS `MessageBodyReader`/`Writer` providers for RDF Model, Dataset, ResultSet, Query, UpdateRequest |
| `com.atomgraph.core.server` | `Dispatcher` routes incoming requests to resource implementations |
| `com.atomgraph.core.riot` | Custom Jena RIOT language registration (RDF/POST) |
| `com.atomgraph.core.vocabulary` | Jena `OntModel`s for the AtomGraph (`A`) and SPARQL Service Description (`SD`) vocabularies |
| `com.atomgraph.core.mapper` | JAX-RS `ExceptionMapper`s that translate Jena/HTTP exceptions to appropriate HTTP responses |

### Request Flow

1. Servlet container → Jersey `Dispatcher` (`server/Dispatcher.java`)
2. `Dispatcher` resolves the request URI to a `GraphStore` or `SPARQLEndpoint` resource
3. Resource delegates to an `EndpointAccessor` or `DatasetAccessor` (local dataset or remote HTTP client)
4. JAX-RS `MessageBodyWriter` in `io/` serializes the Jena `Model`/`Dataset`/`ResultSet` using the negotiated content type

### Testing Pattern

Tests extend Jersey's `JerseyTest` (embedded Grizzly2 server). Each test class sets up an in-memory `DatasetFactory.createTxnMem()`, wires up a minimal JAX-RS application, and exercises HTTP CRUD operations, validating RDF graph isomorphism and HTTP status codes.
