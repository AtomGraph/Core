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
import org.graphity.core.MediaTypes;
import org.graphity.core.model.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of SPARQL Graph Stores.
 * Unfortunately cannot extend ResourceBase because of clashing JAX-RS method annotations.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.core.model.GraphStore
 */
public abstract class GraphStoreBase implements GraphStore
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreBase.class);

    private final Request request;
    private final ServletConfig servletConfig;
    private final MediaTypes mediaTypes;
    private final org.graphity.core.model.impl.Response response;
    
    /**
     * Constructs Graph Store from request metadata.
     * 
     * @param request request
     * @param servletConfig servlet config
     * @param mediaTypes
     */
    public GraphStoreBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes)
    {
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
	if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");
	
	this.request = request;
        this.servletConfig = servletConfig;
        this.mediaTypes = mediaTypes;
        this.response = org.graphity.core.model.impl.Response.fromRequest(request);        
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
     * Builds a list of acceptable response variants.
     * 
     * @return supported variants
     */
    public List<Variant> getVariants()
    {
        return getVariantListBuilder().add().build();
    }
    
    /**
     * Returns a builder object for supported response variants.
     * 
     * @return variant builder
     */
    public Variant.VariantListBuilder getVariantListBuilder()
    {
        return getResponse().getVariantListBuilder(getMediaTypes().forClass(Model.class), getLanguages(), getEncodings());
    }
        
    /**
     * Returns a list of supported languages.
     * 
     * @return list of languages
     */
    public List<Locale> getLanguages()
    {
        return new ArrayList<>();
    }

    /**
     * Returns a list of supported HTTP encodings.
     * Note: this is different from content encodings such as UTF-8.
     * 
     * @return list of encodings
     */
    public List<String> getEncodings()
    {
        return new ArrayList<>();
    }
    
    /**
     * Implements GET method of SPARQL Graph Store Protocol.
     * 
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */
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

    /**
     * Implements POST method of SPARQL Graph Store Protocol.
     * 
     * @param model RDF request body
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */
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

    /**
     * Implements PUT method of SPARQL Graph Store Protocol.
     * 
     * @param model RDF request body
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */    
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

    /**
     * Implements DELETE method of SPARQL Graph Store Protocol.
     * 
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */
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
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    public org.graphity.core.model.impl.Response getResponse()
    {
        return response;
    }
    
}