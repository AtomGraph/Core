/*
 * Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>.
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

import com.atomgraph.core.model.EndpointAccessor;
import com.atomgraph.core.model.Service;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class EndpointAccessorProvider  extends PerRequestTypeInjectableProvider<Context, EndpointAccessor> implements ContextResolver<EndpointAccessor>
{

    private final Service service;

    public EndpointAccessorProvider(Service service)
    {
        super(EndpointAccessorProvider.class);
        
        this.service = service;
    }
    
    @Override
    public Injectable<EndpointAccessor> getInjectable(ComponentContext ic, Context a)
    {
        return new Injectable<EndpointAccessor>()
        {
            @Override
            public EndpointAccessor getValue()
            {
                return getEndpointAccessor();
            }
        };
        
    }

    @Override
    public EndpointAccessor getContext(Class<?> type)
    {
        return getEndpointAccessor();
    }
    
    public EndpointAccessor getEndpointAccessor()
    {
        return getService().getEndpointAccessor();
    }
    
    public Service getService()
    {
        return service;
    }
    
}
