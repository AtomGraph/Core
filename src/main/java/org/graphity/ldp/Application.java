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
package org.graphity.ldp;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import org.graphity.ldp.model.ResourceBase;
import org.graphity.ldp.provider.ModelProvider;
import org.graphity.ldp.provider.QueryParamProvider;
import org.graphity.ldp.provider.RDFPostReader;
import org.graphity.ldp.provider.ResultSetWriter;
import org.graphity.ldp.provider.xslt.ModelXSLTWriter;
import org.graphity.util.locator.LocatorGRDDL;
import org.graphity.util.locator.PrefixMapper;
import org.graphity.util.locator.grddl.LocatorAtom;
import org.graphity.util.manager.DataManager;
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
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    /**
     * Configuration property for ontology file location (set in web.xml)
     * 
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
     */
    public static final String PROPERTY_ONTOLOGY_LOCATION = "org.graphity.ldp.ontology.location";
    
    /**
     * Configuration property for ontology path relative to the base URI (set in web.xml)
     * 
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     * @see <a href="http://docs.oracle.com/cd/E24329_01/web.1211/e24983/configure.htm#CACEAEGG">Packaging the RESTful Web Service Application Using web.xml With Application Subclass</a>
     */
    public static final String PROPERTY_ONTOLOGY_PATH = "org.graphity.ldp.ontology.path";

    private Set<Class<?>> classes = new HashSet<Class<?>>();
    private Set<Object> singletons = new HashSet<Object>();

    /**
     * Initializes (post construction) DataManager, its LocationMapper and Locators
     * @see org.graphity.util.manager.DataManager
     * @see org.graphity.util.locator
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/FileManager.html">FileManager</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/LocationMapper.html">LocationMapper</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/Locator.html">Locator</a>
     */
    @PostConstruct
    public void init()
    {
	if (log.isDebugEnabled()) log.debug("Application.init()");

	// initialize locally cached ontology mapping
	LocationMapper mapper = new PrefixMapper("location-mapping.ttl");
	LocationMapper.setGlobalLocationMapper(mapper);
	if (log.isDebugEnabled())
	{
	    log.debug("LocationMapper.get(): {}", LocationMapper.get());
	    log.debug("FileManager.get().getLocationMapper(): {}", FileManager.get().getLocationMapper());
	}
	
	DataManager.get().setLocationMapper(mapper);
	// WARNING! ontology caching can cause concurrency/consistency problems
	DataManager.get().setModelCaching(false);
	if (log.isDebugEnabled())
	{
	    log.debug("FileManager.get(): {} DataManager.get(): {}", FileManager.get(), DataManager.get());
	    log.debug("DataManager.get().getLocationMapper(): {}", DataManager.get().getLocationMapper());
	}
	
	OntDocumentManager.getInstance().setFileManager(DataManager.get());
	if (log.isDebugEnabled()) log.debug("OntDocumentManager.getInstance(): {} OntDocumentManager.getInstance().getFileManager(): {}", OntDocumentManager.getInstance(), OntDocumentManager.getInstance().getFileManager());

	SysRIOT.wireIntoJena(); // enable RIOT parser
	SPINModuleRegistry.get().init(); // needs to be called before any SPIN-related code

	try
	{
	    DataManager.get().addLocator(new LocatorAtom(getStylesheet("org/graphity/util/locator/grddl/atom-grddl.xsl")));
	    DataManager.get().addLocator(new LocatorGRDDL(getStylesheet("org/graphity/util/locator/grddl/twitter-grddl.xsl")));
	}
	catch (TransformerConfigurationException ex)
	{
	    log.error("XSLT stylesheet error", ex);
	}
	catch (FileNotFoundException ex)
	{
	    log.error("XSLT stylesheet not found", ex);
	}
	catch (URISyntaxException ex)
	{
	    log.error("XSLT stylesheet URI error", ex);
	}
    }

    /**
     * Provides JAX-RS root resource classes.
     * @return set of root resource classes
     * @see org.graphity.ldp.model
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
     * @return set of singleton objects
     * @see org.graphity.ldp.provider
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getSingletons()">Application.getSingletons()</a>
     */
    @Override
    public Set<Object> getSingletons()
    {
	singletons.add(new ModelProvider());
	singletons.add(new ResultSetWriter());
	singletons.add(new RDFPostReader());
	singletons.add(new QueryParamProvider(Query.class));
	
	try
	{
	    singletons.add(new ModelXSLTWriter(getStylesheet("org/graphity/ldp/provider/xslt/Resource.xsl"), DataManager.get())); // writes XHTML responses
	}
	catch (TransformerConfigurationException ex)
	{
	    log.error("XSLT stylesheet error", ex);
	}
	catch (FileNotFoundException ex)
	{
	    log.error("XSLT stylesheet not found", ex);
	}
	catch (URISyntaxException ex)
	{
	    log.error("XSLT stylesheet URI error", ex);
	}

	return singletons;
    }
    
    /**
     * Provides stylesheet as XML string
     * @param filename
     * @return XML source
     * @throws FileNotFoundException
     * @throws URISyntaxException 
     * @see <a href="http://docs.oracle.com/javase/6/docs/api/javax/xml/transform/Source.html">Source</a>
     */
    public Source getStylesheet(String filename) throws FileNotFoundException, URISyntaxException
    {
	// using getResource() because getResourceAsStream() does not retain systemId
	URL xsltUrl = getClass().getClassLoader().getResource(filename);
	if (xsltUrl == null) throw new FileNotFoundException();
	String xsltUri = xsltUrl.toURI().toString();
	if (log.isDebugEnabled()) log.debug("XSLT stylesheet URI: {}", xsltUri);
	return new StreamSource(xsltUri);
    }

}