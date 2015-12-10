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

import com.hp.hpl.jena.rdf.model.Property;
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
import org.graphity.core.model.SPARQLEndpointOrigin;
import org.graphity.core.model.impl.SPARQLEndpointOriginBase;
import org.graphity.core.vocabulary.SD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for SPARQL endpoint origin.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.core.model.SPARQLEndpointOrigin
 */
@Provider
public class SPARQLEndpointOriginProvider extends PerRequestTypeInjectableProvider<Context, SPARQLEndpointOrigin> implements ContextResolver<SPARQLEndpointOrigin>
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointOriginProvider.class);

    @Context ServletConfig servletConfig;
    @Context Providers providers;

    public SPARQLEndpointOriginProvider()
    {
	super(SPARQLEndpointOrigin.class);
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }

    public Providers getProviders()
    {
        return providers;
    }

    public Client getClient()
    {
	ContextResolver<Client> cr = getProviders().getContextResolver(Client.class, null);
	return cr.getContext(Client.class);
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

    /**
     * Returns configured SPARQL endpoint origin.
     * Uses <code>sd:endpoint</code> context parameter value as endpoint URI.
     * 
     * @return configured origin
     */
    public SPARQLEndpointOrigin getSPARQLEndpointOrigin()
    {
        SPARQLEndpointOrigin origin = getSPARQLEndpointOrigin(SD.endpoint);
        
        if (origin == null)
        {
            if (log.isErrorEnabled()) log.error("SPARQL endpoint not configured (sd:endpoint not set in web.xml)");
            throw new WebApplicationException(new ConfigurationException("SPARQL endpoint not configured (sd:endpoint not set in web.xml)"), Response.Status.INTERNAL_SERVER_ERROR);
        }

        return origin;
    }
    
    /**
     * Returns SPARQL endpoint origin for supplied webapp context configuration.
     * 
     * @param property configuration property
     * @return endpoint origin
     */
    public SPARQLEndpointOrigin getSPARQLEndpointOrigin(Property property)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object endpointURI = getServletConfig().getInitParameter(property.getURI());
        if (endpointURI != null) return new SPARQLEndpointOriginBase(getClient().resource(endpointURI.toString()));
        
        return null;
    }
    
}
