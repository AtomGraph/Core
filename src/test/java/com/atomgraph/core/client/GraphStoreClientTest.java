/*
 * Copyright 2025 Martynas.Jusevicius.
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
import static com.atomgraph.core.model.impl.GraphStoreImplTest.NAMED_GRAPH_URI;
import static com.atomgraph.core.model.impl.GraphStoreImplTest.dataset;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Application;
import java.net.URI;
import java.util.UUID;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Martynas.Jusevicius
 */
public class GraphStoreClientTest extends JerseyTest
{
    
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
    
    @Test(expected = NotFoundException.class)
    public void testGetNotFoundNamedModel()
    {
        gsc.getModel("http://host/" + UUID.randomUUID().toString());
    }
    
    @Test(expected = NotFoundException.class)
    public void testDeleteNotFoundNamedModel()
    {
        gsc.deleteModel("http://host/" + UUID.randomUUID().toString());
    }
    
}
