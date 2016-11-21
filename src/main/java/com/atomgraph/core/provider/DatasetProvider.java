/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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

import com.atomgraph.core.model.DatasetService;
import com.atomgraph.core.model.Service;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class DatasetProvider extends PerRequestTypeInjectableProvider<Context, Dataset> implements ContextResolver<Dataset>
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLClientProvider.class);
    
    @Context Providers providers;

    public DatasetProvider()
    {
        super(Dataset.class);
    }
    
    @Override
    public Injectable<Dataset> getInjectable(ComponentContext cc, Context context)
    {
	return new Injectable<Dataset>()
	{
	    @Override
	    public Dataset getValue()
	    {
		return getDataset();
	    }
	};
    }
    
    @Override
    public Dataset getContext(Class<?> type)
    {
        return getDataset();
    }
    
    public Dataset getDataset()
    {
        if (!(getService() instanceof DatasetService)) return null;
        
        return getDataset((DatasetService)getService());
    }
    
    public Dataset getDataset(DatasetService service)
    {
        if (service == null) throw new IllegalArgumentException("DatasetService must be not null");
        
        return service.getDataset();
    }
    
    public Service getService()
    {
	return getProviders().getContextResolver(Service.class, null).getContext(Service.class);
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
}
