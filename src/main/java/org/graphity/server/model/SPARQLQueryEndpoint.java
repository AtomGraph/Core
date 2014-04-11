/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.UpdateRequest;
import java.net.URI;
import javax.ws.rs.*;
import org.graphity.server.MediaType;
import javax.ws.rs.core.Response;

/**
 * Generic SPARQL 1.1 Protocol for RDF query interface
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">JAX-RS Response</a>
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol for RDF</a>
 */
@Path("/sparql")
public interface SPARQLQueryEndpoint extends Resource
{
    /**
     * Handles GET query request and returns result as response
     * 
     * @param query the submitted SPARQL query
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-get">2.1.1 query via GET</a>
     */
    @GET Response get(@QueryParam("query") Query query, @QueryParam("default-graph-uri") URI defaultGraphUri, @QueryParam("named-graph-uri") URI graphUri);
    
    /**
     * Handles encoded POST query request and returns result as response
     * 
     * @param query the submitted SPARQL query
     * @param update update request (possibly multiple operations)
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-post-urlencoded">2.1.2 query via POST with URL-encoded parameters</a>
     */
    //@POST @Consumes(MediaType.APPLICATION_FORM_URLENCODED) Response post(@FormParam("query") Query query, @FormParam("update") UpdateRequest update, @FormParam("default-graph-uri") URI defaultGraphUri, @FormParam("named-graph-uri") URI graphUri);
    @POST @Consumes(MediaType.APPLICATION_FORM_URLENCODED) Response post(@FormParam("query") String queryString, @FormParam("update") String updateString, @FormParam("using-graph-uri") URI defaultGraphUri, @FormParam("using-named-graph-uri") URI graphUri);

    /**
     * Handles direct POST query request and returns result as response
     * 
     * @param query the submitted SPARQL query
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-post-direct">2.1.3 query via POST directly</a>
     */
    @POST @Consumes(MediaType.APPLICATION_SPARQL_QUERY) Response post(Query query, @QueryParam("default-graph-uri") URI defaultGraphUri, @QueryParam("named-graph-uri") URI graphUri);
   
}