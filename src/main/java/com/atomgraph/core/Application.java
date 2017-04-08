/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core;

import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.client.SPARQLClient;
import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.core.io.DatasetProvider;
import com.atomgraph.core.io.ResultSetProvider;
import com.atomgraph.core.provider.DataManagerProvider;
import com.atomgraph.core.io.ModelProvider;
import com.atomgraph.core.io.QueryWriter;
import com.atomgraph.core.provider.QueryParamProvider;
import com.atomgraph.core.io.UpdateRequestReader;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import org.apache.jena.riot.RDFParserRegistry;
import com.atomgraph.core.mapper.ClientExceptionMapper;
import com.atomgraph.core.mapper.NotFoundExceptionMapper;
import com.atomgraph.core.model.RemoteService;
import com.atomgraph.core.model.Service;
import com.atomgraph.core.model.impl.ApplicationImpl;
import com.atomgraph.core.model.impl.proxy.GraphStoreBase;
import com.atomgraph.core.model.impl.QueriedResourceBase;
import com.atomgraph.core.model.impl.proxy.SPARQLEndpointBase;
import com.atomgraph.core.provider.ApplicationProvider;
import com.atomgraph.core.provider.ClientProvider;
import com.atomgraph.core.provider.GraphStoreClientProvider;
import com.atomgraph.core.provider.GraphStoreProvider;
import com.atomgraph.core.provider.MediaTypesProvider;
import com.atomgraph.core.provider.SPARQLClientProvider;
import com.atomgraph.core.provider.SPARQLEndpointProvider;
import com.atomgraph.core.provider.ServiceProvider;
import com.atomgraph.core.riot.RDFLanguages;
import com.atomgraph.core.riot.lang.RDFPostReaderFactory;
import com.atomgraph.core.util.jena.DataManager;
import com.atomgraph.core.vocabulary.A;
import com.atomgraph.core.vocabulary.SD;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import javax.annotation.PostConstruct;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.LocationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AtomGraph Core JAX-RS application base class.
 * Can be extended or used as it is (needs to be configured in web.xml).
 * Needs to register JAX-RS root resource classes and providers.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html">JAX-RS Application</a>
 * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
 */
public class Application extends javax.ws.rs.core.Application
{

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();

    private final Dataset dataset;
    private final Service service;
    private final com.atomgraph.core.model.Application application;
    private final MediaTypes mediaTypes;
    private final Client client;
    private final SPARQLClient sparqlClient;
    private final GraphStoreClient graphStoreClient;
    private final DataManager dataManager;
    private final Integer maxGetRequestSize;
    private final boolean preemptiveAuth;

