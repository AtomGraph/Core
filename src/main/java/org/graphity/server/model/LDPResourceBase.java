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
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.core.ResourceContext;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-write Linked Data resources.
 * RDF representations are queried from, and stored into, SPARQL endpoints.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://www.w3.org/TR/2013/WD-ldp-20130307/">Linked Data Platform 1.0</a>
 */
@Path("{path: .*}")
public class LDPResourceBase extends LinkedDataResourceBase implements LDPResource
{    
    private static final Logger log = LoggerFactory.getLogger(LDPResourceBase.class);

    /**
     * JAX-RS-compatible resource constructor with injected initialization objects.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the request
     * @param request current request
     * @param servletContext webapp context
     * @param resourceContext resource context
     */
    public LDPResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletContext servletContext, @Context ResourceContext resourceContext)
    {
	super(uriInfo, request, servletContext);
    }

    /**
     * Protected constructor. Not suitable for JAX-RS but can be used when subclassing.
     * 
     * @param resource this resource as RDF resource
     * @param request current request
     * @param servletContext webapp context
     */
    protected LDPResourceBase(Resource resource, Request request, ServletContext servletContext)
    {
	super(resource, request, servletContext);
    }

    /**
     * Handles POST method, stores the submitted RDF model in the SPARQL endpoint, and returns response.
     * 
     * @param model RDF payload
     * @return response
     */
    public Response post(Model model)
    {
	if (log.isWarnEnabled()) log.warn("POST request with RDF payload: {}. Graphity Server is read-only!  Only GET is supported");
	throw new WebApplicationException(405);
    }

    /**
     * Handles PUT method, stores the submitted RDF model in the SPARQL endpoint, and returns response.
     * 
     * @param model RDF payload
     * @return response
     */
    public Response put(Model model)
    {
	if (log.isWarnEnabled()) log.warn("PUT request with RDF payload: {}. Graphity Server is read-only!  Only GET is supported");
	throw new WebApplicationException(405);
    }

    /**
     * Handles DELETE method, deletes the RDF representation of this resource from the SPARQL endpoint, and
     * returns response.
     * 
     * @return response
     */
    public Response delete()
    {
	if (log.isWarnEnabled()) log.warn("DELETE request with RDF payload: {}. Graphity Server is read-only! Only GET is supported");
	throw new WebApplicationException(405);
    }

}