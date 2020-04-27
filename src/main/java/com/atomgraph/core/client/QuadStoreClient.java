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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.model.DatasetQuadAccessor;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Quad Store client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://lists.w3.org/Archives/Public/public-sparql-dev/2014AprJun/0008.html">Extending SPARQL Graph Store HTTP Protocol with quad semantics</a>
 */
public class QuadStoreClient extends ClientBase implements DatasetQuadAccessor
{
    private static final Logger log = LoggerFactory.getLogger(QuadStoreClient.class);
    
    public QuadStoreClient(WebTarget endpoint, MediaTypes mediaTypes)
    {
        super(endpoint, mediaTypes);
    }

    public QuadStoreClient(WebTarget endpoint)
    {
        this(endpoint, new MediaTypes());
    }

    public static QuadStoreClient create(WebTarget endpoint, MediaTypes mediaTypes)
    {
        return new QuadStoreClient(endpoint, mediaTypes);
    }

    public static QuadStoreClient create(WebTarget endpoint)
    {
        return new QuadStoreClient(endpoint);
    }
    
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
        try (Response cr = get(getReadableMediaTypes(Dataset.class), null))
        {
            return cr.readEntity(Dataset.class);
        }
    }
    
    @Override
    public void add(Dataset dataset)
    {
        post(dataset, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}, null).close();
    }
    
    @Override
    public void replace(Dataset dataset)
    {
        put(dataset, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}, null).close();
    }
    
    @Override
    public void delete()
    {
        delete(null, null).close();
    }

    @Override
    public void patch(Dataset dataset)
    {
        patch(dataset, null).close();
    }
    
    public Response patch(Dataset dataset, MultivaluedMap<String, String> params)
    {
        WebTarget target = applyParams(params);
        if (log.isDebugEnabled()) log.debug("PATCH Dataset to Quad Store {}", getEndpoint().getUri());

        Response cr = target.request().method(HttpMethod.PATCH, Entity.entity(dataset, getDefaultMediaType()));
        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isErrorEnabled()) log.error("Request to graph store: {} unsuccessful. Reason: {}", target.getUri(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientErrorException(cr);
        }

        return cr;
    }
    
}
