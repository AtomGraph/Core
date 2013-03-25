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
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import org.graphity.server.util.DataManager;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class QueriedResourceBase extends LinkedDataResourceBase implements QueriedResource
{
    /**
     * Configuration property for default Cache-Control header value (set in web.xml)
     * 
     */
    public static final String PROPERTY_ENDPOINT_URI = "org.graphity.server.endpoint-uri";

    /**
     * Configuration property for default Cache-Control header value (set in web.xml)
     * 
     */
    public static final String PROPERTY_CACHE_CONTROL = "org.graphity.server.cache-control";

    private final Resource endpoint;

    public QueriedResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders httpHeaders,
	    @Context ResourceConfig config)
    {
	this((config.getProperty(PROPERTY_ENDPOINT_URI) == null) ? null : ResourceFactory.createResource(config.getProperty(PROPERTY_ENDPOINT_URI).toString()),
	    uriInfo, request, httpHeaders, VARIANTS,
	    (config.getProperty(PROPERTY_CACHE_CONTROL) == null) ? null : CacheControl.valueOf(config.getProperty(PROPERTY_CACHE_CONTROL).toString()));
    }

    protected QueriedResourceBase(Resource endpoint,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants, CacheControl cacheControl)
    {
	this(ResourceFactory.createResource(uriInfo.getAbsolutePath().toString()), endpoint,
		uriInfo, request, httpHeaders, variants, cacheControl);
    }

    protected QueriedResourceBase(Resource resource, Resource endpoint,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants, CacheControl cacheControl)
    {
	super(resource,
		uriInfo, request, httpHeaders, variants, cacheControl);

	if (endpoint == null) throw new IllegalArgumentException("Endpoint cannot be null");
	this.endpoint = endpoint;
    }

    @Override
    public Response getResponse()
    {
	return getResponse(describe());
    }
    
    @Override
    public Model describe()
    {
	return loadModel(getEndpoint(), getQuery());
    }

    public Model describe(Model model)
    {
	return DataManager.get().loadModel(model, getQuery());
    }

    public Model loadModel(Model model, Query query)
    {
	return DataManager.get().loadModel(model, query);
    }

    public Model describe(Resource endpoint)
    {
	return loadModel(endpoint, getQuery());
    }

    public Model loadModel(Resource endpoint, Query query)
    {
	return DataManager.get().loadModel(endpoint.getURI(), query);
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
    public Resource getEndpoint()
    {
	return endpoint;
    }
}
