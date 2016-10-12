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
import com.atomgraph.core.model.Application;
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

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class GraphStoreClientProvider extends PerRequestTypeInjectableProvider<Context, GraphStoreClient> implements ContextResolver<GraphStoreClient>
{
    
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
        return getGraphStoreClient(getApplication().getService(), getClient());
    }
    
    public GraphStoreClient getGraphStoreClient(Service service, Client client)
    {
	if (service == null) throw new IllegalArgumentException("Service must be not null");
        if (client == null) throw new IllegalArgumentException("Client must be not null");

        return GraphStoreClient.create(getOrigin(service, client), getMediaTypes());
    }

    public MediaTypes getMediaTypes()
    {
	return getProviders().getContextResolver(MediaTypes.class, null).getContext(MediaTypes.class);
    }
    
    public Client getClient()
    {
	return getProviders().getContextResolver(Client.class, null).getContext(Client.class);
    }

    public Application getApplication()
    {
	return getProviders().getContextResolver(Application.class, null).getContext(Application.class);
    }

    public WebResource getOrigin(Service service)
    {
        return getOrigin(service, getClient());
    }
        
    public WebResource getOrigin(Service service, Client client)
    {
	if (service == null) throw new IllegalArgumentException("Service must be not null");        
	if (client == null) throw new IllegalArgumentException("Client must be not null");

        WebResource origin = client.resource(service.getGraphStore().getURI());

        if (service.getAuthUser() != null && service.getAuthPwd() != null)
            origin.addFilter(new HTTPBasicAuthFilter(service.getAuthUser(), service.getAuthPwd()));
        
        return origin;
    }

    public Providers getProviders()
    {
        return providers;
    }
    
}
