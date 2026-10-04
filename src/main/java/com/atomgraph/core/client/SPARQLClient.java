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
import com.atomgraph.core.io.SPARQLResultProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.apache.jena.sparql.resultset.ResultsReader;
import org.apache.jena.sparql.resultset.SPARQLResult;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL Protocol client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public class SPARQLClient extends EndpointClientBase
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLClient.class);
    
    public static final String QUERY_PARAM_NAME = "query";
    public static final String UPDATE_PARAM_NAME = "update";

    private final int maxGetRequestSize;

    protected SPARQLClient(MediaTypes mediaTypes, WebTarget endpoint, int maxGetRequestSize)
    {
        super(mediaTypes, endpoint);
        this.maxGetRequestSize = maxGetRequestSize;
    }

    protected SPARQLClient(MediaTypes mediaTypes, WebTarget endpoint)
    {
        this(mediaTypes, endpoint, 8192);
    }

    protected SPARQLClient(WebTarget endpoint)
    {
        this(new MediaTypes(), endpoint);
    }

    public static SPARQLClient create(MediaTypes mediaTypes, WebTarget endpoint, int maxGetRequestSize)
    {
        return new SPARQLClient(mediaTypes, endpoint, maxGetRequestSize);
    }

    public static SPARQLClient create(MediaTypes mediaTypes, WebTarget endpoint)
    {
        return new SPARQLClient(mediaTypes, endpoint);
    }

    public static SPARQLClient create(WebTarget endpoint)
    {
        return new SPARQLClient(endpoint);
    }
    
    /**
     * Registers client filter.
     * Can cause performance problems with <code>ApacheConnector</code>.
     * 
     * @param filter client request filter
     * @return this SPARQL client
     * @see <a href="https://blogs.oracle.com/japod/how-to-use-jersey-client-efficiently">How To Use Jersey Client Efficiently</a>
     */
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

    public Response query(Query query, Class clazz)
    {
        return query(query, clazz, new MultivaluedHashMap());
    }
    
    public Response query(Query query, Class clazz, MultivaluedMap<String, String> params)
    {
        return query(query, clazz, params, new MultivaluedHashMap());
    }
    
    public Response query(Query query, Class clazz, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return query(query, getReadableMediaTypes(clazz), params, headers);
    }

    /**
     * Executes the query, asking for the result in the given media types.
     * 
     * @param query the query
     * @param acceptedTypes the media types the result is acceptable in, in order of preference
     * @param params request parameters
     * @param headers request headers
     * @return response
     */
    public Response query(Query query, MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        if (acceptedTypes == null) throw new IllegalArgumentException("MediaType[] acceptedTypes cannot be null");
        if (params == null) throw new IllegalArgumentException("MultivaluedMap<String, String> params cannot be null");
        if (headers == null) throw new IllegalArgumentException("MultivaluedMap<String, Object> headers cannot be null");
        
        MultivaluedMap<String, String> mergedParams = new MultivaluedHashMap();
        mergedParams.putAll(params);
        mergedParams.putSingle(QUERY_PARAM_NAME, query.toString());
        
        // the query string is what overflows the URL, so measure the params it was merged into.
        // Measuring params left the POST branch unreachable: it carries only the (usually absent)
        // graph URIs, so the length was that of the bare endpoint URI and every query, however
        // large, went out as GET
        if (getQueryURLLength(mergedParams) > getMaxGetRequestSize())
            return applyHeaders(getEndpoint().request(acceptedTypes), headers).post(Entity.form(mergedParams));
        else
            return applyHeaders(applyParams(mergedParams).request(acceptedTypes), headers).get();
    }
    
    public Model loadModel(Query query)
    {
        try (Response cr = query(query, Model.class))
        {
            return cr.readEntity(Model.class);
        }
    }

    public Dataset loadDataset(Query query)
    {
        try (Response cr = query(query, Dataset.class))
        {
            return cr.readEntity(Dataset.class);
        }
    }
    
    public ResultSetRewindable select(Query query)
    {
        try (Response cr = query(query, ResultSet.class))
        {
            return cr.readEntity(ResultSetRewindable.class);
        }
    }

    /**
     * The readable result set media types a boolean can be read from: what an ASK query asks for.
     * 
     * @return media types, in order of preference
     */
    public MediaType[] getReadableBooleanMediaTypes()
    {
        return Arrays.stream(getReadableMediaTypes(ResultSet.class)).filter(SPARQLResultProvider::isBooleanReadable).toArray(MediaType[]::new);
    }

    public boolean ask(Query query)
    {
        try (Response cr = query(query, getReadableBooleanMediaTypes(), new MultivaluedHashMap(), new MultivaluedHashMap()))
        {
            try
            {
                return parseBoolean(cr);
            }
            catch (IOException ex)
            {
                if (log.isErrorEnabled()) log.error("Could not parse ASK result: {}", ex);
                throw new ServerErrorException(cr, ex);
            }
        }
    }

    /**
     * Reads the boolean result of an ASK query from the response, in whichever registered results format
     * it came in.
     * 
     * @param cr the response
     * @return the boolean result
     * @throws IOException if the body cannot be read
     * @throws IllegalStateException if the response is not in a results format, or holds no boolean
     */
    public static boolean parseBoolean(Response cr) throws IOException
    {
        MediaType mediaType = cr.getMediaType();
        if (mediaType == null) throw new IllegalStateException("Response has no Content-Type");
        Lang lang = SPARQLResultProvider.getLang(mediaType);
        if (lang == null || !ResultSetReaderRegistry.isRegistered(lang)) throw new IllegalStateException("Unsupported SPARQL results format: " + mediaType);

        try (InputStream is = cr.readEntity(InputStream.class))
        {
            SPARQLResult result = ResultsReader.create().lang(lang).build().readAny(is);
            // the TSV, Thrift and Protobuf readers never yield a boolean, so an ASK answered in them lands here
            if (!result.isBoolean()) throw new IllegalStateException("Response in " + lang + " is not a boolean result");
            return result.getBooleanResult();
        }
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
        formData.putSingle(UPDATE_PARAM_NAME, updateRequest.toString());

        try (Response response = post(formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE, new MediaType[]{}, null))
        {
            // Response automatically closed by try-with-resources
        }
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
