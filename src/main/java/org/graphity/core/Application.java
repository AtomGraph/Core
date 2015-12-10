/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.core;

import com.hp.hpl.jena.rdf.model.Property;
import org.graphity.core.provider.ResultSetProvider;
import org.graphity.core.provider.DataManagerProvider;
import org.graphity.core.provider.ModelProvider;
import org.graphity.core.provider.QueryParamProvider;
import org.graphity.core.provider.SPARQLEndpointOriginProvider;
import org.graphity.core.provider.UpdateRequestReader;
import org.graphity.core.provider.GraphStoreOriginProvider;
import org.graphity.core.provider.SPARQLEndpointProvider;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import org.apache.jena.riot.IO_Jena;
import org.apache.jena.riot.RDFParserRegistry;
import org.graphity.core.mapper.NotFoundExceptionMapper;
import org.graphity.core.model.impl.GraphStoreProxyBase;
import org.graphity.core.model.impl.QueriedResourceBase;
import org.graphity.core.model.impl.SPARQLEndpointProxyBase;
import org.graphity.core.provider.ClientProvider;
import org.graphity.core.provider.DatasetProvider;
import org.graphity.core.provider.MediaTypesProvider;
import org.graphity.core.riot.RDFLanguages;
import org.graphity.core.riot.lang.RDFPostReaderAdapter;
import org.graphity.core.riot.lang.RDFPostReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graphity Core JAX-RS application base class.
 * Can be extended or used as it is (needs to be configured in web.xml).
 * Needs to register JAX-RS root resource classes and providers.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
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
        RDFLanguages.register(RDFLanguages.RDFPOST) ;
        RDFParserRegistry.registerLangTriples(RDFLanguages.RDFPOST, new RDFPostReaderFactory());
        IO_Jena.registerForModelRead(RDFLanguages.strLangRDFPOST, RDFPostReaderAdapter.class) ;

	classes.add(QueriedResourceBase.class); // handles all
	classes.add(SPARQLEndpointProxyBase.class); // handles /sparql queries
	classes.add(GraphStoreProxyBase.class); // handles /service requests

	singletons.add(new ModelProvider());
	singletons.add(new DatasetProvider());
        singletons.add(new ResultSetProvider());
	singletons.add(new QueryParamProvider());
	singletons.add(new UpdateRequestReader());
        singletons.add(new DataManagerProvider());
        singletons.add(new ClientProvider());        
        singletons.add(new SPARQLEndpointOriginProvider());
        singletons.add(new GraphStoreOriginProvider());
        singletons.add(new SPARQLEndpointProvider());
        //singletons.add(new GraphStoreProvider());
        singletons.add(new MediaTypesProvider());
        singletons.add(new NotFoundExceptionMapper());
    }
    
    /**
     * Provides JAX-RS root resource classes.
     * 
     * @return set of root resource classes
     * @see org.graphity.core.model
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
     * @see org.graphity.core.provider
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
            value = Boolean.parseBoolean(servletConfig.getInitParameter(property.getURI()).toString());
        return value;
    }

}