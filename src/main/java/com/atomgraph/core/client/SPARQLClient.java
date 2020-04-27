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
import java.io.InputStream;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
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

    protected SPARQLClient(WebTarget endpoint, MediaTypes mediaTypes, int maxGetRequestSize)
    {
        super(endpoint, mediaTypes);
        this.maxGetRequestSize = maxGetRequestSize;
    }

    protected SPARQLClient(WebTarget endpoint, MediaTypes mediaTypes)
    {
        this(endpoint, mediaTypes, 8192);
    }

    protected SPARQLClient(WebTarget endpoint)
    {
        this(endpoint, new MediaTypes());
    }

    public static SPARQLClient create(WebTarget endpoint, MediaTypes mediaTypes, int maxGetRequestSize)
    {
        return new SPARQLClient(endpoint, mediaTypes, maxGetRequestSize);
    }

    public static SPARQLClient create(WebTarget endpoint, MediaTypes mediaTypes)
    {
        return new SPARQLClient(endpoint, mediaTypes);
    }

    public static SPARQLClient create(WebTarget endpoint)
    {
        return new SPARQLClient(endpoint);
    }
    
    @Override
    public SPARQLClient register(ClientRequestFilter filter)
    {
        if (filter == null) throw new IllegalArgumentException("ClientRequestFilter cannot be null");

        super.register(filter);

        return this;
    }
    
    public int getQueryURLLength(MultivaluedMap<String, String> params)
    {        
        return applyParams(params).getUri().toString().length();
    }

    public Response query(Query query, Class clazz, MultivaluedMap<String, String> params)
    {
        MultivaluedMap<String, String> mergedParams = new MultivaluedHashMap();
        if (params != null) mergedParams.putAll(params);
        mergedParams.putSingle("query", query.toString());

        if (getQueryURLLength(mergedParams) > getMaxGetRequestSize())
            return post(query, MediaType.APPLICATION_FORM_URLENCODED_TYPE, getReadableMediaTypes(clazz), params);
        else
            return get(getReadableMediaTypes(clazz), mergedParams);
    }
    
    public Model loadModel(Query query)
    {
        try (Response cr = query(query, Model.class, null))
        {
            return cr.readEntity(Model.class);
        }
    }

    public Dataset loadDataset(Query query)
    {
        try (Response cr = query(query, Dataset.class, null))
        {
            return cr.readEntity(Dataset.class);
        }
    }
    
    public ResultSetRewindable select(Query query)
    {
        try (Response cr = query(query, ResultSet.class, null))
        {
            return cr.readEntity(ResultSetRewindable.class);
        }
    }

    public boolean ask(Query query)
    {
        try (Response cr = query(query, ResultSet.class, null))
        {
            return parseBoolean(cr);
        }
    }

    public static boolean parseBoolean(Response cr)
    {
        InputStream is = cr.readEntity(InputStream.class);
        
        if (cr.getMediaType().isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE))
            return JSONInput.booleanFromJSON(is);
        if (cr.getMediaType().isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE))
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
        MultivaluedMap formData = new MultivaluedHashMap();
        if (params != null) formData.putAll(params);
        formData.putSingle("update", updateRequest.toString());
        
        post(formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE, new MediaType[]{}, null).close();
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
