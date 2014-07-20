/*
 * Copyright (C) 2014 Martynas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graphity.server.provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import org.graphity.server.model.GraphStore;
import org.graphity.server.model.GraphStoreFactory;
import org.graphity.server.model.GraphStoreOrigin;
import org.graphity.server.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas
 */
public class GraphStoreProvider extends PerRequestTypeInjectableProvider<Context, GraphStore> implements ContextResolver<GraphStore>
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreProvider.class);
    
    @Context Request request;
    @Context ServletContext servletContext;
    @Context Providers providers;
    
    public GraphStoreProvider()
    {
	super(GraphStore.class);
    }

    public Request getRequest()
    {
        return request;
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

    public GraphStoreOrigin getGraphStoreOrigin()
    {
	ContextResolver<GraphStoreOrigin> cr = getProviders().getContextResolver(GraphStoreOrigin.class, null);
	return cr.getContext(GraphStoreOrigin.class);
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

    public GraphStore getGraphStore()
    {
        return GraphStoreFactory.createProxy(getRequest(), getServletContext(), getGraphStoreOrigin(), getDataManager());
    }

    @Override
    public GraphStore getContext(Class<?> type)
    {
        return getGraphStore();
    }

}
