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
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import org.graphity.core.MediaType;
import org.graphity.core.MediaTypes;
import org.graphity.core.client.GraphStoreClient;
import org.graphity.core.model.GraphStoreOrigin;
import org.graphity.core.model.GraphStoreProxy;
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

    private final GraphStoreOrigin origin;
    private final GraphStoreClient client;
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
        client = GraphStoreClient.create(origin.getWebResource());
    }

     /**
     * Returns configured Graph Store resource.
     * This graph store is a proxy for the remote one.
     * 
     * @return graph store resource
     */
    @Override
    public GraphStoreOrigin getOrigin()
    {
        return origin;
    }

    public GraphStoreClient getClient()
    {
        return client;
    }
    
    public javax.ws.rs.core.MediaType[] getModelMediaTypes()
    {
        return modelMediaTypes;
    }
    
    @Override
    public Model getModel()
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getClient().getWebResource().getURI());
	return getClient().getModel(getModelMediaTypes()).getEntity(Model.class);
    }

    @Override
    public Model getModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getClient().getWebResource().getURI(), uri);
	return getClient().getModel(getModelMediaTypes()).getEntity(Model.class);
    }

    @Override
    public boolean containsModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("Checking if Graph Store {} contains GRAPH with URI {}", getClient().getWebResource().getURI(), uri);
	return getClient().headNamed(uri).
            getStatusInfo().
            getFamily().equals(javax.ws.rs.core.Response.Status.Family.SUCCESSFUL);
    }
    
    @Override
    public void putModel(Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getClient().getWebResource().getURI());
	getClient().putModel(MediaType.TEXT_NTRIPLES_TYPE, model);
    }

    @Override
    public void putModel(String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getClient().getWebResource().getURI(), uri);
	getClient().putModel(MediaType.TEXT_NTRIPLES_TYPE, uri, model);
    }

    @Override
    public void deleteDefault()
    {
	if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getClient().getWebResource().getURI());
	getClient().deleteDefault();
    }

    @Override
    public void deleteModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", uri, getClient().getWebResource().getURI());
	getClient().deleteModel(uri);
    }

    @Override
    public void add(Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getClient().getWebResource().getURI());
	getClient().add(MediaType.TEXT_NTRIPLES_TYPE, model);
    }

    @Override
    public void add(String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getClient().getWebResource().getURI(), uri);
	getClient().add(MediaType.TEXT_NTRIPLES_TYPE, uri, model);
    }

}
