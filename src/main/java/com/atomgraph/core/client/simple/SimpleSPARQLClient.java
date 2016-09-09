/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.core.client.simple;

import org.apache.jena.query.Query;
import org.apache.jena.update.UpdateRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import com.atomgraph.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class SimpleSPARQLClient
{

    private static final Logger log = LoggerFactory.getLogger(SimpleSPARQLClient.class);
    
    private final WebResource webResource;
    private final int maxGetRequestSize;

    protected SimpleSPARQLClient(WebResource webResource, int maxGetRequestSize)
    {
        if (webResource == null) throw new IllegalArgumentException("WebResource cannot be null");
        
        this.webResource = webResource;
        this.maxGetRequestSize = maxGetRequestSize;
    }

    protected SimpleSPARQLClient(WebResource webResource)
    {
        this(webResource, 8192);
    }
    
    public WebResource getWebResource()
    {
        return webResource;
    }
    
    public static SimpleSPARQLClient create(WebResource webResource)
    {
        return new SimpleSPARQLClient(webResource);
    }

    public static SimpleSPARQLClient create(WebResource webResource, int maxGetRequestSize)
    {
        return new SimpleSPARQLClient(webResource, maxGetRequestSize);
    }
    
    public WebResource.Builder queryBuilder(Query query, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, int maxGetRequestSize)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {}", getWebResource().getURI(), query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	if (acceptedTypes == null) throw new IllegalArgumentException("Accepted MediaType[] must be not null");

        // POST if request size is over limit, GET otherwise        
        if (query.toString().length() > maxGetRequestSize)
        {
            MultivaluedMap formData = new MultivaluedMapImpl();
            if (params != null) formData.putAll(params);
            formData.putSingle("query", query.toString());

            return getWebResource().accept(acceptedTypes).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        }
        else
        {
            // workaround for Jersey UriBuilder to encode { } brackets using UNRESERVED type
            String escapedQueryString = UriComponent.encode(query.toString(), UriComponent.Type.UNRESERVED);
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
        
            return queryResource.accept(acceptedTypes);
        }
    }

    public WebResource.Builder queryBuilder(Query query, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return queryBuilder(query, acceptedTypes, params, getMaxGetRequestSize());
    }

    public WebResource.Builder queryBuilder(Query query, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return queryBuilder(query, acceptedTypes, null);
    }
    
    /**
     * Loads RDF model from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param query query object
     * @param acceptedTypes accepted media types
     * @param params name/value pairs of request parameters or null, if none
     * @param maxGetRequestSize request size limit for <pre>GET</pre>
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */    
    public ClientResponse query(Query query, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, int maxGetRequestSize)
    {
        return queryBuilder(query, acceptedTypes, params, maxGetRequestSize).get(ClientResponse.class);
    }

    public ClientResponse query(Query query, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return query(query, acceptedTypes, params, getMaxGetRequestSize());
    }
    
    /**
     * Loads RDF model from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * This is a convenience method for {@link #loadModel(String,Query,MultivaluedMap<String, String>)}
     * with null request parameters.
     * 
     * @param query query object
     * @param acceptedTypes accepted media types
     * @return RDF model result
     */
    public ClientResponse query(Query query, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
	return query(query, acceptedTypes, null);
    }

    public ClientResponse update(UpdateRequest updateRequest)
    {
        return post(updateRequest, null);
    }
    
    /**
     * Executes post request on a remote SPARQL endpoint.
     * 
     * @param updateRequest post request
     * @param params name/value pairs of request parameters or null, if none
     * @return client response
     */
    public ClientResponse post(UpdateRequest updateRequest, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", getWebResource().getURI(), updateRequest);
	if (updateRequest == null) throw new IllegalArgumentException("UpdateRequest must be not null");

        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("update", updateRequest.toString());
        
	return getWebResource().
            type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
            post(ClientResponse.class, formData);
    }
    
    public int getMaxGetRequestSize()
    {
        return maxGetRequestSize;
    }
    
}
