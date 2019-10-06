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
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common base class for all HTTP-based protocol clients.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public abstract class ClientBase
{
    
    private static final Logger log = LoggerFactory.getLogger(ClientBase.class);

    private final WebResource webResource;
    private final MediaTypes mediaTypes;
    
    protected ClientBase(WebResource webResource, MediaTypes mediaTypes)
    {
        if (webResource == null) throw new IllegalArgumentException("WebResource cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");

        this.webResource = webResource;
        this.mediaTypes = mediaTypes;
    }
    
    public abstract MediaType getDefaultMediaType();
    
    public ClientBase addFilter(ClientFilter authFilter)
    {
        if (authFilter == null) throw new IllegalArgumentException("ClientFilter cannot be null");

        getWebResource().addFilter(authFilter);

        return this;
    }
    
    protected WebResource applyParams(MultivaluedMap<String, String> params)
    {
        return applyParams(getWebResource(), params);
    }
    
    protected WebResource applyParams(WebResource webResource, MultivaluedMap<String, String> params)
    {
        if (params != null)
        {
            MultivaluedMap<String, String> encodedParams = new MultivaluedMapImpl();
            for (Map.Entry<String, List<String>> entry : params.entrySet())
                for (String value : entry.getValue())
                    encodedParams.add(UriComponent.encode(entry.getKey(), UriComponent.Type.UNRESERVED),
                        UriComponent.encode(value, UriComponent.Type.UNRESERVED));

            webResource = webResource.queryParams(encodedParams);
        }
        
        return webResource;
    }
    
    public ClientResponse head(Class clazz, javax.ws.rs.core.MediaType[] acceptedTypes, String uri, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("HEAD {}", getWebResource().getURI(), uri);
            WebResource.Builder builder = applyParams(params).accept(acceptedTypes);
            cr = builder.method("HEAD", ClientResponse.class);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Request to Graph Store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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

    public WebResource.Builder getBuilder(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        if (log.isDebugEnabled()) log.debug("GET {}", getWebResource().getURI());
        return applyParams(params).accept(acceptedTypes);
    }
    
    public ClientResponse get(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return get(getBuilder(acceptedTypes, params));
    }
    
    public ClientResponse get(WebResource.Builder builder)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("GET {}", getWebResource().getURI());
            cr = builder.get(ClientResponse.class);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("GET {} request unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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

    public WebResource.Builder postBuilder(MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        WebResource.Builder builder = applyParams(params).type(contentType);
        if (acceptedTypes != null) builder.accept(acceptedTypes);
        return builder;
    }
    
    public ClientResponse post(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return post(postBuilder(contentType, acceptedTypes, params), body);
    }
    
    public ClientResponse post(WebResource.Builder builder, Object body)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("POST {}", getWebResource().getURI());
            cr = builder.post(ClientResponse.class, body);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Request to {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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

    public WebResource.Builder putBuilder(MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        WebResource.Builder builder = applyParams(params).type(contentType);
        if (acceptedTypes != null) builder.accept(acceptedTypes);
        return builder;
    }
    
    public ClientResponse put(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return put(putBuilder(contentType, acceptedTypes, params), body);
    }

    public ClientResponse put(WebResource.Builder builder, Object body)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("PUT {}", getWebResource().getURI());
            cr = builder.put(ClientResponse.class, body);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("PUT {} request unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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
    
    public WebResource.Builder deleteBuilder(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        WebResource.Builder builder = applyParams(params).getRequestBuilder();
        if (acceptedTypes != null) builder.accept(acceptedTypes);
        return builder;
    }

    public ClientResponse delete(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return delete(deleteBuilder(acceptedTypes, params));
    }

    public ClientResponse delete(WebResource.Builder builder)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("DELETE {}", getWebResource().getURI());
            cr = builder.delete(ClientResponse.class);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("DELETE {} request unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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

    
    public MediaType[] getReadableMediaTypes(Class clazz)
    {
        return getMediaTypes().getReadable(clazz).toArray(new javax.ws.rs.core.MediaType[0]);
    }

    public final WebResource getWebResource()
    {
        return webResource;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
}
