/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graphity.ldp.model.query.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.query.EndpointResultSetResource;
import org.graphity.util.manager.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class EndpointResultSetResourceImpl implements EndpointResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(EndpointResultSetResourceImpl.class);

    private String endpointUri = null;
    private Query query = null;
    private ResultSet resultSet = null;
    private Request request = null;
    private UriInfo uriInfo = null;
    private MediaType mediaType = org.graphity.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE;

    public EndpointResultSetResourceImpl(String endpointUri, Query query,
	    UriInfo uriInfo, Request request,
	    MediaType mediaType)
    {
	if (endpointUri == null) throw new IllegalArgumentException("Endpoint URI must be not null");
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	this.endpointUri = endpointUri;
	this.query = query;
	this.request = request;
	this.uriInfo = uriInfo;
	if (mediaType != null) this.mediaType = mediaType;
	
	if (log.isDebugEnabled()) log.debug("Endpoint URI: {} Query: {}", endpointUri, query);
	if (log.isDebugEnabled()) log.debug("Querying remote service: {} with Query: {}", endpointUri, query);
	resultSet = DataManager.get().loadResultSet(endpointUri, query);
    }

    @Override
    public ResultSet getResultSet()
    {
	return resultSet;
    }

    @Override
    public String getEndpointURI()
    {
	return endpointUri;
    }

    @Override
    public Query getQuery()
    {
	return query;
    }

    @Override
    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    @Override
    public Request getRequest()
    {
	return request;
    }
    
    @Override
    public Response getResponse()
    {
	if (log.isDebugEnabled()) log.debug("Accept param: {}, writing SPARQL results (XML or JSON)", getMediaType());

	// uses ResultSetWriter
	return Response.ok(getResultSet(), getMediaType()).build();
    }

    public MediaType getMediaType()
    {
	return mediaType;
    }

    @Override
    public EntityTag getEntityTag()
    {
	//return super.getEntityTag();
	return null;
    }

}
