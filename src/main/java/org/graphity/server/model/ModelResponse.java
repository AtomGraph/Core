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

package org.graphity.server.model;

import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
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
 * @author Martynas
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
        //this.responseBuilder = ResponseBuilder.newInstance();
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
     * Builds response of an RDF model
     * 
     * @param model RDF model
     * @return response with the representation of the model
     */
    /*
    public Response getResponse(Model model)
    {
	return getResponseBuilder(model).build();
    }

    public ResponseBuilder getResponseBuilder(Model model)
    {
	return getResponseBuilder(model, getVariants());
    }
    */
    
    public ResponseBuilder getResponseBuilder(Model model, List<Variant> variants)
    {
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

    /*
    // http://stackoverflow.com/questions/5647570/content-type-when-accept-header-is-empty-or-unknown-jax-rs
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
    */
    
    public long getModelVariantHash(Model model, Variant variant)
    {
        return ModelUtils.hashModel(model) + variant.hashCode();
    }
    
    public EntityTag getEntityTag(Model model, Variant variant)
    {
        return new EntityTag(Long.toHexString(getModelVariantHash(model, variant)));
    }

    /*
    public ResponseBuilder getResponseBuilder()
    {
        return responseBuilder;
    }

    @Override
    public Response build()
    {
        return getResponseBuilder().build();
    }

    @Override
    public ResponseBuilder clone()
    {
        return getResponseBuilder().clone();
    }

    @Override
    public ResponseBuilder status(int i)
    {
        return getResponseBuilder().status(i);
    }

    @Override
    public ResponseBuilder entity(Object o)
    {
        return getResponseBuilder().entity(o);
    }

    @Override
    public ResponseBuilder type(MediaType mt)
    {
        return getResponseBuilder().type(mt);
    }

    @Override
    public ResponseBuilder type(String string)
    {
        return getResponseBuilder().type(string);
    }

    @Override
    public ResponseBuilder variant(Variant vrnt)
    {
        return getResponseBuilder().variant(vrnt);
    }

    @Override
    public ResponseBuilder variants(List<Variant> list)
    {
        return getResponseBuilder().variants(list);
    }

    @Override
    public ResponseBuilder language(String string)
    {
        return getResponseBuilder().language(string);
    }

    @Override
    public ResponseBuilder language(Locale locale)
    {
        return getResponseBuilder().language(locale);
    }

    @Override
    public ResponseBuilder location(URI uri)
    {
        return getResponseBuilder().location(uri);
    }

    @Override
    public ResponseBuilder contentLocation(URI uri)
    {
        return getResponseBuilder().contentLocation(uri);
    }

    @Override
    public ResponseBuilder tag(EntityTag et)
    {
        return getResponseBuilder().tag(et);
    }

    @Override
    public ResponseBuilder tag(String string)
    {
        return getResponseBuilder().tag(string);
    }

    @Override
    public ResponseBuilder lastModified(Date date)
    {
        return getResponseBuilder().lastModified(date);
    }

    @Override
    public ResponseBuilder cacheControl(CacheControl cc)
    {
        return getResponseBuilder().cacheControl(cc);
    }

    @Override
    public ResponseBuilder expires(Date date)
    {
        return getResponseBuilder().expires(date);
    }

    @Override
    public ResponseBuilder header(String string, Object o)
    {
        return getResponseBuilder().header(string, o);
    }

    @Override
    public ResponseBuilder cookie(NewCookie... ncs)
    {
        return getResponseBuilder().cookie(ncs);
    }
    */
}
