/**
 *  Copyright 2014 Martynas Jusevičius <martynas@graphity.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.graphity.core.model.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.JSONInput;
import com.hp.hpl.jena.sparql.resultset.XMLInput;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.ClientResponse;
import java.io.InputStream;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.Status.Family;
import org.graphity.core.MediaType;
import org.graphity.core.MediaTypes;
import org.graphity.core.client.SPARQLClient;
import org.graphity.core.exception.ClientException;
import org.graphity.core.model.SPARQLEndpointOrigin;
import org.graphity.core.model.SPARQLEndpointProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of SPARQL endpoint.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/sparql")
public class SPARQLEndpointProxyBase extends SPARQLEndpointBase implements SPARQLEndpointProxy
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProxyBase.class);

    private final SPARQLEndpointOrigin origin;
    private final SPARQLClient client;
    private final javax.ws.rs.core.MediaType[] readableModelMediaTypes, readableResultSetMediaTypes;

    /**
     * Constructs SPARQL endpoint proxy from request metadata and origin.
     * 
     * @param request
     * @param servletConfig
     * @param origin
     * @param mediaTypes 
     */
    public SPARQLEndpointProxyBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context SPARQLEndpointOrigin origin)
    {
        super(request, servletConfig, mediaTypes);
        if (origin == null) throw new IllegalArgumentException("SPARQLEndpointOrigin cannot be null");
        this.origin = origin;
        
        List<javax.ws.rs.core.MediaType> modelTypeList = mediaTypes.getReadable(Model.class);
        readableModelMediaTypes = modelTypeList.toArray(new javax.ws.rs.core.MediaType[modelTypeList.size()]);
        List<javax.ws.rs.core.MediaType> resultSetTypeList = mediaTypes.getReadable(ResultSet.class);        
        readableResultSetMediaTypes = resultSetTypeList.toArray(new javax.ws.rs.core.MediaType[resultSetTypeList.size()]);

        client = SPARQLClient.create(origin.getWebResource());
    }
    
    @Override
    public SPARQLEndpointOrigin getOrigin()
    {
        return origin;
    }
    
    public SPARQLClient getClient()
    {
        return client;
    }
    
    public javax.ws.rs.core.MediaType[] getReadableModelMediaTypes()
    {
        return readableModelMediaTypes;
    }

    public javax.ws.rs.core.MediaType[] getReadableResultSetMediaTypes()
    {
        return readableResultSetMediaTypes;
    }
    
    @Override
    public Model loadModel(Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading Model from SPARQL endpoint: {} using Query: {}", getOrigin().getWebResource().getURI(), query);
	ClientResponse cr = getClient().query(query, getReadableModelMediaTypes());
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr.getStatusInfo());
        }

        return cr.getEntity(Model.class);
    }

    @Override
    public ResultSetRewindable select(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isSelectType()) throw new IllegalArgumentException("Query must be SELECT");
        
	if (log.isDebugEnabled()) log.debug("Loading ResultSet from SPARQL endpoint: {} using Query: {}", getOrigin().getWebResource().getURI(), query);
	ClientResponse cr = getClient().query(query, getReadableResultSetMediaTypes());
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr.getStatusInfo());
        }
        
        return cr.getEntity(ResultSetRewindable.class);
    }

    /**
     * Returns boolean result from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>ASK</code> queries can be used with this method.
     * 
     * @param query query object
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */    
    @Override
    public boolean ask(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isAskType()) throw new IllegalArgumentException("Query must be ASK");
        
        ClientResponse cr = getClient().query(query, getReadableResultSetMediaTypes());
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr.getStatusInfo());
        }

        if (cr.getType().isCompatible(MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE))
            return JSONInput.booleanFromJSON(cr.getEntity(InputStream.class));
        if (cr.getType().isCompatible(MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE))        
            return XMLInput.booleanFromXML(cr.getEntity(InputStream.class));
        
        throw new ClientException(cr.getStatusInfo()); // TO-DO: refactor
    }

    /*
    public boolean ask(Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query execution: {} ", getWebResource().getURI(), query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("query", query.toString());
        
        return XMLInput.booleanFromXML(getWebResource().
            accept(MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE). // needs to be XML since we're reading with XMLInput
            type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
            post(ClientResponse.class, formData).
            getEntity(InputStream.class));
    }
    */
    
    @Override
    public void update(UpdateRequest updateRequest)
    {
	if (log.isDebugEnabled()) log.debug("Executing update on SPARQL endpoint: {} using UpdateRequest: {}", getOrigin().getWebResource().getURI(), updateRequest);
	ClientResponse cr = getClient().update(updateRequest, null);
        if (!cr.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
        {
            if (log.isDebugEnabled()) log.debug("Query request to endpoint: {} unsuccessful. Reason: {}", getOrigin().getWebResource().getURI(), cr.getStatusInfo().getReasonPhrase());
            throw new ClientException(cr.getStatusInfo());
        }        
    }
    
}