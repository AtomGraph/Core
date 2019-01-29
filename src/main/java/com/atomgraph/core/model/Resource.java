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

import java.net.URI;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import org.apache.jena.query.Dataset;

/**
 * Read-write Linked Data resource.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public interface Resource
{

    /**
     * Returns URI of the resource.
     * @return absolute path (excludes the query string)
     */
    public URI getURI();
    
    /**
     * Handles GET request and returns response
     * 
     * @return response to the current request
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">Response</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.1">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.1. GET</a>
     */
    @GET Response get();

    /**
     * Handles POST methods with RDF request body and returns response
     * 
     * @param dataset the RDF payload
     * @return response to current request
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.3">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.3. POST</a>
     */
    @POST Response post(Dataset dataset);

    /**
     * Handles PUT methods with RDF request body and returns response
     * 
     * @param dataset the RDF payload
     * @return response to current request
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.4">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.4. PUT</a>
     */
    @PUT Response put(Dataset dataset);
    
    /**
     * Handles DELETE methods
     * 
     * @return response to current request
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.5">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.5. DELETE</a>
     */
    @DELETE Response delete();

}