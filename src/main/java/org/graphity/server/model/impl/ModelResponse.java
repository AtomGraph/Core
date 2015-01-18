/**
 *  Copyright 2014 Martynas Jusevičius <martynas@graphity.org>
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

import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;
import org.graphity.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the content negotiation logic used to build HTTP response from RDF model.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see com.hp.hpl.jena.rdf.model.Model
 * @see javax.ws.rs.core.Variant
 */
public class ModelResponse // extends ResponseBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ModelResponse.class);

    private final Request request;
    
    /**
     * Builds model response from request.
     * 
     * @param request current request
     */
    protected ModelResponse(Request request)
    {
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
        this.request = request;
    }

    public Request getRequest()
    {
        return request;
    }
    
    public static ModelResponse fromRequest(Request request)
    {
	return new ModelResponse(request);
    }
    
    /**
     * Returns response builder for RDF model.
     * 
     * @param model RDF model
     * @param variants supported response variants
     * @return 
     */
    public ResponseBuilder getResponseBuilder(Model model, List<Variant> variants)
    {
	if (variants == null) throw new IllegalArgumentException("List<Variant> cannot be null");

        Variant variant = getRequest().selectVariant(variants);
        if (variant == null)
        {
            if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, variants);
            throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).build());
        }

        EntityTag entityTag = getEntityTag(model, variant);
        ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb.variant(variant); // Jersey doesn't seem to set "Vary" header
	}
	else
	{
            if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", variant, entityTag);
            return Response.ok(model, variant).
                    tag(entityTag);
	}	
    }
    
    public ResponseBuilder getResponseBuilder(Model model, Locale[] languages, String[] encodings)
    {
        return getResponseBuilder(model, getVariants(org.graphity.server.MediaType.getRegistered(), languages, encodings));
    }
    
    public ResponseBuilder getResponseBuilder(Model model, MediaType[] mediaTypes, Locale[] languages, String[] encodings)
    {
        return getResponseBuilder(model, getVariants(mediaTypes, languages, encodings));
    }
    
    /**
     * Builds a list of acceptable response variants
     * 
     * @param mediaTypes
     * @param languages
     * @param encodings
     * @return supported variants
     */
    public List<Variant> getVariants(MediaType[] mediaTypes, Locale[] languages, String[] encodings)
    {
        return getVariantListBuilder(mediaTypes, languages, encodings).add().build();
    }
    
    /**
     * Produces a Variant builder from a list of media types.
     * 
     * @param mediaTypes
     * @param languages
     * @param encodings
     * @return variant builder
     */    
    public Variant.VariantListBuilder getVariantListBuilder(MediaType[] mediaTypes, Locale[] languages, String[] encodings)
    {        
        return Variant.VariantListBuilder.newInstance().
                mediaTypes(mediaTypes).
                languages(languages).
                encodings(encodings);
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
    
}
