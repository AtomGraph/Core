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

import com.atomgraph.core.Application;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import com.atomgraph.core.model.SPARQLEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Application application;
    
    public SPARQLEndpointProvider(final Application application)
    {
	super(SPARQLEndpoint.class);
        this.application = application;
    }

    public Request getRequest()
    {
        return request;
    }

    public Application getApplication()
    {
	return application;
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
        if (getApplication().getDataset() != null) return new com.atomgraph.core.model.impl.dataset.SPARQLEndpointBase(getApplication(), getRequest());
        
        return new com.atomgraph.core.model.impl.proxy.SPARQLEndpointBase(getApplication(), getRequest());
    }
    
}