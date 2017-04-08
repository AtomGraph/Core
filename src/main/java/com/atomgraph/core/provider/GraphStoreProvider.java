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

package com.atomgraph.core.provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.model.GraphStore;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for Graph Store.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.atomgraph.core.model.GraphStore
 */
@Provider
public class GraphStoreProvider extends PerRequestTypeInjectableProvider<Context, GraphStore> implements ContextResolver<GraphStore>
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreProvider.class);
    
    @Context Request request;
    
    private final Dataset dataset;
    private final MediaTypes mediaTypes;
    private final GraphStoreClient graphStoreClient;
        
    public GraphStoreProvider(final MediaTypes mediaTypes, final Dataset dataset, final GraphStoreClient graphStoreClient)
    {
	super(GraphStore.class);
        this.dataset = dataset;
        this.mediaTypes = mediaTypes;
        this.graphStoreClient = graphStoreClient;
    }

    public Request getRequest()
    {
        return request;
    }
    
    public Dataset getDataset()
    {
	return dataset;
    }
    
    public MediaTypes getMediaTypes()
    {
	return mediaTypes;
    }

    public GraphStoreClient getGraphStoreClient()
    {
	return graphStoreClient;
    }

    @Override
    public Injectable<GraphStore> getInjectable(ComponentContext cc, Context context)
    {
	return new Injectable<GraphStore>()
	{
	    @Override
	    public GraphStore getValue()
	    {
		return getGraphStore();
	    }
	};
    }
    
    @Override
    public GraphStore getContext(Class<?> type)
    {
        return getGraphStore();
    }

    public GraphStore getGraphStore()
    {
        if (getDataset() != null) return new com.atomgraph.core.model.impl.dataset.GraphStoreBase(getRequest(), getMediaTypes(), getDataset());
        
        return new com.atomgraph.core.model.impl.proxy.GraphStoreBase(getRequest(), getMediaTypes(), getGraphStoreClient());
    }

}