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
import com.atomgraph.core.model.SPARQLEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.client.SPARQLClient;
import org.apache.jena.query.Dataset;

/**
 * JAX-RS provider for SPARQL endpoint.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.atomgraph.core.model.SPARQLEndpoint
 */
@Provider
public class SPARQLEndpointProvider extends PerRequestTypeInjectableProvider<Context, SPARQLEndpoint> implements ContextResolver<SPARQLEndpoint>
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProvider.class);

    @Context Request request;
    @Context Providers providers;
    
    private final ServletConfig servletConfig;
    
    public SPARQLEndpointProvider(ServletConfig servletConfig)
    {
	super(SPARQLEndpoint.class);        
        this.servletConfig = servletConfig;
    }
    
    public Providers getProviders()
    {
        return providers;
    }

    public Request getRequest()
    {
        return request;
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }

    public Dataset getDataset()
    {
	return getProviders().getContextResolver(Dataset.class, null).getContext(Dataset.class);
    }
    
    public MediaTypes getMediaTypes()
    {
	return getProviders().getContextResolver(MediaTypes.class, null).getContext(MediaTypes.class);
    }
    
    public SPARQLClient getSPARQLClient()
    {
	return getProviders().getContextResolver(SPARQLClient.class, null).getContext(SPARQLClient.class);
    }
        
    @Override
    public Injectable<SPARQLEndpoint> getInjectable(ComponentContext cc, Context context)
    {
	return new Injectable<SPARQLEndpoint>()
	{
	    @Override
	    public SPARQLEndpoint getValue()
	    {
		return getSPARQLEndpoint();
	    }
	};
    }
    
    @Override
    public SPARQLEndpoint getContext(Class<?> type)
    {
        return getSPARQLEndpoint();
    }

    public SPARQLEndpoint getSPARQLEndpoint()
    {
        if (getDataset() != null) return new com.atomgraph.core.model.impl.dataset.SPARQLEndpointBase(getRequest(), getMediaTypes(), getDataset());
        
        return new com.atomgraph.core.model.impl.proxy.SPARQLEndpointBase(getRequest(), getMediaTypes(), getSPARQLClient());
    }
    
}