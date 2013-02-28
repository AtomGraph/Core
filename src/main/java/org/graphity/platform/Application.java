/*
 * Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graphity.platform;

import com.hp.hpl.jena.query.Query;
import com.sun.jersey.api.core.ResourceConfig;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.graphity.platform.model.ResourceBase;
import org.graphity.platform.provider.ModelProvider;
import org.graphity.platform.provider.QueryParamProvider;
import org.graphity.platform.provider.ResultSetWriter;
import org.openjena.riot.SysRIOT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.system.SPINModuleRegistry;

/**
 * Graphity JAX-RS application base class.
 * Can be extended or used as it is (needs to be registered in web.xml).
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html">JAX-RS Application</a>
 */
public class Application extends javax.ws.rs.core.Application
{
    @Context ResourceConfig resourceConfig;
    @Context ServletContext servletContext;

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    /**
     * Configuration property for ontology file location (set in web.xml)
     * 
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
     */
    public static final String PROPERTY_ONTOLOGY_LOCATION = "org.graphity.platform.ontology.location";
    
    /**
     * Configuration property for ontology path relative to the base URI (set in web.xml)
     * 
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
     */
    public static final String PROPERTY_ONTOLOGY_PATH = "org.graphity.platform.ontology.path";

    /**
     * Configuration property for absolute ontology URI (set in web.xml)
     * 
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
     */
    public static final String PROPERTY_ONTOLOGY_URI = "org.graphity.platform.ontology.uri";

    /**
     * Configuration property for ontology SPARQL endpoint (set in web.xml)
     * 
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
     */
    public static final String PROPERTY_ONTOLOGY_ENDPOINT = "org.graphity.platform.ontology.endpoint";

    /**
     * Configuration property for default Cache-Control header value (set in web.xml)
     * 
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
     */
    public static final String PROPERTY_CACHE_CONTROL = "org.graphity.platform.model.cache-control";

    private Set<Class<?>> classes = new HashSet<Class<?>>();
    private Set<Object> singletons = new HashSet<Object>();

    /**
     * Initializes (post construction) DataManager, its LocationMapper and Locators
     * 
     * @see org.graphity.util.manager.DataManager
     * @see org.graphity.util.locator
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/FileManager.html">FileManager</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/LocationMapper.html">LocationMapper</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/Locator.html">Locator</a>
     */
    @PostConstruct
    public void init()
    {
	SysRIOT.wireIntoJena(); // enable RIOT parser
	SPINModuleRegistry.get().init(); // needs to be called before any SPIN-related code
    }
    
    /**
     * Provides JAX-RS root resource classes.
     * 
     * @return set of root resource classes
     * @see org.graphity.platform.model
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getClasses()">Application.getClasses()</a>
     */
    @Override
    public Set<Class<?>> getClasses()
    {
	classes.add(ResourceBase.class); // handles all
	
        return classes;
    }

    /**
     * Provides JAX-RS singleton objects (e.g. resources or Providers)
     * 
     * @return set of singleton objects
     * @see org.graphity.platform.provider
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getSingletons()">Application.getSingletons()</a>
     */
    @Override
    public Set<Object> getSingletons()
    {
	singletons.add(new ModelProvider());
	singletons.add(new ResultSetWriter());
	singletons.add(new QueryParamProvider(Query.class));

	return singletons;
    }

    public ResourceConfig getResourceConfig()
    {
	return resourceConfig;
    }

    public ServletContext getServletContext()
    {
	return servletContext;
    }

}