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
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.PreemptiveBasicAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.web.DatasetAdapter;
import org.graphity.server.vocabulary.GS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Utility class for retrieval of SPARQL query results from local RDF models and remote endpoints.
* Uses portions of Jena code
* (c) Copyright 2010 Epimorphics Ltd.
* All rights reserved.
*
* @see org.openjena.fuseki.FusekiLib
* {@link http://jena.apache.org}
*
* @author Martynas Jusevičius <martynas@graphity.org>
* @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/FileManager.html">Jena FileManager</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/sparql/util/Context.html">ARQ Context</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Jena Model</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSet.html">ARQ ResultSet</a>
*/

public class DataManager extends FileManager
{

    private static final Logger log = LoggerFactory.getLogger(DataManager.class);

    private final Context context;
    private final ServletContext servletContext;
    
    /**
     * Creates data manager from file manager and SPARQL context.
     * @param mapper location mapper
     * @param context SPARQL context
     * @param servletContext webapp context
     */
    public DataManager(LocationMapper mapper, Context context, ServletContext servletContext)
    {
	super(mapper);
	if (context == null) throw new IllegalArgumentException("Context cannot be null");
	if (servletContext == null) throw new IllegalArgumentException("ServletContext cannot be null");
	this.context = context;
        this.servletContext = servletContext;
    }

    /**
     * Creates remote SPARQL execution based on a query and optional request parameters
     * 
     * @param endpointURI remote endpoint URI
     * @param query query object
     * @param params name/value pairs of request parameters or null, if none
     * @return query execution
     */
    public QueryExecution sparqlService(String endpointURI, Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", endpointURI, query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryEngineHTTP request = new QueryEngineHTTP(endpointURI, query, getHttpAuthenticator(endpointURI));
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
    
    /**
     * Loads RDF model from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param endpointURI remote endpoint URI
     * @param query query object
     * @param params name/value pairs of request parameters or null, if none
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
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
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Remote query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }
    
    /**
     * Loads RDF model from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * This is a convenience method for {@link loadModel(String,Query,MultivaluedMap<String, String>)}
     * with null request parameters.
     * 
     * @param endpointURI remote endpoint URI
     * @param query query object
     * @return RDF model result
     */
    public Model loadModel(String endpointURI, Query query)
    {
	return loadModel(endpointURI, query, null);
    }
    
    /**
     * Loads RDF model from another RDF model using a SPARQL query.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param model the RDF model to be queried
     * @param query query object
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
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
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }

    /**
     * Loads RDF model from an RDF dataset using a SPARQL query.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    public Model loadModel(Dataset dataset, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	
	QueryExecution qex = QueryExecutionFactory.create(query, dataset);
	try
	{	
	    if (query.isConstructType()) return qex.execConstruct();
	    if (query.isDescribeType()) return qex.execDescribe();
	
	    throw new QueryExecException("Query to load Model must be CONSTRUCT or DESCRIBE"); // return null;
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }

    /**
     * Loads result set from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>SELECT</code> queries can be used with this method.
     * 
     * @param endpointURI remote endpoint URI
     * @param query query object
     * @param params name/value pairs of request parameters or null, if none
     * @return result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
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
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Remote query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }
    
    /**
     * Loads result set from a remote SPARQL endpoint using a query.
     * Only <code>SELECT</code> queries can be used with this method.
     * This is a convenience method for {@link loadResultSet(String,Query,MultivaluedMap<String, String>)} with
     * null request parameters.
     * 
     * @param endpointURI remote endpoint URI
     * @param query query object
     * @return result set
     */
    public ResultSetRewindable loadResultSet(String endpointURI, Query query)
    {
	return loadResultSet(endpointURI, query, null);
    }
    
    /**
     * Loads result set from an RDF model using a SPARQL query.
     * Only <code>SELECT</code> queries can be used with this method.
     * 
     * @param model the RDF model to be queried
     * @param query query object
     * @return result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
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
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }

    /**
     * Loads result set from an RDF dataset using a SPARQL query.
     * Only <code>SELECT</code> queries can be used with this method.
     * 
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    public ResultSetRewindable loadResultSet(Dataset dataset, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	
	QueryExecution qex = QueryExecutionFactory.create(query, dataset);
	try
	{
	    if (query.isSelectType()) return ResultSetFactory.copyResults(qex.execSelect());
	    
	    throw new QueryExecException("Query to load ResultSet must be SELECT");
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }

    /**
     * Returns boolean result from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>ASK</code> queries can be used with this method.
     * 
     * @param endpointURI remote endpoint URI
     * @param query query object
     * @param params name/value pairs of request parameters or null, if none
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public boolean ask(String endpointURI, Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query execution: {} ", endpointURI, query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryExecution qex = sparqlService(endpointURI, query, params);
	try
	{
	    if (query.isAskType()) return qex.execAsk();
	    
	    throw new QueryExecException("Query to load ResultSet must be ASK");
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Remote query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }

    /**
     * Loads result set from a remote SPARQL endpoint using a query.
     * Only <code>ASK</code> queries can be used with this method.
     * This is a convenience method for {@link ask(String,Query,MultivaluedMap<String, String>)} with
     * null request parameters.
     * 
     * @param endpointURI remote endpoint URI
     * @param query query object
     * @return boolean result
     */
    public boolean ask(String endpointURI, Query query)
    {
	return ask(endpointURI, query, null);
    }

    /**
     * Returns boolean result from an RDF model using a SPARQL query.
     * Only <code>ASK</code> queries can be used with this method.
     *
     * @param model the RDF model to be queried
     * @param query query object
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public boolean ask(Model model, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Model Query: {}", query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryExecution qex = QueryExecutionFactory.create(query, model);
	try
	{
	    if (query.isAskType()) return qex.execAsk();

	    throw new QueryExecException("Query to load ResultSet must be SELECT");
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }

    /**
     * Returns boolean result from an RDF dataset using a SPARQL query.
     * Only <code>ASK</code> queries can be used with this method.
     *
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public boolean ask(Dataset dataset, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryExecution qex = QueryExecutionFactory.create(query, dataset);
	try
	{
	    if (query.isAskType()) return qex.execAsk();

	    throw new QueryExecException("Query to load ResultSet must be SELECT");
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }

    /**
     * Executes update request on a remote SPARQL endpoint.
     * 
     * @param endpointURI remote endpoint URI
     * @param updateRequest update request
     */
    public void executeUpdateRequest(String endpointURI, UpdateRequest updateRequest)
    {
        UpdateExecutionFactory.createRemote(updateRequest, endpointURI, getHttpAuthenticator(endpointURI)).execute();
    }

    /**
     * Checks whether Graph Store contains a certain named graph.
     * 
     * @param graphStoreURI remote graph store URI
     * @param graphURI named graph URI
     * @return true if graph store contains named graph, false otherwise
     */
    public boolean containsModel(String graphStoreURI, String graphURI)
    {
	if (log.isDebugEnabled()) log.debug("Checking if Graph Store {} contains GRAPH with URI {}", graphStoreURI, graphURI);
        return getDatasetAccessor(graphStoreURI).containsModel(graphURI);
    }
    
    /**
     * Loads RDF model from the default graph on a remote SPARQL Graph Store.
     * In comparison, <code>loadModel()</code> variants operate on SPARQL Protocol, but can be used for the
     * same purpose.
     * 
     * @param graphStoreURI Graph Store URI
     * @return RDF model of the default graph
     */
    public Model getModel(String graphStoreURI)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", graphStoreURI);
        return getDatasetAccessor(graphStoreURI).getModel();
    }
    
    /**
     * Loads RDF model from a named graph on a remote SPARQL Graph Store.
     * In comparison, <code>loadModel()</code> variants operate on SPARQL Protocol, but can be used for the
     * same purpose.
     * 
     * @param graphStoreURI Graph Store URI
     * @param graphURI named graph URI
     * @return RDF model of the named graph
     */
    public Model getModel(String graphStoreURI, String graphURI)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", graphStoreURI, graphURI);
	return getDatasetAccessor(graphStoreURI).getModel(graphURI);	
    }

    /**
     * Adds RDF model to the default graph on a remote SPARQL Graph Store.
     * 
     * @param graphStoreURI remote graph store URI
     * @param model RDF model to be added
     */
    public void addModel(String graphStoreURI, Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", graphStoreURI);
	getDatasetAccessor(graphStoreURI).add(model);
    }
    
    /**
     * Adds RDF model to a named graph on a remote SPARQL Graph Store.
     * 
     * @param graphStoreURI remote graph store URI
     * @param graphURI named graph URI
     * @param model RDF model to be added
     */
    public void addModel(String graphStoreURI, String graphURI, Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", graphStoreURI, graphURI);
	getDatasetAccessor(graphStoreURI).add(graphURI, model);
    }

    /**
     * Stores RDF model into the default graph on a remote SPARQL Graph Store.
     * Uses SPARQL Graph Store protocol.
     * 
     * @param graphStoreURI remote graph store URI
     * @param model RDF model to be stored
     */
    public void putModel(String graphStoreURI, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", graphStoreURI);
	getDatasetAccessor(graphStoreURI).putModel(model);
    }

    /**
     * Creates/replaces a named graph on a remote SPARQL Graph Store and stores RDF model.
     * Uses SPARQL Graph Store protocol.
     * 
     * @param graphStoreURI remote graph store URI
     * @param graphURI named graph URI
     * @param model RDF model to be stored
     */
    public void putModel(String graphStoreURI, String graphURI, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", graphStoreURI, graphURI);
	getDatasetAccessor(graphStoreURI).putModel(graphURI, model);
    }

    /**
     * Deletes contents of the default graph on a remote SPARQL Graph Store.
     * Uses SPARQL Graph Store protocol.
     * 
     * @param graphStoreURI remote graph store URI
     */
    public void deleteDefault(String graphStoreURI)
    {
	if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", graphStoreURI);
	getDatasetAccessor(graphStoreURI).deleteDefault();
    }

    /**
     * Deletes contents of a named graph on a remote SPARQL Graph Store.
     * Uses SPARQL Graph Store protocol.
     * 
     * @param graphStoreURI remote graph store URI
     * @param graphURI named graph URI
     */
    public void deleteModel(String graphStoreURI, String graphURI)
    {
	if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", graphURI, graphStoreURI);
        getDatasetAccessor(graphStoreURI).deleteModel(graphURI);
    }

    /**
     * Returns dataset accessor for a given graph store URI.
     * 
     * @param graphStoreURI graph store URI
     * @return accessor
     */
    public DatasetAccessor getDatasetAccessor(String graphStoreURI)
    {
        HttpAuthenticator authenticator = getHttpAuthenticator(graphStoreURI);

        if (authenticator != null) return new DatasetAdapter(new DatasetGraphAccessorHTTP(graphStoreURI, authenticator));
        else return new DatasetAdapter(new DatasetGraphAccessorHTTP(graphStoreURI));
    }
    
    /**
     * Returns authenticator (used by Jena) for a given endpoint or graph store URI.
     * 
     * @param endpointURI endpoint or graph store URI
     * @return authenticator
     */
    public HttpAuthenticator getHttpAuthenticator(String endpointURI)
    {
        Context serviceContext = getServiceContext(endpointURI);
        if (serviceContext == null) return null;
        
        return getHttpAuthenticator(serviceContext);
    }
    
    /**
     * Converts context with username/password credentials into authenticator (used by Jena).
     * 
     * @param serviceContext authentication context
     * @return authenticator
     */
    public HttpAuthenticator getHttpAuthenticator(Context serviceContext)
    {
        if (serviceContext == null) throw new IllegalArgumentException("Context cannot be null");

        String usr = serviceContext.getAsString(Service.queryAuthUser);
        String pwd = serviceContext.getAsString(Service.queryAuthPwd);

        if (usr != null || pwd != null)
        {
            usr = usr==null?"":usr;
            pwd = pwd==null?"":pwd;

            if (getServletContext() != null && getServletContext().getInitParameter(GS.preemptiveAuth.getURI()) != null)
            {
                boolean preemptive = Boolean.parseBoolean(getServletContext().getInitParameter(GS.preemptiveAuth.getURI()).toString());
                if (preemptive)
                {
                    if (log.isDebugEnabled()) log.debug("Creating PreemptiveBasicAuthenticator for Context {} with username: {} ", serviceContext, usr);
                    return new PreemptiveBasicAuthenticator(new SimpleAuthenticator(usr, pwd.toCharArray()));
                }
            }

            if (log.isDebugEnabled()) log.debug("Creating SimpleAuthenticator for Context {} with username: {} ", serviceContext, usr);
            return new SimpleAuthenticator(usr, pwd.toCharArray());
        }

        return null;
    }
    
    /**
     * Returns SPARQL context
     * 
     * @return global context
     */
    public Context getContext()
    {
	return context;
    }

    /**
     * Given a URI (e.g. with encoded SPARQL query string), finds matching SPARQL endpoint in the service
     * context map.
     * 
     * @param filenameOrURI SPARQL request URI
     * @return matching map entry, or null if none
     */
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

    /**
     * Returns the service context map. Endpoint URIs are used as keys.
     * 
     * @return service context map
     */
    public Map<String,Context> getServiceContextMap()
    {
	if (!getContext().isDefined(Service.serviceContext))
	{
	    Map<String,Context> serviceContext = new HashMap<>();
	    getContext().put(Service.serviceContext, serviceContext);
	}
	
	return (Map<String,Context>)getContext().get(Service.serviceContext);
    }

    /**
     * Adds service context for a SPARQL endpoint.
     * 
     * @param endpointURI endpoint URI
     * @param context context
     */
    public void addServiceContext(String endpointURI, Context context)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");
	
	getServiceContextMap().put(endpointURI, context);
    }

    /**
     * Adds service context for a SPARQL endpoint.
     * 
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     * @param context context
     */
    public void addServiceContext(Resource endpoint, Context context)
    {
	if (endpoint == null) throw new IllegalArgumentException("Endpoint Resource must be not null");
	if (!endpoint.isURIResource()) throw new IllegalArgumentException("Endpoint Resource must be URI Resource (not a blank node)");
	
	getServiceContextMap().put(endpoint.getURI(), context);
    }

    /**
     * Adds empty service context for a SPARQL endpoint.
     * 
     * @param endpointURI endpoint URI
     */
    public void addServiceContext(String endpointURI)
    {
	addServiceContext(endpointURI, new Context());
    }

    /**
     * Adds empty service context for a SPARQL endpoint.
     *
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     */
    public void addServiceContext(Resource endpoint)
    {
	addServiceContext(endpoint, new Context());
    }

    /**
     * Returns service context of a SPARQL endpoint.
     * 
     * @param endpointURI endpoint URI
     * @return context of the endpoint
     */
    public Context getServiceContext(String endpointURI)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");

	return getServiceContextMap().get(endpointURI);
    }
    
    public Context putServiceContext(String endpointURI, Context context)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");
	if (context == null) throw new IllegalArgumentException("Context must be not null");

        return getServiceContextMap().put(endpointURI, context);
    }
    
    /**
     * Returns service context of a SPARQL endpoint.
     * 
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     * @return context of the endpoint
     */    
    public Context getServiceContext(Resource endpoint)
    {
	if (endpoint == null) throw new IllegalArgumentException("Endpoint Resource must be not null");
	if (!endpoint.isURIResource()) throw new IllegalArgumentException("Endpoint Resource must be URI Resource (not a blank node)");

	return getServiceContext(endpoint.getURI());
    }

    /**
     * Checks if SPARQL endpoint has service context.
     * 
     * @param endpointURI endpoint URI
     * @return true if endpoint URI is bound to a context, false otherwise
     */    
    public boolean hasServiceContext(String endpointURI)
    {
	return getServiceContext(endpointURI) != null;
    }

    /**
     * Checks if SPARQL endpoint has service context.
     * 
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     * @return true if endpoint resource is bound to a context, false otherwise
     */    
    public boolean hasServiceContext(Resource endpoint)
    {
	return getServiceContext(endpoint) != null;
    }

    /**
     * Configures HTTP Basic authentication for SPARQL service context
     * 
     * @param endpointURI endpoint or graph store URI
     * @param authUser username
     * @param authPwd password
     * @return service context
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/sparql/util/Context.html">Context</a>
     */
    public Context putAuthContext(String endpointURI, String authUser, String authPwd)
    {
	if (endpointURI == null) throw new IllegalArgumentException("SPARQL endpoint URI cannot be null");
	if (authUser == null) throw new IllegalArgumentException("SPARQL endpoint authentication username cannot be null");
	if (authPwd == null) throw new IllegalArgumentException("SPARQL endpoint authentication password cannot be null");

	if (log.isDebugEnabled()) log.debug("Setting username/password credentials for SPARQL endpoint: {}", endpointURI);
	Context queryContext = new Context();
	queryContext.put(Service.queryAuthUser, authUser);
	queryContext.put(Service.queryAuthPwd, authPwd);

        return putServiceContext(endpointURI, queryContext);
    }

    public ServletContext getServletContext()
    {
        return servletContext;
    }
    
}