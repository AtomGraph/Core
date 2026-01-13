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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.model.DatasetQuadAccessor;
import com.atomgraph.core.model.Service;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;

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
    
    /**
     * Constructs Graph Store from request metadata.
     * 
     * @param request request
     * @param service SPARQL service
     * @param mediaTypes supported media types
     */
    @Inject
    public QuadStoreImpl(@Context Request request, Service service, MediaTypes mediaTypes)
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
    }
    
    /**
     * Returns response for the given RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return response object
     */
    public jakarta.ws.rs.core.Response getResponse(Dataset dataset)
    {
        return getResponseBuilder(dataset).build();
    }

    /**
     * Returns response builder for the given RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return response builder
     */
    public jakarta.ws.rs.core.Response.ResponseBuilder getResponseBuilder(Dataset dataset)
    {
        return new com.atomgraph.core.model.impl.Response(getRequest(),
                dataset,
                null,
                getEntityTag(dataset),
                getWritableMediaTypes(Dataset.class),
                getLanguages(),
                getEncodings()).
            getResponseBuilder();
    }
    
    /**
     * Generate the <code>ETag</code> response header value of the current RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return hash value
     */
    public EntityTag getEntityTag(Dataset dataset)
    {
        return new EntityTag(Long.toHexString(com.atomgraph.core.model.impl.Response.hashDataset(dataset)));
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
    public jakarta.ws.rs.core.Response get()
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
    public jakarta.ws.rs.core.Response post(Dataset dataset)
    {
        getQuadDatasetAccessor().add(dataset);
        return jakarta.ws.rs.core.Response.ok().build();
    }

    /**
     * Implements PUT method of SPARQL Graph Store Protocol.
     * 
     * @param dataset RDF request body
     * @return response
     */    
    @PUT
    @Override
    public jakarta.ws.rs.core.Response put(Dataset dataset)
    {
        getQuadDatasetAccessor().replace(dataset);
        return jakarta.ws.rs.core.Response.ok().build();
    }

    /**
     * Implements DELETE method of SPARQL Graph Store Protocol.
     * 
     * @return response
     */
    @DELETE
    @Override
    public jakarta.ws.rs.core.Response delete()
    {
        getQuadDatasetAccessor().delete();
        return jakarta.ws.rs.core.Response.noContent().build();
    }

    public List<MediaType> getWritableMediaTypes(Class clazz)
    {
        return getMediaTypes().getWritable(clazz);
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
    
}
