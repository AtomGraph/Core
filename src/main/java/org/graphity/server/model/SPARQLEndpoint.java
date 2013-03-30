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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Generic SPARQL endpoint interface
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://www.w3.org/TR/rdf-sparql-protocol/">SPARQL Protocol for RDF</a>
 */
@Path("/sparql")
@Produces({org.graphity.server.MediaType.APPLICATION_RDF_XML + "; charset=UTF-8", org.graphity.server.MediaType.TEXT_TURTLE + "; charset=UTF-8", org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_XML + "; charset=UTF-8", org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_JSON + "; charset=UTF-8"})
public interface SPARQLEndpoint extends Resource
{
    /**
     * Handles SPARQL Protocol for RDF request and returns query result as response
     * 
     * @param query the submitted SPARQL query or null
     * @return result response (in one of the representation variants)
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
     */
    @GET Response query(@QueryParam("query") Query query); // @QueryParam("default-graph-uri") String uri @QueryParam("named-graph-uri") String graphUri
    
    // Context getServiceContext();
}
