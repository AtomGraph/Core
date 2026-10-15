/*
 * Copyright 2026 Martynas.Jusevicius.
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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Relative IRIs in a SPARQL update resolve against the request URI, as relative IRIs in RDF
 * request bodies do (ModelProvider). Read with no base, Jena resolved them against the server
 * process's working directory, so a PATCH of <code>&lt;#this&gt;</code> to a document wrote
 * <code>file:///usr/local/tomcat/#this</code>.
 *
 * @author Martynas.Jusevicius
 */
public class UpdateRequestProviderTest extends JerseyTest
{

    private static final String UPDATE = "INSERT DATA { <#this> <" + FOAF.name.getURI() + "> \"Smth\" }";

    /**
     * Applies the update it is sent to an empty model and returns the result as N-Triples. It takes
     * a POST: the provider reads the body the same for any method, and the test client sends PATCH
     * only through a reflective workaround the JDK's module system refuses.
     */
    @Path("docs/doc")
    public static class Document
    {

        @POST
        @Consumes(MediaType.APPLICATION_SPARQL_UPDATE)
        @Produces("application/n-triples")
        public String patch(UpdateRequest update)
        {
            Model model = ModelFactory.createDefaultModel();
            UpdateAction.execute(update, model);
            java.io.StringWriter out = new java.io.StringWriter();
            model.write(out, "N-TRIPLES");
            return out.toString();
        }

    }

    @Override
    protected Application configure()
    {
        return new ResourceConfig(Document.class).register(new UpdateRequestProvider());
    }

    @Test
    public void testRelativeIRIsResolvedAgainstBase()
    {
        UpdateRequest update = new UpdateRequestProvider().read(new ByteArrayInputStream(UPDATE.getBytes(StandardCharsets.UTF_8)), "http://example.com/docs/doc");
        Model model = ModelFactory.createDefaultModel();
        UpdateAction.execute(update, model);

        assertTrue(model.contains(ResourceFactory.createResource("http://example.com/docs/doc#this"), FOAF.name, "Smth"),
            "the relative IRI did not resolve against the base: " + model.listSubjects().toList());
    }

    @Test
    public void testRelativeIRIsResolvedAgainstRequestURI()
    {
        String nt = target("docs/doc").request().
            post(Entity.entity(UPDATE, MediaType.APPLICATION_SPARQL_UPDATE), String.class);
        String expected = "<" + getBaseUri().resolve("docs/doc#this") + ">";

        assertTrue(nt.startsWith(expected), "the relative IRI did not resolve against the request URI: " + nt);
    }

}
