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
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.GraphStore;
import org.apache.jena.query.DatasetAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of SPARQL Graph Stores.
 * Unfortunately cannot extend ResourceBase because of clashing JAX-RS method annotations.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see com.atomgraph.core.model.GraphStore
 */
public class GraphStoreBase implements GraphStore
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreBase.class);

    private final Request request;
    private final DatasetAccessor accessor;
    private final MediaTypes mediaTypes;
    private final com.atomgraph.core.model.impl.Response response;
    
    /**
     * Constructs Graph Store from request metadata.
     * 
     * @param request request
     * @param accessor dataset accessor
     * @param mediaTypes supported media types
     */
    public GraphStoreBase(@Context Request request, @Context DatasetAccessor accessor, @Context MediaTypes mediaTypes)
    {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (accessor == null) throw new IllegalArgumentException("DatasetAccessor cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");
        
        this.request = request;
        this.accessor = accessor;
        this.mediaTypes = mediaTypes;
        this.response = com.atomgraph.core.model.impl.Response.fromRequest(request);
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
        return com.atomgraph.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(model, getVariants(getMediaTypes().getWritable(Model.class)));
    }
    
    /**
     * Builds a list of acceptable response variants
     * 
     * @param mediaTypes
     * @return supported variants
     */
    public List<Variant> getVariants(List<MediaType> mediaTypes)
    {
        return getResponse().getVariantListBuilder(mediaTypes, getLanguages(), getEncodings()).add().build();
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
            Model model = getDatasetAccessor().getModel();
            if (log.isDebugEnabled()) log.debug("GET Graph Store default graph, returning Model of size(): {}", model.size());
            return getResponse(model);
        }
        else
        {
            Model model = getDatasetAccessor().getModel(graphUri.toString());
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
            getDatasetAccessor().add(model);
            return Response.ok().build();
        }
        else
        {
            boolean existingGraph = getDatasetAccessor().containsModel(graphUri.toString());

            // is this implemented correctly? The specification is not very clear.
            if (log.isDebugEnabled()) log.debug("POST Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
            getDatasetAccessor().add(graphUri.toString(), model);
            
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
            getDatasetAccessor().putModel(model);
            return Response.ok().build();
        }
        else
        {
            boolean existingGraph = getDatasetAccessor().containsModel(graphUri.toString());
            
            if (log.isDebugEnabled()) log.debug("PUT Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
            getDatasetAccessor().putModel(graphUri.toString(), model);
            
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
            getDatasetAccessor().deleteDefault();
            if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store");
            return Response.noContent().build();
        }
        else
        {
            if (!getDatasetAccessor().containsModel(graphUri.toString()))
            {
                if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {}: not found", graphUri);
                return Response.status(Status.NOT_FOUND).build();
            }
            else
            {
                if (log.isDebugEnabled()) log.debug("DELETE named graph with URI: {}", graphUri);
                getDatasetAccessor().deleteModel(graphUri.toString());
                return Response.noContent().build();
            }
        }
    }

    public Request getRequest()
    {
        return request;
    }
    
    public DatasetAccessor getDatasetAccessor()
    {
        return accessor;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    public com.atomgraph.core.model.impl.Response getResponse()
    {
        return response;
    }
    
}