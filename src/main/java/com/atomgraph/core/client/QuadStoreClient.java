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
package com.atomgraph.core.client;

import com.atomgraph.core.MediaTypes;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.model.DatasetQuadAccessor;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;

/**
 * Quad Store client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://lists.w3.org/Archives/Public/public-sparql-dev/2014AprJun/0008.html">Extending SPARQL Graph Store HTTP Protocol with quad semantics</a>
 */
public class QuadStoreClient extends EndpointClientBase implements DatasetQuadAccessor
{
    private static final Logger log = LoggerFactory.getLogger(QuadStoreClient.class);
    
    public QuadStoreClient(MediaTypes mediaTypes, WebTarget endpoint)
    {
        super(mediaTypes, endpoint);
    }

    public QuadStoreClient(WebTarget endpoint)
    {
        this(new MediaTypes(), endpoint);
    }

    public static QuadStoreClient create(MediaTypes mediaTypes, WebTarget endpoint)
    {
        return new QuadStoreClient(mediaTypes, endpoint);
    }

    public static QuadStoreClient create(WebTarget endpoint)
    {
        return new QuadStoreClient(endpoint);
    }
    
    /**
     * Registers client filter.
     * Can cause performance problems with <code>ApacheConnector</code>.
     * 
     * @param filter client request filter
     * @return this SPARQL client
     * @see <a href="https://blogs.oracle.com/japod/how-to-use-jersey-client-efficiently">How To Use Jersey Client Efficiently</a>
     */
    @Override
    public QuadStoreClient register(ClientRequestFilter filter)
    {
        if (filter == null) throw new IllegalArgumentException("ClientRequestFilter cannot be null");

        super.register(filter);

        return this;
    }
    
    @Override
    public MediaType getDefaultMediaType()
    {
        return com.atomgraph.core.MediaType.TEXT_NQUADS_TYPE;
    }
    
    @Override
    public Dataset get()
    {
        try (Response cr = get(getReadableMediaTypes(Dataset.class)))
        {
            return cr.readEntity(Dataset.class);
        }
    }
    
    @Override
    public void add(Dataset dataset)
    {
        try (Response response = post(dataset, getDefaultMediaType(), new MediaType[]{}))
        {
            // Response automatically closed by try-with-resources
        }
    }
    
    @Override
    public void replace(Dataset dataset)
    {
        try (Response response = put(dataset, getDefaultMediaType(), new MediaType[]{}))
        {
            // Response automatically closed by try-with-resources
        }
    }
    
    @Override
    public void delete()
    {
        try (Response response = delete(new MediaType[]{}))
        {
            // Response automatically closed by try-with-resources
        }
    }

    @Override
    public void patch(Dataset dataset)
    {
        try (Response response = patch(dataset, new MultivaluedHashMap()))
        {
            // Response automatically closed by try-with-resources
        }
    }
    
    public Response patch(Dataset dataset, MultivaluedMap<String, String> params)
    {
        WebTarget target = applyParams(params);
        if (log.isDebugEnabled()) log.debug("PATCH Dataset to Quad Store {}", getEndpoint().getUri());

        return target.request().method(HttpMethod.PATCH, Entity.entity(dataset, getDefaultMediaType()));
    }
    
}
