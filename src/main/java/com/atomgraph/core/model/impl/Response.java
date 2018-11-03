/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
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

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;
import com.atomgraph.core.util.ModelUtils;
import com.atomgraph.core.util.ResultSetUtils;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the content negotiation logic used to build HTTP response from RDF model.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see org.apache.jena.rdf.model.Model
 * @see javax.ws.rs.core.Variant
 */
public class Response // extends ResponseBuilder
{
    private static final Logger log = LoggerFactory.getLogger(Response.class);

    private final Request request;
    
    /**
     * Builds model response from request.
     * 
     * @param request current request
     */
    protected Response(Request request)
    {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        this.request = request;
    }

    public Request getRequest()
    {
        return request;
    }
    
    public static Response fromRequest(Request request)
    {
        return new Response(request);
    }

    public static MediaType[] mediaTypeListToArray(List<MediaType> list)
    {
        if (list == null) throw new IllegalArgumentException("List cannot be null");
        
        MediaType[] array = new MediaType[list.size()];
        list.toArray(array);
        return array;
    }

    public static Locale[] localeListToArray(List<Locale> list)
    {
        if (list == null) throw new IllegalArgumentException("List cannot be null");
        
        Locale[] array = new Locale[list.size()];
        list.toArray(array);
        return array;
    }

    public static String[] stringListToArray(List<String> list)
    {
        if (list == null) throw new IllegalArgumentException("List cannot be null");
        
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
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
                mediaTypes(mediaTypeListToArray(mediaTypes)).
                languages(localeListToArray(languages)).
                encodings(stringListToArray(encodings));
    }
    
    /**
     * Returns response builder for RDF model.
     * 
     * @param model RDF model
     * @param variants supported response variants
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Model model, List<Variant> variants)
    {
        if (variants == null) throw new IllegalArgumentException("List<Variant> cannot be null");

        Variant variant = getRequest().selectVariant(variants);
        if (variant == null)
        {
            variant = getRequest().selectVariant(removeLanguages(variants));
            if (log.isTraceEnabled()) log.trace("Conneg did not produce acceptable response Variants; attempting conneg without language");
            
            if (variant == null)
            {
                if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, variants);
                throw new WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE).build());
            }
        }

        return getResponseBuilder(model, getEntityTag(model, variant), variant);
    }

    /**
     * Clones variants while stripping languages.
     * 
     * @param variants variant list
     * @return variant list
     */
    public List<Variant> removeLanguages(List<Variant> variants)
    {
        if (variants == null) throw new IllegalArgumentException("List<Variant> cannot be null");
        
        List<Variant> list = new ArrayList<>();
        
        for (Variant variant : variants)
            list.add(new Variant(variant.getMediaType(), null, variant.getEncoding()));
        
        return list;
    }
    
    /**
     * Returns response builder for SPARQL result set.
     * 
     * @param resultSet result set
     * @param variants supported response variants
     * @return response builder
     */    
    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet, List<Variant> variants)
    {
        if (resultSet == null) throw new IllegalArgumentException("ResultSetRewindable cannot be null");
        if (variants == null) throw new IllegalArgumentException("List<Variant> cannot be null");
        
        Variant variant = getRequest().selectVariant(variants);
        if (variant == null)
        {
            if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, variants);
            return javax.ws.rs.core.Response.notAcceptable(variants);
        }

        resultSet.reset();
        EntityTag entityTag = getEntityTag(resultSet, variant);
        resultSet.reset(); // ResultSet needs to be rewinded back to the beginning
        return getResponseBuilder(resultSet, entityTag, variant);
    }

    /**
     * Returns generic response builder.
     * 
     * @param entity response entity
     * @param entityTag entity tag
     * @param variant response variant
     * @return response builder
     */        
    public ResponseBuilder getResponseBuilder(Object entity, EntityTag entityTag, Variant variant)
    {
        if (entity == null) throw new IllegalArgumentException("Object cannot be null");
        if (entityTag == null) throw new IllegalArgumentException("EntityTag cannot be null");
        if (variant == null) throw new IllegalArgumentException("Variant cannot be null");

        ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
        if (rb != null)
        {
            if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
            return rb.variant(variant); // Jersey doesn't seem to set "Vary" header
        }
        else
        {
            if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", variant, entityTag);
            return javax.ws.rs.core.Response.ok(entity, variant).
                    tag(entityTag);
        }
    }
        
    /**
     * Calculates hash for an RDF model and a given response variant.
     * 
     * @param model RDF model
     * @param variant response variant
     * @return hash code
     */
    public long getModelVariantHash(Model model, Variant variant)
    {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        if (variant == null) throw new IllegalArgumentException("Variant cannot be null");
        
        return ModelUtils.hashModel(model) + variant.hashCode();
    }

    /**
     * Calculates hash for a SPARQL result set and a given response variant.
     * 
     * @param resultSet result set
     * @param variant response variant
     * @return hash code
     */    
    public long getResultSetVariantHash(ResultSet resultSet, Variant variant)
    {
        if (resultSet == null) throw new IllegalArgumentException("ResultSet cannot be null");
        if (variant == null) throw new IllegalArgumentException("Variant cannot be null");

        return ResultSetUtils.hashResultSet(resultSet) + variant.hashCode();
    }
    
    /**
     * Calculates ETag for an RDF model and a given response variant.
     * 
     * @param model RDF model
     * @param variant response variant
     * @return entity tag object
     */
    public EntityTag getEntityTag(Model model, Variant variant)
    {
        return new EntityTag(Long.toHexString(getModelVariantHash(model, variant)));
    }

    /**
     * Calculates ETag for a SPARQL result set and a given response variant.
     * 
     * @param resultSet result set
     * @param variant response variant
     * @return entity tag object
     */    
    public EntityTag getEntityTag(ResultSet resultSet, Variant variant)
    {
        return new EntityTag(Long.toHexString(getResultSetVariantHash(resultSet, variant)));
    }

}
