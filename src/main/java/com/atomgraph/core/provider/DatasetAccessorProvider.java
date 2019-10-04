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

import com.atomgraph.core.model.Service;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import org.apache.jena.query.DatasetAccessor;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class DatasetAccessorProvider extends PerRequestTypeInjectableProvider<Context, DatasetAccessor> implements ContextResolver<DatasetAccessor>
{

    private final Service service;

    public DatasetAccessorProvider(Service service)
    {
        super(DatasetAccessorProvider.class);
        
        this.service = service;
    }
    
    @Override
    public Injectable<DatasetAccessor> getInjectable(ComponentContext ic, Context a)
    {
        return new Injectable<DatasetAccessor>()
        {
            @Override
            public DatasetAccessor getValue()
            {
                return getDatasetAccessor();
            }
        };
    }

    @Override
    public DatasetAccessor getContext(Class<?> type)
    {
        return getDatasetAccessor();
    }
    
    public DatasetAccessor getDatasetAccessor()
    {
        return getService().getDatasetAccessor();
    }
    
    public Service getService()
    {
        return service;
    }
    
}
