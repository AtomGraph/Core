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
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.XMLInput;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.InputStream;
import java.net.URI;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import org.graphity.core.MediaType;
import org.graphity.core.MediaTypes;
import org.graphity.core.model.Origin;
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

    private final Origin origin;
    private final javax.ws.rs.core.MediaType[] modelMediaTypes, resultSetMediaTypes;

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
        modelMediaTypes = mediaTypes.getModelMediaTypes().toArray(new javax.ws.rs.core.MediaType[mediaTypes.getModelMediaTypes().size()]);
        resultSetMediaTypes = mediaTypes.getResultSetMediaTypes().toArray(new javax.ws.rs.core.MediaType[mediaTypes.getResultSetMediaTypes().size()]);
    }
    
    @Override
    public Origin getOrigin()
    {
        return origin;
    }

    public javax.ws.rs.core.MediaType[] getModelMediaTypes()
    {
        return modelMediaTypes;
    }

    public javax.ws.rs.core.MediaType[] getResultSetMediaTypes() {
        return resultSetMediaTypes;
    }

    @Override
    public Model loadModel(Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading Model from SPARQL endpoint: {} using Query: {}", getOrigin().getURI(), query);
	return executeQuery(query, getModelMediaTypes()).
                getEntity(Model.class);
    }

    @Override
    public ResultSetRewindable select(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isSelectType()) throw new IllegalArgumentException("Query must be SELECT");
        
	if (log.isDebugEnabled()) log.debug("Loading ResultSet from SPARQL endpoint: {} using Query: {}", getOrigin().getURI(), query);
	return executeQuery(query, getResultSetMediaTypes()).
            getEntity(ResultSetRewindable.class);
    }

    @Override
    public boolean ask(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isAskType()) throw new IllegalArgumentException("Query must be ASK");
        
	return ask(query, null);
    }

    @Override
    public void update(UpdateRequest updateRequest)
    {
	if (log.isDebugEnabled()) log.debug("Executing update on SPARQL endpoint: {} using UpdateRequest: {}", getOrigin().getURI(), updateRequest);
	executeUpdateRequest(updateRequest, null);
    }

    /**
     * Loads RDF model from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param query query object
     * @param acceptedTypes accepted media types
     * @param params name/value pairs of request parameters or null, if none
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    public ClientResponse executeQuery(Query query, javax.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {}", getOrigin().getURI(), query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	if (acceptedTypes == null) throw new IllegalArgumentException("Accepted MediaType[] must be not null");

        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("query", query.toString());
        
        return getEndpoint(params).
            accept(acceptedTypes).
            type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
            post(ClientResponse.class, formData);
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
    public ClientResponse executeQuery(Query query, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
	return executeQuery(query, acceptedTypes, null);
    }
    
    /**
     * Returns boolean result from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>ASK</code> queries can be used with this method.
     * 
     * @param query query object
     * @param params name/value pairs of request parameters or null, if none
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public boolean ask(Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query execution: {} ", getOrigin().getURI(), query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("query", query.toString());
        
        return XMLInput.booleanFromXML(getEndpoint(params).
            accept(MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE). // needs to be XML since we're reading with XMLInput
            type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
            post(ClientResponse.class, formData).
            getEntity(InputStream.class));
    }

    /**
     * Executes update request on a remote SPARQL endpoint.
     * 
     * @param updateRequest update request
     * @param params name/value pairs of request parameters or null, if none
     * @return client response
     */
    public ClientResponse executeUpdateRequest(UpdateRequest updateRequest, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", getOrigin().getURI(), updateRequest);
	if (updateRequest == null) throw new IllegalArgumentException("UpdateRequest must be not null");
	//if (acceptedTypes == null) throw new IllegalArgumentException("Accepted MediaType[] must be not null");

        MultivaluedMap formData = new MultivaluedMapImpl();
        if (params != null) formData.putAll(params);
        formData.putSingle("update", updateRequest.toString());
        
	return getEndpoint(params).
            //accept(acceptedTypes).
            type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
            post(ClientResponse.class, formData);
    }
            
    public WebResource getEndpoint(MultivaluedMap<String, String> params)
    {
        Client client = Client.create(new DefaultClientConfig());
        if (getOrigin().getUsername() != null && getOrigin().getPassword() != null)
            client.addFilter(new HTTPBasicAuthFilter(getOrigin().getUsername(), getOrigin().getPassword()));
        if (log.isDebugEnabled()) client.addFilter(new LoggingFilter(System.out));
        
        return client.resource(URI.create(getOrigin().getURI()));
    }
    
}