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
package org.graphity.server.provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.server.model.SPARQLEndpoint;
import org.graphity.server.model.SPARQLEndpointFactory;
import org.graphity.server.model.SPARQLEndpointOrigin;
import org.graphity.server.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for SPARQL endpoint.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.server.model.SPARQLEndpoint
 */
@Provider
public class SPARQLEndpointProvider extends PerRequestTypeInjectableProvider<Context, SPARQLEndpoint> implements ContextResolver<SPARQLEndpoint>
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProvider.class);

    @Context Request request;
    @Context ServletConfig filtertConfig;
    @Context Providers providers;
    
    public SPARQLEndpointProvider()
    {
	super(SPARQLEndpoint.class);
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
        return filtertConfig;
    }

    public DataManager getDataManager()
    {
	ContextResolver<DataManager> cr = getProviders().getContextResolver(DataManager.class, null);
	return cr.getContext(DataManager.class);
    }

    public SPARQLEndpointOrigin getSPARQLEndpointOrigin()
    {
	ContextResolver<SPARQLEndpointOrigin> cr = getProviders().getContextResolver(SPARQLEndpointOrigin.class, null);
	return cr.getContext(SPARQLEndpointOrigin.class);
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

    public SPARQLEndpoint getSPARQLEndpoint()
    {
        return getSPARQLEndpoint(getRequest(), getServletConfig(), getSPARQLEndpointOrigin(), getDataManager());
    }

    public SPARQLEndpoint getSPARQLEndpoint(Request request, ServletConfig servletConfig, SPARQLEndpointOrigin origin, DataManager dataManager)
    {
        return SPARQLEndpointFactory.createProxy(request, servletConfig, origin, dataManager);
    }
    
    @Override
    public SPARQLEndpoint getContext(Class<?> type)
    {
        return getSPARQLEndpoint();
    }

}