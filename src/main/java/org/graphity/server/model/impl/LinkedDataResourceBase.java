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
package org.graphity.server.model.impl;

import com.hp.hpl.jena.rdf.model.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.graphity.server.model.LinkedDataResource;
import org.graphity.server.vocabulary.GS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-only Linked Data resources.
 * 
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Resource.html">Jena Resource</a>
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public abstract class LinkedDataResourceBase implements LinkedDataResource
{
    private static final Logger log = LoggerFactory.getLogger(LinkedDataResourceBase.class);

    private final String uri;
    private @Context Request request;
    private @Context ServletContext servletContext;

    /** 
     * JAX-RS-compatible resource constructor with injected initialization objects.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the 
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/UriInfo.html#getAbsolutePath()">JAX-RS UriInfo.getAbsolutePath()</a>
     */
    public LinkedDataResourceBase(@Context UriInfo uriInfo)
    {
	this(uriInfo.getAbsolutePath().toString());
    }
    
    /**
     * Protected constructor. Not suitable for JAX-RS but can be used when subclassing.
     * 
     * @param uri URI string of this resource
     */
    protected LinkedDataResourceBase(String uri)
    {
	if (uri == null) throw new IllegalArgumentException("URI cannot be null");

	this.uri = uri;	
	if (log.isDebugEnabled()) log.debug("Creating LinkedDataResource from Resource with URI: {}", uri);
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
        return ModelResponse.fromRequest(getRequest()).
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
        List<Variant> variants = new ArrayList<>();
        Iterator<Lang> it = RDFLanguages.getRegisteredLanguages().iterator();
        
        // RDF/XML as the default one - the first one gets selected by selectVariant()
        variants.add(new Variant(new MediaType(Lang.RDFXML.getContentType().getType(),
                Lang.RDFXML.getContentType().getSubType()),
            null, null));

        while (it.hasNext())
        {
            Lang lang = it.next();
            if (!lang.equals(Lang.RDFNULL) && !lang.equals(Lang.RDFXML))
            {
                ContentType ct = lang.getContentType();
                //List<String> altTypes = lang.getAltContentTypes();
                MediaType mediaType = new MediaType(ct.getType(), ct.getSubType()); // MediaType.valueOf(ct.getContentType()
                variants.add(new Variant(mediaType, null, null));
            }
        }
        
        return variants;
    }

    /**
     * Returns URI of this resource
     * 
     * @return URI of this resource
     */
    @Override
    public String getURI()
    {
	return uri;
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
     * Returns context for this web application (including parameters specified in web.xml).
     * 
     * @return webapp context
     */
    public ServletContext getServletContext()
    {
	return servletContext;
    }

    /**
     * Returns <code>Cache-Control</code> header configuration for this resource
     * 
     * @return cache control of this resource
     */
    public CacheControl getCacheControl()
    {
	if (getServletContext().getInitParameter(GS.cacheControl.getURI()) == null) return null;
        
        return CacheControl.valueOf(getServletContext().getInitParameter(GS.cacheControl.getURI()).toString());
    }
    
}