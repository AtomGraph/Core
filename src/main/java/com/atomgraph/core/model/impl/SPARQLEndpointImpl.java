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
import jakarta.ws.rs.core.*;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.EndpointAccessor;
import com.atomgraph.core.model.SPARQLEndpoint;
import static com.atomgraph.core.model.SPARQLEndpoint.DEFAULT_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.NAMED_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.QUERY;
import static com.atomgraph.core.model.SPARQLEndpoint.UPDATE;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_NAMED_GRAPH_URI;
import com.atomgraph.core.model.Service;
import com.atomgraph.core.util.ModelUtils;
import com.atomgraph.core.util.ResultSetUtils;
import java.util.Collections;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.vocabulary.ResultSetGraphVocab;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of SPARQL endpoints.
 * Unfortunately cannot extend ResourceBase because of clashing JAX-RS method annotations.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see com.atomgraph.core.model.SPARQLEndpoint
 */
public class SPARQLEndpointImpl implements SPARQLEndpoint
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointImpl.class);
    
    private final Request request;
    private final EndpointAccessor accessor;
    private final MediaTypes mediaTypes;
    
    /**
     * Constructs SPARQL endpoint from request metadata.
     * 
     * @param request current request
     * @param service SPARQL service
     * @param mediaTypes supported media types
     */
    @Inject
    public SPARQLEndpointImpl(@Context Request request, Service service, MediaTypes mediaTypes)
    {
        this(request, service.getEndpointAccessor(), mediaTypes);
    }
    
    public SPARQLEndpointImpl(Request request, EndpointAccessor accessor, MediaTypes mediaTypes)
    {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (accessor == null) throw new IllegalArgumentException("EndpointAccessor cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");

        this.request = request;
        this.accessor = accessor;
        this.mediaTypes = mediaTypes;
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
        try
        {
            if (queryString != null) return get(QueryFactory.create(queryString), defaultGraphUris, namedGraphUris);
            if (updateString != null) return post(UpdateFactory.create(updateString), usingGraphUris, usingNamedGraphUris);
        }
        catch (QueryParseException ex)
        {
            throw new BadRequestException(ex);
        }
        
        throw new BadRequestException("Neither query nor update provided");
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
        getEndpointAccessor().update(update, usingGraphUris, usingNamedGraphUris);

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
        if (query == null) throw new BadRequestException("Query string not provided");

        if (query.isSelectType())
        {
            if (log.isDebugEnabled()) log.debug("Loading ResultSet using SELECT/ASK query: {}", query);
            return getResponseBuilder(getEndpointAccessor().select(query, defaultGraphUris, namedGraphUris));
        }
        if (query.isAskType())
        {
            Model model = ModelFactory.createDefaultModel();
            model.createResource().
                addProperty(RDF.type, ResultSetGraphVocab.ResultSet).
                addLiteral(ResultSetGraphVocab.p_boolean, getEndpointAccessor().ask(query, defaultGraphUris, namedGraphUris));
                
            if (log.isDebugEnabled()) log.debug("Loading ResultSet using SELECT/ASK query: {}", query);
            return getResponseBuilder(ResultSetFactory.copyResults(ResultSetFactory.makeResults(model)));
        }

        if (query.isConstructType() || query.isDescribeType())
        {
            if (log.isDebugEnabled()) log.debug("Loading Model using CONSTRUCT/DESCRIBE query: {}", query);
            return getResponseBuilder(getEndpointAccessor().loadModel(query, defaultGraphUris, namedGraphUris));
        }
        
        if (log.isWarnEnabled()) log.warn("SPARQL endpoint received unknown type of query: {}", query);
        throw new BadRequestException("Unknown query type");
    }

    /**
     * Returns response builder for the given RDF model.
     * 
     * @param model RDF model
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Model model)
    {
        return new com.atomgraph.core.model.impl.Response(getRequest(),
                model,
                null,
                new EntityTag(Long.toHexString(ModelUtils.hashModel(model))),
                getWritableMediaTypes(Model.class),
                getLanguages(),
                getEncodings()).
            getResponseBuilder();
    }
        
    /**
     * Returns response builder for the given SPARQL result set.
     * 
     * @param resultSet result set
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet)
    {
        long hash = ResultSetUtils.hashResultSet(resultSet);
        resultSet.reset();
        
        return new com.atomgraph.core.model.impl.Response(getRequest(),
                resultSet,
                null,
                new EntityTag(Long.toHexString(hash)),
                getWritableMediaTypes(ResultSet.class),
                getLanguages(),
                getEncodings()).
            getResponseBuilder();
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
     * Get writable media types for a certain class.
     * 
     * @param clazz class
     * @return list of media types
     */
    public List<MediaType> getWritableMediaTypes(Class clazz)
    {
        return getMediaTypes().getWritable(clazz);
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
    
    public EndpointAccessor getEndpointAccessor()
    {
        return accessor;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
 
}