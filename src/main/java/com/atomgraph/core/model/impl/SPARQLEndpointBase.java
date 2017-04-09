/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.model.impl;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.*;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.SPARQLEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of SPARQL endpoints.
 * Unfortunately cannot extend ResourceBase because of clashing JAX-RS method annotations.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.atomgraph.core.model.SPARQLEndpoint
 */
public abstract class SPARQLEndpointBase implements SPARQLEndpoint
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);

    private final com.atomgraph.core.Application application;
    private final Request request;
    private final com.atomgraph.core.model.impl.Response response;
    
    /**
     * Constructs SPARQL endpoint from request metadata.
     * 
     * @param application application
     * @param request current request
     */
    public SPARQLEndpointBase(@Context Application application, @Context Request request)
    {
	if (application == null) throw new IllegalArgumentException("Application cannot be null");
        if (request == null) throw new IllegalArgumentException("Request cannot be null");

        this.application = (com.atomgraph.core.Application)application;
	this.request = request;
        this.response = com.atomgraph.core.model.impl.Response.fromRequest(request);
	if (log.isDebugEnabled()) log.debug("Constructing SPARQLEndpointBase");        
    }
    
    /**
     * Implements SPARQL 1.1 Protocol query GET method.
     * Query object is injected using a provider, which must be registered in the application.
     * 
     * @param query SPARQL query
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return result response
     * @see com.atomgraph.core.provider.QueryParamProvider
     */
    @Override
    @GET
    public Response get(@QueryParam("query") Query query,
	@QueryParam("default-graph-uri") URI defaultGraphUri, @QueryParam("named-graph-uri") URI graphUri)
    {
	return getResponseBuilder(query).build();
    }
    
    /**
     * Implements SPARQL 1.1 Protocol query direct POST method.
     * Query object is injected using a provider, which must be registered in the application.
     * 
     * @param query SPARQL query
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return result response
     */
    @Override
    @POST
    @Consumes(com.atomgraph.core.MediaType.APPLICATION_SPARQL_QUERY)
    public Response post(Query query, @QueryParam("default-graph-uri") URI defaultGraphUri,
	@QueryParam("named-graph-uri") URI graphUri)
    {
	return get(query, defaultGraphUri, graphUri);
    }
    
    /**
     * Implements SPARQL 1.1 Protocol encoded POST method.
     * Query or update object are injected as form parameters.
     * 
     * @param queryString
     * @param updateString SPARQL update (possibly multiple operations)
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return response with success or failure
     */
    @Override
    @POST
    @Consumes(com.atomgraph.core.MediaType.APPLICATION_FORM_URLENCODED)
    public Response post(@FormParam("query") String queryString, @FormParam("update") String updateString,
	@FormParam("using-graph-uri") URI defaultGraphUri,
	@FormParam("using-named-graph-uri") URI graphUri)
    {
        if (queryString != null) return get(QueryFactory.create(queryString), defaultGraphUri, graphUri);
        if (updateString != null) return post(UpdateFactory.create(updateString), defaultGraphUri, graphUri);

        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    
    /**
     * Implements SPARQL 1.1 Protocol update direct POST method.
     * Update object is injected using a provider, which must be registered in the application.
     * 
     * @param update update request (possibly multiple operations)
     * @param defaultGraphUri default graph URI
     * @param graphUri named graph URI
     * @return response with success or failure
     */
    @Override
    @POST
    @Consumes(com.atomgraph.core.MediaType.APPLICATION_SPARQL_UPDATE)
    public Response post(UpdateRequest update, @QueryParam("using-graph-uri") URI defaultGraphUri,
	@QueryParam("using-named-graph-uri") URI graphUri)
    {
	update(update);

        return Response.ok().build();
    }

    /**
     * Returns response builder for a SPARQL query.
     * Contains the main SPARQL endpoint JAX-RS implementation logic.
     * Uses <code>gs:resultLimit</code> parameter value from web.xml as <code>LIMIT</code> value on <code>SELECT</code> queries, if present.
     * 
     * @param query SPARQL query
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Query query)
    {
	if (query == null) throw new WebApplicationException(Response.Status.BAD_REQUEST);

        if (query.isSelectType())
        {
            /*
            if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing SELECT query: {}", query);
            if (getServletConfig().getInitParameter(A.resultLimit.getURI()) != null)
                query.setLimit(Long.parseLong(getServletConfig().getInitParameter(A.resultLimit.getURI())));
            */

            return getResponseBuilder(select(query));
        }

        if (query.isConstructType() || query.isDescribeType())
        {
            if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing CONSTRUCT/DESCRIBE query: {}", query);
            return getResponseBuilder(loadModel(query));
        }
        
	if (log.isWarnEnabled()) log.warn("SPARQL endpoint received unknown type of query: {}", query);
	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    /**
     * Returns response for the given RDF model.
     * 
     * @param model RDF model
     * @return response object
     */
    public Response getResponse(Model model)
    {
        return getResponseBuilder(model).build();
    }

    /**
     * Returns response builder for the given RDF model.
     * 
     * @param model RDF model
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Model model)
    {
        return com.atomgraph.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(model, getVariants(getMediaTypes().getWritable(Model.class)));
    }
        
    /**
     * Returns response builder for the given SPARQL result set.
     * 
     * @param resultSet result set
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet)
    {
	return com.atomgraph.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(resultSet, getVariants(getMediaTypes().getWritable(ResultSet.class)));
    }
    
    /**
     * Builds a list of acceptable response variants
     * 
     * @param mediaTypes
     * @return supported variants
     */
    public List<Variant> getVariants(List<MediaType> mediaTypes)
    {
        return getResponse().getVariantListBuilder(mediaTypes, getLanguages(), getEncodings()).add().build();
    }
        
    /**
     * Returns supported languages.
     * 
     * @return list of languages
    */
    public List<Locale> getLanguages()
    {
        return new ArrayList<>();
    }

    /**
     * Returns supported HTTP encodings.
     * Note: this is different from content encodings such as UTF-8.
     * 
     * @return list of encodings
     */
    public List<String> getEncodings()
    {
        return new ArrayList<>();
    }

    /**
     * Convenience method for <pre>DESCRIBE</pre> queries.
     * 
     * @link #loadModel(query)
     * @param query
     * @return RDF model
     */
    //@Override
    public Model describe(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isDescribeType()) throw new IllegalArgumentException("Query must be DESCRIBE");
        
	return loadModel(query);
    }

    /**
     * Convenience method for <pre>CONSTRUCT</pre> queries.
     * 
     * @link #loadModel(query)
     * @param query
     * @return RDF model
     */
    //@Override
    public Model construct(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isConstructType()) throw new IllegalArgumentException("Query must be CONSTRUCT");
        
	return loadModel(query);
    }

    public com.atomgraph.core.Application getApplication()
    {
        return (com.atomgraph.core.Application)application;
    }
    
    public Request getRequest()
    {
	return request;
    }
    
    public MediaTypes getMediaTypes()
    {
        return getApplication().getMediaTypes();
    }
    
    public com.atomgraph.core.model.impl.Response getResponse()
    {
        return response;
    }
 
    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<code>DESCRIBE</code> or <code>CONSTRUCT</code>)
     * 
     * @param query SPARQL query
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    public abstract Model loadModel(Query query);
    
    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<pre>SELECT</pre>)
     * 
     * @param query SPARQL query
     * @return SPARQL result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    public abstract ResultSetRewindable select(Query query);

    /**
     * Asks boolean result from the endpoint by executing a SPARQL query (<pre>ASK</pre>)
     * 
     * @param query SPARQL query
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public abstract boolean ask(Query query);

    /**
     * Execute SPARQL update request
     * 
     * @param updateRequest update request
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-update-20130321/">SPARQL 1.1 Update</a>
     */
    public abstract void update(UpdateRequest updateRequest);
    
}