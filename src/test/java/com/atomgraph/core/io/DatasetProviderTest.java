/*
 * Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.core.io;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reading and writing {@link Dataset} entities.
 *
 * A dataset reads from both triples and quads — triples land in the default graph — and writes to
 * both, but only a triples-ONLY language is allowed to reduce it to its default graph.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class DatasetProviderTest
{

    public static final String DEFAULT_RESOURCE_URI = "http://default/graph/resource";
    public static final String NAMED_GRAPH_URI = "http://named/graph";
    public static final String NAMED_RESOURCE_URI = "http://named/graph/resource";

    public static final MediaType NQUADS = MediaType.valueOf("application/n-quads");
    public static final MediaType TRIG = MediaType.valueOf("application/trig");
    /** Jena registers JSON-LD as triples AND quads; it has to be treated as quads. */
    public static final MediaType JSONLD = MediaType.valueOf("application/ld+json");
    public static final MediaType TRIX = MediaType.valueOf("application/trix");
    public static final MediaType TURTLE = MediaType.valueOf("text/turtle");
    public static final MediaType NTRIPLES = MediaType.valueOf("application/n-triples");
    public static final MediaType RDFXML = MediaType.valueOf("application/rdf+xml");

    private DatasetProvider provider;

    @BeforeEach
    public void init()
    {
        provider = new DatasetProvider();
    }

    public static Dataset dataset()
    {
        Dataset dataset = DatasetFactory.create();
        dataset.setDefaultModel(ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.name, "Smth"));
        dataset.addNamedModel(NAMED_GRAPH_URI, ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(NAMED_RESOURCE_URI), FOAF.name, "Whateverest"));

        return dataset;
    }

    private byte[] write(Dataset dataset, MediaType mediaType) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        provider.writeTo(dataset, Dataset.class, null, null, mediaType, new MultivaluedHashMap<>(), out);

        return out.toByteArray();
    }

    private Dataset read(byte[] entity, MediaType mediaType) throws IOException
    {
        return provider.readFrom(Dataset.class, null, null, mediaType, new MultivaluedHashMap<>(),
            new ByteArrayInputStream(entity));
    }

    // READER / WRITER SELECTION

    /** The provider reads and writes datasets — not models, which are ModelProvider's. */
    @Test
    public void testHandlesDatasetTypeOnly()
    {
        assertTrue(provider.isReadable(Dataset.class, null, null, NQUADS), "cannot read a Dataset");
        assertTrue(provider.isWriteable(Dataset.class, null, null, NQUADS), "cannot write a Dataset");

        assertFalse(provider.isReadable(Model.class, null, null, NQUADS), "reads a Model, which is ModelProvider's");
        assertFalse(provider.isWriteable(Model.class, null, null, NQUADS), "writes a Model, which is ModelProvider's");
    }

    /** Writing accepts a Dataset subclass, since the entity is whatever the resource returned. */
    @Test
    public void testWriteableAcceptsDatasetSubclass()
    {
        assertTrue(provider.isWriteable(dataset().getClass(), null, null, NQUADS),
            "cannot write the concrete Dataset implementation");
    }

    @Test
    public void testHandlesBothTriplesAndQuads()
    {
        for (MediaType mediaType : new MediaType[]{ NQUADS, TRIG, JSONLD, TRIX, TURTLE, NTRIPLES, RDFXML })
        {
            assertTrue(provider.isReadable(Dataset.class, null, null, mediaType), "cannot read " + mediaType);
            assertTrue(provider.isWriteable(Dataset.class, null, null, mediaType), "cannot write " + mediaType);
        }
    }

    @Test
    public void testRejectsNonRDFMediaType()
    {
        assertFalse(provider.isReadable(Dataset.class, null, null, MediaType.APPLICATION_SVG_XML_TYPE));
        assertFalse(provider.isWriteable(Dataset.class, null, null, MediaType.APPLICATION_SVG_XML_TYPE));
    }

    @Test
    public void testIgnoresCharsetParameter()
    {
        assertTrue(provider.isReadable(Dataset.class, null, null, NQUADS.withCharset(StandardCharsets.UTF_8.name())));
        assertTrue(provider.isWriteable(Dataset.class, null, null, NQUADS.withCharset(StandardCharsets.UTF_8.name())));
    }

    // WRITING

    /**
     * Every quad-capable language carries the named graphs. JSON-LD is the regression: Jena
     * registers it as triples as well, and testing isTriples() first reduced it to the default
     * graph — losing named graphs on a format picked by content negotiation.
     */
    @Test
    public void testQuadFormatsWriteEveryGraph() throws IOException
    {
        for (MediaType mediaType : new MediaType[]{ NQUADS, TRIG, JSONLD, TRIX })
        {
            Dataset result = read(write(dataset(), mediaType), mediaType);

            assertTrue(result.containsNamedModel(NAMED_GRAPH_URI), "named graph lost in " + mediaType);
            assertTrue(result.getNamedModel(NAMED_GRAPH_URI).isIsomorphicWith(dataset().getNamedModel(NAMED_GRAPH_URI)),
                "named graph altered in " + mediaType);
            assertTrue(result.getDefaultModel().isIsomorphicWith(dataset().getDefaultModel()),
                "default graph altered in " + mediaType);
        }
    }

    /** A triples-only language can only carry the default graph. */
    @Test
    public void testTriplesFormatsWriteDefaultGraphOnly() throws IOException
    {
        for (MediaType mediaType : new MediaType[]{ TURTLE, NTRIPLES, RDFXML })
        {
            Dataset result = read(write(dataset(), mediaType), mediaType);

            assertTrue(result.getDefaultModel().isIsomorphicWith(dataset().getDefaultModel()),
                "default graph does not round-trip in " + mediaType);
            assertFalse(result.listNames().hasNext(), mediaType + " is triples-only but produced a named graph");
        }
    }

    // READING

    /** Triples read into the default graph, so a dataset can be built from a plain graph document. */
    @Test
    public void testReadsTriplesIntoDefaultGraph() throws IOException
    {
        byte[] turtle = "<http://default/graph/resource> <http://xmlns.com/foaf/0.1/name> \"Smth\" .".
            getBytes(StandardCharsets.UTF_8);

        Dataset result = read(turtle, TURTLE);

        assertTrue(result.getDefaultModel().isIsomorphicWith(dataset().getDefaultModel()));
        assertFalse(result.listNames().hasNext());
    }

    @Test
    public void testReadsQuadsIntoNamedGraphs() throws IOException
    {
        byte[] nquads = ("<http://named/graph/resource> <http://xmlns.com/foaf/0.1/name> \"Whateverest\" <" + NAMED_GRAPH_URI + "> .").
            getBytes(StandardCharsets.UTF_8);

        Dataset result = read(nquads, NQUADS);

        assertTrue(result.getDefaultModel().isEmpty(), "quads leaked into the default graph");
        assertTrue(result.containsNamedModel(NAMED_GRAPH_URI));
        assertTrue(result.getNamedModel(NAMED_GRAPH_URI).isIsomorphicWith(dataset().getNamedModel(NAMED_GRAPH_URI)));
    }

    /** Relative URIs resolve against the base the request carries, as for models. */
    @Test
    public void testResolvesRelativeURIsAgainstRequestURIHeader() throws IOException
    {
        byte[] turtle = "<relative> <http://xmlns.com/foaf/0.1/name> \"Smth\" .".getBytes(StandardCharsets.UTF_8);
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(DatasetProvider.REQUEST_URI_HEADER, "http://base/graph/");

        Dataset result = provider.readFrom(Dataset.class, null, null, TURTLE, headers, new ByteArrayInputStream(turtle));

        assertTrue(result.getDefaultModel().containsResource(ResourceFactory.createResource("http://base/graph/relative")),
            "relative URI was not resolved against the request URI");
    }

    @Test
    public void testSizeIsUnknown()
    {
        assertEquals(-1, provider.getSize(dataset(), Dataset.class, null, null, NQUADS));
    }

}
