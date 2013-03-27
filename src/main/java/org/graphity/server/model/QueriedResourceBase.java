/*
 * Copyright (C) 2013 Martynas Jusevičius <martynas@graphity.org>
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
    
    private final SPARQLEndpointBase endpoint;

    public QueriedResourceBase(@Context UriInfo uriInfo, @Context ResourceConfig resourceConfig, @Context ResourceContext resourceContext)
    {
	this(ResourceFactory.createResource(uriInfo.getAbsolutePath().toString()),
		resourceContext.getResource(SPARQLEndpointBase.class),
		(resourceConfig.getProperty(PROPERTY_CACHE_CONTROL) == null) ? null : CacheControl.valueOf(resourceConfig.getProperty(PROPERTY_CACHE_CONTROL).toString()));
    }

    protected QueriedResourceBase(Resource resource, SPARQLEndpointBase endpoint, CacheControl cacheControl)
    {
	super(resource, cacheControl);
	if (endpoint == null) throw new IllegalArgumentException("SPARQL endpoint cannot be null");
	this.endpoint = endpoint;
    }

    /*
    private Model getModel()
    {
	return getEndpoint().loadModel(getQuery());
    }
    */
    
    @GET
    @Override
    public Response getResponse()
    {
	Model model = getEndpoint().loadModel(getQuery());

	if (model.isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("DESCRIBE Model is empty; returning 404 Not Found");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
	if (log.isDebugEnabled()) log.debug("Returning @GET Response with {} statements in Model", model.size());
	return getResponseBuilder(model).build();

    }
    
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
    public SPARQLEndpointBase getEndpoint()
    {
	return endpoint;
    }

}
