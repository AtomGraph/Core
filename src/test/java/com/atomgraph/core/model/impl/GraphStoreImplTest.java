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
import static com.atomgraph.core.model.impl.SPARQLEndpointImplTest.assertIsomorphic;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;
import java.util.UUID;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityTag;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import java.net.URI;
import java.util.Arrays;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
    public URI endpoint;
    public GraphStoreClient gsc;

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
        endpoint = getBaseUri().resolve("service");
        gsc = GraphStoreClient.create(system.getClient(), new MediaTypes(), endpoint);
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
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }

    @Test
    public void testGetNamedModel()
    {        
        assertIsomorphic(getDataset().getNamedModel(NAMED_GRAPH_URI), gsc.getModel(NAMED_GRAPH_URI));
    }

    @Test
    public void testGetNotFoundNamedModel()
    {
        assertEquals(NOT_FOUND.getStatusCode(), gsc.get(URI.create("http://host/" + UUID.randomUUID().toString())).getStatus());
    }

    @Test
    public void testGetNotAcceptableType()
    {
        assertEquals(NOT_ACCEPTABLE.getStatusCode(), gsc.get(null, new jakarta.ws.rs.core.MediaType[]{ MediaType.APPLICATION_SVG_XML_TYPE}).getStatus());
    }
    
    @Test
    public void testAddModel()
    {
        getDataset().getDefaultModel().removeAll();
        gsc.add(getRequestModel());
        
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }
    
    @Test
    public void testAddNamedModel()
    {
        gsc.add(NAMED_GRAPH_URI, getRequestModel());
        
        assertIsomorphic(getDataset().getNamedModel(NAMED_GRAPH_URI), gsc.getModel(NAMED_GRAPH_URI));
    }

    @Test
    public void testPostEmptyNamedModel()
    {
        assertEquals(NO_CONTENT.getStatusCode(), gsc.post(URI.create("http://host/" + UUID.randomUUID().toString()),
                Entity.entity(ModelFactory.createDefaultModel(), MediaType.TEXT_TURTLE_TYPE), 
                new jakarta.ws.rs.core.MediaType[]{}).
            getStatus());
    }

    @Test
    public void testPostNotFoundNamedModel()
    {
        assertEquals(CREATED.getStatusCode(), gsc.post(URI.create("http://host/" + UUID.randomUUID().toString()),
                Entity.entity(ModelFactory.createDefaultModel().createResource().addLiteral(ResourceFactory.createProperty("http://prop"), "obj").getModel(),
                MediaType.TEXT_TURTLE_TYPE),
                new jakarta.ws.rs.core.MediaType[]{}).
            getStatus());
    }
    
    @Test
    public void testPostUnsupportedAddType()
    {
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(),
                gsc.post(URI.create(NAMED_GRAPH_URI), Entity.entity("BAD RDF", jakarta.ws.rs.core.MediaType.TEXT_XML_TYPE),
                new jakarta.ws.rs.core.MediaType[]{}).getStatus());
    }
    
    @Test
    public void testInvalidTurtlePost()
    {
        assertEquals(BAD_REQUEST.getStatusCode(), gsc.post(URI.create(NAMED_GRAPH_URI),
                Entity.entity("BAD TURTLE", com.atomgraph.core.MediaType.TEXT_TURTLE_TYPE),
                new MediaType[]{}).
            getStatus());
    }
    
    @Test
    public void testPutModel()
    {
        gsc.putModel(getRequestModel());
        
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }
    
    @Test
    public void testPutNamedModel()
    {
        gsc.putModel(NAMED_GRAPH_URI, getRequestModel());
        
        assertIsomorphic(getDataset().getNamedModel(NAMED_GRAPH_URI), gsc.getModel(NAMED_GRAPH_URI));
    }
    
    @Test
    public void testPutNotFoundNamedModel()
    {
        assertEquals(CREATED.getStatusCode(), gsc.put(URI.create("http://host/" + UUID.randomUUID().toString()),
                Entity.entity(ModelFactory.createDefaultModel(), MediaType.TEXT_TURTLE_TYPE),
                new jakarta.ws.rs.core.MediaType[]{}).
            getStatus());
    }
    
    @Test
    public void testNotUnsupportedPutType()
    {
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), gsc.put(URI.create(NAMED_GRAPH_URI),
                Entity.entity("BAD RDF", jakarta.ws.rs.core.MediaType.TEXT_XML_TYPE),
                new jakarta.ws.rs.core.MediaType[]{}).
            getStatus());
    }

    @Test
    public void testInvalidTurtlePut()
    {
        assertEquals(BAD_REQUEST.getStatusCode(), gsc.put(URI.create(NAMED_GRAPH_URI),
                Entity.entity("BAD TURTLE", com.atomgraph.core.MediaType.TEXT_TURTLE_TYPE),
                new MediaType[]{}).
            getStatus());
    }

    @Test
    public void testDefaultModel()
    {
        gsc.deleteDefault();
        
        assertIsomorphic(getDataset().getDefaultModel(), gsc.getModel());
    }
    
    @Test(expected = NotFoundException.class)
    public void testDeleteNamedModel()
    {
        gsc.deleteModel(NAMED_GRAPH_URI);
        
        assertEquals(null, gsc.getModel(NAMED_GRAPH_URI));
    }
    
    @Test
    public void testDeleteNotFoundNamedModel()
    {
        assertEquals(NOT_FOUND.getStatusCode(), gsc.delete(URI.create("http://host/" + UUID.randomUUID().toString()),
                gsc.getReadableMediaTypes(Model.class)).
            getStatus());
    }
    
    @Test
    public void testDifferentMediaTypesDifferentETags()
    {
        jakarta.ws.rs.core.Response nTriplesResp = gsc.get(URI.create(NAMED_GRAPH_URI),
                Arrays.asList(MediaType.APPLICATION_NTRIPLES_TYPE).toArray(MediaType[]::new));
        EntityTag nTriplesETag = nTriplesResp.getEntityTag();
        assertEquals(nTriplesResp.getLanguage(), null);

        jakarta.ws.rs.core.Response rdfXmlResp = gsc.get(URI.create(NAMED_GRAPH_URI),
                Arrays.asList(MediaType.APPLICATION_RDF_XML_TYPE).toArray(MediaType[]::new));
        EntityTag rdfXmlETag = rdfXmlResp.getEntityTag();
        assertEquals(rdfXmlResp.getLanguage(), null);
        
        assertNotEquals(nTriplesETag, rdfXmlETag);
    }
    
}
