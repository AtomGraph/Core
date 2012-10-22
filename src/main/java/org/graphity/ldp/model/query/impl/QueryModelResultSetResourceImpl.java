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
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.query.QueryModelResultSetResource;
import org.graphity.util.manager.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryModelResultSetResourceImpl implements QueryModelResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(QueryModelResultSetResourceImpl.class);

    private Model queryModel = null;
    private Query query = null;
    private ResultSet resultSet = null;
    private Request req = null;
    private UriInfo uriInfo = null;
    private MediaType mediaType = org.graphity.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE;
    private Response response = null;

    public QueryModelResultSetResourceImpl(Model queryModel, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	if (queryModel == null) throw new IllegalArgumentException("Query Model must be not null");
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	this.queryModel = queryModel;
	this.query = query;
	this.req = req;
	this.uriInfo = uriInfo;
	if (mediaType != null) this.mediaType = mediaType;
	
	if (log.isDebugEnabled()) log.debug("Querying Model: {} with Query: {}", queryModel, query);
	resultSet = DataManager.get().loadResultSet(queryModel, query);
    }

    @Override
    public ResultSet getResultSet()
    {
	return resultSet;
    }

    @Override
    public Query getQuery()
    {
	return query;
    }

    @Override
    public Model getQueryModel()
    {
	return queryModel;
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