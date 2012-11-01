/*
 * Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graphity.ldp.model.query.impl;

import com.hp.hpl.jena.query.Query;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.graphity.ldp.model.query.ResultSetResource;
import org.graphity.util.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class EndpointResultSetResourceImpl extends org.graphity.model.query.impl.EndpointResultSetResourceImpl implements ResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(EndpointResultSetResourceImpl.class);

    private Request request = null;
    private MediaType mediaType = org.graphity.ldp.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE;
    private EntityTag entityTag = null;
    private Response response = null;
    
    public EndpointResultSetResourceImpl(String endpointUri, Query query,
	    Request request, MediaType mediaType)
    {
	super(endpointUri, query);
	if (request == null) throw new IllegalArgumentException("Request must be not null");
	//if (mediaType == null) throw new IllegalArgumentException("MediaType must be not null");
	this.request = request;
	if (mediaType != null) this.mediaType = mediaType;
	
	Response.ResponseBuilder rb = null;
	if (getResultSet().size() > 0)
	{
	    entityTag = new EntityTag(Long.toHexString(ResultSetUtils.hashResultSet(getResultSet())));
	    rb = request.evaluatePreconditions(entityTag);
	}

	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    response = rb.build();
	}
	else
	{
	    if (log.isTraceEnabled()) log.trace("Generating SPARQL results Response with MediaType: {} and EntityTag: {}", mediaType, entityTag);
	    response = Response.ok(getResultSet(), mediaType).tag(entityTag).build(); // uses ResultSetWriter
	}
    }

    @Override
    public Request getRequest()
    {
	return request;
    }

    @Override
    public Response getResponse()
    {
	return response;
    }

    public MediaType getMediaType()
    {
	return mediaType;
    }

    @Override
    public EntityTag getEntityTag()
    {
	return entityTag;
    }

}
