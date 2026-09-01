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
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Variant;
import com.atomgraph.core.util.ModelUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import jakarta.ws.rs.NotAcceptableException;
import java.util.function.Predicate;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the content negotiation logic used to build HTTP response from RDF an dataset, model, or result set.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see jakarta.ws.rs.core.Variant
 */
public class Response
{
    private static final Logger log = LoggerFactory.getLogger(Response.class);
 
    private final Request request;
    private final Object entity;
    private final Date lastModified;
    private final EntityTag entityTag;
    private final Variant variant;

    /**
     * A predicate to decide if a media type is language-significant.
     * When true, the language is preserved in the ETag calculation.
     */
    private final Predicate<MediaType> isMediaTypeLangSignificant;

    /**
     * The languages the request accepts, in priority order.
     *
     * Distinct from the languages offered: a language-significant entity is rendered against the whole acceptable list,
     * falling back per value, so two requests selecting the same variant can still differ in content. Empty when the
     * caller does not supply it, in which case the entity tag ignores language as it did before.
     */
    private final List<Locale> acceptableLanguages;
    
    public Response(Request request, Object entity, Date lastModified, EntityTag entityTag, List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings)
    {
        this(request, entity, lastModified, entityTag, getVariants(mediaTypes, languages, encodings), mediaType -> false);
    }
    
    /**
     * Builds model response from request.
     * 
     * @param request response entity
     * @param entity response dataset
     * @param lastModified last modified date
     * @param mediaTypes supported media type
     * @param languages content languages
     * @param encodings content type encodings
     * @param entityTag entity tag
     * @param isMediaTypeLangSignificant predicate indicating if language is significant
     */
    public Response(Request request, Object entity, Date lastModified, EntityTag entityTag, List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings, Predicate<MediaType> isMediaTypeLangSignificant)
    {
        this(request, entity, lastModified, entityTag, mediaTypes, languages, encodings, isMediaTypeLangSignificant, List.of());
    }

    /**
     * Builds model response from request, carrying the languages the request accepts.
     *
     * Supplying them makes the entity tag distinguish representations that differ only by accepted language - a
     * language-significant entity is rendered against the whole list, not against the single language of the selected
     * variant. See the seven-argument variant constructor for why the variant alone is the wrong granularity.
     *
     * @param request response entity
     * @param entity response dataset
     * @param lastModified last modified date
     * @param entityTag entity tag
     * @param mediaTypes supported media types
     * @param languages content languages offered
     * @param encodings content type encodings
     * @param isMediaTypeLangSignificant predicate indicating if language is significant
     * @param acceptableLanguages languages the request accepts, in priority order
     */
    public Response(Request request, Object entity, Date lastModified, EntityTag entityTag, List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings, Predicate<MediaType> isMediaTypeLangSignificant, List<Locale> acceptableLanguages)
    {
        this(request, entity, lastModified, entityTag,
            selectVariant(request, getVariants(mediaTypes, languages, encodings, isMediaTypeLangSignificant)),
            isMediaTypeLangSignificant, acceptableLanguages);
    }

    /**
     * Builds model response from request.
     * 
     * @param request response entity
     * @param entity response dataset
     * @param lastModified last modified date
     * @param entityTag entity tag
     * @param variants media type variants
     * @param isMediaTypeLangSignificant predicate indicating if language is significant
     */
    public Response(Request request, Object entity, Date lastModified, EntityTag entityTag, List<Variant> variants, Predicate<MediaType> isMediaTypeLangSignificant)
    {
        this(request, entity, lastModified, entityTag, selectVariant(request, variants), isMediaTypeLangSignificant);
    }

    /**
     * Selects the response variant, falling back to a language-neutral representation when the request accepts none of
     * the offered languages.
     *
     * The language-neutral representations are offered alongside the language-specific ones in a single selection pass,
     * rather than retried in a second pass over a language-stripped list. <code>ContainerRequest.selectVariant</code>
     * overwrites its <code>varyValue</code> field on every call, and Jersey builds the <code>Vary</code> response header
     * from whatever the most recent call left behind - dropping the header entirely when that call matched nothing. A
     * second pass therefore published either a <code>Vary</code> with no <code>Accept-Language</code> dimension or no
     * <code>Vary</code> at all, advertising a cache key that ignores a language the entity was in fact negotiated over,
     * and leaving a shared cache free to serve one language's representation to a client that asked for another. Offering
     * both in one list keeps the dimension in <code>Vary</code> and still serves a representation when no offered
     * language is acceptable.
     *
     * @param request current request
     * @param variants variant list
     * @return selected variant, or null if not even a language-neutral representation is acceptable
     */
    protected static Variant selectVariant(Request request, List<Variant> variants)
    {
        List<Variant> offer = new ArrayList<>(variants);

        for (Variant languageNeutral : removeLanguages(variants))
            if (!offer.contains(languageNeutral)) offer.add(languageNeutral);

        return request.selectVariant(offer);
    }

