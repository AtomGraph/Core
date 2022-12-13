/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.atomgraph.core.model;

import org.apache.jena.query.Query;
import org.apache.jena.update.UpdateRequest;
import java.net.URI;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import com.atomgraph.core.MediaType;
import java.util.List;

/**
 * Generic SPARQL 1.1 Protocol interface
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see org.apache.jena.query.Query
 * @see org.apache.jena.update.UpdateRequest
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol for RDF</a>
 * 
 */
public interface SPARQLEndpoint
{

    public static String QUERY = "query";
    public static String UPDATE = "update";
    public static String DEFAULT_GRAPH_URI = "default-graph-uri";
    public static String NAMED_GRAPH_URI = "named-graph-uri";
    public static String USING_GRAPH_URI = "using-graph-uri";
    public static String USING_NAMED_GRAPH_URI = "using-named-graph-uri";

    /**
     * Handles GET query request and returns result as response
     * 
     * @param query the submitted SPARQL query
     * @param defaultGraphUris default graph URI
     * @param namedGraphUris named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-get">2.1.1 query via GET</a>
     */
    @GET Response get(@QueryParam(QUERY) Query query, @QueryParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUris, @QueryParam(NAMED_GRAPH_URI) List<URI> namedGraphUris);
    
    /**
     * Handles URL-encoded POST query request and returns result as response.
     * Query and UpdateRequest would be more appropriate form parameter types,
     * but the injection provider does not have access to request entity or form parameters.
     * 
     * @param queryString submitted SPARQL query string
     * @param updateString submitted SPARQL update string
     * @param defaultGraphUris default graph URI
     * @param namedGraphUris named graph URI
     * @param usingGraphUris default graph URI
     * @param usingNamedGraphUris named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-post-urlencoded">2.1.2 query via POST with URL-encoded parameters</a>
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-urlencoded">2.2.1 update via POST with URL-encoded parameters</a>
    */
    @POST @Consumes(MediaType.APPLICATION_FORM_URLENCODED) Response post(@FormParam(QUERY) String queryString, @FormParam(UPDATE) String updateString, @FormParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUris, @FormParam(NAMED_GRAPH_URI) List<URI> namedGraphUris, @FormParam(USING_GRAPH_URI) List<URI> usingGraphUris, @FormParam(USING_NAMED_GRAPH_URI) List<URI> usingNamedGraphUris);

    /**
     * Handles direct POST query request and returns result as response
     * 
     * @param query the submitted SPARQL query
     * @param defaultGraphUris default graph URI
     * @param namedGraphUris named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-post-direct">2.1.3 query via POST directly</a>
     */
    @POST @Consumes(MediaType.APPLICATION_SPARQL_QUERY) Response post(Query query, @QueryParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUris, @QueryParam(NAMED_GRAPH_URI) List<URI> namedGraphUris);
    
    /**
     * Handles direct POST update request and returns result as response
     * 
     * @param update update request (possibly multiple operations)
     * @param usingGraphUris default graph URI
     * @param usingNamedGraphUris named graph URI
     * @return success or failure response
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-direct">2.2.2 update via POST directly</a>
     */
    @POST @Consumes(MediaType.APPLICATION_SPARQL_UPDATE) Response post(UpdateRequest update, @QueryParam(USING_GRAPH_URI) List<URI> usingGraphUris, @QueryParam(USING_NAMED_GRAPH_URI) List<URI> usingNamedGraphUris);

}