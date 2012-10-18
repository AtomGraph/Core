/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graphity.ldp.model.impl;

import com.hp.hpl.jena.query.Query;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.query.EndpointResultSetResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class EndpointResultSetResourceImpl extends org.graphity.model.impl.EndpointResultSetResourceImpl implements EndpointResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(EndpointResultSetResourceImpl.class);

    private Request req = null;
    private UriInfo uriInfo = null;
    private MediaType mediaType = org.graphity.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE;

    public EndpointResultSetResourceImpl(String endpointUri, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	super(endpointUri, query);
	this.req = req;
	this.uriInfo = uriInfo;
	if (mediaType != null) this.mediaType = mediaType;
    }

    @Override
    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    @Override
    public Request getRequest()
    {
	return req;
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
