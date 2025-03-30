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
import static com.atomgraph.core.MediaType.APPLICATION_SPARQL_UPDATE_TYPE;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.SPARQLClient;
import static com.atomgraph.core.client.SPARQLClient.QUERY_PARAM_NAME;
import static com.atomgraph.core.client.SPARQLClient.UPDATE_PARAM_NAME;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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
    
    @BeforeClass
    public static void initClass()
    {
        dataset = DatasetFactory.createTxnMem();
        dataset.setDefaultModel(ModelFactory.createDefaultModel().add(ResourceFactory.createResource(RESOURCE_URI), FOAF.name, "Smth"));
    }
    
    @Before
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
                null, false, false);
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

    @Test
    @Ignore
    // TO-DO: fix after Jena is upgraded using MessageBodyReader<SPARQLResult> instead of MessageBodyReader<ResultSet>
    // https://jena.apache.org/documentation/javadoc/arq/org/apache/jena/sparql/resultset/SPARQLResult.html
    public void testAsk()
    {
        Query query = QueryFactory.create("ASK { <" + RESOURCE_URI + "> ?p ?o }");
        
        assertTrue(sc.ask(query));
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