    /**
     * Initializes root resource classes and provider singletons
     * 
     * @param servletConfig filter config
     */
    public Application(@Context ServletConfig servletConfig)
    {
        this(
            servletConfig.getInitParameter(A.dataset.getURI()) != null ? getDataset(servletConfig.getInitParameter(A.dataset.getURI()), null) : null,
            servletConfig.getInitParameter(SD.endpoint.getURI()) != null ? servletConfig.getInitParameter(SD.endpoint.getURI()) : null,
            servletConfig.getInitParameter(A.graphStore.getURI()) != null ? servletConfig.getInitParameter(A.graphStore.getURI()) : null,
            servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) != null ? servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) : null,
            servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) != null ? servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) : null,
            new MediaTypes(), null,
            servletConfig.getInitParameter(A.preemptiveAuth.getURI()) != null ? Boolean.parseBoolean(servletConfig.getInitParameter(A.preemptiveAuth.getURI())) : false
        );
    }
    
    public Application(final Dataset dataset,
            final String endpointURI, final String graphStoreURI, final String authUser, final String authPwd,
            final MediaTypes mediaTypes, final Integer maxGetRequestSize, final boolean preemptiveAuth)
    {
        this.dataset = dataset;
        this.mediaTypes = mediaTypes;
        this.maxGetRequestSize = maxGetRequestSize;
        this.preemptiveAuth = preemptiveAuth;
        
        // add RDF/POST serialization
        RDFLanguages.register(RDFLanguages.RDFPOST);
        RDFParserRegistry.registerLangTriples(RDFLanguages.RDFPOST, new RDFPostReaderFactory());
        
        if (dataset != null)
        {
            client = null;
            sparqlClient = null;
            graphStoreClient = null;
            service = new com.atomgraph.core.model.impl.dataset.ServiceImpl(dataset);
        }            
        else
        {
            if (endpointURI == null)
            {
                if (log.isErrorEnabled()) log.error("SPARQL endpoint not configured ('{}' not set in web.xml)", SD.endpoint.getURI());
                throw new ConfigurationException(SD.endpoint);
            }
            final Resource endpoint = ResourceFactory.createResource(endpointURI);
            if (graphStoreURI == null)
            {
                if (log.isErrorEnabled()) log.error("Graph Store not configured ('{}' not set in web.xml)", A.graphStore.getURI());
                throw new ConfigurationException(A.graphStore);
            }
            final Resource graphStore = ResourceFactory.createResource(graphStoreURI);
            final RemoteService remoteService = new com.atomgraph.core.model.impl.proxy.ServiceImpl(endpoint, graphStore, authUser, authPwd);            
        
            ClientConfig clientConfig = new DefaultClientConfig();
            clientConfig.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
            clientConfig.getSingletons().add(new ModelProvider());
            clientConfig.getSingletons().add(new DatasetProvider());
            clientConfig.getSingletons().add(new ResultSetProvider());
            clientConfig.getSingletons().add(new QueryWriter());
            clientConfig.getSingletons().add(new UpdateRequestReader()); // TO-DO: UpdateRequestProvider

            client = Client.create(clientConfig);
            if (log.isDebugEnabled()) client.addFilter(new LoggingFilter(System.out));

            WebResource sparqlEndpointOrigin = client.resource(remoteService.getSPARQLEndpoint().getURI());            
            WebResource graphStoreOrigin = client.resource(remoteService.getGraphStore().getURI());            
            if (remoteService.getAuthUser() != null && remoteService.getAuthPwd() != null)
            {
                ClientFilter authFilter = new HTTPBasicAuthFilter(remoteService.getAuthUser(), remoteService.getAuthPwd());
                sparqlEndpointOrigin.addFilter(authFilter);
                graphStoreOrigin.addFilter(authFilter);
            }
            
            if (maxGetRequestSize != null) sparqlClient = SPARQLClient.create(sparqlEndpointOrigin, mediaTypes, maxGetRequestSize);
            else sparqlClient = SPARQLClient.create(sparqlEndpointOrigin, mediaTypes);
            graphStoreClient = GraphStoreClient.create(graphStoreOrigin, mediaTypes);

            service = remoteService;
        }
        
        application = new ApplicationImpl(service);
        dataManager = new DataManager(LocationMapper.get(), client, mediaTypes, preemptiveAuth); // client == null?
    }
    
    @PostConstruct
    public void init()
    {
        classes.add(QueriedResourceBase.class); // handles all
        classes.add(SPARQLEndpointBase.class); // handles /sparql queries
        classes.add(GraphStoreBase.class); // handles /service requests

        singletons.add(new ModelProvider());
        singletons.add(new com.atomgraph.core.io.DatasetProvider());
        singletons.add(new ResultSetProvider());
        singletons.add(new QueryParamProvider());
        singletons.add(new UpdateRequestReader());
        singletons.add(new DataManagerProvider(getDataManager()));
        singletons.add(new ApplicationProvider(getApplication()));
        singletons.add(new ServiceProvider(getService()));
        singletons.add(new SPARQLEndpointProvider(getMediaTypes(), getDataset(), getSPARQLClient()));
        singletons.add(new GraphStoreProvider(getMediaTypes(), getDataset(), getGraphStoreClient()));
        singletons.add(new com.atomgraph.core.provider.DatasetProvider(getDataset()));
        singletons.add(new SPARQLClientProvider(getSPARQLClient()));
        singletons.add(new GraphStoreClientProvider(getGraphStoreClient()));
        singletons.add(new ClientProvider(getClient()));        
        singletons.add(new MediaTypesProvider(getMediaTypes()));
        singletons.add(new ClientExceptionMapper());        
        singletons.add(new NotFoundExceptionMapper());
    }
    
    /**
     * Provides JAX-RS root resource classes.
     * 
     * @return set of root resource classes
     * @see com.atomgraph.core.model
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getClasses()">Application.getClasses()</a>
     */
    @Override
    public Set<Class<?>> getClasses()
    {        
        return classes;
    }

    /**
     * Provides JAX-RS singleton objects (e.g. resources or Providers)
     * 
     * @return set of singleton objects
     * @see com.atomgraph.core.provider
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getSingletons()">Application.getSingletons()</a>
     */
    @Override
    public Set<Object> getSingletons()
    {
        return singletons;
    }

    public Dataset getDataset()
    {
        return dataset;
    }
    
    public Service getService()
    {
        return service;
    }
    
    public com.atomgraph.core.model.Application getApplication()
    {
        return application;
    }
    
    private Client getClient()
    {
        return client;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    public Integer getMaxGetRequestSize()
    {
        return maxGetRequestSize;
    }    
    
    public SPARQLClient getSPARQLClient()
    {
        return sparqlClient;
    }
    
    public GraphStoreClient getGraphStoreClient()
    {
        return graphStoreClient;
    }
    
    public DataManager getDataManager()
    {
        return dataManager;
    }
    
    public boolean isPreemptiveAuth()
    {
        return preemptiveAuth;
    }

    public static Dataset getDataset(String location, Lang lang)
    {
        Dataset dataset = DatasetFactory.createTxnMem();
        RDFDataMgr.read(dataset, location, lang);
        return dataset;
    }
    
}