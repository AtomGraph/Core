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

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import jakarta.ws.rs.core.Application;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Martynas.Jusevicius
 */
public class ModelProviderTest extends JerseyTest
{

    public com.atomgraph.core.Application system;
    public GraphStoreClient gsc;

    @BeforeEach
    public void init()
    {
        gsc = GraphStoreClient.create(system.getClient(), new MediaTypes());
    }
    
    @Override
    protected Application configure()
    {
        system = new com.atomgraph.core.Application(DatasetFactory.createTxnMem(),
                null, null, null, null, null,
                new MediaTypes(), com.atomgraph.core.Application.getClient(new ClientConfig()),
                null);
        system.init();
        
        return system;
    }
    

    /**
     * ModelProvider and DatasetProvider are both registered and neither declares @Consumes, so both
     * are candidates for every media type. They never compete because the isReadable/isWriteable
     * guards are exclusive: this one handles Model in triples languages, the other Dataset. When
     * DatasetProvider's guards named Model (they did), both claimed Model entities and nothing
     * claimed Dataset — which is a provider-selection ordering away from a ClassCastException.
     */
    @Test
    public void testHandlesModelTypeOnly()
    {
        ModelProvider provider = new ModelProvider();
        jakarta.ws.rs.core.MediaType turtle = jakarta.ws.rs.core.MediaType.valueOf("text/turtle");
        jakarta.ws.rs.core.MediaType nquads = jakarta.ws.rs.core.MediaType.valueOf("application/n-quads");

        assertTrue(provider.isReadable(Model.class, null, null, turtle), "cannot read a Model");
        assertTrue(provider.isWriteable(Model.class, null, null, turtle), "cannot write a Model");

        assertFalse(provider.isReadable(org.apache.jena.query.Dataset.class, null, null, turtle),
            "reads a Dataset, which is DatasetProvider's");
        assertFalse(provider.isWriteable(org.apache.jena.query.Dataset.class, null, null, turtle),
            "writes a Dataset, which is DatasetProvider's");

        assertFalse(provider.isReadable(Model.class, null, null, nquads), "reads quads into a Model");
        assertFalse(provider.isWriteable(Model.class, null, null, nquads), "writes a Model as quads");
    }

    @Test
    public void testRelativeURIsInNTriples()
    {
        String invalidNTriples = """
            <relative> <http://xmlns.com/foaf/0.1/name> "Smth" .
        """;

        InputStream inputStream = new ByteArrayInputStream(invalidNTriples.getBytes(StandardCharsets.UTF_8));

        Model model = ModelFactory.createDefaultModel();

        ModelProvider modelProvider = new ModelProvider();
        assertThrows(RiotException.class, () -> modelProvider.read(model, inputStream, Lang.NTRIPLES, null));
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
    
    public static void assertIsomorphic(Model wanted, Model got)
    {
        if (!wanted.isIsomorphicWith(got))
            fail("Models not isomorphic (not structurally equal))");
    }
    
}
