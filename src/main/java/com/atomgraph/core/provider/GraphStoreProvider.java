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
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.model.GraphStore;
import com.atomgraph.core.model.impl.GraphStoreProxyBase;
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
    @Context Providers providers;
    
    private final ServletConfig servletConfig;
    
    public GraphStoreProvider(ServletConfig servletConfig)
    {
	super(GraphStore.class);
        this.servletConfig = servletConfig;
    }

    public Request getRequest()
    {
        return request;
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
    public MediaTypes getMediaTypes()
    {
	return getProviders().getContextResolver(MediaTypes.class, null).getContext(MediaTypes.class);
    }

    public GraphStoreClient getGraphStoreClient()
    {
	return getProviders().getContextResolver(GraphStoreClient.class, null).getContext(GraphStoreClient.class);
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
        return new GraphStoreProxyBase(getRequest(), getServletConfig(), getMediaTypes(), getGraphStoreClient());
    }

}