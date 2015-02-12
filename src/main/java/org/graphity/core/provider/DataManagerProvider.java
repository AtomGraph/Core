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

package org.graphity.core.provider;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.util.LocationMapper;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.graphity.core.util.DataManager;
import org.graphity.core.vocabulary.G;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for data manager class.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.core.util.DataManager
 */
@Provider
public class DataManagerProvider extends PerRequestTypeInjectableProvider<Context, DataManager> implements ContextResolver<DataManager>
{

    private static final Logger log = LoggerFactory.getLogger(DataManagerProvider.class);

    @Context UriInfo uriInfo;
    @Context ServletConfig servletConfig;

    public ServletConfig getServletConfig()
    {
	return servletConfig;
    }

    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    public DataManagerProvider()
    {
        super(DataManager.class);
    }

    @Override
    public Injectable<DataManager> getInjectable(ComponentContext cc, Context a)
    {
	return new Injectable<DataManager>()
	{
	    @Override
	    public DataManager getValue()
	    {
		return getDataManager();
	    }
	};
    }

    /**
     * Returns default data manager instance.
     * @return data manager instance
     */
    public DataManager getDataManager()
    {
        return getDataManager(LocationMapper.get(), ARQ.getContext(), getServletConfig());
    }
    
    public boolean getPreemptiveAuth(ServletConfig servletConfig, Property property)
    {
	if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        boolean preemptiveAuth = false;
        if (servletConfig.getInitParameter(property.getURI()) != null)
            preemptiveAuth = Boolean.parseBoolean(servletConfig.getInitParameter(property.getURI()).toString());
        return preemptiveAuth;
    }
    
    public DataManager getDataManager(LocationMapper mapper, com.hp.hpl.jena.sparql.util.Context context, ServletConfig servletConfig)
    {
	if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        
        return getDataManager(mapper, context, getPreemptiveAuth(getServletConfig(), G.preemptiveAuth));
    }

    public DataManager getDataManager(LocationMapper mapper, com.hp.hpl.jena.sparql.util.Context context, boolean preemptiveAuth)
    {
        return new DataManager(mapper, context, preemptiveAuth);
    }

    @Override
    public DataManager getContext(Class<?> type)
    {
        return getDataManager();
    }
    
}