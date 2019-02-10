/*
 * Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.core.model.impl;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.QuadStore;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public abstract class QuadStoreBase implements QuadStore
{
    private static final Logger log = LoggerFactory.getLogger(QuadStoreBase.class);

    private final Request request;
    private final MediaTypes mediaTypes;
    private final com.atomgraph.core.model.impl.Response response;
    
    /**
     * Constructs Graph Store from request metadata.
     * 
     * @param request request
     * @param mediaTypes
     */
    public QuadStoreBase(@Context Request request, @Context MediaTypes mediaTypes)
    {
        // if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");
        
        this.request = request;
        this.mediaTypes = mediaTypes;
        this.response = request != null ? com.atomgraph.core.model.impl.Response.fromRequest(request) : null;
    }
    
    /**
     * Returns response for the given RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return response object
     */
    public javax.ws.rs.core.Response getResponse(Dataset dataset)
    {
        return getResponseBuilder(dataset).build();
    }

    /**
     * Returns response builder for the given RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return response builder
     */
    public javax.ws.rs.core.Response.ResponseBuilder getResponseBuilder(Dataset dataset)
    {
        return com.atomgraph.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(dataset, getVariants(getMediaTypes().getWritable(Dataset.class)));
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
    public javax.ws.rs.core.Response get(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        if (defaultGraph)
        {
            Dataset dataset = DatasetFactory.create(getModel());
            if (log.isDebugEnabled()) log.debug("GET Graph Store default graph, returning Model of size(): {}", dataset.getDefaultModel().size());
            return getResponse(dataset);
        }
        
        if (graphUri != null)
        {
            Dataset dataset = DatasetFactory.create(getModel(graphUri.toString()));
            if (dataset == null)
            {
                if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} not found", graphUri);
                return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
            }
            else
            {
                if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} found, returning Model of size(): {}", graphUri, dataset.getDefaultModel().size());
                return getResponse(dataset);
            }
        }
        
        return getResponse(get());
    }

    /**
     * Implements POST method of SPARQL Graph Store Protocol.
     * 
     * @param dataset RDF request body
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */
    @POST
    @Override
    public javax.ws.rs.core.Response post(Dataset dataset, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        if (defaultGraph)
        {
            if (log.isDebugEnabled()) log.debug("POST Model to default graph");
            add(dataset.getDefaultModel());
            return javax.ws.rs.core.Response.ok().build();
        }

        if (graphUri != null)
        {
            boolean existingGraph = containsModel(graphUri.toString());

            // is this implemented correctly? The specification is not very clear.
            if (log.isDebugEnabled()) log.debug("POST Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
            add(graphUri.toString(), dataset.getDefaultModel());
            
            if (existingGraph) return javax.ws.rs.core.Response.ok().build();
            else return javax.ws.rs.core.Response.created(graphUri).build();
        }
        
        add(dataset);
        return javax.ws.rs.core.Response.ok().build();
    }

    /**
     * Implements PUT method of SPARQL Graph Store Protocol.
     * 
     * @param dataset RDF request body
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */    
    @PUT
    @Override
    public javax.ws.rs.core.Response put(Dataset dataset, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        if (defaultGraph)
        {
            if (log.isDebugEnabled()) log.debug("PUT Model to default graph");
            putModel(dataset.getDefaultModel());
            return javax.ws.rs.core.Response.ok().build();
        }
        
        if (graphUri != null)
        {
            boolean existingGraph = containsModel(graphUri.toString());
            
            if (log.isDebugEnabled()) log.debug("PUT Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
            putModel(graphUri.toString(), dataset.getDefaultModel());
            
            if (existingGraph) return javax.ws.rs.core.Response.ok().build();
            else return javax.ws.rs.core.Response.created(graphUri).build();
        }
        
        replace(dataset);
        return javax.ws.rs.core.Response.ok().build();
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
    public javax.ws.rs.core.Response delete(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        if (defaultGraph)
        {
            deleteDefault();
            if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store");
            return javax.ws.rs.core.Response.noContent().build();
        }
        
        if (graphUri != null)
        {
            if (!containsModel(graphUri.toString()))
            {
                if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {}: not found", graphUri);
                return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
            }
            else
            {
                if (log.isDebugEnabled()) log.debug("DELETE named graph with URI: {}", graphUri);
                deleteModel(graphUri.toString());
                return javax.ws.rs.core.Response.noContent().build();
            }
        }
        
        delete();
        return javax.ws.rs.core.Response.noContent().build();
    }

    public Request getRequest()
    {
        return request;
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
