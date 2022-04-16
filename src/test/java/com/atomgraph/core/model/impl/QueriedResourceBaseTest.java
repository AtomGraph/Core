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

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.LinkedDataClient;
import com.atomgraph.core.model.Service;
import java.net.URI;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import javax.ws.rs.core.UriInfo;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class QueriedResourceBaseTest extends JerseyTest
{

    public static final String RELATIVE_PATH = "test";
    public static final String RESOURCE_URI = "http://localhost:9998/" + RELATIVE_PATH; // assume that base URI is http://localhost:9998/ - has to match getBaseUri()
    public static Dataset dataset;
    
    public com.atomgraph.core.Application system;
    public LinkedDataClient ldc;
    public URI uri;
    
    @BeforeClass
    public static void initClass()
    {
        dataset = DatasetFactory.createTxnMem();
        dataset.setDefaultModel(ModelFactory.createDefaultModel().add(ResourceFactory.createResource(RESOURCE_URI), FOAF.name, "Smth"));
    }
    
    @Before
    public void init()
    {
        uri = getBaseUri().resolve(RELATIVE_PATH);
        ldc = LinkedDataClient.create(system.getClient(), new MediaTypes());
    }
    
    @Path(RELATIVE_PATH)
    public static class TestResource extends QueriedResourceBase
    {

        @Inject
        public TestResource(@Context UriInfo uriInfo, @Context Request request, MediaTypes mediaTypes, Service service)
        {
            super(uriInfo, request, mediaTypes, service);
        }

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
        system.register(TestResource.class);
        
        return system;
    }
 
    @Test
    public void testGet()
    {
        assertIsomorphic(getDataset().getDefaultModel(), ldc.getModel(uri.toString()));
    }
    
    @Test
    public void testNotAcceptableGetType()
    {
        assertEquals(NOT_ACCEPTABLE.getStatusCode(), ldc.get(uri, ldc.getReadableMediaTypes(ResultSet.class)).getStatus());
    }
    
    @Test
    public void testNotFound()
    {
        URI nonExisting = getBaseUri().resolve("non-existing");
        assertEquals(NOT_FOUND.getStatusCode(), ldc.get(nonExisting, ldc.getReadableMediaTypes(Model.class)).getStatus());
    }
    
    @Test
    public void testNotUnsupportedPostType()
    {
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), ldc.post(uri, ldc.getReadableMediaTypes(Model.class), "BAD RDF", MediaType.TEXT_XML_TYPE).getStatus());
    }

    @Test
    public void testInvalidTurtlePost()
    {
        assertEquals(BAD_REQUEST.getStatusCode(), ldc.post(uri, ldc.getReadableMediaTypes(Model.class), "BAD TURTLE", com.atomgraph.core.MediaType.TEXT_TURTLE_TYPE).getStatus());
    }

    @Test
    public void testInvalidTurtlePut()
    {
        assertEquals(BAD_REQUEST.getStatusCode(), ldc.put(uri, ldc.getReadableMediaTypes(Model.class), "BAD TURTLE", com.atomgraph.core.MediaType.TEXT_TURTLE_TYPE).getStatus());
    }

    public static void assertIsomorphic(Model wanted, Model got)
    {
        if (!wanted.isIsomorphicWith(got))
            fail("Models not isomorphic (not structurally equal))");
    }
}
