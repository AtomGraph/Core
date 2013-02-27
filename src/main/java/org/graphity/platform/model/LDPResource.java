/*
 * Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graphity.platform.model;

import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.graphity.platform.MediaType;

/**
 * Generic read-write Linked Data resource interface
 * 
 * @see LinkedDataResource
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Consumes({MediaType.APPLICATION_RDF_XML + "; charset=UTF-8", MediaType.TEXT_TURTLE + "; charset=UTF-8"})
public interface LDPResource extends Resource
{
    //@GET Response getResponse();
    
    /**
     * Handles POST methods with RDF request body and returns response
     * 
     * @param model the RDF Model that was POSTed
     * @return response to current request
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Model</a>
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">Response</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.3">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.3. POST</a>
     */
    @POST Response post(Model model);

    /**
     * Handles PUT methods with RDF request body and returns response
     * 
     * @param model the RDF Model that was PUT
     * @return response to current request
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Model</a>
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">Response</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.4">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.4. PUT</a>
     */
    @PUT Response put(Model model);
    
    /**
     * Handles DELETE methods
     * 
     * @return response to current request
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">Response</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.5">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.5. DELETE</a>
     */
    @DELETE Response delete();
    
    //EntityTag getEntityTag();
}