/**
 *  Copyright 2014 Martynas Jusevičius <martynas@graphity.org>
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

package org.graphity.core.model.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import java.net.URI;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import org.graphity.core.MediaType;
import org.graphity.core.MediaTypes;
import org.graphity.core.model.GraphStoreOrigin;
import org.graphity.core.model.GraphStoreProxy;
import org.graphity.core.model.Origin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of Graph Store.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/service") // not standard
public class GraphStoreProxyBase extends GraphStoreBase implements GraphStoreProxy
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreProxyBase.class);

    private final Origin origin;
    private final javax.ws.rs.core.MediaType[] modelMediaTypes;
    
    /**
     * Constructs Graph Store proxy from request metadata and origin.
     * 
     * @param request request
     * @param servletConfig servlet config
     * @param mediaTypes supported media types
     * @param origin graph store origin
     */
    public GraphStoreProxyBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context GraphStoreOrigin origin)
    {
        super(request, servletConfig, mediaTypes);
        if (origin == null) throw new IllegalArgumentException("GraphStoreOrigin cannot be null");
        this.origin = origin;
        modelMediaTypes = mediaTypes.getModelMediaTypes().toArray(new javax.ws.rs.core.MediaType[mediaTypes.getModelMediaTypes().size()]);        
    }

     /**
     * Returns configured Graph Store resource.
     * This graph store is a proxy for the remote one.
     * 
     * @return graph store resource
     */
    @Override
    public Origin getOrigin()
    {
        return origin;
    }

    public javax.ws.rs.core.MediaType[] getModelMediaTypes()
    {
        return modelMediaTypes;
    }
    
    @Override
    public Model getModel()
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getOrigin().getURI());
	return getEndpoint().queryParam("default", "").
            accept(getModelMediaTypes()).
            get(ClientResponse.class).
                getEntity(Model.class);
    }

    @Override
    public Model getModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getOrigin().getURI(), uri);
	return getEndpoint().queryParam("graph", uri).
            accept(getModelMediaTypes()).
            get(ClientResponse.class).
                getEntity(Model.class);
    }

    @Override
    public boolean containsModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("Checking if Graph Store {} contains GRAPH with URI {}", getOrigin().getURI(), uri);
	return headNamed(getOrigin().getURI(), uri).
            getStatusInfo().
            getFamily().equals(javax.ws.rs.core.Response.Status.Family.SUCCESSFUL);
    }

    public ClientResponse headNamed(String graphStoreURI, String graphURI)
    {
	return getEndpoint().queryParam("graph", graphURI).
            method("HEAD", ClientResponse.class);
    }
    
    @Override
    public void putModel(Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getOrigin().getURI());
	getEndpoint().queryParam("default", "").
            type(MediaType.TEXT_NTRIPLES).
            put(ClientResponse.class, model);
    }

    @Override
    public void putModel(String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getOrigin().getURI(), uri);
	getEndpoint().queryParam("graph", uri).
            type(MediaType.TEXT_NTRIPLES).
            put(ClientResponse.class, model);
    }

    @Override
    public void deleteDefault()
    {
	if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getOrigin().getURI());
	getEndpoint().queryParam("default", "").
            delete(ClientResponse.class);
    }

    @Override
    public void deleteModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", uri, getOrigin().getURI());
	getEndpoint().queryParam("graph", uri).
            delete(ClientResponse.class);
    }

    @Override
    public void add(Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getOrigin().getURI());
	getEndpoint().queryParam("default", "").
            type(MediaType.TEXT_NTRIPLES).
            post(ClientResponse.class, model);
    }

    @Override
    public void add(String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getOrigin().getURI(), uri);
	getEndpoint().queryParam("graph", uri).
            type(MediaType.TEXT_NTRIPLES).
            post(ClientResponse.class, model);
    }

    public WebResource getEndpoint()
    {
        return getEndpoint(null);
    }
    
    public WebResource getEndpoint(MultivaluedMap<String, String> params)
    {      
        Client client = Client.create(new DefaultClientConfig());
        if (getOrigin().getUsername() != null && getOrigin().getPassword() != null)
            client.addFilter(new HTTPBasicAuthFilter(getOrigin().getUsername(), getOrigin().getPassword()));
        if (log.isDebugEnabled()) client.addFilter(new LoggingFilter(System.out));
        
        return client.resource(URI.create(getOrigin().getURI()));
    }
    
}
