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
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.*;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.SPARQLEndpoint;
import static com.atomgraph.core.model.SPARQLEndpoint.DEFAULT_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.NAMED_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.QUERY;
import static com.atomgraph.core.model.SPARQLEndpoint.UPDATE;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_NAMED_GRAPH_URI;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of SPARQL endpoints.
 * Unfortunately cannot extend ResourceBase because of clashing JAX-RS method annotations.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see com.atomgraph.core.model.SPARQLEndpoint
 */
public abstract class SPARQLEndpointBase implements SPARQLEndpoint
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);
    
    private final Request request;
    private final MediaTypes mediaTypes;
    private final com.atomgraph.core.model.impl.Response response;
    
    /**
     * Constructs SPARQL endpoint from request metadata.
     * 
     * @param request current request
     * @param mediaTypes supported media types
     */
    public SPARQLEndpointBase(@Context Request request, @Context MediaTypes mediaTypes)
    {
        //if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");

        this.request = request;
        this.mediaTypes = mediaTypes;
        this.response = request != null ? com.atomgraph.core.model.impl.Response.fromRequest(request) : null;
        if (log.isDebugEnabled()) log.debug("Constructing SPARQLEndpointBase");
    }
    
    @Override
    @GET
    public Response get(@QueryParam(QUERY) Query query,
            @QueryParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUris, @QueryParam(NAMED_GRAPH_URI) List<URI> namedGraphUris)
    {
        return getResponseBuilder(query, defaultGraphUris, namedGraphUris).build();
    }
    
    @Override
    @POST
    @Consumes(com.atomgraph.core.MediaType.APPLICATION_FORM_URLENCODED)
    public Response post(@FormParam(QUERY) String queryString, @FormParam(UPDATE) String updateString,
            @FormParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUris, @FormParam(NAMED_GRAPH_URI) List<URI> namedGraphUris,
            @FormParam(USING_GRAPH_URI) List<URI> usingGraphUris, @FormParam(USING_NAMED_GRAPH_URI) List<URI> usingNamedGraphUris)
    {
        if (queryString != null) return get(QueryFactory.create(queryString), defaultGraphUris, namedGraphUris);
        if (updateString != null) return post(UpdateFactory.create(updateString), usingGraphUris, usingNamedGraphUris);

        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    
    @Override
    @POST
    @Consumes(com.atomgraph.core.MediaType.APPLICATION_SPARQL_QUERY)
    public Response post(Query query, @QueryParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUris, @QueryParam(NAMED_GRAPH_URI) List<URI> namedGraphUris)
    {
        return get(query, defaultGraphUris, namedGraphUris);
    }
    
    @Override
    @POST
    @Consumes(com.atomgraph.core.MediaType.APPLICATION_SPARQL_UPDATE)
    public Response post(UpdateRequest update, @QueryParam(USING_GRAPH_URI) List<URI> usingGraphUris, @QueryParam(USING_NAMED_GRAPH_URI) List<URI> usingNamedGraphUris)
    {
        update(update, usingGraphUris, usingNamedGraphUris);

        return Response.ok().build();
    }
            
    /**
     * Returns response builder for a SPARQL query.
     * Contains the main SPARQL endpoint JAX-RS implementation logic.
     * Uses <code>a:resultLimit</code> parameter value from web.xml as <code>LIMIT</code> value on <code>SELECT</code> queries, if present.
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (query == null) throw new WebApplicationException(Response.Status.BAD_REQUEST);

        if (query.isSelectType())
        {
            /*
            if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing SELECT query: {}", query);
            if (getServletConfig().getInitParameter(A.resultLimit.getURI()) != null)
                query.setLimit(Long.parseLong(getServletConfig().getInitParameter(A.resultLimit.getURI())));
            */

            return getResponseBuilder(select(query, defaultGraphUris, namedGraphUris));
        }

        if (query.isConstructType() || query.isDescribeType())
        {
            if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing CONSTRUCT/DESCRIBE query: {}", query);
            return getResponseBuilder(loadModel(query, defaultGraphUris, namedGraphUris));
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
        if (getRequest() == null) return Response.ok(model);
                    
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
        if (getRequest() == null) return Response.ok(resultSet);
        
        return com.atomgraph.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(resultSet, getVariants(getMediaTypes().getWritable(ResultSet.class)));
    }

    /**
     * Convenience method for <pre>DESCRIBE</pre> queries.
     * 
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @link #loadModel(query)
     * @param query
     * @return RDF model
     */
    public Model describe(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)    
    {
        if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isDescribeType()) throw new IllegalArgumentException("Query must be DESCRIBE");
        
        return loadModel(query, defaultGraphUris, namedGraphUris);
    }

    /**
     * Convenience method for <pre>CONSTRUCT</pre> queries.
     * 
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @link #loadModel(query)
     * @param query
     * @return RDF model
     */
    public Model construct(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isConstructType()) throw new IllegalArgumentException("Query must be CONSTRUCT");
        
        return loadModel(query, defaultGraphUris, namedGraphUris);
    }

    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<code>DESCRIBE</code> or <code>CONSTRUCT</code>)
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    public abstract Model loadModel(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris);
    
    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<pre>SELECT</pre>)
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return SPARQL result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    public abstract ResultSetRewindable select(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris);

    /**
     * Asks boolean result from the endpoint by executing a SPARQL query (<pre>ASK</pre>)
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public abstract boolean ask(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris);

    /**
     * Execute SPARQL update request
     * 
     * @param updateRequest update request
     * @param usingGraphUris using graph URIs
     * @param usingNamedGraphUris using named graph URIs
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-update-20130321/">SPARQL 1.1 Update</a>
     */
    public abstract void update(UpdateRequest updateRequest, List<URI> usingGraphUris, List<URI> usingNamedGraphUris);

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
    
    public Request getRequest()
    {
        return request;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    public com.atomgraph.core.model.impl.Response getResponse()
    {
        return response;
    }
 
}