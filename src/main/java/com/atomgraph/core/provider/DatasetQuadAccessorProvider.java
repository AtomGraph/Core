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

import com.atomgraph.core.model.DatasetQuadAccessor;
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
public class DatasetQuadAccessorProvider extends PerRequestTypeInjectableProvider<Context, DatasetQuadAccessor> implements ContextResolver<DatasetQuadAccessor>
{

    private final Service service;

    public DatasetQuadAccessorProvider(Service service)
    {
        super(DatasetQuadAccessorProvider.class);
        
        this.service = service;
    }
    
    @Override
    public Injectable<DatasetQuadAccessor> getInjectable(ComponentContext ic, Context a)
    {
        return new Injectable<DatasetQuadAccessor>()
        {
            @Override
            public DatasetQuadAccessor getValue()
            {
                return getDatasetQuadAccessor();
            }
        };
    }

    @Override
    public DatasetQuadAccessor getContext(Class<?> type)
    {
        return getDatasetQuadAccessor();
    }
    
    public DatasetQuadAccessor getDatasetQuadAccessor()
    {
        return getService().getDatasetQuadAccessor();
    }
    
    public Service getService()
    {
        return service;
    }
    
}