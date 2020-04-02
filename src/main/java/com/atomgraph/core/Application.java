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

import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.core.io.DatasetProvider;
import com.atomgraph.core.io.ResultSetProvider;
import com.atomgraph.core.io.ModelProvider;
import com.atomgraph.core.io.QueryProvider;
import com.atomgraph.core.provider.QueryParamProvider;
import com.atomgraph.core.io.UpdateRequestReader;
import javax.ws.rs.core.Context;
import org.apache.jena.riot.RDFParserRegistry;
import com.atomgraph.core.mapper.ClientExceptionMapper;
import com.atomgraph.core.mapper.NotFoundExceptionMapper;
import com.atomgraph.core.model.Service;
import com.atomgraph.core.model.impl.GraphStoreImpl;
import com.atomgraph.core.model.impl.QueriedResourceBase;
import com.atomgraph.core.model.impl.SPARQLEndpointImpl;
import com.atomgraph.core.provider.MediaTypesProvider;
import com.atomgraph.core.riot.RDFLanguages;
import com.atomgraph.core.riot.lang.RDFPostReaderFactory;
import com.atomgraph.core.util.jena.DataManager;
import com.atomgraph.core.vocabulary.A;
import com.atomgraph.core.vocabulary.SD;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.LocationMapper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AtomGraph Core JAX-RS application base class.
 * Can be extended or used as it is (needs to be configured in web.xml).
 * Needs to register JAX-RS root resource classes and providers.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see javax.ws.rs.core.Application
 * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
 */
public class Application extends ResourceConfig implements com.atomgraph.core.model.Application
{

    private static final Logger log = LoggerFactory.getLogger(Application.class);

//    private final Set<Class<?>> classes = new HashSet<>();
//    private final Set<Object> singletons = new HashSet<>();

    private final Dataset dataset;
    private final Service service;
//    private final com.atomgraph.core.model.Application application;
    private final MediaTypes mediaTypes;
    private final Client client;
    private final DataManager dataManager;
    private final Integer maxGetRequestSize;
    private final boolean preemptiveAuth;

