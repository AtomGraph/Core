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
package com.atomgraph.core.client;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.impl.QuadStoreImpl;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Quad Store Protocol client, against a live dataset-backed quad store.
 *
 * The quad store is not part of the default Dispatcher, so the test routes one itself. Each test
 * starts from a known dataset (a default graph and a named graph) restored in {@link #init()},
 * since add/replace/delete mutate it.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class QuadStoreClientTest extends JerseyTest
{

    public static final String DEFAULT_RESOURCE_URI = "http://default/graph/resource";
    public static final String NAMED_GRAPH_URI = "http://named/graph";
    public static final String NAMED_RESOURCE_URI = "http://named/graph/resource";

    public static Dataset dataset = DatasetFactory.createTxnMem();

    public com.atomgraph.core.Application system;
    public QuadStoreClient qsc;

    /** The quad store has no route of its own in the platform's Dispatcher. */
    @Path("quads")
    public static class QuadStoreDispatcher
    {

        @Path("/")
        public Class getQuadStore()
        {
            return QuadStoreImpl.class;
        }

    }

    @BeforeEach
    public void init()
    {
        // restore the fixture: the mutating tests leave the dataset in their own state
        dataset.begin(org.apache.jena.query.ReadWrite.WRITE);
        try
        {
            dataset.asDatasetGraph().clear();
            dataset.setDefaultModel(defaultModel());
            dataset.addNamedModel(NAMED_GRAPH_URI, namedModel());
            dataset.commit();
        }
        finally
        {
            dataset.end();
        }

        qsc = QuadStoreClient.create(new MediaTypes(), system.getClient().target(getBaseUri().resolve("quads")));
    }

    public static Model defaultModel()
    {
        return ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.name, "Smth");
    }

    public static Model namedModel()
    {
        return ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(NAMED_RESOURCE_URI), FOAF.name, "Whateverest");
    }

    protected Dataset getDataset()
    {
        return dataset;
    }

    @Override
    protected Application configure()
    {
        system = new com.atomgraph.core.Application(getDataset(),
                null, null, null, null, null,
                new MediaTypes(), com.atomgraph.core.Application.getClient(new ClientConfig()),
                null);
        system.init();
        system.register(QuadStoreDispatcher.class);

        return system;
    }

    @Test
    public void testGetReturnsAllQuads()
    {
        Dataset result = qsc.get();

        assertTrue(result.getDefaultModel().isIsomorphicWith(defaultModel()), "default graph does not round-trip");
        assertTrue(result.containsNamedModel(NAMED_GRAPH_URI), "named graph is missing");
        assertTrue(result.getNamedModel(NAMED_GRAPH_URI).isIsomorphicWith(namedModel()), "named graph does not round-trip");
    }

    @Test
    public void testAddMergesIntoBothDefaultAndNamedGraphs()
    {
        Dataset addition = DatasetFactory.createTxnMem();
        addition.setDefaultModel(ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.nick, "Added"));
        addition.addNamedModel(NAMED_GRAPH_URI, ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(NAMED_RESOURCE_URI), FOAF.nick, "AlsoAdded"));

        qsc.add(addition);

        Dataset result = qsc.get();
        assertTrue(result.getDefaultModel().containsAll(defaultModel()), "add() dropped the existing default graph");
        assertTrue(result.getDefaultModel().contains(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.nick, "Added"),
            "add() did not merge into the default graph");
        assertTrue(result.getNamedModel(NAMED_GRAPH_URI).contains(ResourceFactory.createResource(NAMED_RESOURCE_URI), FOAF.nick, "AlsoAdded"),
            "add() did not merge into the named graph");
    }

    @Test
    public void testReplaceOverwritesExistingQuads()
    {
        Dataset replacement = DatasetFactory.createTxnMem();
        replacement.setDefaultModel(ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.name, "Replaced"));

        qsc.replace(replacement);

        Dataset result = qsc.get();
        assertTrue(result.getDefaultModel().contains(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.name, "Replaced"),
            "replace() did not write the new default graph");
        assertFalse(result.getDefaultModel().isIsomorphicWith(defaultModel()), "replace() kept the old default graph");
        assertFalse(result.containsNamedModel(NAMED_GRAPH_URI), "replace() kept a named graph it did not contain");
    }

    @Test
    public void testDeleteEmptiesTheStore()
    {
        qsc.delete();

        Dataset result = qsc.get();
        assertTrue(result.getDefaultModel().isEmpty(), "default graph survived delete()");
        assertFalse(result.listNames().hasNext(), "named graphs survived delete()");
    }

    /**
     * Named graphs survive whichever quad-capable format is negotiated. Jena registers some
     * languages as BOTH triples and quads (JSON-LD), and the writer used to test isTriples() first
     * and emit only the default graph for them — silently dropping every named graph, on a format
     * chosen by content negotiation rather than by anything the caller wrote.
     */
    @Test
    public void testNamedGraphsSurviveEveryQuadFormat()
    {
        jakarta.ws.rs.core.MediaType[] quadFormats =
        {
            com.atomgraph.core.MediaType.TEXT_NQUADS_TYPE,
            com.atomgraph.core.MediaType.TEXT_TRIG_TYPE,
            jakarta.ws.rs.core.MediaType.valueOf("application/ld+json"),
            jakarta.ws.rs.core.MediaType.valueOf("application/trix")
        };

        for (jakarta.ws.rs.core.MediaType format : quadFormats)
        {
            try (Response response = qsc.get(new jakarta.ws.rs.core.MediaType[]{ format }))
            {
                Dataset result = response.readEntity(Dataset.class);

                assertTrue(result.containsNamedModel(NAMED_GRAPH_URI), "named graph lost in " + format);
                assertTrue(result.getNamedModel(NAMED_GRAPH_URI).isIsomorphicWith(namedModel()), "named graph altered in " + format);
                assertTrue(result.getDefaultModel().isIsomorphicWith(defaultModel()), "default graph altered in " + format);
            }
        }
    }

    /**
     * A plain graph document is accepted as a request body: the resource takes a Dataset, so
     * DatasetProvider (not ModelProvider) reads it, and triples land in the default graph.
     */
    @Test
    public void testAcceptsTriplesRequestBody()
    {
        Model triples = ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.nick, "FromTurtle");

        try (Response response = qsc.post(triples, jakarta.ws.rs.core.MediaType.valueOf("text/turtle"),
            new jakarta.ws.rs.core.MediaType[]{}))
        {
            assertTrue(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL,
                "a text/turtle body was not accepted: " + response.getStatus());
        }

        Dataset result = qsc.get();
        assertTrue(result.getDefaultModel().contains(ResourceFactory.createResource(DEFAULT_RESOURCE_URI), FOAF.nick, "FromTurtle"),
            "triples from the request body did not land in the default graph");
        assertTrue(result.containsNamedModel(NAMED_GRAPH_URI), "a triples body disturbed the named graphs");
    }

    /**
     * A dataset can also be asked for as triples, which is answered with its default graph —
     * the client asked for a format that cannot carry named graphs.
     */
    @Test
    public void testNegotiatesTriplesResponseAsDefaultGraph()
    {
        try (Response response = qsc.get(new jakarta.ws.rs.core.MediaType[]{ jakarta.ws.rs.core.MediaType.valueOf("text/turtle") }))
        {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "a triples representation was refused");
            assertEquals("text/turtle", response.getMediaType().getType() + "/" + response.getMediaType().getSubtype());

            Model result = response.readEntity(Model.class);
            assertTrue(result.isIsomorphicWith(defaultModel()), "the default graph does not round-trip as triples");
        }
    }

    /**
     * A client that accepts anything must not be handed the lossy representation: quad formats
     * come first among the variants, so a wildcard Accept keeps the named graphs.
     */
    @Test
    public void testAcceptAnythingKeepsNamedGraphs()
    {
        try (Response response = qsc.get(new jakarta.ws.rs.core.MediaType[]{ jakarta.ws.rs.core.MediaType.WILDCARD_TYPE }))
        {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            Dataset result = response.readEntity(Dataset.class);
            assertTrue(result.containsNamedModel(NAMED_GRAPH_URI),
                "a wildcard Accept negotiated a lossy representation: " + response.getMediaType());
        }
    }

    @Test
    public void testDefaultMediaTypeIsNQuads()
    {
        assertEquals(com.atomgraph.core.MediaType.TEXT_NQUADS_TYPE, qsc.getDefaultMediaType());
    }

    /**
     * The quad store resource implements no PATCH, so this asserts the method the client sends
     * rather than a round-trip: the request is recorded and aborted before it leaves.
     */
    @Test
    public void testPatchUsesPatchMethod()
    {
        String[] method = new String[1];
        URI[] requestURI = new URI[1];

        QuadStoreClient client = QuadStoreClient.create(new MediaTypes(), system.getClient().target(getBaseUri().resolve("quads")));
        client.register((ClientRequestFilter) request ->
        {
            method[0] = request.getMethod();
            requestURI[0] = request.getUri();
            request.abortWith(Response.ok().build());
        });

        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle("graph", NAMED_GRAPH_URI);

        try (Response response = client.patch(DatasetFactory.createTxnMem(), params))
        {
            assertEquals("PATCH", method[0]);
            assertTrue(requestURI[0].getQuery().contains("graph=" + NAMED_GRAPH_URI), "params did not reach the request URI");
        }
    }

}
