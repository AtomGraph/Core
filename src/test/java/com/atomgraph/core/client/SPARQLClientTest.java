/*
 * Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>.
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
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Which HTTP method the SPARQL Protocol client submits a query with.
 *
 * A query is sent as GET while its URL fits maxGetRequestSize and as a form POST once it does not:
 * proxies and servlet containers cap the request line, so a large query can only travel in a body.
 * The request never leaves the process — a filter records the method and aborts it.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class SPARQLClientTest
{

    public static final String ENDPOINT_URI = "http://localhost/sparql";
    public static final int MAX_GET_REQUEST_SIZE = 1024;

    private String method;
    private URI requestURI;

    /** Records the outgoing method/URI and aborts, so no server is needed. */
    private SPARQLClient client(int maxGetRequestSize)
    {
        WebTarget endpoint = ClientBuilder.newClient().target(ENDPOINT_URI);
        SPARQLClient client = SPARQLClient.create(new MediaTypes(), endpoint, maxGetRequestSize);

        client.register((ClientRequestFilter) request ->
        {
            method = request.getMethod();
            requestURI = request.getUri();
            request.abortWith(Response.ok().build());
        });

        return client;
    }

    /** A query padded with VALUES until its URL exceeds the threshold. */
    private Query query(int valueCount)
    {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < valueCount; i++) values.append("<http://localhost/resources/").append(i).append("> ");

        return QueryFactory.create("SELECT * WHERE { VALUES ?s { " + values + " } ?s ?p ?o }");
    }

    @Test
    public void testSmallQueryIsSentAsGet()
    {
        Query query = query(1);
        assertTrue(query.toString().length() < MAX_GET_REQUEST_SIZE, "test query is not small enough");

        client(MAX_GET_REQUEST_SIZE).query(query, ResultSet.class);

        assertEquals("GET", method);
        assertTrue(requestURI.getRawQuery().startsWith(SPARQLClient.QUERY_PARAM_NAME + "="), "query is not in the URL");
    }

    @Test
    public void testLargeQueryIsSentAsPost()
    {
        Query query = query(100);
        assertTrue(query.toString().length() > MAX_GET_REQUEST_SIZE, "test query is not large enough");

        client(MAX_GET_REQUEST_SIZE).query(query, ResultSet.class);

        assertEquals("POST", method);
        assertEquals(ENDPOINT_URI, requestURI.toString(), "query should travel in the body, not the URL");
    }

    /** The length the branch is chosen on has to include the query, not just the endpoint URI. */
    @Test
    public void testQueryURLLengthIncludesQuery()
    {
        SPARQLClient client = client(MAX_GET_REQUEST_SIZE);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        int emptyLength = client.getQueryURLLength(params);

        params.putSingle(SPARQLClient.QUERY_PARAM_NAME, query(100).toString());

        assertTrue(client.getQueryURLLength(params) > emptyLength + MAX_GET_REQUEST_SIZE,
            "query length is not reflected in the request URL length");
    }

    /** The form POST carries the query as application/x-www-form-urlencoded. */
    @Test
    public void testPostUsesFormMediaType()
    {
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED_TYPE, client(MAX_GET_REQUEST_SIZE).getDefaultMediaType());
    }

}
