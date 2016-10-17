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

import org.apache.jena.util.LocationMapper;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.util.jena.DataManager;
import com.atomgraph.core.vocabulary.A;
import com.sun.jersey.api.client.Client;
import javax.ws.rs.ext.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for data manager class.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.atomgraph.core.util.DataManager
 */
@Provider
public class DataManagerProvider extends PerRequestTypeInjectableProvider<Context, DataManager> implements ContextResolver<DataManager>
{

    private static final Logger log = LoggerFactory.getLogger(DataManagerProvider.class);

    @Context Providers providers;
    
    public final boolean preemptiveAuth;
    
    public DataManagerProvider(ServletConfig servletConfig)
    {
        super(DataManager.class);
        
        if (servletConfig.getInitParameter(A.preemptiveAuth.getURI()) != null)
            preemptiveAuth = Boolean.parseBoolean(servletConfig.getInitParameter(A.preemptiveAuth.getURI()));
        else preemptiveAuth = false;
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

    @Override
    public DataManager getContext(Class<?> type)
    {
        return getDataManager();
    }

    /**
     * Returns default data manager instance.
     * @return data manager instance
     */
    public DataManager getDataManager()
    {
        return new DataManager(LocationMapper.get(), getClient(), getMediaTypes(), getPreemptiveAuth());
    }
    
    public Client getClient()
    {
	return getProviders().getContextResolver(Client.class, null).getContext(Client.class);
    }
    
    public MediaTypes getMediaTypes()
    {
	return getProviders().getContextResolver(MediaTypes.class, null).getContext(MediaTypes.class);
    }
 
    public Providers getProviders()
    {
        return providers;
    }

    public boolean getPreemptiveAuth()
    {
        return preemptiveAuth;
    }
    
}