/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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

import com.atomgraph.core.MediaType;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.exception.ClientException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph Store Protocol 1.1 client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class GraphStoreClient implements DatasetAccessor
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreClient.class);

    private final WebResource webResource;
    private final MediaTypes mediaTypes;
    
    protected GraphStoreClient(WebResource webResource, MediaTypes mediaTypes)
    {
        if (webResource == null) throw new IllegalArgumentException("WebResource cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");

        this.webResource = webResource;
        this.mediaTypes = mediaTypes;
    }

    protected GraphStoreClient(WebResource webResource)
    {
        this(webResource, new MediaTypes());
    }

    public static GraphStoreClient create(WebResource webResource, MediaTypes mediaTypes)
    {
        return new GraphStoreClient(webResource, mediaTypes);
    }

    public static GraphStoreClient create(WebResource webResource)
    {
        return new GraphStoreClient(webResource);
    }

    public MediaType getDefaultMediaType()
    {
        return MediaType.TEXT_NTRIPLES_TYPE;
    }
    
    public WebResource.Builder setHeaders(WebResource.Builder builder, MultivaluedMap<String, String> headers)
    {
        if (builder == null) throw new IllegalArgumentException("WebResource.Builder must be not null");
        if (headers == null) throw new IllegalArgumentException("Map<String, Object> must be not null");

        Iterator<Map.Entry<String, List<String>>> it = headers.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, List<String>> entry = it.next();
            for (String value : entry.getValue())
                builder.header(entry.getKey(), value);
        }
        
        return builder;
    }
    
    public WebResource getResource(MultivaluedMap<String, String> params)
    {   
        WebResource queryResource = getWebResource();
        
        if (params != null)
        {
            MultivaluedMap<String, String> encodedParams = new MultivaluedMapImpl();
            for (Map.Entry<String, List<String>> entry : params.entrySet())
                for (String value : entry.getValue())
                    encodedParams.add(UriComponent.encode(entry.getKey(), UriComponent.Type.UNRESERVED),
                        UriComponent.encode(value, UriComponent.Type.UNRESERVED));

            queryResource = queryResource.queryParams(encodedParams);
        }
        
        return queryResource;
    }
    
    public ClientResponse get(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        if (acceptedTypes == null) throw new IllegalArgumentException("MediaType[] cannot be null");
        
        if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getWebResource().getURI());
        WebResource.Builder builder =  getResource(params).accept(acceptedTypes);
        if (headers != null) setHeaders(builder, headers);
        return builder.get(ClientResponse.class);
    }
    
    @Override
    public Model getModel()
    {
        return getModel(null, null).getEntity(Model.class);
    }
    
    public ClientResponse getModel(MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("default", "");

            if (log.isDebugEnabled()) log.debug("GET default graph from Graph Store {}", getWebResource().getURI());
            cr = get(getReadableMediaTypes(Model.class), mergedParams, headers);
            
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

    @Override
    public Model getModel(String uri)
    {
        return getModel(uri, null, null).getEntity(Model.class);
    }
    
    public ClientResponse getModel(String uri, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("graph", uri);
            
            if (log.isDebugEnabled()) log.debug("GET named graph with URI {} from Graph Store {}", uri, getWebResource().getURI());
            cr = get(getReadableMediaTypes(Model.class), mergedParams, headers);
            
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

    public ClientResponse head(javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        WebResource.Builder builder = getResource(params).accept(acceptedTypes);
        if (headers != null) setHeaders(builder, headers);
        return builder.method("HEAD", ClientResponse.class);
    }
    
    @Override
    public boolean containsModel(String uri)
    {
        return containsModel(uri, null, null).getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL);
    }
    
    public ClientResponse containsModel(String uri, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("graph", uri);
            
            if (log.isDebugEnabled()) log.debug("Checking if Graph Store {} contains graph with URI {}", getWebResource().getURI(), uri);
            cr = head(getReadableMediaTypes(Model.class), mergedParams, headers);
            
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
    
    public ClientResponse put(MediaType contentType, Object body, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (body == null) throw new IllegalArgumentException("Model cannot be null");
        
        if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getWebResource().getURI());
        WebResource.Builder builder = getResource(params).type(contentType); // getWebResource().queryParam("default", "").
        if (headers != null) setHeaders(builder, headers);
        return builder.put(ClientResponse.class, body);
    }
    
    @Override
    public void putModel(Model model)
    {
        putModel(null, null);
    }
    
    public ClientResponse putModel(Model model, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("default", "");
            
            if (log.isDebugEnabled()) log.debug("PUT default graph to Graph Store {}", getWebResource().getURI());
            cr = put(getDefaultMediaType(), model, mergedParams, headers);
            
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

    @Override
    public void putModel(String uri, Model model)
    {
        putModel(uri, model, null, null);
    }
    
    public ClientResponse putModel(String uri, Model model, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("graph", uri);
            
            if (log.isDebugEnabled()) log.debug("PUT named graph with URI {} to Graph Store {}", uri, getWebResource().getURI());
            cr = put(getDefaultMediaType(), model, mergedParams, headers);
            
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

    public ClientResponse delete(MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getWebResource().getURI());
        WebResource.Builder builder = getResource(params).getRequestBuilder(); // getWebResource().queryParam("default", "").
        if (headers != null) setHeaders(builder, headers);
        return builder.delete(ClientResponse.class);
    }
    
    @Override
    public void deleteDefault()
    {
        deleteDefault(null, null);
    }
    
    public ClientResponse deleteDefault(MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("default", "");
            
            if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getWebResource().getURI());
            cr = delete(mergedParams, headers);
            
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

    
    @Override
    public void deleteModel(String uri)
    {
        deleteModel(uri, null, null);
    }
    
    public ClientResponse deleteModel(String uri, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("graph", uri);
            
            if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", uri, getWebResource().getURI());
            cr = delete(mergedParams, headers);
            
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

    public ClientResponse post(MediaType contentType, Object body, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (body == null) throw new IllegalArgumentException("Model cannot be null");
        
        if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getWebResource().getURI());
        WebResource.Builder builder = getResource(params).type(contentType); // getWebResource().queryParam("default", "").
        if (headers != null) setHeaders(builder, headers);
        return builder.post(ClientResponse.class, body);
    }

    
    @Override
    public void add(Model model)
    {
        add(model, null, null);
    }
    
    public ClientResponse add(Model model, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("default", "");
            
            if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getWebResource().getURI());
            cr = post(getDefaultMediaType(), model, mergedParams, headers);
            
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
    
    @Override
    public void add(String uri, Model model)
    {
        add(uri, model, null, null);
    }
    
    public ClientResponse add(String uri, Model model, MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("graph", uri);
            
            if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getWebResource().getURI(), uri);
            cr = post(getDefaultMediaType(), model, mergedParams, headers);
            
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

    public final WebResource getWebResource()
    {
        return webResource;
    }
        
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }

    public javax.ws.rs.core.MediaType[] getReadableMediaTypes(Class clazz)
    {
        return getMediaTypes().getReadable(clazz).toArray(new javax.ws.rs.core.MediaType[0]);
    }

}
