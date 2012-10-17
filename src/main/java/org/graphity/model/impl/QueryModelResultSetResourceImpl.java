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
package org.graphity.model.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import org.graphity.model.query.QueryModelResultSetResource;
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

    public QueryModelResultSetResourceImpl(Model queryModel, Query query)
    {
	if (queryModel == null) throw new IllegalArgumentException("Query Model must be not null");
	if (query == null || !query.isSelectType()) throw new IllegalArgumentException("Query must be not null and SELECT");
	this.queryModel = queryModel;
	this.query = query;
	if (log.isDebugEnabled()) log.debug("Query Model: {} Query: {}", queryModel, query);
    }

    @Override
    public ResultSet getResultSet()
    {
	if (resultSet == null)
	{
	    if (log.isDebugEnabled()) log.debug("Querying Model: {} with Query: {}", getQueryModel(), getQuery());
	    resultSet = DataManager.get().loadResultSet(getQueryModel(), getQuery());
	}

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

}
