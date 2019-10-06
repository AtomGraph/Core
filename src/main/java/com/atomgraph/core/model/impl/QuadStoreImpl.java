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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.model.DatasetQuadAccessor;
import com.atomgraph.core.model.Service;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class QuadStoreImpl implements QuadStore
{
    private static final Logger log = LoggerFactory.getLogger(QuadStoreImpl.class);

    private final Request request;
    private final DatasetQuadAccessor accessor;
    private final MediaTypes mediaTypes;
    private final com.atomgraph.core.model.impl.Response response;
    
    /**
     * Constructs Graph Store from request metadata.
     * 
     * @param request request
     * @param service SPARQL service
     * @param mediaTypes supported media types
     */
    public QuadStoreImpl(@Context Request request, @Context Service service, @Context MediaTypes mediaTypes)
    {
        this(request, service.getDatasetQuadAccessor(), mediaTypes);
    }
    
    public QuadStoreImpl(Request request, DatasetQuadAccessor accessor, MediaTypes mediaTypes)
    {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (accessor == null) throw new IllegalArgumentException("DatasetQuadAccessor cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");
        
        this.request = request;
        this.accessor = accessor;
        this.mediaTypes = mediaTypes;
        this.response = com.atomgraph.core.model.impl.Response.fromRequest(request);
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
     * @return response
     */
    @GET
    @Override
    public javax.ws.rs.core.Response get()
    {
        return getResponse(getQuadDatasetAccessor().get());
    }

    /**
     * Implements POST method of SPARQL Graph Store Protocol.
     * 
     * @param dataset RDF request body
     * @return response
     */
    @POST
    @Override
    public javax.ws.rs.core.Response post(Dataset dataset)
    {
        getQuadDatasetAccessor().add(dataset);
        return javax.ws.rs.core.Response.ok().build();
    }

    /**
     * Implements PUT method of SPARQL Graph Store Protocol.
     * 
     * @param dataset RDF request body
     * @return response
     */    
    @PUT
    @Override
    public javax.ws.rs.core.Response put(Dataset dataset)
    {
        getQuadDatasetAccessor().replace(dataset);
        return javax.ws.rs.core.Response.ok().build();
    }

    /**
     * Implements DELETE method of SPARQL Graph Store Protocol.
     * 
     * @return response
     */
    @DELETE
    @Override
    public javax.ws.rs.core.Response delete()
    {
        getQuadDatasetAccessor().delete();
        return javax.ws.rs.core.Response.noContent().build();
    }

    public Request getRequest()
    {
        return request;
    }
    
    public DatasetQuadAccessor getQuadDatasetAccessor()
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