    /**
     * Initializes root resource classes and provider singletons
     * 
     * @param servletConfig servlet config
     */
    public Application(@Context ServletConfig servletConfig)
    {
        this(
            servletConfig.getInitParameter(A.dataset.getURI()) != null ? getDataset(servletConfig.getInitParameter(A.dataset.getURI()), null) : null,
            servletConfig.getInitParameter(SD.endpoint.getURI()) != null ? servletConfig.getInitParameter(SD.endpoint.getURI()) : null,
            servletConfig.getInitParameter(A.graphStore.getURI()) != null ? servletConfig.getInitParameter(A.graphStore.getURI()) : null,
            servletConfig.getInitParameter(A.quadStore.getURI()) != null ? servletConfig.getInitParameter(A.quadStore.getURI()) : null,
            servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) != null ? servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) : null,
            servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) != null ? servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) : null,
            new MediaTypes(), getClient(new ClientConfig()),
            servletConfig.getInitParameter(A.maxGetRequestSize.getURI()) != null ? Integer.parseInt(servletConfig.getInitParameter(A.maxGetRequestSize.getURI())) : null,
            servletConfig.getInitParameter(A.preemptiveAuth.getURI()) != null ? Boolean.parseBoolean(servletConfig.getInitParameter(A.preemptiveAuth.getURI())) : false
        );
    }
    
    public Application(final Dataset dataset,
            final String endpointURI, final String graphStoreURI, final String quadStoreURI,
            final String authUser, final String authPwd,
            final MediaTypes mediaTypes, final Client client, final Integer maxGetRequestSize, final boolean preemptiveAuth)
    {
        this.dataset = dataset;
        this.mediaTypes = mediaTypes;
        this.client = client;
        this.maxGetRequestSize = maxGetRequestSize;
        this.preemptiveAuth = preemptiveAuth;
        
        // add RDF/POST serializer
        RDFLanguages.register(RDFLanguages.RDFPOST);
        RDFParserRegistry.registerLangTriples(RDFLanguages.RDFPOST, new RDFPostReaderFactory());
        
        if (dataset != null)
            service = new com.atomgraph.core.model.impl.dataset.ServiceImpl(dataset, mediaTypes);
        else
        {
            if (endpointURI == null)
            {
                if (log.isErrorEnabled()) log.error("SPARQL endpoint not configured ('{}' not set in web.xml)", SD.endpoint.getURI());
                throw new ConfigurationException(SD.endpoint);
            }
            if (graphStoreURI == null)
            {
                if (log.isErrorEnabled()) log.error("Graph Store not configured ('{}' not set in web.xml)", A.graphStore.getURI());
                throw new ConfigurationException(A.graphStore);
            }

            service = new com.atomgraph.core.model.impl.remote.ServiceImpl(client, mediaTypes,
                    ResourceFactory.createResource(endpointURI), ResourceFactory.createResource(graphStoreURI), ResourceFactory.createResource(quadStoreURI),
                    authUser, authPwd, maxGetRequestSize);
        }
        
//        application = new ApplicationImpl(service);
        dataManager = new DataManager(LocationMapper.get(), client, mediaTypes, preemptiveAuth);
    }
    
    @PostConstruct
    public void init()
    {
        register(QueriedResourceBase.class); // handles all
        register(SPARQLEndpointImpl.class); // handles /sparql queries
        register(GraphStoreImpl.class); // handles /service requests

        register(new ModelProvider());
        register(new DatasetProvider());
        register(new ResultSetProvider());
        register(new QueryParamProvider());
        register(new QueryProvider());
        register(new UpdateRequestReader());
//        register(new DataManagerProvider(getDataManager()));
//        register(new ServiceProvider(getService()));
//        register(new MediaTypesProvider(getMediaTypes()));
        register(new ClientExceptionMapper());
        register(new NotFoundExceptionMapper());
        
//        register(new AbstractBinder()
//        {
//            @Override
//            protected void configure()
//            {
//                bindFactory(MediaTypesProvider.class).to(MediaTypes.class).
//                proxy(true).proxyForSameScope(false).in(RequestScoped.class);
//            }
//        });

        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(new MediaTypes()).to(MediaTypes.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(service).to(Service.class);
            }
        });
    }
    
//    /**
//     * Provides JAX-RS root resource classes.
//     * 
//     * @return set of root resource classes
//     * @see com.atomgraph.core.model
//     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getClasses()">Application.getClasses()</a>
//     */
//    @Override
//    public Set<Class<?>> getClasses()
//    {        
//        return classes;
//    }
//
//    /**
//     * Provides JAX-RS singleton objects (e.g. resources or Providers)
//     * 
//     * @return set of singleton objects
//     * @see com.atomgraph.core.provider
//     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getSingletons()">Application.getSingletons()</a>
//     */
//    @Override
//    public Set<Object> getSingletons()
//    {
//        return singletons;
//    }

    public Dataset getDataset()
    {
        return dataset;
    }
    
    @Override
    public Service getService()
    {
        return service;
    }
    
//    public com.atomgraph.core.model.Application getApplication()
//    {
//        return application;
//    }
    
    public Client getClient()
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
    
    public static Client getClient(ClientConfig clientConfig)
    {
        clientConfig.register(ModelProvider.class);
        clientConfig.register(DatasetProvider.class);
        clientConfig.register(ResultSetProvider.class);
        clientConfig.register(QueryProvider.class);
        clientConfig.register(UpdateRequestReader.class); // TO-DO: UpdateRequestProvider

        Client client = ClientBuilder.newClient(clientConfig);
        //if (log.isDebugEnabled()) client.register(new LoggingFeature(log));
        
        return client;
    }
    
}