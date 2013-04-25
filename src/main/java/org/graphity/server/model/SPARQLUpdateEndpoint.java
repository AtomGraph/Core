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

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.UpdateRequest;
import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.graphity.server.MediaType;

/**
 * Generic SPARQL 1.1 Protocol for RDF update interface
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">JAX-RS Response</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/update/UpdateRequest.html">Jena UpdateRequest</a>
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol for RDF</a>
 */
@Path("/sparql")
public interface SPARQLUpdateEndpoint extends Resource
{
    /**
     * Handles encoded POST update request and returns result as response
     * 
     * @param update update request (possibly multiple operations)
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return success or failure response
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-urlencoded">2.2.1 update via POST with URL-encoded parameters</a>
     */
    @POST @Consumes(MediaType.APPLICATION_FORM_URLENCODED) Response update(@FormParam("update") UpdateRequest update, @FormParam("using-graph-uri") URI defaultGraphUri, @FormParam("using-named-graph-uri") URI graphUri);
    
    /**
     * Handles direct POST update request and returns result as response
     * 
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return success or failure response
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#update-via-post-direct">2.2.2 update via POST directly</a>
     */
    @POST @Consumes(MediaType.APPLICATION_SPARQL_UPDATE) Response update(@QueryParam("using-graph-uri") URI defaultGraphUri, @QueryParam("using-named-graph-uri") URI graphUri);

}