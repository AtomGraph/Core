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
package com.atomgraph.core.model.impl;

import org.apache.jena.rdf.model.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.Resource;
import com.atomgraph.core.util.ModelUtils;
import java.util.Collections;
import java.util.Date;
import javax.inject.Inject;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-only Linked Data resources.
 * 
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Resource.html">Jena Resource</a>
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public abstract class ResourceBase implements Resource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private final UriInfo uriInfo;
    private final Request request;
    private final MediaTypes mediaTypes;
    private final URI uri;

    /** 
     * JAX-RS-compatible resource constructor with injected request metadata.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the request
     * @param request current request object
     * @param mediaTypes media types
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/UriInfo.html#getAbsolutePath()">JAX-RS UriInfo.getAbsolutePath()</a>
     */
    @Inject
    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, MediaTypes mediaTypes)
    {
        this(uriInfo, request, mediaTypes, uriInfo.getAbsolutePath());
    }
    
    protected ResourceBase(final UriInfo uriInfo, final Request request, final MediaTypes mediaTypes, final URI uri)
    {
        if (uriInfo == null) throw new IllegalArgumentException("UriInfo cannot be null");
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");
        if (uri == null) throw new IllegalArgumentException("URI cannot be null");

        this.uriInfo = uriInfo;
        this.request = request;
        this.mediaTypes = mediaTypes;
        this.uri = uri;
        if (log.isDebugEnabled()) log.debug("Request URI: {}", uriInfo.getRequestUri());
    }

    /**
     * Returns response for the given RDF graph.
     * 
     * @param model RDF model
     * @return response object
     */
    public Response getResponse(Model model)
    {
        return getResponseBuilder(model).build();
    }

    /**
     * Returns response for the given RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return response object
     */
    public Response getResponse(Dataset dataset)
    {
        Variant variant = getRequest().selectVariant(getVariants(getWritableMediaTypes(Dataset.class)));
        
        if (variant == null) return getResponse(dataset.getDefaultModel()); // if quads are not acceptable, fallback to responding with the default graph

        return getResponseBuilder(dataset).build();
    }
    
    /**
     * Extract the <code>Last-Modified</code> response header value of the current resource from its RDF model.
     * 
     * @param model RDF model
     * @return date of last modification
     */
    public Date getLastModified(Model model)
    {
        return null;
    }

    /**
     * Generate the <code>ETag</code> response header value of the current resource from its RDF model.
     * 
     * @param model RDF model
     * @return entity tag
     */
    public EntityTag getEntityTag(Model model)
    {
        return new EntityTag(Long.toHexString(ModelUtils.hashModel(model)));
    }
    
    /**
     * Returns response builder for the given RDF graph.
     * 
     * @param model RDF dataset
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Model model)
    {
        return new com.atomgraph.core.model.impl.Response(getRequest(),
                model,
                getLastModified(model),
                getEntityTag(model),
                getWritableMediaTypes(Model.class),
                Collections.<Locale>emptyList(),
                Collections.<String>emptyList()).
            getResponseBuilder();
    }
    
    /**
     * Extract the <code>Last-Modified</code> response header value of the current resource from its RDF model.
     * 
     * @param dataset RDF dataset
     * @return date of last modification
     */
    public Date getLastModified(Dataset dataset)
    {
        return null;
    }

    /**
     * Generate the <code>ETag</code> response header value of the current resource from its RDF model.
     * 
     * @param dataset RDF dataset
     * @return entity tag
     */
    public EntityTag getEntityTag(Dataset dataset)
    {
        return new EntityTag(Long.toHexString(com.atomgraph.core.model.impl.Response.hashDataset(dataset)));
    }
    
    /**
     * Returns response builder for the given RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Dataset dataset)
    {
        return new com.atomgraph.core.model.impl.Response(getRequest(),
                dataset,
                getLastModified(dataset),
                getEntityTag(dataset),
                getWritableMediaTypes(Dataset.class),
                Collections.<Locale>emptyList(),
                Collections.<String>emptyList()).
            getResponseBuilder();
    }
    
    /**
     * Builds a list of acceptable response variants
     * 
     * @param mediaTypes
     * @return supported variants
     */
    public List<Variant> getVariants(List<MediaType> mediaTypes)
    {
        return com.atomgraph.core.model.impl.Response.getVariantListBuilder(mediaTypes, getLanguages(), getEncodings()).add().build();
    }
    
    public List<javax.ws.rs.core.MediaType> getWritableMediaTypes(Class clazz)
    {
        return getMediaTypes().getWritable(clazz);
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
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
    public final URI getURI()
    {
        return uri;
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
    
}