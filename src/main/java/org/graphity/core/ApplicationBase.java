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

import org.graphity.core.provider.ResultSetWriter;
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
import org.graphity.core.model.impl.GraphStoreProxyBase;
import org.graphity.core.model.impl.QueriedResourceBase;
import org.graphity.core.model.impl.SPARQLEndpointProxyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graphity Server JAX-RS application base class.
 * Can be extended or used as it is (needs to be configured in web.xml).
 * Needs to register JAX-RS root resource classes and providers.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html">JAX-RS Application</a>
 * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
 */
public class ApplicationBase extends javax.ws.rs.core.Application
{

    private static final Logger log = LoggerFactory.getLogger(ApplicationBase.class);

    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();

    private final ServletConfig servletConfig;

    /**
     * Initializes root resource classes and provider singletons
     * 
     * @param servletConfig filter config
     */
    public ApplicationBase(@Context ServletConfig servletConfig)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        this.servletConfig = servletConfig;

	classes.add(QueriedResourceBase.class); // handles all
	classes.add(SPARQLEndpointProxyBase.class); // handles /sparql queries
	classes.add(GraphStoreProxyBase.class); // handles /service requests

	singletons.add(new ModelProvider());
	singletons.add(new ResultSetWriter());
	singletons.add(new QueryParamProvider());
	singletons.add(new UpdateRequestReader());
        singletons.add(new DataManagerProvider());
        singletons.add(new SPARQLEndpointOriginProvider());
        singletons.add(new GraphStoreOriginProvider());
        singletons.add(new SPARQLEndpointProvider());
    }
    
    /**
     * Provides JAX-RS root resource classes.
     * 
     * @return set of root resource classes
     * @see org.graphity.server.model
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
     * @see org.graphity.server.provider
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

}