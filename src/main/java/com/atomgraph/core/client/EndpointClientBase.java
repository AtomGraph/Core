/*
 * Copyright 2022 Martynas Jusevičius <martynas@atomgraph.com>.
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
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.uri.UriComponent;

/**
 *
 * @author {@literal Martynas Jusevičius <martynas@atomgraph.com>}
 */
public abstract class EndpointClientBase extends ClientBase
{

    private final WebTarget endpoint;

    protected EndpointClientBase(MediaTypes mediaTypes, WebTarget endpoint)
    {
        super(mediaTypes);
        this.endpoint = endpoint;
    }
    
    public EndpointClientBase register(ClientRequestFilter filter)
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

    public Response head(jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return head(acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }

    public Response head(jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return head(acceptedTypes, params, new MultivaluedHashMap());
    }

    public Response head(jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).head();
    }

    public Response get(jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return get(acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }

    public Response get(jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return get(acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response get(jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).get();
    }

    public Response post(Object body, MediaType contentType, jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return post(body, contentType, acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }
    
    public Response post(Object body, MediaType contentType, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return post(body, contentType, acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response post(Object body, MediaType contentType, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).post(Entity.entity(body, contentType));
    }

    public Response put(Object body, MediaType contentType, jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return put(body, contentType, acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }

    public Response put(Object body, MediaType contentType, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return put(body, contentType, acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response put(Object body, MediaType contentType, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).put(Entity.entity(body, contentType));
    }

    public Response delete(jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return delete(acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }
    
    public Response delete(jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return delete(acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response delete(jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(applyParams(params).request(acceptedTypes), headers).delete();
    }
    
    public final WebTarget getEndpoint()
    {
        return endpoint;
    }
    
}
