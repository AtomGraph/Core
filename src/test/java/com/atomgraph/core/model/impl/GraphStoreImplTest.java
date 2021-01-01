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

import com.atomgraph.core.MediaType;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import static com.atomgraph.core.client.GraphStoreClient.GRAPH_PARAM_NAME;
import static com.atomgraph.core.model.impl.SPARQLEndpointImplTest.assertIsomorphic;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class GraphStoreImplTest extends JerseyTest
{
    public static final String NAMED_GRAPH_URI = "http://named/graph";
    public static Dataset dataset;
    
    public com.atomgraph.core.Application system;
    public WebTarget endpoint;

    @BeforeClass
    public static void initClass()
    {
        dataset = DatasetFactory.createTxnMem();
        dataset.setDefaultModel(ModelFactory.createDefaultModel().add(ResourceFactory.createResource("http://default/graph/resource"), FOAF.name, "Smth"));
        dataset.addNamedModel(NAMED_GRAPH_URI, ModelFactory.createDefaultModel().add(ResourceFactory.createResource("http://default/graph/resource"), FOAF.name, "Whateverest"));
    }
    
    @Before
    public void init()
    {
        endpoint = system.getClient().target(getBaseUri().resolve("service"));
    }
    
    protected Dataset getDataset()
    {
        return dataset;
    }
    
    protected Model getRequestModel()
    {
        return ModelFactory.createDefaultModel().add(ResourceFactory.createResource("http://default/graph/resource"), FOAF.based_near, "Copenhagen");
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
    public void testGetDefaultModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }

    @Test
    public void testGetNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        
        assertIsomorphic(getDataset().getNamedModel(NAMED_GRAPH_URI), gsc.getModel(NAMED_GRAPH_URI));
    }

    @Test
    public void testGetNotFoundNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, "http://host/" + UUID.randomUUID().toString());
        
        assertEquals(NOT_FOUND.getStatusCode(), gsc.get(gsc.getReadableMediaTypes(Model.class), params).getStatus());
    }

    @Test
    public void testAddModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        gsc.add(getRequestModel());
        
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }
    
    @Test
    public void testAddNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        gsc.add(NAMED_GRAPH_URI, getRequestModel());
        
        assertIsomorphic(getDataset().getNamedModel(NAMED_GRAPH_URI), gsc.getModel(NAMED_GRAPH_URI));
    }

    @Test
    public void testPostEmptyNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, "http://host/" + UUID.randomUUID().toString());
        
        assertEquals(NO_CONTENT.getStatusCode(), gsc.post(ModelFactory.createDefaultModel(), MediaType.TEXT_TURTLE_TYPE, new javax.ws.rs.core.MediaType[]{}, params).getStatus());
    }

    @Test
    public void testPostNotFoundNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, "http://host/" + UUID.randomUUID().toString());
        
        assertEquals(CREATED.getStatusCode(), gsc.post(ModelFactory.createDefaultModel().createResource().addLiteral(ResourceFactory.createProperty("http://prop"), "obj").getModel(),
                MediaType.TEXT_TURTLE_TYPE, new javax.ws.rs.core.MediaType[]{}, params).getStatus());
    }
    
    @Test
    public void testPostUnsupportedAddType()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), gsc.post("BAD RDF", javax.ws.rs.core.MediaType.TEXT_XML_TYPE, new javax.ws.rs.core.MediaType[]{}).getStatus());
    }
    
    @Test
    public void testInvalidTurtlePost()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        assertEquals(BAD_REQUEST.getStatusCode(), gsc.post("BAD TURTLE", com.atomgraph.core.MediaType.TEXT_TURTLE_TYPE, new MediaType[]{}).getStatus());
    }
    
    @Test
    public void testPutModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        gsc.putModel(getRequestModel());
        
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }
    
    @Test
    public void testPutNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        gsc.putModel(NAMED_GRAPH_URI, getRequestModel());
        
        assertIsomorphic(getDataset().getNamedModel(NAMED_GRAPH_URI), gsc.getModel(NAMED_GRAPH_URI));
    }
    
    @Test
    public void testPutNotFoundNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, "http://host/" + UUID.randomUUID().toString());
        
        assertEquals(CREATED.getStatusCode(), gsc.put(ModelFactory.createDefaultModel(), MediaType.TEXT_TURTLE_TYPE, new javax.ws.rs.core.MediaType[]{}, params).getStatus());
    }
    
    @Test
    public void testNotUnsupportedPutType()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), gsc.put("BAD RDF", javax.ws.rs.core.MediaType.TEXT_XML_TYPE, new javax.ws.rs.core.MediaType[]{}).getStatus());
    }

    @Test
    public void testInvalidTurtlePut()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        assertEquals(BAD_REQUEST.getStatusCode(), gsc.put("BAD TURTLE", com.atomgraph.core.MediaType.TEXT_TURTLE_TYPE, new MediaType[]{}).getStatus());
    }

    @Test
    public void testDefaultModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        gsc.deleteDefault();
        
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }
    
    public void testDeleteNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        gsc.deleteModel(NAMED_GRAPH_URI);
        
        assertEquals(0, gsc.getModel(NAMED_GRAPH_URI).size());
    }
    
    @Test
    public void testDeleteNotFoundNamedModel()
    {
        GraphStoreClient gsc = GraphStoreClient.create(endpoint, new MediaTypes());
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, "http://host/" + UUID.randomUUID().toString());
        
        assertEquals(NOT_FOUND.getStatusCode(), gsc.delete(gsc.getReadableMediaTypes(Model.class), params).getStatus());
    }
    
}
