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

import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.core.model.Application;
import com.atomgraph.core.model.impl.ApplicationImpl;
import com.atomgraph.core.model.impl.ServiceImpl;
import com.atomgraph.core.vocabulary.A;
import com.atomgraph.core.vocabulary.SD;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import com.sun.jersey.spi.resource.Singleton;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.http.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
@Singleton
public class ApplicationProvider extends PerRequestTypeInjectableProvider<Context, Application> implements ContextResolver<Application>
{

    private static final Logger log = LoggerFactory.getLogger(ApplicationProvider.class);

    private final Application application;
    
    public ApplicationProvider(ServletConfig servletConfig)
    {
	super(Application.class);        
                
        Object endpointURI = servletConfig.getInitParameter(SD.endpoint.getURI());
        if (endpointURI == null)
        {
            if (log.isErrorEnabled()) log.error("SPARQL endpoint not configured ('{}' not set in web.xml)", SD.endpoint.getURI());
            throw new ConfigurationException(SD.endpoint);
        }
        Object graphStoreURI = servletConfig.getInitParameter(A.graphStore.getURI());
        if (graphStoreURI == null)
        {
            if (log.isErrorEnabled()) log.error("SPARQL Graph Store not configured ('{}' not set)", A.graphStore);
            throw new ConfigurationException(A.graphStore);
        }        
        Object authUser = servletConfig.getInitParameter(Service.queryAuthUser.getSymbol());
        Object authPwd = servletConfig.getInitParameter(Service.queryAuthPwd.getSymbol());
        
        Model model = ModelFactory.createDefaultModel();        
        application = new ApplicationImpl(new ServiceImpl(model.createResource(endpointURI.toString()),
                model.createResource(graphStoreURI.toString()),
                authUser == null ? null : authUser.toString(), authPwd == null ? null : authPwd.toString()));
    }

    @Override
    public Injectable<Application> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<Application>()
	{
	    @Override
	    public Application getValue()
	    {
		return getApplication();
	    }
	};
    }

    @Override
    public Application getContext(Class<?> type)
    {
        return getApplication();
    }
    
    public Application getApplication()
    {
        return application;
    }
    
}
