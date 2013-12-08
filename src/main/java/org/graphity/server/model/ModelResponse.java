/*
 * Copyright (C) 2013 Martynas
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

package org.graphity.server.model;

import com.hp.hpl.jena.rdf.model.Model;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.graphity.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas
 */
public class ModelResponse // extends Response
{
    private static final Logger log = LoggerFactory.getLogger(ModelResponse.class);

    private final Request request;
    //private final CacheControl cacheControl;
    
    protected ModelResponse(Request request)
    {
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
        this.request = request;
        //this.cacheControl = cacheControl;
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
    public Response getResponse(Model model)
    {
	return getResponseBuilder(model).build();
    }

    public Response.ResponseBuilder getResponseBuilder(Model model)
    {
	return getResponseBuilder(model, getVariants());
    }
    
    public Response.ResponseBuilder getResponseBuilder(Model model, List<Variant> variants)
    {
	return getResponseBuilder(getEntityTag(model), model, variants);
    }

    public Response.ResponseBuilder getResponseBuilder(EntityTag entityTag, Object entity, List<Variant> variants)
    {	
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb;
	}
	else
	{
	    Variant variant = getRequest().selectVariant(variants);
	    if (variant == null)
	    {
		if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, getVariants());
		return Response.notAcceptable(variants);
	    }	
	    else
	    {
		if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", variant, entityTag);
		return Response.ok(entity, variant).
			tag(entityTag);
	    }
	}	
    }

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

    public EntityTag getEntityTag(Model model)
    {
        return new EntityTag(Long.toHexString(ModelUtils.hashModel(model)));
    }

}
