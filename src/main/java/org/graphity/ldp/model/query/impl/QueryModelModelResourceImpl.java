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
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.LinkedDataResource;
import org.graphity.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryModelModelResourceImpl extends org.graphity.model.query.impl.QueryModelModelResourceImpl implements LinkedDataResource
{
    private static final Logger log = LoggerFactory.getLogger(QueryModelModelResourceImpl.class);

    private Request req = null;
    private UriInfo uriInfo = null;
    private MediaType mediaType = org.graphity.MediaType.APPLICATION_RDF_XML_TYPE;
    private EntityTag entityTag = null;

    public QueryModelModelResourceImpl(Model queryModel, Query query, 
	UriInfo uriInfo, Request req,
	MediaType mediaType)
    {
	super(queryModel, query);
	this.req = req;
	this.uriInfo = uriInfo;
	if (mediaType != null) this.mediaType = mediaType;
	
	entityTag = new EntityTag(Long.toHexString(ModelUtils.hashModel(getModel())));
    }

    public QueryModelModelResourceImpl(Model queryModel, String uri,
	UriInfo uriInfo, Request req,
	MediaType mediaType)
    {
	this(queryModel, QueryFactory.create("DESCRIBE <" + uri + ">"), uriInfo, req, mediaType);
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
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb.build();
	}
	else
	{
	    if (log.isTraceEnabled()) log.trace("Generating RDF Response");
	    return Response.ok(getModel(), getMediaType()).tag(getEntityTag()).build(); // uses ModelWriter
	}
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

    @Override
    public String getURI()
    {
	return getUriInfo().getAbsolutePath().toString();
    }

}