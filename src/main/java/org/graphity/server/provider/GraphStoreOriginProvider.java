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

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.naming.ConfigurationException;
import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.server.model.GraphStoreOrigin;
import org.graphity.server.model.impl.GraphStoreOriginBase;
import org.graphity.server.util.DataManager;
import org.graphity.server.vocabulary.GS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for graph store origin.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.server.model.GraphStoreOrigin
 */
@Provider
public class GraphStoreOriginProvider extends PerRequestTypeInjectableProvider<Context, GraphStoreOrigin> implements ContextResolver<GraphStoreOrigin>
{
    
    private static final Logger log = LoggerFactory.getLogger(GraphStoreOriginProvider.class);

    @Context ServletContext servletContext;
    @Context Providers providers;

    public GraphStoreOriginProvider()
    {
	super(GraphStoreOrigin.class);
    }

    public ServletContext getServletContext()
    {
        return servletContext;
    }

    public Providers getProviders()
    {
        return providers;
    }

    public DataManager getDataManager()
    {
	ContextResolver<DataManager> cr = getProviders().getContextResolver(DataManager.class, null);
	return cr.getContext(DataManager.class);
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
        GraphStoreOrigin origin = getGraphStoreOrigin(GS.graphStore, getDataManager());
        
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
     * @param dataManager data manager
     * @return graph store origin
     */
    public GraphStoreOrigin getGraphStoreOrigin(Property property, DataManager dataManager)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object storeURI = getServletContext().getInitParameter(property.getURI());
        if (storeURI != null)
        {
            return new GraphStoreOriginBase(storeURI.toString(),
                (String)getServletContext().getInitParameter(Service.queryAuthUser.getSymbol()),
                (String)getServletContext().getInitParameter(Service.queryAuthPwd.getSymbol()),
                dataManager);
        }

        return null;
    }

}