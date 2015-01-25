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
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateRequest;
import java.net.URI;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.graphity.server.MediaType;

/**
 * Extended SPARQL endpoint interface, includes query and update as well as JAX-RS helper methods.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/update/UpdateRequest.html">Jena UpdateRequest</a>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">JAX-RS Response</a>
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol for RDF</a>
 * 
 */
@Path("/sparql")
public interface SPARQLEndpoint
{

    /**
     * Media types that can be used to represent of SPARQL result set
     * 
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-success">SPARQL 1.1 Protocol. 2.1.6 Success Responses</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSetRewindable.html">Jena ResultSetRewindable</a>
     */
    public static final javax.ws.rs.core.MediaType[] RESULT_SET_MEDIA_TYPES = new javax.ws.rs.core.MediaType[]{org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE};

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
     * Handles URL-encoded POST query request and returns result as response.
     * Query and UpdateRequest would be more appropriate form parameter types,
     * but the injection provider does not have access to request entity or form parameters.
     * 
     * @param queryString submitted SPARQL query string
     * @param updateString submitted SPARQL update string
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return result response (in one of the representation variants)
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-via-post-urlencoded">2.1.2 query via POST with URL-encoded parameters</a>
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-urlencoded">2.2.1 update via POST with URL-encoded parameters</a>
    */
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
    
    /**
     * Handles direct POST update request and returns result as response
     * 
     * @param update update request (possibly multiple operations)
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return success or failure response
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-direct">2.2.2 update via POST directly</a>
     */
    @POST @Consumes(MediaType.APPLICATION_SPARQL_UPDATE) Response post(UpdateRequest update, @QueryParam("using-graph-uri") URI defaultGraphUri, @QueryParam("using-named-graph-uri") URI graphUri);

    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<code>DESCRIBE</code> or <code>CONSTRUCT</code>)
     * 
     * @param query SPARQL query
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    Model loadModel(Query query);

    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<code>DESCRIBE</code>)
     * 
     * @param query SPARQL query
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     */
    Model describe(Query query);

    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<code>CONSTRUCT</code>)
     * 
     * @param query SPARQL query
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    Model construct(Query query);
    
    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<pre>SELECT</pre>)
     * 
     * @param query SPARQL query
     * @return SPARQL result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    ResultSetRewindable select(Query query);

    /**
     * Asks boolean result from the endpoint by executing a SPARQL query (<pre>ASK</pre>)
     * 
     * @param query SPARQL query
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    boolean ask(Query query);

    /**
     * Execute SPARQL update request
     * 
     * @param updateRequest update request
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-update-20130321/">SPARQL 1.1 Update</a>
     */
    void update(UpdateRequest updateRequest);

}