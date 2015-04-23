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
package org.graphity.core.model.impl;

import com.hp.hpl.jena.rdf.model.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import org.graphity.core.model.Resource;
import org.graphity.core.vocabulary.G;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-only Linked Data resources.
 * 
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Resource.html">Jena Resource</a>
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public abstract class ResourceBase implements Resource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private final UriInfo uriInfo;
    private final Request request;
    private final ServletConfig servletConfig;
    private CacheControl cacheControl;

    /** 
     * JAX-RS-compatible resource constructor with injected request metadata.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the request
     * @param request current request object
     * @param servletConfig webapp context
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/UriInfo.html#getAbsolutePath()">JAX-RS UriInfo.getAbsolutePath()</a>
     */
    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletConfig servletConfig)
    {
	if (uriInfo == null) throw new IllegalArgumentException("UriInfo cannot be null");
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");

        this.uriInfo = uriInfo;
        this.request = request;
        this.servletConfig = servletConfig;
    }

    /**
     * Post-construct initialization. Subclasses need to call super.init() first, just like with super() constructor.
     */
    @PostConstruct
    public void init()
    {
        this.cacheControl = getCacheControl(G.cacheControl);        
    }
    
    /**
     * Returns response for the given RDF model.
     * 
     * @param model RDF model
     * @return response object
     */
    public Response getResponse(Model model)
    {
        return getResponseBuilder(model).build();
    }

    /**
     * Returns response builder for the given RDF model.
     * 
     * @param model RDF model
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Model model)
    {
        return org.graphity.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(model, getVariants()).
                cacheControl(getCacheControl());
    }
    
    /**
     * Builds a list of acceptable response variants
     * 
     * @return supported variants
     */
    public List<Variant> getVariants()
    {
        return getVariantListBuilder().add().build();
    }
    
    public Variant.VariantListBuilder getVariantListBuilder()
    {
        return getVariantListBuilder(getMediaTypes(), getLanguages(), getEncodings());
    }
    
    /**
     * Produces a Variant builder from a list of media types.
     * 
     * @param mediaTypes
     * @param languages
     * @param encodings
     * @return variant builder
     */    
    public Variant.VariantListBuilder getVariantListBuilder(List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings)
    {        
        return Variant.VariantListBuilder.newInstance().
                mediaTypes(org.graphity.core.model.impl.Response.mediaTypeListToArray(mediaTypes)).
                languages(org.graphity.core.model.impl.Response.localeListToArray(languages)).
                encodings(org.graphity.core.model.impl.Response.stringListToArray(encodings));
    }
    
    public List<MediaType> getMediaTypes()
    {
        List<MediaType> list = new ArrayList<>();
        Map<String, String> utf8Param = new HashMap<>();
        utf8Param.put("charset", "UTF-8");
        
        Iterator<MediaType> it = org.graphity.core.MediaType.getRegistered().iterator();
        while (it.hasNext())
        {
            MediaType registered = it.next();
            list.add(new MediaType(registered.getType(), registered.getSubtype(), utf8Param));
        }
        
        MediaType rdfXml = new MediaType(org.graphity.core.MediaType.APPLICATION_RDF_XML_TYPE.getType(), org.graphity.core.MediaType.APPLICATION_RDF_XML_TYPE.getSubtype(), utf8Param);
        list.add(0, rdfXml); // first one becomes default
        
        return list;
    }
    
    public List<Locale> getLanguages()
    {
        return new ArrayList<>();
    }

    public List<String> getEncodings()
    {
        return new ArrayList<>();
    }

    /**
     * Returns URI of this resource
     * 
     * @return URI of this resource
     */
    @Override
    public String getURI()
    {
	return getUriInfo().getAbsolutePath().toString();
    }

    /**
     * Returns URI information.
     * 
     * @return URI info object
     */
    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    /**
     * Returns current request.
     * 
     * @return request object
     */
    public Request getRequest()
    {
	return request;
    }

    /**
     * Returns config for this servlet (including parameters specified in web.xml).
     * 
     * @return webapp context
     */
    public ServletConfig getServletConfig()
    {
	return servletConfig;
    }

    /**
     * Returns <pre>Cache-Control</pre> header value.
     * 
     * @return cache control object
     */
    public CacheControl getCacheControl()
    {
        return cacheControl;
    }

    /**
     * Returns <code>Cache-Control</code> header configuration for this resource
     * 
     * @param property cache control property
     * @return cache control of this resource
     */
    public CacheControl getCacheControl(Property property)
    {
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        if (getServletConfig().getInitParameter(property.getURI()) == null) return null;
        
        return CacheControl.valueOf(getServletConfig().getInitParameter(property.getURI()).toString());
    }
    
}