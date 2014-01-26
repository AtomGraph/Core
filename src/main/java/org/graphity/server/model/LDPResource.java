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

import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * Generic read-write Linked Data resource interface
 * 
 * @see LinkedDataResource
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">Response</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Model</a>
 */
//@Consumes({MediaType.APPLICATION_RDF_XML + "; charset=UTF-8", MediaType.TEXT_TURTLE + "; charset=UTF-8"})
public interface LDPResource extends LinkedDataResource
{

    /**
     * Handles POST methods with RDF request body and returns response
     * 
     * @param model the RDF payload
     * @return response to current request
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.3">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.3. POST</a>
     */
    @POST Response post(Model model);

    /**
     * Handles PUT methods with RDF request body and returns response
     * 
     * @param model the RDF payload
     * @return response to current request
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.4">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.4. PUT</a>
     */
    @PUT Response put(Model model);
    
    /**
     * Handles DELETE methods
     * 
     * @return response to current request
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.5">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.5. DELETE</a>
     */
    @DELETE Response delete();

}