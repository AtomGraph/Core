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
import com.atomgraph.core.client.SPARQLClient;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
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
        
        SPARQLClient sc = SPARQLClient.create(endpoint, new MediaTypes());
        assertIsomorphic(getDataset().getDefaultModel(), sc.loadModel(query));
    }
    
    @Test
    public void testConstruct()
    {
        Query query = QueryFactory.create("CONSTRUCT WHERE { <" + RESOURCE_URI + "> ?p ?o }");
        
        SPARQLClient sc = SPARQLClient.create(endpoint, new MediaTypes());
        assertIsomorphic(getDataset().getDefaultModel(), sc.loadModel(query));
    }
    
    @Test
    public void testSelect()
    {
        Query query = QueryFactory.create("SELECT * { <" + RESOURCE_URI + "> ?p ?o }");
        
        SPARQLClient sc = SPARQLClient.create(endpoint, new MediaTypes());
        assertTrue(sc.select(query).hasNext());
    }

    @Test
    @Ignore
    // TO-DO: fix after Jena is upgraded using MessageBodyReader<SPARQLResult> instead of MessageBodyReader<ResultSet>
    // https://jena.apache.org/documentation/javadoc/arq/org/apache/jena/sparql/resultset/SPARQLResult.html
    public void testAsk()
    {
        Query query = QueryFactory.create("ASK { <" + RESOURCE_URI + "> ?p ?o }");
        
        SPARQLClient sc = SPARQLClient.create(endpoint, new MediaTypes());
        assertTrue(sc.ask(query));
    }
    
    // TO-DO: testUpdate()
    
    public static void assertIsomorphic(Model wanted, Model got)
    {
        if (!wanted.isIsomorphicWith(got))
            fail("Models not isomorphic (not structurally equal))");
    }
    
}
