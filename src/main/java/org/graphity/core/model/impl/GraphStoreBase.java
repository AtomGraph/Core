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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.graphity.core.model.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for SPARQL Graph Stores.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.server.model.GraphStore
 */
public abstract class GraphStoreBase implements GraphStore
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreBase.class);

    private final Request request;
    private final ServletConfig servletConfig;
    
    public GraphStoreBase(@Context Request request, @Context ServletConfig servletConfig)
    {
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
	
	this.request = request;
        this.servletConfig = servletConfig;
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
                getResponseBuilder(model, getVariants());
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
        List<MediaType> list = org.graphity.core.MediaType.getRegistered();
        list.add(0, org.graphity.core.MediaType.APPLICATION_RDF_XML_TYPE); // first one becomes default
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
    
    @GET
    @Override
    public Response get(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);

	if (defaultGraph)
	{
	    Model model = getModel();
            if (log.isDebugEnabled()) log.debug("GET Graph Store default graph, returning Model of size(): {}", model.size());
	    return getResponse(model);
	}
	else
	{
	    Model model = getModel(graphUri.toString());
	    if (model == null)
	    {
		if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} not found", graphUri);
		return Response.status(Status.NOT_FOUND).build();
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} found, returning Model of size(): {}", graphUri, model.size());
		return getResponse(model);
	    }
	}
    }

    @POST
    @Override
    public Response post(Model model, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);
	if (log.isDebugEnabled()) log.debug("POST Graph Store request with RDF payload: {} payload size(): {}", model, model.size());
	
	if (model.isEmpty()) return Response.noContent().build();
	
	if (defaultGraph)
	{
	    if (log.isDebugEnabled()) log.debug("POST Model to default graph");
	    add(model);
	    return Response.ok().build();
	}
	else
	{
	    boolean existingGraph = containsModel(graphUri.toString());

	    // is this implemented correctly? The specification is not very clear.
	    if (log.isDebugEnabled()) log.debug("POST Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
	    add(graphUri.toString(), model);
	    
	    if (existingGraph) return Response.ok().build();
	    else return Response.created(graphUri).build();
	}
    }

    @PUT
    @Override
    public Response put(Model model, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);
	if (log.isDebugEnabled()) log.debug("PUT Graph Store request with RDF payload: {} payload size(): {}", model, model.size());
	
	if (defaultGraph)
	{
	    if (log.isDebugEnabled()) log.debug("PUT Model to default graph");
	    putModel(model);
	    return Response.ok().build();
	}
	else
	{
	    boolean existingGraph = containsModel(graphUri.toString());
	    
	    if (log.isDebugEnabled()) log.debug("PUT Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
	    putModel(graphUri.toString(), model);
	    
	    if (existingGraph) return Response.ok().build();
	    else return Response.created(graphUri).build();
	}	
    }

    @DELETE
    @Override
    public Response delete(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);
	
	if (defaultGraph)
	{
	    deleteDefault();
	    if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store");	    
	    return Response.noContent().build();
	}
	else
	{
	    if (!containsModel(graphUri.toString()))
	    {
		if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {}: not found", graphUri);
		return Response.status(Status.NOT_FOUND).build();
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("DELETE named graph with URI: {}", graphUri);
		deleteModel(graphUri.toString());
		return Response.noContent().build();
	    }
	}
    }

    public Request getRequest()
    {
	return request;
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
}