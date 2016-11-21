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

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.core.model.Service;
import com.atomgraph.core.vocabulary.A;
import com.atomgraph.core.vocabulary.SD;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class ServiceProvider extends PerRequestTypeInjectableProvider<Context, Service> implements ContextResolver<Service>
{
    
    private static final Logger log = LoggerFactory.getLogger(ServiceProvider.class);
    
    @Context Providers providers;

    private final String datasetLocation;
    private final Dataset dataset;
    private final Resource endpoint, graphStore;
    private final String authUser, authPwd;

    public ServiceProvider(ServletConfig servletConfig)
    {
	super(Service.class);
        
        Object datasetLocationParam = servletConfig.getInitParameter(A.dataset.getURI());
        if (datasetLocationParam != null)
        {
            datasetLocation = datasetLocationParam.toString();
            dataset = DatasetFactory.createTxnMem();
            endpoint = graphStore = null;
            authUser = authPwd = null;
            
            //RDFDataMgr.read(dataset, datasetLocation.toString(), "http://localhost/", null);            
        }
        else
        {
            datasetLocation = null;
            dataset = null;
            
            Object endpointURIParam = servletConfig.getInitParameter(SD.endpoint.getURI());
            if (endpointURIParam == null)
            {
                if (log.isErrorEnabled()) log.error("SPARQL endpoint not configured ('{}' not set in web.xml)", SD.endpoint.getURI());
                throw new ConfigurationException(SD.endpoint);
            }
            endpoint = ResourceFactory.createResource(endpointURIParam.toString());

            Object graphStoreURIParam = servletConfig.getInitParameter(A.graphStore.getURI());
            if (graphStoreURIParam == null)
            {
                if (log.isErrorEnabled()) log.error("Graph Store not configured ('{}' not set in web.xml)", A.graphStore.getURI());
                throw new ConfigurationException(A.graphStore);
            }
            graphStore = ResourceFactory.createResource(graphStoreURIParam.toString());
            
            Object authUserParam = servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol());
            Object authPwdParam = servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol());
            authUser = authUserParam == null ? null : authUserParam.toString();
            authPwd = authPwdParam == null ? null : authPwdParam.toString();
        }
    }
    
    @Override
    public Injectable<Service> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<Service>()
	{
	    @Override
	    public Service getValue()
	    {
		return getService();
	    }
	};
    }

    @Override
    public Service getContext(Class<?> type)
    {
        return getService();
    }
    
    public Service getService()
    {
        if (getDataset() != null) return getService(getDataset());
        else return getService(getSPARQLEndpoint(), getGraphStore(), getAuthUser(), getAuthPwd());
    }
    
    public Service getService(Dataset dataset)
    {
        return new com.atomgraph.core.model.impl.dataset.ServiceImpl(dataset);
    }
    
    public Service getService(Resource endpoint, Resource graphStore, String authUser, String authPwd)
    {
        return new com.atomgraph.core.model.impl.proxy.ServiceImpl(endpoint, graphStore, authUser, authPwd);
    }
    
    public Dataset getDataset()
    {
        return dataset;
    }
    
    public Client getClient()
    {
	return getProviders().getContextResolver(Client.class, null).getContext(Client.class);
    }
    
    public MediaTypes getMediaTypes()
    {
	return getProviders().getContextResolver(MediaTypes.class, null).getContext(MediaTypes.class);
    }
    
    public Resource getSPARQLEndpoint()
    {
        return endpoint;
    }
    
    public Resource getGraphStore()
    {
        return graphStore;
    }
    
    public String getAuthUser()
    {
        return authUser;
    }
    
    public String getAuthPwd()
    {
        return authPwd;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
}
