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
package org.graphity.model.query.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import org.graphity.model.query.EndpointResource;
import org.graphity.model.query.ResultSetResource;
import org.graphity.util.manager.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class EndpointResultSetResourceImpl implements EndpointResource, ResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(EndpointResultSetResourceImpl.class);

    private String endpointUri = null;
    private Query query = null;
    private ResultSet resultSet = null;

    public EndpointResultSetResourceImpl(String endpointUri, Query query)
    {
	if (endpointUri == null) throw new IllegalArgumentException("Endpoint URI must be not null");
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	this.endpointUri = endpointUri;
	this.query = query;

	if (log.isDebugEnabled()) log.debug("Querying remote endpoint: {} with Query: {}", endpointUri, query);
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

}
