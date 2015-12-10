/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.graphity.core.provider;

import com.hp.hpl.jena.sparql.engine.http.Service;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ClientProvider extends PerRequestTypeInjectableProvider<Context, Client> implements ContextResolver<Client>
{
    private static final Logger log = LoggerFactory.getLogger(DataManagerProvider.class);

    @Context ServletConfig servletConfig;
    
    public ClientProvider()
    {
        super(Client.class);
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
    @Override
    public Injectable<Client> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<Client>()
	{
	    @Override
	    public Client getValue()
	    {
		return getClient();
	    }
	};
    }

    @Override
    public Client getContext(Class<?> type)
    {
        return getClient();
    }
 
    public Client getClient()
    {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
        clientConfig.getSingletons().add(new ModelProvider());
        clientConfig.getSingletons().add(new DatasetProvider());
        clientConfig.getSingletons().add(new ResultSetProvider());
        clientConfig.getSingletons().add(new QueryWriter());
        clientConfig.getSingletons().add(new UpdateRequestReader()); // TO-DO: UpdateRequestProvider

        Client client = Client.create(clientConfig);
        String authUser = (String)getServletConfig().getInitParameter(Service.queryAuthUser.getSymbol());
        String authPwd = (String)getServletConfig().getInitParameter(Service.queryAuthPwd.getSymbol());
        if (authUser != null && authPwd != null)
            client.addFilter(new HTTPBasicAuthFilter(authUser, authPwd));
        if (log.isDebugEnabled()) client.addFilter(new LoggingFilter(System.out));
        
        return Client.create(clientConfig);
    }
    
}