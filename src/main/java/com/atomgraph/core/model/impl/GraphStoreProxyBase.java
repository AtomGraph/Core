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
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.model.Application;
import com.atomgraph.core.model.GraphStoreProxy;
import com.sun.jersey.api.client.Client;
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

    //private final GraphStoreOrigin origin;
    private final GraphStoreClient graphStoreClient;
    //private final javax.ws.rs.core.MediaType[] readableMediaTypes;
    
    /**
     * Constructs Graph Store proxy from request metadata and origin.
     * 
     * @param request request
     * @param servletConfig servlet config
     * @param mediaTypes supported media types
     * @param client HTTP client
     * @param application LDT application
     */
    public GraphStoreProxyBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context Client client, @Context Application application)
    {
        super(request, servletConfig, mediaTypes);
        if (application == null) throw new IllegalArgumentException("Application cannot be null");
        graphStoreClient = GraphStoreClient.create(application.getService().getSPARQLEndpointOrigin(client), mediaTypes);
    }

    @Override
    public GraphStoreClient getGraphStoreClient()
    {
        return graphStoreClient;
    }
    
    @Override
    public Model getModel()
    {
        return getGraphStoreClient().getModel();
    }

    @Override
    public Model getModel(String uri)
    {
        return getGraphStoreClient().getModel(uri);
    }

    @Override
    public boolean containsModel(String uri)
    {
        return getGraphStoreClient().containsModel(uri);
    }
    
    @Override
    public void putModel(Model model)
    {
        getGraphStoreClient().putModel(model);
    }

    @Override
    public void putModel(String uri, Model model)
    {
        getGraphStoreClient().putModel(uri, model);
    }

    @Override
    public void deleteDefault()
    {
        getGraphStoreClient().deleteDefault();
    }

    @Override
    public void deleteModel(String uri)
    {
        
    }

    @Override
    public void add(Model model)
    {
        getGraphStoreClient().add(model);
    }

    @Override
    public void add(String uri, Model model)
    {
        getGraphStoreClient().add(uri, model);
    }

}
