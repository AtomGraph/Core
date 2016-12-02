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
import javax.ws.rs.core.Response;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph Store Protocol 1.1 client.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
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
    
    protected ClientResponse get(javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        if (acceptedTypes == null) throw new IllegalArgumentException("MediaType[] cannot be null");
        
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            accept(acceptedTypes).
            get(ClientResponse.class);
    }
    
    @Override
    public Model getModel()
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getWebResource().getURI());
            cr = get(getReadableMediaTypes(Model.class));
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return cr.getEntity(Model.class);
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected ClientResponse get(javax.ws.rs.core.MediaType[] acceptedTypes, String graphURI)
    {
        if (acceptedTypes == null) throw new IllegalArgumentException("MediaType[] cannot be null");
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getWebResource().getURI(), graphURI);
	return getWebResource().queryParam("graph", graphURI).
            accept(acceptedTypes).
            get(ClientResponse.class);
    }
    
    @Override
    public Model getModel(String uri)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getWebResource().getURI(), uri);
            cr = get(getReadableMediaTypes(Model.class), uri);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return cr.getEntity(Model.class);
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected ClientResponse head(String graphURI)
    {
	return getWebResource().queryParam("graph", graphURI).
            method("HEAD", ClientResponse.class);
    }
    
    @Override
    public boolean containsModel(String uri)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("Checking if Graph Store {} contains GRAPH with URI {}", getWebResource().getURI(), uri);
            cr = head(uri);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return true;
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }
    
    protected ClientResponse put(MediaType contentType, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            type(contentType).
            put(ClientResponse.class, model);
    }
    
    @Override
    public void putModel(Model model)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getWebResource().getURI());
            cr = put(getDefaultMediaType(), model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected ClientResponse putModel(MediaType contentType, String graphURI, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getWebResource().getURI(), graphURI);
	return getWebResource().queryParam("graph", graphURI).
            type(contentType).
            put(ClientResponse.class, model);
    }
    
    @Override
    public void putModel(String uri, Model model)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getWebResource().getURI(), uri);
            cr = putModel(getDefaultMediaType(), uri, model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected ClientResponse delete()
    {
	if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            delete(ClientResponse.class);
    }
    
    @Override
    public void deleteDefault()
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getWebResource().getURI());
            cr = delete();
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected ClientResponse delete(String graphURI)
    {
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        
	if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", graphURI, getWebResource().getURI());
	return getWebResource().queryParam("graph", graphURI).
            delete(ClientResponse.class);
    }
    
    @Override
    public void deleteModel(String uri)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", uri, getWebResource().getURI());
            cr = delete(uri);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected ClientResponse add(MediaType contentType, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            type(contentType).
            post(ClientResponse.class, model);
    }
    
    @Override
    public void add(Model model)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getWebResource().getURI());
            cr = add(getDefaultMediaType(), model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected ClientResponse add(MediaType contentType, String graphURI, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");

	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getWebResource().getURI(), graphURI);
	return getWebResource().queryParam("graph", graphURI).
            type(contentType).
            post(ClientResponse.class, model);
    }
    
    @Override
    public void add(String uri, Model model)
    {
        ClientResponse cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getWebResource().getURI(), uri);
            cr = add(getDefaultMediaType(), uri, model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
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