    public Response(Request request, Object entity, Date lastModified, EntityTag entityTag, Variant variant, Predicate<MediaType> isMediaTypeLangSignificant) throws NotAcceptableException
    {
        this(request, entity, lastModified, entityTag, variant, isMediaTypeLangSignificant, List.of());
    }

    /**
     * Builds model response from a selected variant and the languages the request accepts.
     *
     * The acceptable languages are what a language-significant entity is actually rendered against - the renderer falls
     * back per value over the whole list - so they, not the selected variant's single language, are what makes one
     * representation different from another. Supplying them makes the entity tag distinguish those representations.
     *
     * @param request response entity
     * @param entity response dataset
     * @param lastModified last modified date
     * @param entityTag entity tag
     * @param variant selected variant
     * @param isMediaTypeLangSignificant predicate indicating if language is significant
     * @param acceptableLanguages languages the request accepts, in priority order
     */
    public Response(Request request, Object entity, Date lastModified, EntityTag entityTag, Variant variant, Predicate<MediaType> isMediaTypeLangSignificant, List<Locale> acceptableLanguages) throws NotAcceptableException
    {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (entity == null) throw new IllegalArgumentException("Object cannot be null");
        if (variant == null)
        {
            if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants", variant);
            throw new NotAcceptableException();
        }

        this.request = request;
        this.entity = entity;
        this.lastModified = lastModified;
        this.entityTag = entityTag;
        this.variant = variant;
        this.isMediaTypeLangSignificant = isMediaTypeLangSignificant;
        this.acceptableLanguages = acceptableLanguages;
    }

    public static List<Variant> getVariants(List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings)
    {
        return getVariants(mediaTypes, languages, encodings, mediaType -> false);
    }

    /**
     * Builds the list of Variants based on the provided media types, languages, and encodings.
     * For media types that are language-significant (as determined by isMediaTypeLangSignificant), the provided languages are included.
     * Otherwise, the language is ignored.
     *
     * @param mediaTypes the list of media types
     * @param languages the list of locales
     * @param encodings the list of encodings
     * @param isMediaTypeLangSignificant determines whether language is significant for given media type
     * @return a list of Variants to be used for content negotiation
     */
    public static List<Variant> getVariants(List<MediaType> mediaTypes, List<Locale> languages, List<String> encodings, Predicate<MediaType> isMediaTypeLangSignificant)
    {
        Variant.VariantListBuilder builder = Variant.VariantListBuilder.newInstance();

        for (MediaType mediaType : mediaTypes)
        {
            // Determine whether to include languages based on the predicate.
            if (isMediaTypeLangSignificant.test(mediaType))
            {
                if (languages != null && !languages.isEmpty())
                {
                    for (Locale locale : languages)
                    {
                        if (encodings != null && !encodings.isEmpty())
                        {
                            for (String encoding : encodings)
                            {
                                builder.mediaTypes(mediaType)
                                       .languages(locale)
                                       .encodings(encoding)
                                       .add();
                            }
                        }
                        else
                        {
                            builder.mediaTypes(mediaType)
                                   .languages(locale)
                                   .add();
                        }
                    }
                }
                else
                {
                    // Media type is language-significant but no locales provided.
                    if (encodings != null && !encodings.isEmpty())
                    {
                        for (String encoding : encodings)
                        {
                            builder.mediaTypes(mediaType)
                                   .languages((Locale) null)
                                   .encodings(encoding)
                                   .add();
                        }
                    }
                    else
                    {
                        builder.mediaTypes(mediaType)
                               .languages((Locale) null)
                               .add();
                    }
                }
            }
            else
            {
                // For non-language-significant media types, always set language to null.
                if (encodings != null && !encodings.isEmpty())
                {
                    for (String encoding : encodings)
                    {
                        builder.mediaTypes(mediaType)
                               .languages((Locale) null)
                               .encodings(encoding)
                               .add();
                    }
                }
                else
                {
                    builder.mediaTypes(mediaType)
                           .languages((Locale) null)
                           .add();
                }
            }
        }
        
        // Build and return the list of variants.
        return builder.build();
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
            list.add(new Variant(variant.getMediaType(), (Locale)null, variant.getEncoding()));
        
        return list;
    }
    
