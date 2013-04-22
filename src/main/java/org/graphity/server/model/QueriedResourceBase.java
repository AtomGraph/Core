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
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.core.ResourceContext;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import org.graphity.server.vocabulary.GS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class QueriedResourceBase extends LinkedDataResourceBase implements QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(QueriedResourceBase.class);
    
    private final SPARQLEndpoint endpoint;

    /**
     * JAX-RS-compatible resource constructor with injected initialization objects
     * 
     * @param uriInfo URI information of the request
     * @param resourceConfig webapp configuration
     * @param resourceContext resource context
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/UriInfo.html">JAX-RS UriInfo</a>
     * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">Jersey ResourceConfig</a>
     * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceContext.html">Jersey ResourceContext</a>
     */
    public QueriedResourceBase(@Context UriInfo uriInfo, @Context ResourceConfig resourceConfig, @Context ResourceContext resourceContext)
    {
	this(ResourceFactory.createResource(uriInfo.getAbsolutePath().toString()),
		resourceContext.getResource(SPARQLEndpointBase.class),
		resourceConfig.getProperty(GS.cacheControl.getURI()) == null ?
		    null :
		    CacheControl.valueOf(resourceConfig.getProperty(GS.cacheControl.getURI()).toString()));
    }

    /**
     * Protected constructor. Not suitable for JAX-RS but can be used when subclassing.
     * 
     * @param resource This resource as Jena RDF resource (must be URI resource, not a blank node)
     * @param endpoint SPARQL endpoint of this resource
     * @param cacheControl Cache control config of this resource
     */
    protected QueriedResourceBase(Resource resource, SPARQLEndpoint endpoint, CacheControl cacheControl)
    {
	super(resource, cacheControl);
	if (endpoint == null) throw new IllegalArgumentException("SPARQL endpoint cannot be null");
	this.endpoint = endpoint;
    }

    /**
     * Returns RDF description of this resource.
     * The description is the result of a query executed on the SPARQL endpoint of this resource.
     * By default, the query is <code>DESCRIBE</code> with URI of this resource.
     * 
     * @return RDF description
     * @see getQuery()
     */
    public Model describe()
    {
	return getEndpoint().loadModel(getQuery());
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
	Model description = describe();

	if (description.isEmpty())
	{
	    if (log.isDebugEnabled()) log.debug("DESCRIBE Model is empty; returning 404 Not Found");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
	if (log.isDebugEnabled()) log.debug("Returning @GET Response with {} statements in Model", description.size());
	return getResponse(description);

    }

    /**
     * Builds response of an RDF model
     * 
     * @param model RDF model
     * @return response with the representation of the model
     */
    public Response getResponse(Model model)
    {
	return getResponseBuilder(model).build();
    }
    
    /**
     * Creates response builder for an RDF model
     * 
     * @param model RDF model
     * @return 
     */
    @Override
    public ResponseBuilder getResponseBuilder(Model model)
    {
	return getEndpoint().getResponseBuilder(model).
		cacheControl(getCacheControl());
    }

    public ResponseBuilder getResponseBuilder(Model model, List<Variant> variants)
    {
	return getEndpoint().getResponseBuilder(model, variants).
		cacheControl(getCacheControl());
    }
    
    @Override
    public Query getQuery()
    {
	return getQuery(getURI());
    }
    
    public Query getQuery(String uri)
    {
	return QueryFactory.create("DESCRIBE <" + uri + ">");
    }

    @Override
    public SPARQLEndpoint getEndpoint()
    {
	return endpoint;
    }

}