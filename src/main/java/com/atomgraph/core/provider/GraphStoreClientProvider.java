/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.core.provider;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.model.RemoteService;
import com.atomgraph.core.model.Service;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class GraphStoreClientProvider extends PerRequestTypeInjectableProvider<Context, GraphStoreClient> implements ContextResolver<GraphStoreClient>
{
    
    private static final Logger log = LoggerFactory.getLogger(GraphStoreClientProvider.class);

    @Context Providers providers;

    public GraphStoreClientProvider()
    {
        super(GraphStoreClient.class);
    }

    @Override
    public Injectable<GraphStoreClient> getInjectable(ComponentContext cc, Context context)
    {
	return new Injectable<GraphStoreClient>()
	{
	    @Override
	    public GraphStoreClient getValue()
	    {
		return getGraphStoreClient();
	    }
	};
    }
        
    @Override
    public GraphStoreClient getContext(Class<?> type)
    {
        return getGraphStoreClient();
    }

    public GraphStoreClient getGraphStoreClient()
    {
        if (!(getService() instanceof RemoteService)) return null;
        
        return getGraphStoreClient(getOrigin(getClient(), (RemoteService)getService()), getMediaTypes());
    }
    
    public GraphStoreClient getGraphStoreClient(WebResource origin, MediaTypes mediaTypes)
    {
        if (origin == null) throw new IllegalArgumentException("WebResource must be not null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes must be not null");

        return GraphStoreClient.create(origin, mediaTypes);
    }

    public WebResource getOrigin(Client client, RemoteService service)
    {
        if (client == null) throw new IllegalArgumentException("Client must be not null");
	if (service == null) throw new IllegalArgumentException("RemoteService must be not null");

        WebResource origin = client.resource(service.getGraphStore().getURI());

        if (service.getAuthUser() != null && service.getAuthPwd() != null)
            origin.addFilter(new HTTPBasicAuthFilter(service.getAuthUser(), service.getAuthPwd())); 
        
        return origin;
    }
    
    public Service getService()
    {
	return getProviders().getContextResolver(Service.class, null).getContext(Service.class);
    }
    
    public MediaTypes getMediaTypes()
    {
	return getProviders().getContextResolver(MediaTypes.class, null).getContext(MediaTypes.class);
    }
    
    public Client getClient()
    {
	return getProviders().getContextResolver(Client.class, null).getContext(Client.class);
    }

    public Providers getProviders()
    {
        return providers;
    }
    
}