    /**
     * Evaluates request preconditions.
     * 
     * @return response builder or null if the preconditions were met
     */
    public ResponseBuilder evaluatePreconditions()
    {
        return evaluatePreconditions(getLastModified(), getVariantEntityTag());
    }
    
    /**
     * Returns generic response builder.
     * 
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder()
    {
        return getResponseBuilder(getLastModified(), getVariantEntityTag());
    }
    
    /**
     * Evaluates request preconditions based on last modified date and/or entity tag.
     * 
     * @param lastModified last modified date
     * @param entityTag entity tag
     * @return response builder or null if the preconditions were met
     */
    protected ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag entityTag)
    {
        final ResponseBuilder rb;
        
        if (lastModified != null && entityTag != null) rb = getRequest().evaluatePreconditions(lastModified, entityTag);
        else
        {
            if (lastModified != null) rb = getRequest().evaluatePreconditions(lastModified);
            else
            {
                if (entityTag != null) rb = getRequest().evaluatePreconditions(entityTag);
                else rb = getRequest().evaluatePreconditions();
            }
        }
        
        return rb;
    }
    
    /**
     * Returns generic response builder from last modified date and/or entity tag.
     * 
     * @param lastModified last modified date
     * @param entityTag entity tag
     * @return response builder
     */
    protected ResponseBuilder getResponseBuilder(Date lastModified, EntityTag entityTag)
    {
        final ResponseBuilder rb = evaluatePreconditions(lastModified, entityTag);

        if (rb != null)
        {
            if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
            return rb.variant(getVariant()). // Jersey doesn't seem to set "Vary" header
                lastModified(lastModified); // if rb != null, Jersey sets ETag but not Last-Modified
        }
        else
        {
            if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", getVariant(), entityTag);
            return jakarta.ws.rs.core.Response.ok(getEntity(), getVariant()). // status will be overriden if necessary
                lastModified(lastModified).
                tag(entityTag);
        }
    }

    /**
     * Calculates variant-specific (strong) <code>ETag</code> value by adding variant hash to the content hash.
     * As a result, the same RDF graph in different syntaxes produces different <code>ETag</code>s.
     * 
     * @return entity tag
     */
    public EntityTag getVariantEntityTag()
    {
        if (getEntityTag() != null)
        {
            BigInteger entityTagHash = new BigInteger(getEntityTag().getValue(), 16);
            BigInteger variantHash = BigInteger.valueOf(getVariant().hashCode());
            entityTagHash = entityTagHash.add(variantHash);

            // a language-significant entity is rendered against the whole acceptable-language list, not the one language the
            // selected variant carries. Two requests selecting the same language-neutral variant still differ: with "lt" and
            // "de" against an offer of English, one renders the Lithuanian values the data holds and the other falls back to
            // English. Hashing the variant alone gave those two representations one strong ETag, so a conditional request
            // could be answered 304 with the wrong language
            if (!getAcceptableLanguages().isEmpty() && getIsMediaTypeLangSignificant().test(getVariant().getMediaType()))
                entityTagHash = entityTagHash.add(BigInteger.valueOf(getAcceptableLanguages().hashCode()));

            return new EntityTag(entityTagHash.toString(16));
        }
        
        return null;
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

    public Predicate<MediaType> getIsMediaTypeLangSignificant()
    {
        return isMediaTypeLangSignificant;
    }

    /**
     * Returns the languages the request accepts, in priority order, or an empty list when the caller did not supply them.
     *
     * @return acceptable languages
     */
    public List<Locale> getAcceptableLanguages()
    {
        return acceptableLanguages;
    }
    
    public Request getRequest()
    {
        return request;
    }
    
    public Object getEntity()
    {
        return entity;
    }

    public EntityTag getEntityTag()
    {
        return entityTag;
    }

    public Date getLastModified()
    {
        return lastModified;
    }
    
    public Variant getVariant()
    {
        return variant;
    }
    
}
