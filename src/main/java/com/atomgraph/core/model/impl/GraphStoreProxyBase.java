/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
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

package com.atomgraph.core.model.impl;

import org.apache.jena.rdf.model.Model;
import com.sun.jersey.api.client.ClientResponse;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.Status.Family;
import com.atomgraph.core.MediaType;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.client.simple.SimpleGraphStoreClient;
import com.atomgraph.core.exception.ClientException;
import com.atomgraph.core.model.GraphStoreOrigin;
import com.atomgraph.core.model.GraphStoreProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of Graph Store.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Path("/service") // not standard
public class GraphStoreProxyBase extends GraphStoreBase implements GraphStoreProxy
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreProxyBase.class);

    private final GraphStoreOrigin origin;
    private final GraphStoreClient client;
    //private final javax.ws.rs.core.MediaType[] readableMediaTypes;
    
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
        //List<javax.ws.rs.core.MediaType> modelTypeList = mediaTypes.getReadable(Model.class);
        //readableMediaTypes = modelTypeList.toArray(new javax.ws.rs.core.MediaType[modelTypeList.size()]);        
        client = GraphStoreClient.create(origin.getWebResource(), mediaTypes);
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
    public Model getModel()
    {
        return getClient().getModel();
    }

    @Override
    public Model getModel(String uri)
    {
        return getClient().getModel(uri);
    }

    @Override
    public boolean containsModel(String uri)
    {
        return getClient().containsModel(uri);
    }
    
    @Override
    public void putModel(Model model)
    {
        getClient().putModel(model);
    }

    @Override
    public void putModel(String uri, Model model)
    {
        getClient().putModel(uri, model);
    }

    @Override
    public void deleteDefault()
    {
        getClient().deleteDefault();
    }

    @Override
    public void deleteModel(String uri)
    {
        
    }

    @Override
    public void add(Model model)
    {
        getClient().add(model);
    }

    @Override
    public void add(String uri, Model model)
    {
        getClient().add(uri, model);
    }

}
