/*
 * Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>
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

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Conditional requests evaluated by {@link Response}: the order of RFC 9110 section 13.2.2, the weak comparison of
 * <code>If-None-Match</code> on writes, and the headers a <code>304</code> carries.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class PreconditionsTest extends JerseyTest
{

    /** Last modified date of the test resource */
    public static final Date LAST_MODIFIED = Date.from(ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC).toInstant());
    /** An earlier date */
    public static final String EARLIER = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    /** A later date */
    public static final String LATER = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    @Path("test")
    public static class TestResource
    {

        private final Request request;

        @Inject
        public TestResource(@Context Request request)
        {
            this.request = request; // injected the way the graph store resources take it
        }

        public Response getInternalResponse()
        {
            return new Response(request, "entity", LAST_MODIFIED, new EntityTag("abcdef"),
                List.of(MediaType.TEXT_PLAIN_TYPE), List.of(Locale.ENGLISH, Locale.GERMAN), List.of(), mediaType -> true);
        }

        @GET
        public jakarta.ws.rs.core.Response get()
        {
            return getInternalResponse().getResponseBuilder().build();
        }

        @PUT
        public jakarta.ws.rs.core.Response put(String body)
        {
            ResponseBuilder rb = getInternalResponse().evaluatePreconditions();
            if (rb != null) return rb.build();

            return jakarta.ws.rs.core.Response.noContent().build();
        }

    }

    @Override
    protected Application configure()
    {
        return new ResourceConfig(TestResource.class);
    }

    private String currentTag()
    {
        jakarta.ws.rs.core.Response resp = target("test").request(MediaType.TEXT_PLAIN_TYPE).acceptLanguage(Locale.ENGLISH).get();
        assertEquals(200, resp.getStatus());
        return resp.getHeaderString(HttpHeaders.ETAG);
    }

    private jakarta.ws.rs.core.Response put(String header, String value, String header2, String value2)
    {
        return target("test").request().acceptLanguage(Locale.ENGLISH).
            header(header, value).header(header2, value2).
            put(Entity.text("body"));
    }

    private jakarta.ws.rs.core.Response get(String header, String value, String header2, String value2)
    {
        return target("test").request(MediaType.TEXT_PLAIN_TYPE).acceptLanguage(Locale.ENGLISH).
            header(header, value).header(header2, value2).
            get();
    }

    @Test
    public void testIfMatchOverridesIfUnmodifiedSince()
    {
        // the tag is current, so the older date is not evaluated
        assertEquals(204, put(HttpHeaders.IF_MATCH, currentTag(), HttpHeaders.IF_UNMODIFIED_SINCE, EARLIER).getStatus());
    }

    @Test
    public void testIfUnmodifiedSinceWithoutIfMatch()
    {
        assertEquals(412, put(HttpHeaders.IF_UNMODIFIED_SINCE, EARLIER, "X-None", "x").getStatus());
        assertEquals(204, put(HttpHeaders.IF_UNMODIFIED_SINCE, LATER, "X-None", "x").getStatus());
    }

    @Test
    public void testIfMatchStale()
    {
        assertEquals(412, put(HttpHeaders.IF_MATCH, "\"stale\"", HttpHeaders.IF_UNMODIFIED_SINCE, LATER).getStatus());
    }

    @Test
    public void testIfMatchWeakNeverMatches()
    {
        assertEquals(412, put(HttpHeaders.IF_MATCH, "W/" + currentTag(), "X-None", "x").getStatus());
    }

    @Test
    public void testIfNoneMatchOverridesIfModifiedSince()
    {
        // the tag matches, so the older date - which on its own would answer 200 - is not evaluated
        jakarta.ws.rs.core.Response resp = get(HttpHeaders.IF_NONE_MATCH, currentTag(), HttpHeaders.IF_MODIFIED_SINCE, EARLIER);
        assertEquals(304, resp.getStatus());

        // the tag does not match, so the later date - which on its own would answer 304 - is not evaluated
        assertEquals(200, get(HttpHeaders.IF_NONE_MATCH, "\"other\"", HttpHeaders.IF_MODIFIED_SINCE, LATER).getStatus());
    }

    @Test
    public void testIfModifiedSinceWithoutIfNoneMatch()
    {
        assertEquals(304, get(HttpHeaders.IF_MODIFIED_SINCE, LATER, "X-None", "x").getStatus());
        assertEquals(200, get(HttpHeaders.IF_MODIFIED_SINCE, EARLIER, "X-None", "x").getStatus());
    }

    @Test
    public void testWeakIfNoneMatchOnWrite()
    {
        // If-None-Match uses the weak comparison function whatever the method
        assertEquals(412, put(HttpHeaders.IF_NONE_MATCH, "W/" + currentTag(), "X-None", "x").getStatus());
        assertEquals(412, put(HttpHeaders.IF_NONE_MATCH, currentTag(), "X-None", "x").getStatus());
        assertEquals(412, put(HttpHeaders.IF_NONE_MATCH, "*", "X-None", "x").getStatus());
        assertEquals(204, put(HttpHeaders.IF_NONE_MATCH, "W/\"other\"", "X-None", "x").getStatus());
    }

    @Test
    public void testWeakIfNoneMatchOnRead()
    {
        assertEquals(304, get(HttpHeaders.IF_NONE_MATCH, "\"other\", W/" + currentTag(), "X-None", "x").getStatus());
    }

    @Test
    public void testNotModifiedCarriesValidatorsAndVaryOnly()
    {
        String tag = currentTag();
        jakarta.ws.rs.core.Response resp = get(HttpHeaders.IF_NONE_MATCH, tag, "X-None", "x");

        assertEquals(304, resp.getStatus());
        assertEquals(tag, resp.getHeaderString(HttpHeaders.ETAG));
        assertNotNull(resp.getHeaderString(HttpHeaders.LAST_MODIFIED));
        assertNotNull(resp.getHeaderString(HttpHeaders.VARY));
        assertNull(resp.getHeaderString(HttpHeaders.CONTENT_LANGUAGE));
        assertNull(resp.getHeaderString(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void testMatches()
    {
        EntityTag tag = new EntityTag("abc");

        assertTrue(Response.matches("*", tag, true));
        assertTrue(Response.matches("\"x\", \"abc\"", tag, true));
        assertFalse(Response.matches("W/\"abc\"", tag, true));
        assertTrue(Response.matches("W/\"abc\"", tag, false));
        assertFalse(Response.matches("\"abcd\"", tag, false));
        assertFalse(Response.matches("\"abc\"", new EntityTag("abc", true), true));
    }

    @Test
    public void testUnparseableDateIsIgnored()
    {
        assertNull(Response.parseHttpDate("yesterday"));
        assertEquals(204, put(HttpHeaders.IF_UNMODIFIED_SINCE, "yesterday", "X-None", "x").getStatus());
    }

}
