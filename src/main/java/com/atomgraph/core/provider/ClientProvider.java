/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.core.provider;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import com.sun.jersey.spi.resource.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for HTTP client.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see import com.sun.jersey.api.client.Client
 * @see javax.ws.rs.core.Context
 */
@Provider
@Singleton
public class ClientProvider extends PerRequestTypeInjectableProvider<Context, Client> implements ContextResolver<Client>
{
    private static final Logger log = LoggerFactory.getLogger(ClientProvider.class);
    
    private final Client client;
    
    public ClientProvider(final Client client)
    {
        super(Client.class);
        this.client = client;
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
        return client;
    }
    
}