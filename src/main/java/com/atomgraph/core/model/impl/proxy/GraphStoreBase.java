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

package com.atomgraph.core.model.impl.proxy;

import org.apache.jena.rdf.model.Model;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of Graph Store.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
//@Path("/service") // not standard
public class GraphStoreBase extends com.atomgraph.core.model.impl.GraphStoreBase 
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreBase.class);

    private final GraphStoreClient graphStoreClient;
    
    /**
     * Constructs Graph Store proxy from request metadata and origin.
     * 
     * @param request request
     * @param mediaTypes supported media types
     * @param graphStoreClient graph store client
     */
    public GraphStoreBase(@Context Request request, @Context MediaTypes mediaTypes,
            @Context GraphStoreClient graphStoreClient)
    {
        super(request, mediaTypes);
        this.graphStoreClient = graphStoreClient;
    }

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
        getGraphStoreClient().deleteModel(uri);
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
