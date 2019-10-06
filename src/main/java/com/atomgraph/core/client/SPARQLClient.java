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

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.exception.ClientException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.jena.query.Dataset;
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
 * SPARQL Protocol client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public class SPARQLClient extends ClientBase
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLClient.class);

    private final int maxGetRequestSize;

    protected SPARQLClient(WebResource webResource, MediaTypes mediaTypes, int maxGetRequestSize)
    {
        super(webResource, mediaTypes);
        this.maxGetRequestSize = maxGetRequestSize;
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
    
    @Override
    public SPARQLClient addFilter(ClientFilter authFilter)
    {
        if (authFilter == null) throw new IllegalArgumentException("ClientFilter cannot be null");

        super.addFilter(authFilter);

        return this;
    }
    
    public int getQueryURLLength(MultivaluedMap<String, String> params)
    {        
        return applyParams(params).getURI().toString().length();
    }

    public ClientResponse query(Query query, Class clazz, MultivaluedMap<String, String> params)
    {
        ClientResponse cr = null;
        
        try
        {
            MultivaluedMap<String, String> mergedParams = new MultivaluedMapImpl();
            if (params != null) mergedParams.putAll(params);
            mergedParams.putSingle("query", query.toString());
            
            if (getQueryURLLength(mergedParams) > getMaxGetRequestSize())
                cr = post(query, MediaType.APPLICATION_FORM_URLENCODED_TYPE, getReadableMediaTypes(clazz), params);
            else
                cr = get(getBuilder(getReadableMediaTypes(clazz), mergedParams));

            if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                if (log.isErrorEnabled()) log.error("Query request to endpoint: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
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
    
    public Model loadModel(Query query)
    {
        return query(query, Model.class, null).getEntity(Model.class);
    }

    public Dataset loadDataset(Query query)
    {
        return query(query, Dataset.class, null).getEntity(Dataset.class);
    }
    
    public ResultSetRewindable select(Query query)
    {
        return query(query, ResultSet.class, null).getEntity(ResultSetRewindable.class);
    }

    public boolean ask(Query query)
    {
        return parseBoolean(query(query, ResultSet.class, null));
    }

    public static boolean parseBoolean(ClientResponse cr)
    {
        InputStream is = cr.getEntity(InputStream.class);
        
        if (cr.getType().isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE))
            return JSONInput.booleanFromJSON(is);
        if (cr.getType().isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE))
            return XMLInput.booleanFromXML(is);

        throw new IllegalStateException("Unsupported ResultSet format");
    }

    /**
     * POSTs update to a remote SPARQL endpoint.
     * 
     * @param updateRequest post request
     * @param params name/value pairs of request parameters or null, if none
     */
    public void update(UpdateRequest updateRequest, MultivaluedMap<String, String> params)
    {
        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("update", updateRequest.toString());
        
        post(formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, formData);
    }

    public int getMaxGetRequestSize()
    {
        return maxGetRequestSize;
    }

    @Override
    public MediaType getDefaultMediaType()
    {
        return MediaType.APPLICATION_FORM_URLENCODED_TYPE;
    }

}
