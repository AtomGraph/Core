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

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class QuadStoreClient extends GraphStoreClient implements DatasetQuadAccessor
{
    private static final Logger log = LoggerFactory.getLogger(QuadStoreClient.class);

    public QuadStoreClient(WebResource webResource, MediaTypes mediaTypes)
    {
        super(webResource, mediaTypes);
    }

    public QuadStoreClient(WebResource webResource)
    {
        super(webResource);
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
    public Dataset get()
    {
        return get(null, null).getEntity(Dataset.class);
    }

    public ClientResponse get(MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("GET Dataset from quad store", getWebResource().getURI());
            cr = get(getReadableMediaTypes(Dataset.class), params, headers);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Request to quad store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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
    
    @Override
    public void add(Dataset dataset)
    {
        addDataset(dataset, null, null);
    }

    public ClientResponse addDataset(Dataset dataset, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("POST Dataset from quad store", getWebResource().getURI());
            cr = post(getDefaultMediaType(), dataset, params, headers);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Request to quad store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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
    
    @Override
    public void replace(Dataset dataset)
    {
        putDataset(dataset, null, null);
    }

    public ClientResponse putDataset(Dataset dataset, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("PUT Dataset to quad store {}", getWebResource().getURI());
            cr = put(getDefaultMediaType(), dataset, params, headers);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Request to quad store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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
    
    @Override
    public void delete()
    {
        deleteDataset(null, null);
    }
    
    public ClientResponse deleteDataset(MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("DELETE Dataset from quad store {}", getWebResource().getURI());
            cr = delete(params, headers);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Request to quad store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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

    @Override
    public void patch(Dataset dataset)
    {
        patch(dataset, null, null);
    }
    
    public ClientResponse patch(Dataset dataset, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        try
        {
            cr = getWebResource().type(com.atomgraph.core.MediaType.TEXT_NQUADS_TYPE).
                method("PATCH", ClientResponse.class, dataset);
            
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
