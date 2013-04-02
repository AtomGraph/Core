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
package org.graphity.server.util;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
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

    public QueryExecution sparqlService(String endpointURI, Query query, MultivaluedMap<String, String> params)
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

	QueryExecution qex = sparqlService(endpointURI, query, params);
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

	QueryExecution qex = sparqlService(endpointURI, query, params);
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
	if (log.isDebugEnabled()) log.debug("PUTting Model to endpoint {} with GRAPH URI {}", endpointURI, graphURI);
	
	DatasetAccessor accessor = new DatasetAdapter(new DatasetGraphAccessorHTTP(endpointURI));
	accessor.putModel(graphURI, model);
    }

    public void putModel(String endpointURI, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUTting Model to endpoint {} default graph", endpointURI);
	
	DatasetAccessor accessor = new DatasetAdapter(new DatasetGraphAccessorHTTP(endpointURI));
	accessor.putModel(model);
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

    public void addServiceContext(Resource endpoint, Context context)
    {
	if (endpoint == null) throw new IllegalArgumentException("SPARQLEndpoint must be not null");
	getServiceContextMap().put(endpoint.getURI(), context);
    }

    public void addServiceContext(String endpointURI)
    {
	addServiceContext(endpointURI, new Context());
    }

    public void addServiceContext(Resource endpoint)
    {
	if (endpoint == null) throw new IllegalArgumentException("SPARQLEndpoint must be not null");
	addServiceContext(endpoint.getURI(), new Context());
    }

    public Context getServiceContext(String endpointURI)
    {
	return getServiceContextMap().get(endpointURI);
    }
    
    public Context getServiceContext(Resource endpoint)
    {
	if (endpoint == null) throw new IllegalArgumentException("SPARQLEndpoint must be not null");
	return getServiceContext(endpoint.getURI());
    }
    
    public boolean hasServiceContext(String endpointURI)
    {
	return getServiceContextMap().get(endpointURI) != null;
    }

    public boolean hasServiceContext(Resource endpoint)
    {
	if (endpoint == null) throw new IllegalArgumentException("SPARQLEndpoint must be not null");
	return hasServiceContext(endpoint.getURI());
    }

}