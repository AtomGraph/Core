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

import com.atomgraph.core.model.Service;
import javax.ws.rs.ext.Provider;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for application's SPARQL service.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see com.atomgraph.core.model.Service
 */
@Provider
public class ServiceProvider implements Factory<Service>
{
    
    private static final Logger log = LoggerFactory.getLogger(ServiceProvider.class);
    
    private final Service service;
    
    public ServiceProvider(final Service service)
    {
        this.service = service;
    }
    
    @Override
    public Service provide()
    {
        return getService();
    }

    @Override
    public void dispose(Service t)
    {
    }

    public Service getService()
    {
        return service;
    }
    
}
