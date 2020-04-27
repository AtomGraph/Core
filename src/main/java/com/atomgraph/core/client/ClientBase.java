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
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common base class for all HTTP-based protocol clients.
 * Note that <code>Response</code> objects returned by methods of this class <em>are not</em> closed.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
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
    
    public Response head(Class clazz, javax.ws.rs.core.MediaType[] acceptedTypes, String uri, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        WebTarget target = applyParams(params);
        if (log.isDebugEnabled()) log.debug("HEAD {}", target.getUri());
        
        Response cr = target.request(acceptedTypes).method("HEAD", Response.class);
        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isErrorEnabled()) log.error("Request to Graph Store: {} unsuccessful. Reason: {}", target.getUri(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientErrorException(cr);
        }

        return cr;
    }

    public Response get(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        WebTarget target = applyParams(params);
        if (log.isDebugEnabled()) log.debug("GET {}", target.getUri());
        
        Response cr = target.request(acceptedTypes).get();
        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isErrorEnabled()) log.error("GET {} request unsuccessful. Reason: {}", target.getUri(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientErrorException(cr);
        }

        return cr;
    }
    
    public Response post(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        WebTarget target = applyParams(params);
        if (log.isDebugEnabled()) log.debug("POST {}", target.getUri());
        
        Response cr = target.request(acceptedTypes).post(Entity.entity(body, contentType));
        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isErrorEnabled()) log.error("Request to {} unsuccessful. Reason: {}", target.getUri(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientErrorException(cr);
        }
        
        return cr;
    }
    
    public Response put(Object body, MediaType contentType, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        WebTarget target = applyParams(params);
        if (log.isDebugEnabled()) log.debug("PUT {}", target.getUri());
        
        Response cr = target.request(acceptedTypes).put(Entity.entity(body, contentType));
        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isErrorEnabled()) log.error("PUT {} request unsuccessful. Reason: {}", target.getUri(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientErrorException(cr);
        }

        return cr;
    }

    public Response delete(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        WebTarget target = applyParams(params);
        if (log.isDebugEnabled()) log.debug("DELETE {}", target.getUri());
        
        Response cr = target.request(acceptedTypes).delete();
        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isErrorEnabled()) log.error("DELETE {} request unsuccessful. Reason: {}", target.getUri(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientErrorException(cr);
        }

        return cr;
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
