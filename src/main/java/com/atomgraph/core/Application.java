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

import org.apache.jena.rdf.model.Property;
import com.atomgraph.core.provider.ResultSetProvider;
import com.atomgraph.core.provider.DataManagerProvider;
import com.atomgraph.core.provider.ModelProvider;
import com.atomgraph.core.provider.QueryParamProvider;
import com.atomgraph.core.provider.UpdateRequestReader;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import org.apache.jena.riot.RDFParserRegistry;
import com.atomgraph.core.mapper.ClientExceptionMapper;
import com.atomgraph.core.mapper.NotFoundExceptionMapper;
import com.atomgraph.core.model.impl.GraphStoreProxyBase;
import com.atomgraph.core.model.impl.QueriedResourceBase;
import com.atomgraph.core.model.impl.SPARQLEndpointProxyBase;
import com.atomgraph.core.provider.ApplicationProvider;
import com.atomgraph.core.provider.ClientProvider;
import com.atomgraph.core.provider.DatasetProvider;
import com.atomgraph.core.provider.GraphStoreClientProvider;
import com.atomgraph.core.provider.GraphStoreProvider;
import com.atomgraph.core.provider.MediaTypesProvider;
import com.atomgraph.core.provider.SPARQLClientProvider;
import com.atomgraph.core.provider.SPARQLEndpointProvider;
import com.atomgraph.core.riot.RDFLanguages;
import com.atomgraph.core.riot.lang.RDFPostReaderFactory;
import javax.annotation.PostConstruct;
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

    private final ServletConfig servletConfig;

    /**
     * Initializes root resource classes and provider singletons
     * 
     * @param servletConfig filter config
     */
    public Application(@Context ServletConfig servletConfig)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        this.servletConfig = servletConfig;

        // add RDF/POST serialization
        RDFLanguages.register(RDFLanguages.RDFPOST);
        RDFParserRegistry.registerLangTriples(RDFLanguages.RDFPOST, new RDFPostReaderFactory());
    }
    
    @PostConstruct
    public void init()
    {
	classes.add(QueriedResourceBase.class); // handles all
	classes.add(SPARQLEndpointProxyBase.class); // handles /sparql queries
	classes.add(GraphStoreProxyBase.class); // handles /service requests

	singletons.add(new ModelProvider());
	singletons.add(new DatasetProvider());
        singletons.add(new ResultSetProvider());
	singletons.add(new QueryParamProvider());
	singletons.add(new UpdateRequestReader());
        singletons.add(new DataManagerProvider(getServletConfig()));
	singletons.add(new ApplicationProvider(getServletConfig()));
        singletons.add(new SPARQLClientProvider(getServletConfig()));
        singletons.add(new SPARQLEndpointProvider(getServletConfig()));
        singletons.add(new GraphStoreClientProvider());
        singletons.add(new GraphStoreProvider(getServletConfig()));
        singletons.add(new ClientProvider());        
        singletons.add(new MediaTypesProvider());
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

    /**
     * Returns servlet configuration.
     * 
     * @return servlet config
     */
    public ServletConfig getServletConfig()
    {
	return servletConfig;
    }

    public boolean getBooleanParam(ServletConfig servletConfig, Property property)
    {
	if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        boolean value = false;
        if (servletConfig.getInitParameter(property.getURI()) != null)
            value = Boolean.parseBoolean(servletConfig.getInitParameter(property.getURI()));
        return value;
    }

}