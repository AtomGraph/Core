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
import com.atomgraph.core.exception.ClientException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.model.DatasetQuadAccessor;
import com.sun.jersey.api.client.filter.ClientFilter;
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
    
    public QuadStoreClient(WebResource webResource, MediaTypes mediaTypes)
    {
        super(webResource, mediaTypes);
    }

    public QuadStoreClient(WebResource webResource)
    {
        this(webResource, new MediaTypes());
    }

    public static QuadStoreClient create(WebResource webResource, MediaTypes mediaTypes)
    {
        return new QuadStoreClient(webResource, mediaTypes);
    }

    public static QuadStoreClient create(WebResource webResource)
    {
        return new QuadStoreClient(webResource);
    }
    
    @Override
    public QuadStoreClient addFilter(ClientFilter authFilter)
    {
        if (authFilter == null) throw new IllegalArgumentException("ClientFilter cannot be null");

        super.addFilter(authFilter);

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
        return get(getReadableMediaTypes(Dataset.class), null).getEntity(Dataset.class);
    }
    
    @Override
    public void add(Dataset dataset)
    {
        post(dataset, getDefaultMediaType(), null, null);
    }
    
    @Override
    public void replace(Dataset dataset)
    {
        put(dataset, getDefaultMediaType(), null, null);
    }
    
    @Override
    public void delete()
    {
        delete(null, null);
    }

    @Override
    public void patch(Dataset dataset)
    {
        patch(dataset, null);
    }
    
    public ClientResponse patch(Dataset dataset, MultivaluedMap<String, String> params)
    {
        ClientResponse cr = null;
        try
        {
            if (log.isDebugEnabled()) log.debug("PATCH Dataset to Quad Store {}", getWebResource().getURI());
            
            WebResource.Builder builder = applyParams(params).type(getDefaultMediaType());
            cr = builder.method("PATCH", ClientResponse.class, dataset);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            cr.bufferEntity();
            return cr;
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }
    
}
