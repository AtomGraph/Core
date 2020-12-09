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
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common base class for all HTTP-based protocol clients.
 * Note that <code>Response</code> objects returned by methods of this class <em>are not</em> closed.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public abstract class ClientBase
{
    
    private static final Logger log = LoggerFactory.getLogger(ClientBase.class);

    private final WebTarget endpoint;
    private final MediaTypes mediaTypes;
    
    protected ClientBase(WebTarget endpoint, MediaTypes mediaTypes)
    {
        if (endpoint == null) throw new IllegalArgumentException("WebTarget cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");

        this.endpoint = endpoint;
        this.mediaTypes = mediaTypes;
    }
    
    public abstract MediaType getDefaultMediaType();
    
    public ClientBase register(ClientRequestFilter filter)
    {
        if (filter == null) throw new IllegalArgumentException("ClientRequestFilter cannot be null");

        getEndpoint().register(filter);

        return this;
    }
    
    protected WebTarget applyParams(MultivaluedMap<String, String> params)
    {
        return applyParams(getEndpoint(), params);
    }
    
    protected WebTarget applyParams(WebTarget webTarget, MultivaluedMap<String, String> params)
    {
        if (params != null)
            for (Map.Entry<String, List<String>> entry : params.entrySet())
                for (String value : entry.getValue())
                    webTarget = webTarget.queryParam(UriComponent.encode(entry.getKey(), UriComponent.Type.UNRESERVED),
                        UriComponent.encode(value, UriComponent.Type.UNRESERVED));
        
        return webTarget;
    }

    protected Invocation.Builder applyHeaders(Invocation.Builder builder, MultivaluedMap<String, Object> headers)
    {
        if (headers != null)
            for (Map.Entry<String, List<Object>> entry : headers.entrySet())
                for (Object value : entry.getValue())
                    builder = builder.header(entry.getKey(), value);
        
        return builder;
    }

    public Response head(javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return head(acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }

    public Response head(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return head(acceptedTypes, params, new MultivaluedHashMap());
    }

    public Response head(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).head();
    }

    public Response get(javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return get(acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }

    public Response get(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return get(acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response get(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).get();
    }

    public Response post(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return post(body, contentType, acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }
    
    public Response post(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return post(body, contentType, acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response post(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).post(Entity.entity(body, contentType));
    }

    public Response put(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return put(body, contentType, acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }

    public Response put(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return put(body, contentType, acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response put(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).put(Entity.entity(body, contentType));
    }

    public Response delete(javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return delete(acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }
    
    public Response delete(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return delete(acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response delete(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).delete();
    }
    
    public MediaType[] getReadableMediaTypes(Class clazz)
    {
        return getMediaTypes().getReadable(clazz).toArray(new javax.ws.rs.core.MediaType[0]);
    }

    public final WebTarget getEndpoint()
    {
        return endpoint;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
}
