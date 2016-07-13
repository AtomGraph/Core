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

import org.apache.jena.rdf.model.Model;
import com.sun.jersey.api.client.ClientResponse;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.Status.Family;
import org.graphity.core.MediaType;
import org.graphity.core.MediaTypes;
import org.graphity.core.client.GraphStoreClient;
import org.graphity.core.exception.ClientException;
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
    private final javax.ws.rs.core.MediaType[] readableMediaTypes;
    
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
        List<javax.ws.rs.core.MediaType> modelTypeList = mediaTypes.getReadable(Model.class);
        readableMediaTypes = modelTypeList.toArray(new javax.ws.rs.core.MediaType[modelTypeList.size()]);        
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

    @Override
    public GraphStoreClient getClient()
    {
        return client;
    }
    
    @Override
    public javax.ws.rs.core.MediaType[] getReadableMediaTypes()
    {
        return readableMediaTypes;
    }
    
    @Override
    public Model getModel()
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getOrigin().getWebResource().getURI());
	ClientResponse cr = getClient().getModel(getReadableMediaTypes());
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
        
        return cr.getEntity(Model.class);
    }

    @Override
    public Model getModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getOrigin().getWebResource().getURI(), uri);
	ClientResponse cr = getClient().get(getReadableMediaTypes(), uri);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
        
        return cr.getEntity(Model.class);
    }

    @Override
    public boolean containsModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("Checking if Graph Store {} contains GRAPH with URI {}", getOrigin().getWebResource().getURI(), uri);
	ClientResponse cr = getClient().headNamed(uri);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }

        return true;
    }
    
    @Override
    public void putModel(Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getOrigin().getWebResource().getURI());
	ClientResponse cr = getClient().putModel(MediaType.TEXT_NTRIPLES_TYPE, model);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
    }

    @Override
    public void putModel(String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getOrigin().getWebResource().getURI(), uri);
	ClientResponse cr = getClient().putModel(MediaType.TEXT_NTRIPLES_TYPE, uri, model);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
    }

    @Override
    public void deleteDefault()
    {
	if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getOrigin().getWebResource().getURI());
	ClientResponse cr = getClient().deleteDefault();
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
    }

    @Override
    public void deleteModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", uri, getOrigin().getWebResource().getURI());
	ClientResponse cr = getClient().deleteModel(uri);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
    }

    @Override
    public void add(Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getOrigin().getWebResource().getURI());
	ClientResponse cr = getClient().add(MediaType.TEXT_NTRIPLES_TYPE, model);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
    }

    @Override
    public void add(String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getOrigin().getWebResource().getURI(), uri);
	ClientResponse cr = getClient().add(MediaType.TEXT_NTRIPLES_TYPE, uri, model);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
    }

}
