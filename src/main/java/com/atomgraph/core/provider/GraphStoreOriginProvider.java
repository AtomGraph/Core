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

import org.apache.jena.rdf.model.Property;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.naming.ConfigurationException;
import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import com.atomgraph.core.model.GraphStoreOrigin;
import com.atomgraph.core.model.impl.GraphStoreOriginBase;
import com.atomgraph.core.vocabulary.AC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for graph store origin.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.atomgraph.core.model.GraphStoreOrigin
 */
@Provider
public class GraphStoreOriginProvider extends PerRequestTypeInjectableProvider<Context, GraphStoreOrigin> implements ContextResolver<GraphStoreOrigin>
{
    
    private static final Logger log = LoggerFactory.getLogger(GraphStoreOriginProvider.class);

    @Context ServletConfig servletConfig;
    @Context Providers providers;

    public GraphStoreOriginProvider()
    {
	super(GraphStoreOrigin.class);
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }

    public Providers getProviders()
    {
        return providers;
    }
    
    @Override
    public Injectable<GraphStoreOrigin> getInjectable(ComponentContext cc, Context a)
    {
	return new Injectable<GraphStoreOrigin>()
	{
	    @Override
	    public GraphStoreOrigin getValue()
	    {
		return getGraphStoreOrigin();
	    }
	};
    }

    @Override
    public GraphStoreOrigin getContext(Class<?> type)
    {
        return getGraphStoreOrigin();
    }

    /**
     * Returns configured Graph Store origin.
     * Uses <code>gs:graphStore</code> context parameter value from web.xml as graph store URI.
     * 
     * @return graph store origin
     */
    public GraphStoreOrigin getGraphStoreOrigin()
    {
        GraphStoreOrigin origin = getGraphStoreOrigin(AC.graphStore);
        
        if (origin == null)
        {
            if (log.isErrorEnabled()) log.error("SPARQL Graph Store not configured (gs:graphStore not set)");
            throw new WebApplicationException(new ConfigurationException("SPARQL Graph Store not configured (gs:graphStore not set)"), Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        return origin;
    }
    
     /**
     * Returns Graph Store origin for supplied webapp context configuration.
     * 
     * @param property configuration property string
     * @return graph store origin
     */
    public GraphStoreOrigin getGraphStoreOrigin(Property property)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object storeURI = getServletConfig().getInitParameter(property.getURI());        
        if (storeURI != null)
        {
            /*
            String authUser = (String)getServletConfig().getInitParameter(Service.queryAuthUser.getSymbol());
            String authPwd = (String)getServletConfig().getInitParameter(Service.queryAuthPwd.getSymbol());
            if (authUser != null && authPwd != null)
                return new GraphStoreOriginBase(storeURI.toString(), authUser, authPwd);
            */
            
            return new GraphStoreOriginBase(getClient().resource(storeURI.toString()));
        }

        return null;
    }

    public Client getClient()
    {
	ContextResolver<Client> cr = getProviders().getContextResolver(Client.class, null);
	return cr.getContext(Client.class);
    }
    
}