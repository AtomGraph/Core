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

import java.util.List;
import java.util.Locale;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;
import com.atomgraph.core.util.ModelUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the content negotiation logic used to build HTTP response from RDF an dataset, model, or result set.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see javax.ws.rs.core.Variant
 */
public class Response
{
    private static final Logger log = LoggerFactory.getLogger(Response.class);

    private final Request request;
    private final Object entity;
    private final Variant variant;
    private final EntityTag entityTag;

    /**
     * Builds model response from request.
     * 
     * @param request response entity
     * @param entity response dataset
     * @param mediaTypes supported media type
     * @param languages content languages
     * @param encodings content type encodings
     * @param entityTag entity tag
     */
    public Response(Request request, Object entity, EntityTag entityTag, List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings)
    {
        this(request, entity, entityTag, getVariantListBuilder(mediaTypes, languages, encodings).add().build());
    }

    /**
     * Builds model response from request.
     * 
     * @param request response entity
     * @param entity response dataset
     * @param variants media type variants
     * @param entityTag entity tag
     */
    public Response(Request request, Object entity, EntityTag entityTag, List<Variant> variants)
    {
        this(request, entity, entityTag, request.selectVariant(variants) != null ? request.selectVariant(variants) : request.selectVariant(removeLanguages(variants)));
    }

    public Response(Request request, Object entity, EntityTag entityTag, Variant variant)
    {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (entity == null) throw new IllegalArgumentException("Object cannot be null");
        if (entityTag == null) throw new IllegalArgumentException("EntityTag cannot be null");
        if (variant == null) throw new IllegalArgumentException("Variant cannot be null");
        
        if (variant == null)
        {
            if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants", variant);
            throw new WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE).build());
        }

        this.request = request;
        this.entity = entity;
        this.entityTag = entityTag;
        this.variant = variant;
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
    public static Variant.VariantListBuilder getVariantListBuilder(List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings)
    {
        return Variant.VariantListBuilder.newInstance().
                mediaTypes(mediaTypeListToArray(mediaTypes)).
                languages(localeListToArray(languages)).
                encodings(stringListToArray(encodings));
    }
    
    /**
     * Clones variants while stripping languages.
     * 
     * @param variants variant list
     * @return variant list
     */
    public static List<Variant> removeLanguages(List<Variant> variants)
    {
        if (variants == null) throw new IllegalArgumentException("List<Variant> cannot be null");
        
        List<Variant> list = new ArrayList<>();
        
        for (Variant variant : variants)
            list.add(new Variant(variant.getMediaType(), null, variant.getEncoding()));
        
        return list;
    }
    
    /**
     * Returns generic response builder.
     * 
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder()
    {
        // add variant hash to make it a strong ETag (i.e. the same RDF graph in different syntaxes produces different hashes)
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
        EntityTag newEntityTag = null;
        if (getEntityTag() != null)
        {
            BigInteger entityTagHash = new BigInteger(getEntityTag().getValue(), 16);
            entityTagHash = entityTagHash.add(BigInteger.valueOf(getVariant().hashCode()));
            newEntityTag = new EntityTag(entityTagHash.toString(16));
        }
        
        ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
        if (rb != null)
        {
            if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
            return rb.variant(getVariant()); // Jersey doesn't seem to set "Vary" header
        }
        else
        {
            if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", variant, entityTag);
            return javax.ws.rs.core.Response.ok(getEntity(), getVariant()).
                    tag(newEntityTag);
        }
    }
        
    /**
     * Calculates hash for an RDF dataset and a given response variant.
     * 
     * @param dataset RDF dataset
     * @return hash code
     */
    public static long hashDataset(Dataset dataset)
    {
        if (dataset == null) throw new IllegalArgumentException("Model cannot be null");
        
        long hash = ModelUtils.hashModel(dataset.getDefaultModel());
        
        Iterator<String> it = dataset.listNames();
        while (it.hasNext()) hash += ModelUtils.hashModel(dataset.getNamedModel(it.next()));
            
        return hash;
    }

    public Request getRequest()
    {
        return request;
    }
    
    public Object getEntity()
    {
        return entity;
    }
    
    public Variant getVariant()
    {
        return variant;
    }
    
    public EntityTag getEntityTag()
    {
        return entityTag;
    }
    
}
