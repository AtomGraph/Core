/*
 * Copyright (C) 2013 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.util;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.jena.fuseki.DatasetAccessor;
import org.apache.jena.fuseki.http.DatasetAdapter;
import org.graphity.query.QueryEngineHTTP;
import org.graphity.update.DatasetGraphAccessorHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Uses portions of Jena code
* (c) Copyright 2010 Epimorphics Ltd.
* All rights reserved.
*
* @see org.openjena.fuseki.FusekiLib
* {@link http://openjena.org}
*
 * @author Martynas Jusevičius <martynas@graphity.org>
*/

public class DataManager extends FileManager
{
    private static DataManager s_instance = null;

    private static final Logger log = LoggerFactory.getLogger(DataManager.class);

    private Context context;

    public static DataManager get() {
        if (s_instance == null) {
            s_instance = new DataManager(FileManager.get(), ARQ.getContext());
	    if (log.isDebugEnabled()) log.debug("new DataManager({}): {}", FileManager.get(), s_instance);
        }
        return s_instance;
    }

    public DataManager(FileManager fMgr, Context context)
    {
	super(fMgr);
	this.context = context;
    }

    public QueryExecution makeQueryExecution(String endpointURI, Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", endpointURI, query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryEngineHTTP request = new QueryEngineHTTP(endpointURI, query);
	if (params != null)
	    for (Entry<String, List<String>> entry : params.entrySet())
		if (!entry.getKey().equals("query")) // query param is handled separately
		    for (String value : entry.getValue())
		    {
			if (log.isTraceEnabled()) log.trace("Adding param to SPARQL request with name: {} and value: {}", entry.getKey(), value);
			request.addParam(entry.getKey(), value);
		    }
	
	return request;
    }
    
    public Model loadModel(String endpointURI, Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", endpointURI, query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryExecution qex = makeQueryExecution(endpointURI, query, params);
	try
	{
	    if (query.isConstructType()) return qex.execConstruct();
	    if (query.isDescribeType()) return qex.execDescribe();

	    throw new QueryExecException("Query to load Model must be CONSTRUCT or DESCRIBE");
	}
	finally
	{
	    qex.close();
	}
    }
    
    public Model loadModel(String endpointURI, Query query)
    {
	return loadModel(endpointURI, query, null);
    }
    
    public Model loadModel(Model model, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Model Query: {}", query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	
	QueryExecution qex = QueryExecutionFactory.create(query, model);
	try
	{	
	    if (query.isConstructType()) return qex.execConstruct();
	    if (query.isDescribeType()) return qex.execDescribe();
	
	    throw new QueryExecException("Query to load Model must be CONSTRUCT or DESCRIBE"); // return null;
	}
	finally
	{
	    qex.close();
	}
    }
    
    public Entry<String, Context> findEndpoint(String filenameOrURI)
    {
	if (getServiceContextMap() != null)
	{
	    Iterator<Entry<String, Context>> it = getServiceContextMap().entrySet().iterator();

	    while (it.hasNext())
	    {
		Entry<String, Context> endpoint = it.next(); 
		if (filenameOrURI.startsWith(endpoint.getKey()))
		    return endpoint;
	    }
	}
	
	return null;
    }

    public ResultSetRewindable loadResultSet(String endpointURI, Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query execution: {} ", endpointURI, query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryExecution qex = makeQueryExecution(endpointURI, query, params);
	try
	{
	    if (query.isSelectType()) return ResultSetFactory.copyResults(qex.execSelect());
	    
	    throw new QueryExecException("Query to load ResultSet must be SELECT");
	}
	finally
	{
	    qex.close();
	}
    }
    
    public ResultSetRewindable loadResultSet(String endpointURI, Query query)
    {
	return loadResultSet(endpointURI, query, null);
    }
    
    public ResultSetRewindable loadResultSet(Model model, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Model Query: {}", query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	
	QueryExecution qex = QueryExecutionFactory.create(query, model);
	try
	{
	    if (query.isSelectType()) return ResultSetFactory.copyResults(qex.execSelect());
	    
	    throw new QueryExecException("Query to load ResultSet must be SELECT");
	}
	finally
	{
	    qex.close();
	}
    }

    // uses graph store protocol - expects /sparql service!
    public void putModel(String endpointURI, String graphURI, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUTting Model to service {} with GRAPH URI {}", endpointURI, graphURI);
	
	DatasetAccessor accessor = new DatasetAdapter(new DatasetGraphAccessorHTTP(endpointURI));
	accessor.putModel(graphURI, model);
    }

    public void putModel(String endpointURI, Model model)
    {
	
    }

    public Context getContext()
    {
	return context;
    }

    public Map<String,Context> getServiceContextMap()
    {
	if (!getContext().isDefined(Service.serviceContext))
	{
	    Map<String,Context> serviceContext = new HashMap<String,Context>();
	    getContext().put(Service.serviceContext, serviceContext);
	}
	
	return (Map<String,Context>)getContext().get(Service.serviceContext);
    }

    public void addServiceContext(String endpointURI, Context context)
    {
	getServiceContextMap().put(endpointURI, context);
    }
    
    public void addServiceContext(String endpointURI)
    {
	addServiceContext(endpointURI, new Context());
    }

    public Context getServiceContext(String endpointURI)
    {
	return getServiceContextMap().get(endpointURI);
    }
}