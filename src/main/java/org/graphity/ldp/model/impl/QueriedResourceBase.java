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
package org.graphity.ldp.model.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.graphity.model.QueriedResource;
import org.graphity.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
abstract public class QueriedResourceBase extends ResourceBase implements QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(QueriedResourceBase.class);

    private String endpointUri = null;
    private Query query = null;
    private Model queryModel, model = null;

    public QueriedResourceBase(UriInfo uriInfo, Request req, String endpointUri, Query query)
    {
	super(uriInfo, req);
	if (endpointUri == null || query == null || !(query.isConstructType() || query.isDescribeType())) throw new IllegalArgumentException("Endpoint URI and Query must be not null; Query must be CONSTRUCT or DESCRIBE");
	this.endpointUri = endpointUri;
	this.query = query;
	if (log.isDebugEnabled()) log.debug("Endpoint URI: {} Query: {}", endpointUri, query);
    }

    public QueriedResourceBase(UriInfo uriInfo, Request req, String endpointUri, String uri)
    {
	this(uriInfo, req, endpointUri, QueryFactory.create("DESCRIBE <" + uri + ">"));
    }

    public QueriedResourceBase(UriInfo uriInfo, Request req, Model queryModel, Query query)
    {
	super(uriInfo, req);
	if (queryModel == null || query == null || !(query.isConstructType() || query.isDescribeType())) throw new IllegalArgumentException("Endpoint URI and query Model must be not null; Query must be CONSTRUCT or DESCRIBE");
	this.queryModel = queryModel;
	this.query = query;
	if (log.isDebugEnabled()) log.debug("Model: {} Query: {}", queryModel, query);
    }

    public QueriedResourceBase(UriInfo uriInfo, Request req, Model queryModel, String uri)
    {
	this(uriInfo, req, queryModel, QueryFactory.create("DESCRIBE <" + uri + ">"));
    }

    @Override
    public Model getModel()
    {
	if (model == null)
	{
	    if (queryModel != null) model = ResourceFactory.getResource(queryModel, getQuery()).getModel();
	    else model = ResourceFactory.getResource(endpointUri, getQuery()).getModel();
	}

	return model;
    }

    @Override
    public Query getQuery()
    {
	return query;
    }

}
