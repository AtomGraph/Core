/*
 * Copyright 2020 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.core.model.impl;

import static com.atomgraph.core.MediaType.APPLICATION_SPARQL_QUERY_TYPE;
import static com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_CSV_TYPE;
import static com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE;
import static com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE;
import static com.atomgraph.core.MediaType.APPLICATION_SPARQL_UPDATE_TYPE;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.SPARQLClient;
import static com.atomgraph.core.client.SPARQLClient.QUERY_PARAM_NAME;
import static com.atomgraph.core.client.SPARQLClient.UPDATE_PARAM_NAME;
import static com.atomgraph.core.client.SPARQLClient.parseBoolean;
import com.atomgraph.core.io.SPARQLResultProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.resultset.ResultsReader;
import org.apache.jena.sparql.resultset.SPARQLResult;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.util.Arrays;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class SPARQLEndpointImplTest extends JerseyTest
{

    public static final String RESOURCE_URI = "http://default/graph/resource";
    public static Dataset dataset;

    public com.atomgraph.core.Application system;
    public WebTarget endpoint;
    public SPARQLClient sc;
    
    @BeforeAll
    public static void initClass()
    {
        dataset = DatasetFactory.createTxnMem();
        dataset.setDefaultModel(ModelFactory.createDefaultModel().add(ResourceFactory.createResource(RESOURCE_URI), FOAF.name, "Smth"));
    }
    
    @BeforeEach
    public void init()
    {
        endpoint = system.getClient().target(getBaseUri().resolve("sparql"));
        sc = SPARQLClient.create(new MediaTypes(), endpoint);
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
        
        return system;
    }
    
    @Test
    public void testDescribe()
    {
        Query query = QueryFactory.create("DESCRIBE <" + RESOURCE_URI + ">");
        
        assertIsomorphic(getDataset().getDefaultModel(), sc.loadModel(query));
    }
    
    @Test
    public void testConstruct()
    {
        Query query = QueryFactory.create("CONSTRUCT WHERE { <" + RESOURCE_URI + "> ?p ?o }");
        
        assertIsomorphic(getDataset().getDefaultModel(), sc.loadModel(query));
    }
    
    @Test
    public void testSelect()
    {
        Query query = QueryFactory.create("SELECT * { <" + RESOURCE_URI + "> ?p ?o }");
        
        assertTrue(sc.select(query).hasNext());
    }

    public static final String ASK_TRUE = "ASK { <" + RESOURCE_URI + "> ?p ?o }";
    public static final String ASK_FALSE = "ASK { <http://default/graph/absent> ?p ?o }";
    public static final MediaType PROTOBUF = MediaType.valueOf(ResultSetLang.RS_Protobuf.getContentType().getContentTypeStr());

    private static MultivaluedMap<String, String> query(String query)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add(QUERY_PARAM_NAME, query);
        return params;
    }

    @Test
    public void testAsk()
    {
        assertTrue(sc.ask(QueryFactory.create(ASK_TRUE)));
    }

    @Test
    public void testAskFalse()
    {
        assertFalse(sc.ask(QueryFactory.create(ASK_FALSE)));
    }

    /** The body is the boolean in the format asked for, read back with Jena and, as a second opinion, by eye */
    @Test
    public void testAskBodies() throws IOException
    {
        for (MediaType mediaType : Arrays.asList(APPLICATION_SPARQL_RESULTS_JSON_TYPE, APPLICATION_SPARQL_RESULTS_XML_TYPE, APPLICATION_SPARQL_RESULTS_CSV_TYPE))
            try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ mediaType }, query(ASK_TRUE)))
            {
                assertEquals(OK.getStatusCode(), cr.getStatus(), mediaType.toString());
                assertTrue(cr.getMediaType().isCompatible(mediaType), cr.getMediaType() + " for " + mediaType);
                String body = cr.readEntity(String.class);
                SPARQLResult result = ResultsReader.create().lang(SPARQLResultProvider.getLang(cr.getMediaType())).build().readAny(new java.io.ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
                assertTrue(result.isBoolean() && result.getBooleanResult(), mediaType + ": " + body);
                if (mediaType.equals(APPLICATION_SPARQL_RESULTS_JSON_TYPE)) assertTrue(body.replace(" ", "").contains("\"boolean\":true"), body);
                if (mediaType.equals(APPLICATION_SPARQL_RESULTS_XML_TYPE)) assertTrue(body.contains("<boolean>true</boolean>"), body);
                if (mediaType.equals(APPLICATION_SPARQL_RESULTS_CSV_TYPE)) assertTrue(body.startsWith("_askResult"), body);
            }
    }

    /** True, false and a result set tag differently at one media type, and one ASK tags differently across media types */
    @Test
    public void testAskETags()
    {
        EntityTag trueTag, falseTag, selectTag, trueXmlTag;
        try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ APPLICATION_SPARQL_RESULTS_JSON_TYPE }, query(ASK_TRUE))) { trueTag = cr.getEntityTag(); }
        try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ APPLICATION_SPARQL_RESULTS_JSON_TYPE }, query(ASK_FALSE))) { falseTag = cr.getEntityTag(); }
        try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ APPLICATION_SPARQL_RESULTS_JSON_TYPE }, query("SELECT * { <http://default/graph/absent> ?p ?o }"))) { selectTag = cr.getEntityTag(); }
        try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ APPLICATION_SPARQL_RESULTS_XML_TYPE }, query(ASK_TRUE))) { trueXmlTag = cr.getEntityTag(); }

        assertNotEquals(trueTag, falseTag);
        assertNotEquals(trueTag, selectTag, "an empty result set and a boolean are different answers");
        assertNotEquals(falseTag, selectTag);
        assertNotEquals(trueTag, trueXmlTag, "a different representation is a different entity");
    }

    /** The client's full result set preference ranks the binary formats first; the endpoint offers only what carries a boolean */
    @Test
    public void testAskNegotiatesAFormatThatCarriesABoolean()
    {
        try (jakarta.ws.rs.core.Response cr = sc.get(sc.getReadableMediaTypes(ResultSet.class), query(ASK_TRUE)))
        {
            assertEquals(OK.getStatusCode(), cr.getStatus());
            assertTrue(SPARQLResultProvider.isBooleanReadable(cr.getMediaType()), cr.getMediaType().toString());
        }
        try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ PROTOBUF }, query(ASK_TRUE)))
        {
            assertEquals(NOT_ACCEPTABLE.getStatusCode(), cr.getStatus(), "Jena has no boolean encoding in Protobuf");
        }
    }

    @Test
    public void testParseBoolean() throws IOException
    {
        for (MediaType mediaType : Arrays.asList(APPLICATION_SPARQL_RESULTS_JSON_TYPE, APPLICATION_SPARQL_RESULTS_XML_TYPE, APPLICATION_SPARQL_RESULTS_CSV_TYPE))
        {
            try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ mediaType }, query(ASK_TRUE))) { assertTrue(parseBoolean(cr), mediaType.toString()); }
            try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ mediaType }, query(ASK_FALSE))) { assertFalse(parseBoolean(cr), mediaType.toString()); }
        }
        // a result set is not a boolean, whatever the format
        try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ PROTOBUF }, query("SELECT * { ?s ?p ?o }")))
        {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> parseBoolean(cr));
            assertTrue(ex.getMessage().contains("not a boolean result"), ex.getMessage());
        }
        // RDF is not a results format
        try (jakarta.ws.rs.core.Response cr = sc.get(new MediaType[]{ MediaType.valueOf("text/turtle") }, query("CONSTRUCT WHERE { ?s ?p ?o }")))
        {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> parseBoolean(cr));
            assertTrue(ex.getMessage().contains("Unsupported SPARQL results format"), ex.getMessage());
        }
    }

    @Test
    public void testSelectStillRewindable()
    {
        assertTrue(sc.select(QueryFactory.create("SELECT * { <" + RESOURCE_URI + "> ?p ?o }")) instanceof org.apache.jena.query.ResultSetRewindable);
    }

    /** The remote accessor is what a proxying endpoint asks through: its ask goes over HTTP and reads the boolean back */
    @Test
    public void testRemoteAccessorAsk()
    {
        com.atomgraph.core.model.impl.remote.EndpointAccessorImpl remote = new com.atomgraph.core.model.impl.remote.EndpointAccessorImpl(sc);
        assertTrue(remote.ask(QueryFactory.create(ASK_TRUE), java.util.List.of(), java.util.List.of()));
        assertFalse(remote.ask(QueryFactory.create(ASK_FALSE), java.util.List.of(), java.util.List.of()));
    }
    
    @Test
    public void testMissingGetQuery()
    {
        try (jakarta.ws.rs.core.Response cr = sc.get(sc.getReadableMediaTypes(Model.class)))
        {
            assertEquals(BAD_REQUEST.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }
    
    @Test
    public void testInvalidGetQuery()
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.add(QUERY_PARAM_NAME, "BAD QUERY");
        
        try (jakarta.ws.rs.core.Response cr = sc.get(sc.getReadableMediaTypes(Model.class), params))
        {
            assertEquals(BAD_REQUEST.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }
    
    @Test
    public void testNotAcceptableSelectResultType()
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.add(QUERY_PARAM_NAME, "SELECT * { ?s ?p ?o }");
        
        try (jakarta.ws.rs.core.Response cr = sc.get(sc.getReadableMediaTypes(Model.class), params))
        {
            assertEquals(NOT_ACCEPTABLE.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }

    @Test
    public void testNotAcceptableConstructResultType()
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.add(QUERY_PARAM_NAME, "CONSTRUCT WHERE { ?s ?p ?o }");
        
        try (jakarta.ws.rs.core.Response cr = sc.get(sc.getReadableMediaTypes(ResultSet.class), params))
        {
            assertEquals(NOT_ACCEPTABLE.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }

    @Test
    public void testMissingPostQuery()
    {
        try (jakarta.ws.rs.core.Response cr = sc.post(new Form(), APPLICATION_FORM_URLENCODED_TYPE, sc.getReadableMediaTypes(Model.class)))
        {
            assertEquals(BAD_REQUEST.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }

    @Test
    public void testInvalidPostQuery()
    {
        Form params = new Form();
        params.param(QUERY_PARAM_NAME, "BAD QUERY");
        
        try (jakarta.ws.rs.core.Response cr = sc.post(params, APPLICATION_FORM_URLENCODED_TYPE, sc.getReadableMediaTypes(Model.class)))
        {
            assertEquals(BAD_REQUEST.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }
    
    @Test
    public void testInvalidPostUpdate()
    {
        Form params = new Form();
        params.param(UPDATE_PARAM_NAME, "BAD UPDATE");
        
        try (jakarta.ws.rs.core.Response cr = sc.post(params, APPLICATION_FORM_URLENCODED_TYPE, new MediaType[]{}))
        {
            assertEquals(BAD_REQUEST.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }
    
    @Test
    public void testInvalidPostDirectQuery()
    {
        try (jakarta.ws.rs.core.Response cr = sc.post("BAD QUERY", APPLICATION_SPARQL_QUERY_TYPE, new MediaType[]{}))
        {
            assertEquals(BAD_REQUEST.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }
    
    @Test
    public void testInvalidPostDirectUpdate()
    {
        try (jakarta.ws.rs.core.Response cr = sc.post("BAD UPDATE", APPLICATION_SPARQL_UPDATE_TYPE, new MediaType[]{}))
        {
            assertEquals(BAD_REQUEST.getStatusCode(), cr.getStatusInfo().getStatusCode());
        }
    }
        
    public static void assertIsomorphic(Model wanted, Model got)
    {
        if (!wanted.isIsomorphicWith(got))
            fail("Models not isomorphic (not structurally equal))");
    }
    
    @Test
    public void testDifferentMediaTypesDifferentETags()
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.add(QUERY_PARAM_NAME, "CONSTRUCT WHERE { ?s ?p ?o }");
        
        jakarta.ws.rs.core.Response nTriplesResp = sc.get(Arrays.asList(com.atomgraph.core.MediaType.APPLICATION_NTRIPLES_TYPE).toArray(com.atomgraph.core.MediaType[]::new), params);
        EntityTag nTriplesETag = nTriplesResp.getEntityTag();
        assertEquals(nTriplesResp.getLanguage(), null);

        jakarta.ws.rs.core.Response rdfXmlResp = sc.get(Arrays.asList(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE).toArray(com.atomgraph.core.MediaType[]::new), params);
        EntityTag rdfXmlETag = rdfXmlResp.getEntityTag();
        assertEquals(rdfXmlResp.getLanguage(), null);
        
        assertNotEquals(nTriplesETag, rdfXmlETag);
    }
    
}
