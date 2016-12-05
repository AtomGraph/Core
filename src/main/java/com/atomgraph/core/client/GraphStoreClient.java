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
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
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

    private final WebTarget webTarget;    
    private final MediaTypes mediaTypes;
    
    protected GraphStoreClient(WebTarget webTarget, MediaTypes mediaTypes)
    {
        if (webTarget == null) throw new IllegalArgumentException("WebTarget cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");

        this.webTarget = webTarget;        
        this.mediaTypes = mediaTypes;
    }

    protected GraphStoreClient(WebTarget webResource)
    {
        this(webResource, new MediaTypes());
    }

    public static GraphStoreClient create(WebTarget webResource, MediaTypes mediaTypes)
    {
        return new GraphStoreClient(webResource, mediaTypes);
    }

    public static GraphStoreClient create(WebTarget webResource)
    {
        return new GraphStoreClient(webResource);
    }

    public MediaType getDefaultMediaType()
    {
        return MediaType.TEXT_NTRIPLES_TYPE;
    }
    
    protected Response get(javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        if (acceptedTypes == null) throw new IllegalArgumentException("MediaType[] cannot be null");
        
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getWebTarget().getUri());
	return getWebTarget().queryParam("default", "").
            request(acceptedTypes).
            get(Response.class);
    }
    
    @Override
    public Model getModel()
    {
        Response cr = null;

        try
        {
            if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getWebTarget().getUri());
            cr = get(getReadableMediaTypes(Model.class));
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return cr.readEntity(Model.class);
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected Response get(javax.ws.rs.core.MediaType[] acceptedTypes, String graphURI)
    {
        if (acceptedTypes == null) throw new IllegalArgumentException("MediaType[] cannot be null");
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getWebTarget().getUri(), graphURI);
	return getWebTarget().queryParam("graph", graphURI).
            request(acceptedTypes).
            get(Response.class);
    }
    
    @Override
    public Model getModel(String uri)
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getWebTarget().getUri(), uri);
            cr = get(getReadableMediaTypes(Model.class), uri);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return cr.readEntity(Model.class);
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected Response head(String graphURI)
    {
	return getWebTarget().queryParam("graph", graphURI).
                request().
                method("HEAD", Response.class);
    }
    
    @Override
    public boolean containsModel(String uri)
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("Checking if Graph Store {} contains GRAPH with URI {}", getWebTarget().getUri(), uri);
            cr = head(uri);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return true;
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }
    
    protected Response put(MediaType contentType, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getWebTarget().getUri());
	return getWebTarget().queryParam("default", "").
                request().
                put(Entity.entity(model, contentType));
    }
    
    @Override
    public void putModel(Model model)
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getWebTarget().getUri());
            cr = put(getDefaultMediaType(), model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected Response putModel(MediaType contentType, String graphURI, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getWebTarget().getUri(), graphURI);
	return getWebTarget().queryParam("graph", graphURI).
                request().
                put(Entity.entity(model, contentType));
    }
    
    @Override
    public void putModel(String uri, Model model)
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getWebTarget().getUri(), uri);
            cr = putModel(getDefaultMediaType(), uri, model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected Response delete()
    {
	if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getWebTarget().getUri());
	return getWebTarget().queryParam("default", "").
                request().
                delete();
    }
    
    @Override
    public void deleteDefault()
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getWebTarget().getUri());
            cr = delete();
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected Response delete(String graphURI)
    {
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        
	if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", graphURI, getWebTarget().getUri());
	return getWebTarget().queryParam("graph", graphURI).
                request().
                delete();
    }
    
    @Override
    public void deleteModel(String uri)
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", uri, getWebTarget().getUri());
            cr = delete(uri);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected Response add(MediaType contentType, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getWebTarget().getUri());
	return getWebTarget().queryParam("default", "").
            request().
            post(Entity.entity(model, contentType));
    }
    
    @Override
    public void add(Model model)
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getWebTarget().getUri());
            cr = add(getDefaultMediaType(), model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    protected Response add(MediaType contentType, String graphURI, Model model)
    {
        if (contentType == null) throw new IllegalArgumentException("MediaType cannot be null");
        if (graphURI == null) throw new IllegalArgumentException("String cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");

	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getWebTarget().getUri(), graphURI);
	return getWebTarget().queryParam("graph", graphURI).
            request().
            post(Entity.entity(model, contentType));
    }
    
    @Override
    public void add(String uri, Model model)
    {
        Response cr = null;
        
        try
        {
            if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getWebTarget().getUri(), uri);
            cr = add(getDefaultMediaType(), uri, model);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Request to graph store: {} unsuccessful. Reason: {}", getWebTarget().getUri(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    public final WebTarget getWebTarget()
    {
        return webTarget;
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
