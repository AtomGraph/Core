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
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import com.atomgraph.core.MediaType;
import java.util.List;

/**
 * Extended SPARQL endpoint interface, includes query and update as well as JAX-RS helper methods.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/update/UpdateRequest.html">Jena UpdateRequest</a>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">JAX-RS Response</a>
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
     * @param defaultGraphUri default graph URI
     * @param namedGraphUri named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-get">2.1.1 query via GET</a>
     */
    @GET Response get(@QueryParam(QUERY) Query query, @QueryParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUri, @QueryParam(NAMED_GRAPH_URI) List<URI> namedGraphUri);
    
    /**
     * Handles URL-encoded POST query request and returns result as response.
     * Query and UpdateRequest would be more appropriate form parameter types,
     * but the injection provider does not have access to request entity or form parameters.
     * 
     * @param queryString submitted SPARQL query string
     * @param updateString submitted SPARQL update string
     * @param defaultGraphUri default graph URI
     * @param namedGraphUri named graph URI
     * @param usingGraphUri default graph URI
     * @param usingNamedGraphUri named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-post-urlencoded">2.1.2 query via POST with URL-encoded parameters</a>
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-urlencoded">2.2.1 update via POST with URL-encoded parameters</a>
    */
    @POST @Consumes(MediaType.APPLICATION_FORM_URLENCODED) Response post(@FormParam(QUERY) String queryString, @FormParam(UPDATE) String updateString, @FormParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUri, @FormParam(NAMED_GRAPH_URI) List<URI> namedGraphUri, @FormParam(USING_GRAPH_URI) List<URI> usingGraphUri, @FormParam(USING_NAMED_GRAPH_URI) List<URI> usingNamedGraphUri);

    /**
     * Handles direct POST query request and returns result as response
     * 
     * @param query the submitted SPARQL query
     * @param defaultGraphUri default graph URI
     * @param namedGraphUri named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-post-direct">2.1.3 query via POST directly</a>
     */
    @POST @Consumes(MediaType.APPLICATION_SPARQL_QUERY) Response post(Query query, @QueryParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUri, @QueryParam(NAMED_GRAPH_URI) List<URI> namedGraphUri);
    
    /**
     * Handles direct POST update request and returns result as response
     * 
     * @param update update request (possibly multiple operations)
     * @param usingGraphUri default graph URI
     * @param usingNamedGraphUri named graph URI
     * @return success or failure response
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-direct">2.2.2 update via POST directly</a>
     */
    @POST @Consumes(MediaType.APPLICATION_SPARQL_UPDATE) Response post(UpdateRequest update, @QueryParam(USING_GRAPH_URI) List<URI> usingGraphUri, @QueryParam(USING_NAMED_GRAPH_URI) List<URI> usingNamedGraphUri);

}