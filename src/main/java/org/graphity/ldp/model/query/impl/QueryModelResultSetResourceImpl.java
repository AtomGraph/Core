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
import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import org.graphity.ldp.model.query.ResultSetResource;
import org.graphity.util.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryModelResultSetResourceImpl extends org.graphity.model.query.impl.QueryModelResultSetResourceImpl implements ResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(QueryModelResultSetResourceImpl.class);

    private Request request = null;
    private List<Variant> variants = null;
    private Variant variant = null;
    private EntityTag entityTag = null;
    private Response response = null;

    public QueryModelResultSetResourceImpl(Model queryModel, Query query,
	    Request request, List<Variant> variants)
    {
	super(queryModel, query);
	if (request == null) throw new IllegalArgumentException("Request must be not null");
	//if (mediaType == null) throw new IllegalArgumentException("MediaType must be not null");
	this.request = request;
	this.variants = variants;
	
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
	    variant = request.selectVariant(variants);
	    if (variant == null)
	    {
		if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, variants);
		response = Response.notAcceptable(variants).build();
	    }	
	    else
	    {
		if (log.isTraceEnabled()) log.trace("Generating SPARQL results Response with Variant: {} and EntityTag: {}", variant, entityTag);
		response = Response.ok(getResultSet(), variant).tag(entityTag).build(); // uses ResultSetWriter
	    }	    
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

    @Override
    public List<Variant> getVariants()
    {
	return variants;
    }

    @Override
    public EntityTag getEntityTag()
    {
	return entityTag;
    }

}