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
import com.sun.jersey.api.core.ResourceConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-only Linked Data resources with RDF representations queried from SPARQL endpoints.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see SPARQLEndpoint
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
 */
@Path("{path: .*}")
public class QueriedResourceBase extends LinkedDataResourceBase //implements QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(QueriedResourceBase.class);
    
    private final Model description;

    /**
     * JAX-RS-compatible resource constructor with injected initialization objects.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the request
     * @param request current request
     * @param resourceConfig webapp configuration
     * @param description description of this resource
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/UriInfo.html">JAX-RS UriInfo</a>
     * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">Jersey ResourceConfig</a>
     * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceContext.html">Jersey ResourceContext</a>
     */
    public QueriedResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context ResourceConfig resourceConfig,
            @Context Model description)
    {
	this(description.createResource(uriInfo.getAbsolutePath().toString()), request, resourceConfig,
                description);
    }

    /**
     * Protected constructor. Not suitable for JAX-RS but can be used when subclassing.
     * 
     * @param resource This resource as RDF resource (must be URI resource, not a blank node)
     * @param request current request
     * @param resourceConfig Resource config
     * @param description description of this resource
     */
    protected QueriedResourceBase(Resource resource, Request request, ResourceConfig resourceConfig,
            Model description)
    {
	super(resource, request, resourceConfig);
	if (description == null) throw new IllegalArgumentException("Resource description Model cannot be null");
        this.description = description;
    }

    /**
     * Returns RDF description of this resource.
     * The description is the result of a query executed on the SPARQL endpoint of this resource.
     * By default, the query is <code>DESCRIBE</code> with URI of this resource.
     * 
     * @return RDF description
     * @see getQuery()
     */
    public Model getDescription()
    {
	return description;
    }
    
    /**
     * Handles GET request and returns response with RDF description of this resource.
     * 
     * @return response with RDF description
     */
    @GET
    @Override
    public Response get()
    {
	if (log.isDebugEnabled()) log.debug("Returning @GET Response with {} statements in Model", description.size());
	return getResponse(getDescription());
    }
}