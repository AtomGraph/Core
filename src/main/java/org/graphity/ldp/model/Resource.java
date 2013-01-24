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
package org.graphity.ldp.model;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.core.*;

/**
 * HTTP resource with content negotiation
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public interface Resource
{
    /**
     * Handles GET request and returns response
     * 
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.html">Response</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.1">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content 5.3.1. GET</a>
     */
    @GET Response getResponse();

    /**
     * Return current request
     * 
     * @return current request for this resource
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Request.html">Request</a>
     */
    Request getRequest(); // Request can be injected

    //UriInfo getUriInfo(); // Request can be injected

    /**
     * Return representation variants
     * 
     * @return representation variants of this resource
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Variant.html">Variant</a>
     */
    List<Variant> getVariants();
    
    //EntityTag getEntityTag();
}