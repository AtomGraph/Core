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
import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.model.Service;
import jakarta.inject.Inject;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Variant;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class LocaleEntityTagTest extends JerseyTest
{
    
    public static final String RELATIVE_PATH = "test", RELATIVE_PATH_LANG = "test-lang";
    public static Dataset dataset;

    private com.atomgraph.core.Application system;
    private GraphStoreClient gsc;
    private URI uri, uriLang;
    
    @BeforeAll
    public static void initClass()
    {
        dataset = DatasetFactory.createTxnMem();
    }
    
    @BeforeEach
    public void init()
    {
        uri = getBaseUri().resolve(RELATIVE_PATH);
        uriLang = getBaseUri().resolve(RELATIVE_PATH_LANG);
        gsc = GraphStoreClient.create(system.getClient(), new MediaTypes());
        
        dataset.addNamedModel(uri.toString(), ModelFactory.createDefaultModel().
                add(ResourceFactory.createResource(uri.toString()), FOAF.name, "Smth"));
        dataset.addNamedModel(uriLang.toString(), ModelFactory.createDefaultModel().
                add(ResourceFactory.createResource(uriLang.toString()), FOAF.name, "Whateverest"));
    }
    
    @Path(RELATIVE_PATH)
    public static class TestResource extends DirectGraphStoreImpl
    {

        @Inject
        public TestResource(@Context Request request, Service service, MediaTypes mediaTypes, @Context UriInfo uriInfo)
        {
            super(request, service, mediaTypes, uriInfo);
        }

    }
    
    @Path(RELATIVE_PATH_LANG)
    public static class LangSpecificTestResource extends DirectGraphStoreImpl
    {

        @Inject
        public LangSpecificTestResource(@Context Request request, Service service, MediaTypes mediaTypes, @Context UriInfo uriInfo)
        {
            super(request, service, mediaTypes, uriInfo);
        }
        
        @Override
        public List<Locale> getLanguages()
        {
            return Arrays.asList(Locale.ENGLISH);
        }
    
        @Override
        public ResponseBuilder getResponseBuilder(Model model, URI graphUri)
        {
            return new com.atomgraph.core.model.impl.Response(getRequest(),
                    model,
                    getLastModified(model, graphUri),
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
                null);
        system.init();
        system.register(TestResource.class);
        system.register(LangSpecificTestResource.class);
        
        return system;
    }
    @Test
    public void testLocales()
    {
        Locale locale = Locale.ENGLISH;
        
        jakarta.ws.rs.core.Response resp = gsc.getClient().
                target(uri).
                request(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE).
                get();
        assertEquals(200, resp.getStatus());
        assertEquals(null, resp.getLanguage());

        jakarta.ws.rs.core.Response langSpecificResp = gsc.getClient().
                target(uriLang).
                request(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE). // RDF/XML media type marked as language significant on this endpoint!
                header(HttpHeaders.ACCEPT_LANGUAGE, locale.getLanguage()).
                get();      
        
        assertEquals(200, langSpecificResp.getStatus());
        assertEquals(locale, langSpecificResp.getLanguage());

        assertNotEquals(langSpecificResp.getEntityTag(), resp.getEntityTag());
    }

    /**
     * Two requests that select the same language-neutral variant but accept different languages are different
     * representations - the renderer falls back per value over the whole acceptable list - so they must not share a strong
     * entity tag. Before the acceptable languages were folded in, "lt" and "de" produced byte-different pages under one ETag,
     * and a conditional request could be answered 304 with the wrong language.
     */
    @Test
    public void testEntityTagVariesByAcceptableLanguages()
    {
        Variant variant = new Variant(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE, (java.util.Locale) null, null);
        EntityTag base = new EntityTag("cafe");
        java.util.function.Predicate<jakarta.ws.rs.core.MediaType> significant = new RDFXMLMediaTypePredicate();

        EntityTag lt = tagFor(variant, base, significant, List.of(java.util.Locale.forLanguageTag("lt")));
        EntityTag de = tagFor(variant, base, significant, List.of(java.util.Locale.forLanguageTag("de")));
        EntityTag alsoLt = tagFor(variant, base, significant, List.of(java.util.Locale.forLanguageTag("lt")));

        assertNotEquals(lt, de);   // different representations, different validators
        assertEquals(alsoLt, lt);  // same request, stable validator

        // a media type whose rendering does not depend on language is unaffected
        EntityTag plainLt = tagFor(variant, base, mediaType -> false, List.of(java.util.Locale.forLanguageTag("lt")));
        EntityTag plainDe = tagFor(variant, base, mediaType -> false, List.of(java.util.Locale.forLanguageTag("de")));
        assertEquals(plainLt, plainDe);

        // callers that supply no acceptable languages keep the previous entity tag exactly
        assertEquals(tagFor(variant, base, significant, List.of()), tagFor(variant, base, mediaType -> false, List.of()));
    }

    /** The entity tag calculation touches no request state, so a stub keeps the test to the thing under test. */
    private Request getRequestStub()
    {
        return new Request()
        {
            @Override public String getMethod() { return "GET"; }
            @Override public Variant selectVariant(List<Variant> variants) { return null; }
            @Override public jakarta.ws.rs.core.Response.ResponseBuilder evaluatePreconditions(EntityTag eTag) { return null; }
            @Override public jakarta.ws.rs.core.Response.ResponseBuilder evaluatePreconditions(java.util.Date lastModified) { return null; }
            @Override public jakarta.ws.rs.core.Response.ResponseBuilder evaluatePreconditions(java.util.Date lastModified, EntityTag eTag) { return null; }
            @Override public jakarta.ws.rs.core.Response.ResponseBuilder evaluatePreconditions() { return null; }
        };
    }

    private EntityTag tagFor(Variant variant, EntityTag base, java.util.function.Predicate<jakarta.ws.rs.core.MediaType> significant, List<java.util.Locale> acceptable)
    {
        return new com.atomgraph.core.model.impl.Response(getRequestStub(), "entity", null, base, variant, significant, acceptable).
            getVariantEntityTag();
    }

    // a language-negotiated entity has to advertise Accept-Language as a cache key dimension whether or not one of the
    // offered languages was acceptable - otherwise a shared cache may serve one language's representation to a client
    // that asked for another
    @Test
    public void testVaryIncludesAcceptLanguage()
    {
        jakarta.ws.rs.core.Response acceptable = gsc.getClient().
                target(uriLang).
                request(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE).
                header(HttpHeaders.ACCEPT_LANGUAGE, Locale.ENGLISH.getLanguage()). // the only language this resource offers
                get();

        assertEquals(200, acceptable.getStatus());
        assertNotNull(acceptable.getHeaderString(HttpHeaders.VARY));
        assertTrue(acceptable.getHeaderString(HttpHeaders.VARY).toLowerCase(Locale.ROOT).contains(HttpHeaders.ACCEPT_LANGUAGE.toLowerCase(Locale.ROOT)));

        // no offered language matches, so the variant falls back to a language-neutral one. The entity was still
        // negotiated over Accept-Language and its content still depends on it, so the dimension has to survive
        jakarta.ws.rs.core.Response unacceptable = gsc.getClient().
                target(uriLang).
                request(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE).
                header(HttpHeaders.ACCEPT_LANGUAGE, Locale.forLanguageTag("lt").getLanguage()).
                get();

        assertEquals(200, unacceptable.getStatus());
        assertNotNull(unacceptable.getHeaderString(HttpHeaders.VARY));
        assertTrue(unacceptable.getHeaderString(HttpHeaders.VARY).toLowerCase(Locale.ROOT).contains(HttpHeaders.ACCEPT_LANGUAGE.toLowerCase(Locale.ROOT)));

        // a multi-entry header, as sent by every real browser, negotiates the same way
        jakarta.ws.rs.core.Response multiple = gsc.getClient().
                target(uriLang).
                request(com.atomgraph.core.MediaType.APPLICATION_RDF_XML_TYPE).
                header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9,da;q=0.8,lt;q=0.7").
                get();

        assertEquals(200, multiple.getStatus());
        assertNotNull(multiple.getHeaderString(HttpHeaders.VARY));
        assertTrue(multiple.getHeaderString(HttpHeaders.VARY).toLowerCase(Locale.ROOT).contains(HttpHeaders.ACCEPT_LANGUAGE.toLowerCase(Locale.ROOT)));
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
    
    protected GraphStoreClient getGraphStoreClient()
    {
        return gsc;
    }
    
}
