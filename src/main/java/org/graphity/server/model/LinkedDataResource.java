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
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Read-only Linked Data resource. Supports HTTP content negotiation.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Produces({org.graphity.server.MediaType.APPLICATION_RDF_XML + "; charset=UTF-8", org.graphity.server.MediaType.TEXT_TURTLE + "; charset=UTF-8"})
public interface LinkedDataResource extends Resource
{

    /**
     * Handles GET request and returns response
     * 
     * @return response to the current request
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">Response</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.1">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.1. GET</a>
     */
    @GET Response get();

}