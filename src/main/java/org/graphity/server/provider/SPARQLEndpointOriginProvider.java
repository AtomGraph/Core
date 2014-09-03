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
import org.graphity.server.model.SPARQLEndpointOrigin;
import org.graphity.server.model.impl.SPARQLEndpointOriginBase;
import org.graphity.server.util.DataManager;
import org.graphity.server.vocabulary.SD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas
 */
@Provider
public class SPARQLEndpointOriginProvider extends PerRequestTypeInjectableProvider<Context, SPARQLEndpointOrigin> implements ContextResolver<SPARQLEndpointOrigin>
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointOriginProvider.class);

    @Context ServletContext servletContext;
    @Context Providers providers;

    public SPARQLEndpointOriginProvider()
    {
	super(SPARQLEndpointOrigin.class);
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
    public Injectable<SPARQLEndpointOrigin> getInjectable(ComponentContext cc, Context context)
    {
	return new Injectable<SPARQLEndpointOrigin>()
	{
	    @Override
	    public SPARQLEndpointOrigin getValue()
	    {
		return getSPARQLEndpointOrigin();
	    }
	};
    }

    @Override
    public SPARQLEndpointOrigin getContext(Class<?> type)
    {
        return getSPARQLEndpointOrigin();
    }

    public SPARQLEndpointOrigin getSPARQLEndpointOrigin()
    {
        return getSPARQLEndpointOrigin(getServletContext(), SD.endpoint.getURI());
    }
    
    /**
     * Returns SPARQL endpoint resource for supplied webapp context configuration.
     * Uses <code>gs:endpoint</code> context parameter value as endpoint URI.
     * 
     * @param servletContext context config
     * @return endpoint resource
     */
    public SPARQLEndpointOrigin getSPARQLEndpointOrigin(ServletContext servletContext, String property)
    {
        if (servletContext == null) throw new IllegalArgumentException("ServletContext cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        try
        {
            Object endpointUri = servletContext.getInitParameter(property);
            if (endpointUri == null) throw new ConfigurationException("SPARQL endpoint not configured ('" + property + "' not set in web.xml)");

            String authUser = (String)servletContext.getInitParameter(Service.queryAuthUser.getSymbol());
            String authPwd = (String)servletContext.getInitParameter(Service.queryAuthPwd.getSymbol());
            if (authUser != null && authPwd != null)
                getDataManager().putAuthContext(endpointUri.toString(), authUser, authPwd);

            return new SPARQLEndpointOriginBase(endpointUri.toString());
        }
        catch (ConfigurationException ex)
        {
            if (log.isErrorEnabled()) log.warn("SPARQL endpoint configuration error", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);            
        }
    }

}
