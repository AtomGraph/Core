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
package com.atomgraph.core.io;

import com.atomgraph.core.MediaType;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import static com.atomgraph.core.client.GraphStoreClient.DEFAULT_PARAM_NAME;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Martynas.Jusevicius
 */
public class ModelProviderTest extends JerseyTest
{

    public com.atomgraph.core.Application system;
    public WebTarget endpoint;
    public GraphStoreClient gsc;

    @Before
    public void init()
    {
        endpoint = system.getClient().target(getBaseUri().resolve("service"));
        gsc = GraphStoreClient.create(new MediaTypes(), endpoint);
    }
    
    @Override
    protected Application configure()
    {
        system = new com.atomgraph.core.Application(DatasetFactory.createTxnMem(),
                null, null, null, null, null,
                new MediaTypes(), com.atomgraph.core.Application.getClient(new ClientConfig()),
                null, false, false);
        system.init();
        
        return system;
    }
    

    @Test(expected = RiotException.class)
    public void testRelativeURIsInNTriples()
    {
        String invalidNTriples = """
            <relative> <http://xmlns.com/foaf/0.1/name> "Smth" .
        """;

        InputStream inputStream = new ByteArrayInputStream(invalidNTriples.getBytes(StandardCharsets.UTF_8));

        Model model = ModelFactory.createDefaultModel();

        ModelProvider modelProvider = new ModelProvider();
        modelProvider.read(model, inputStream, Lang.NTRIPLES, null);
    }

    @Test
    public void testRelativeURIsResolvedAgainstBaseInNTriples()
    {
        String baseUri = "http://example.com/";
        String relativeUri = "relative";
        
        String invalidNTriples = String.format("""
            <%s> <http://xmlns.com/foaf/0.1/name> "Smth" .
        """, relativeUri);

        InputStream inputStream = new ByteArrayInputStream(invalidNTriples.getBytes(StandardCharsets.UTF_8));
        ModelProvider modelProvider = new ModelProvider();
        // needs to use Turtle in order to allow relative URIs
        Model actual = modelProvider.read(ModelFactory.createDefaultModel(), inputStream, Lang.TURTLE, baseUri);
        
        Model expected = ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(URI.create(baseUri).resolve(relativeUri).toString()), FOAF.name, "Smth");
        
        assertIsomorphic(expected, actual);
    }
    
    @Test
    public void testTurtleRelativeURIsResolvedInWrittenModel()
    {
        String relativeUri = "relative";
        Model modelWithRelativeURIs = ModelFactory.createDefaultModel().
            add(ResourceFactory.createResource(relativeUri), FOAF.name, "Smth");
        
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(DEFAULT_PARAM_NAME, Boolean.TRUE.toString());

        // needs to use Turtle in order to allow relative URIs
        try (Response cr = gsc.post(modelWithRelativeURIs, MediaType.TEXT_TURTLE_TYPE, new jakarta.ws.rs.core.MediaType[]{}, params))
        {
            URI absoluteUri = getBaseUri().resolve(relativeUri);
            Model expected = ModelFactory.createDefaultModel().
                add(ResourceFactory.createResource(absoluteUri.toString()), FOAF.name, "Smth");

            System.out.println(gsc.getModel());

            // check that the model that was written had resolved relative URIs against the base URI
            assertIsomorphic(expected, gsc.getModel());
        }
    }
    
    public static void assertIsomorphic(Model wanted, Model got)
    {
        if (!wanted.isIsomorphicWith(got))
            fail("Models not isomorphic (not structurally equal))");
    }
    
}
