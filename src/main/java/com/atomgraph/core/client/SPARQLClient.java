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

import com.atomgraph.core.client.simple.SimpleSPARQLClient;
import com.atomgraph.core.MediaType;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.exception.ClientException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.io.InputStream;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.resultset.JSONInput;
import org.apache.jena.sparql.resultset.XMLInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class SPARQLClient extends SimpleSPARQLClient // TO-DO: implements SPARQLEndpoint
 // TO-DO: implements SPARQLEndpoint
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLClient.class);

    private final MediaTypes mediaTypes;
    
    protected SPARQLClient(WebResource webResource, MediaTypes mediaTypes, int maxGetRequestSize)
    {
        super(webResource, maxGetRequestSize);
        this.mediaTypes = mediaTypes;
    }

    protected SPARQLClient(WebResource webResource, MediaTypes mediaTypes)
    {
        super(webResource);
        this.mediaTypes = mediaTypes;
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

    public WebResource.Builder loadModelBuilder(Query query, MultivaluedMap<String, String> mvm)
    {
        return queryBuilder(query, getReadableMediaTypes(Model.class), mvm);
    }

    public Model loadModel(Query query)
    {
        return loadModel(query, null);
    }
    
    public Model loadModel(Query query, MultivaluedMap<String, String> mvm)
    {
        ClientResponse cr = loadModelBuilder(query, mvm).get(ClientResponse.class);

        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
        
        return cr.getEntity(Model.class);
    }
    
    public WebResource.Builder selectBuilder(Query query, MultivaluedMap<String, String> mvm)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isSelectType()) throw new IllegalArgumentException("Query must be SELECT");
        
        return queryBuilder(query, getReadableMediaTypes(ResultSet.class), mvm);
    }

    public ResultSetRewindable select(Query query)
    {
        return select(query, null);
    }
    
    public ResultSetRewindable select(Query query, MultivaluedMap<String, String> mvm)
    {        
        ClientResponse cr = selectBuilder(query, mvm).get(ClientResponse.class);
        
        if (!cr.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }
        
        return cr.getEntity(ResultSetRewindable.class);
    }

    public WebResource.Builder askBuilder(Query query, MultivaluedMap<String, String> mvm)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isAskType()) throw new IllegalArgumentException("Query must be ASK");

        return queryBuilder(query, getReadableMediaTypes(ResultSet.class), mvm);
    }

    public boolean ask(Query query)
    {
        return ask(query, null);
    }
    
    public boolean ask(Query query, MultivaluedMap<String, String> mvm)
    {
        ClientResponse cr = askBuilder(query, mvm).get(ClientResponse.class);
        
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

    /*
    @Override
    public void post(UpdateRequest updateRequest)
    {
	if (log.isDebugEnabled()) log.debug("Executing post on SPARQL endpoint: {} using UpdateRequest: {}", getOrigin().getWebResource().getURI(), updateRequest);
	ClientResponse cr = getClient().post(updateRequest, null);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr);
        }        
    }
    */
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }

    public javax.ws.rs.core.MediaType[] getReadableMediaTypes(Class clazz)
    {
        return getMediaTypes().getReadable(clazz).toArray(new javax.ws.rs.core.MediaType[0]);
    }

}
