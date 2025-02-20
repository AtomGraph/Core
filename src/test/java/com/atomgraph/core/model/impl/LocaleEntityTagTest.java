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
package com.atomgraph.core.model.impl;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.LinkedDataClient;
import com.atomgraph.core.model.Service;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
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
public class LocaleEntityTagTest extends JerseyTest
{
    
    public static final String RELATIVE_PATH = "test", RELATIVE_PATH_LANG = "test-lang";
    public static final String RESOURCE_URI = "http://localhost:9998/" + RELATIVE_PATH; // assume that base URI is http://localhost:9998/ - has to match getBaseUri()
    public static final String RESOURCE_URI_LANG = "http://localhost:9998/" + RELATIVE_PATH_LANG; // assume that base URI is http://localhost:9998/ - has to match getBaseUri()
    public static Dataset dataset;

    private com.atomgraph.core.Application system;
    private LinkedDataClient ldc;
    private URI uri, uriLang;
    
    @BeforeClass
    public static void initClass()
    {
        dataset = DatasetFactory.createTxnMem();
        Model defaultModel = ModelFactory.createDefaultModel().
                add(ResourceFactory.createResource(RESOURCE_URI), FOAF.name, "Smth").
                add(ResourceFactory.createResource(RESOURCE_URI_LANG), FOAF.name, "Whateverest");
        dataset.setDefaultModel(defaultModel);
    }
    
    @Before
    public void init()
    {
        uri = getBaseUri().resolve(RELATIVE_PATH);
        uriLang = getBaseUri().resolve(RELATIVE_PATH_LANG);
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
    
    @Path(RELATIVE_PATH_LANG)
    public static class LangSpecificTestResource extends QueriedResourceBase
    {

        @Inject
        public LangSpecificTestResource(@Context UriInfo uriInfo, @Context Request request, MediaTypes mediaTypes, Service service)
        {
            super(uriInfo, request, mediaTypes, service);
        }

        @GET
        @Override
        public jakarta.ws.rs.core.Response get()
        {
            final Model model = describe();

            if (model.isEmpty())
            {
                throw new NotFoundException("Query result Dataset is empty");
            }

            return getResponse(model);
        }
        
        @Override
        public List<Locale> getLanguages()
        {
            return Arrays.asList(Locale.ENGLISH);
        }
    
        @Override
        // 
        public ResponseBuilder getResponseBuilder(Model model)
        {
            return new com.atomgraph.core.model.impl.Response(getRequest(),
                    model,
                    getLastModified(model),
                    getEntityTag(model),
                    getWritableMediaTypes(Model.class),
                    getLanguages(),
                    getEncodings(),
                new RDFXMLMediaTypePredicate()).
                getResponseBuilder();
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
        system.register(LangSpecificTestResource.class);
        
        return system;
    }
    @Test
    public void testLocales()
    {
        Locale locale = Locale.ENGLISH;
        
        jakarta.ws.rs.core.Response resp = ldc.getClient().
                target(uri).
                request(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE).
                get();
        assertEquals(200, resp.getStatus());
        assertEquals(null, resp.getLanguage());

        jakarta.ws.rs.core.Response langSpecificResp = ldc.getClient().
                target(uriLang).
                request(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE). // RDF/XML media type marked as language significant on this endpoint!
                header(HttpHeaders.ACCEPT_LANGUAGE, locale.getLanguage()).
                get();      
        
        assertEquals(200, langSpecificResp.getStatus());
        assertEquals(locale, langSpecificResp.getLanguage());

        assertNotEquals(langSpecificResp.getEntityTag(), resp.getEntityTag());
    }
    
    // make Accept-Language/Content-Language significant for RDF/XML (just as a test)
    public static class RDFXMLMediaTypePredicate implements Predicate<MediaType>
    {

        @Override
        public boolean test(MediaType mediaType)
        {
            if (mediaType == null)
            {
                return false;
            }

            return mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE);
        }

    }
    protected URI getURI()
    {
        return uri;
    }
    
    protected LinkedDataClient getLinkedDataClient()
    {
        return ldc;
    }
    
}
