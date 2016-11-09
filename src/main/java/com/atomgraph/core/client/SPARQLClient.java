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
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.resultset.JSONInput;
import org.apache.jena.sparql.resultset.XMLInput;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class SPARQLClient // TO-DO: implements SPARQLEndpoint
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLClient.class);

    private final WebResource webResource;
    private final int maxGetRequestSize;    
    private final MediaTypes mediaTypes;

    protected SPARQLClient(WebResource webResource, MediaTypes mediaTypes, int maxGetRequestSize)
    {
        if (webResource == null) throw new IllegalArgumentException("WebResource cannot be null");
        
        this.webResource = webResource;
        this.maxGetRequestSize = maxGetRequestSize;
        this.mediaTypes = mediaTypes;
    }

    protected SPARQLClient(WebResource webResource, MediaTypes mediaTypes)
    {
        this(webResource, mediaTypes, 8192);
    }

    protected SPARQLClient(WebResource webResource)
    {
        this(webResource, new MediaTypes());
    }

    public static SPARQLClient create(WebResource webResource, MediaTypes mediaTypes, int maxGetRequestSize)
    {
        return new SPARQLClient(webResource, mediaTypes, maxGetRequestSize);
    }

    public static SPARQLClient create(WebResource webResource, MediaTypes mediaTypes)
    {
        return new SPARQLClient(webResource, mediaTypes);
    }

    public static SPARQLClient create(WebResource webResource)
    {
        return new SPARQLClient(webResource);
    }

    protected int getQueryURLLength(Query query, MultivaluedMap<String, String> params)
    {        
        return getQueryResource(query, params).getURI().toString().length();
    }
    
    protected WebResource.Builder setHeaders(WebResource.Builder builder, Map<String, Object> headers)
    {
	if (builder == null) throw new IllegalArgumentException("WebResource.Builder must be not null");
	if (headers == null) throw new IllegalArgumentException("Map<String, Object> must be not null");

        Iterator<Entry<String, Object>> it = headers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<String, Object> entry = it.next();
            builder.header(entry.getKey(), entry.getValue());
        }
        
        return builder;
    }
    
    protected WebResource getQueryResource(Query query, MultivaluedMap<String, String> params)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");

        String escapedQueryString = UriComponent.encode(query.toString(), UriComponent.Type.UNRESERVED);
        // workaround for Jersey UriBuilder to encode { } brackets using UNRESERVED type
        WebResource queryResource = getWebResource().queryParam("query", escapedQueryString);
        
        if (params != null)
        {
            MultivaluedMap<String, String> encodedParams = new MultivaluedMapImpl();
            for (Map.Entry<String, List<String>> entry : params.entrySet())
                if (!entry.getKey().equals("query")) // query param is handled separately
                    for (String value : entry.getValue())
                        encodedParams.add(UriComponent.encode(entry.getKey(), UriComponent.Type.UNRESERVED),
                            UriComponent.encode(value, UriComponent.Type.UNRESERVED));

            queryResource = queryResource.queryParams(encodedParams);
        }
        
        return queryResource;
    }
    
    protected ClientResponse get(Query query, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, Map<String, Object> headers)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	if (acceptedTypes == null) throw new IllegalArgumentException("Accepted MediaType[] must be not null");
    
        if (log.isDebugEnabled()) log.debug("Remote SPARQL service {} GET query: {}", getWebResource().getURI(), query);        

        WebResource.Builder builder = getQueryResource(query, params).accept(acceptedTypes);
        if (headers != null) setHeaders(builder, headers);
        return builder.get(ClientResponse.class);
    }
    
    protected ClientResponse post(Query query, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, Map<String, Object> headers)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	if (acceptedTypes == null) throw new IllegalArgumentException("Accepted MediaType[] must be not null");
	
        if (log.isDebugEnabled()) log.debug("Remote SPARQL service {} POST query: {}", getWebResource().getURI(), query);
        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("query", query.toString());

        WebResource.Builder builder = getWebResource().accept(acceptedTypes).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        if (headers != null) setHeaders(builder, headers);
        
        return builder.post(ClientResponse.class, formData);
    }

    public Model loadModel(Query query)
    {
        return loadModel(query, null, null);
    }
    
    public Model loadModel(Query query, MultivaluedMap<String, String> params, Map<String, Object> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            if (getQueryURLLength(query, params) > getMaxGetRequestSize())
                cr = post(query, getReadableMediaTypes(Model.class), params, headers);
            else
                cr = get(query, getReadableMediaTypes(Model.class), params, headers);

            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return cr.getEntity(Model.class);
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    public ResultSetRewindable select(Query query)
    {
        return select(query, null, null);
    }
    
    public ResultSetRewindable select(Query query, MultivaluedMap<String, String> params, Map<String, Object> headers)
    {        
        ClientResponse cr = null;

        try
        {
            if (getQueryURLLength(query, params) > getMaxGetRequestSize())
                cr = post(query, getReadableMediaTypes(ResultSet.class), params, headers);
            else
                cr = get(query, getReadableMediaTypes(ResultSet.class), params, headers);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            return cr.getEntity(ResultSetRewindable.class);
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    public boolean ask(Query query)
    {
        return ask(query, null, null);
    }
    
    public boolean ask(Query query, MultivaluedMap<String, String> params, Map<String, Object> headers)
    {
        ClientResponse cr = null;
        
        try
        {
            if (getQueryURLLength(query, params) > getMaxGetRequestSize())
                cr = post(query, getReadableMediaTypes(ResultSet.class), params, headers);
            else
                cr = get(query, getReadableMediaTypes(ResultSet.class), params, headers);
            
            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
                throw new ClientException(cr);
            }

            if (cr.getType().isCompatible(MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE))
                return JSONInput.booleanFromJSON(cr.getEntity(InputStream.class));
            if (cr.getType().isCompatible(MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE))        
                return XMLInput.booleanFromXML(cr.getEntity(InputStream.class));

            throw new ClientException(cr); // TO-DO: refactor
        }
        finally
        {
            if (cr != null) cr.close();
        }
    }

    public void update(UpdateRequest updateRequest)
    {
        post(updateRequest, null, null).close();
    }

    /**
     * Executes post request on a remote SPARQL endpoint.
     * 
     * @param updateRequest post request
     * @param params name/value pairs of request parameters or null, if none
     * @param headers request headers
     * @return client response
     */
    public ClientResponse post(UpdateRequest updateRequest, MultivaluedMap<String, String> params, Map<String, Object> headers)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", getWebResource().getURI(), updateRequest);
	if (updateRequest == null) throw new IllegalArgumentException("UpdateRequest must be not null");

        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("update", updateRequest.toString());
        
	WebResource.Builder builder = getWebResource().type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        if (headers != null) return setHeaders(builder, headers).post(ClientResponse.class, formData);
        return builder.post(ClientResponse.class, formData);
    }

    public final WebResource getWebResource()
    {
        return webResource;
    }
    
    public int getMaxGetRequestSize()
    {
        return maxGetRequestSize;
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
