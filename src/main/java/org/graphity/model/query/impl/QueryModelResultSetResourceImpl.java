/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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

package org.graphity.model.query.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import org.graphity.model.query.QueryModelResource;
import org.graphity.model.query.ResultSetResource;
import org.graphity.util.manager.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryModelResultSetResourceImpl implements QueryModelResource, ResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(QueryModelResultSetResourceImpl.class);

    private Model queryModel = null;
    private Query query = null;
    private ResultSetRewindable resultSet = null;

    public QueryModelResultSetResourceImpl(Model queryModel, Query query)
    {
	if (queryModel == null) throw new IllegalArgumentException("Query Model must be not null");
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	this.queryModel = queryModel;
	this.query = query;
	
	if (log.isDebugEnabled()) log.debug("Querying Model: {} with Query: {}", queryModel, query);
	resultSet = DataManager.get().loadResultSet(queryModel, query);
    }

    @Override
    public ResultSetRewindable getResultSet()
    {
	resultSet.reset();
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
