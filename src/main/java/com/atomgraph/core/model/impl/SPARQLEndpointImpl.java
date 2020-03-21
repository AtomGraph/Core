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
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
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
    public SPARQLEndpointImpl(@Context Request request, @Context Service service, @Context MediaTypes mediaTypes)
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
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
        
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
        if (query == null) throw new WebApplicationException(Response.Status.BAD_REQUEST);

        if (query.isSelectType())
        {
            /*
            if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing SELECT query: {}", query);
            if (getServletConfig().getInitParameter(A.resultLimit.getURI()) != null)
                query.setLimit(Long.parseLong(getServletConfig().getInitParameter(A.resultLimit.getURI())));
            */

            if (log.isDebugEnabled()) log.debug("Loading ResultSet using SELECT/ASK query: {}", query);
            return getResponseBuilder(getEndpointAccessor().select(query, defaultGraphUris, namedGraphUris));
        }

        if (query.isConstructType() || query.isDescribeType())
        {
            List<Variant> variants = com.atomgraph.core.model.impl.Response.getVariantListBuilder(getMediaTypes().getWritable(Dataset.class),
                    getLanguages(),
                    getEncodings()).
                add().
                build();
            Variant variant = getRequest().selectVariant(variants);
            if (variant == null)
            {
                if (log.isDebugEnabled()) log.debug("Loading Model using CONSTRUCT/DESCRIBE query: {}", query);
                return getResponseBuilder(getEndpointAccessor().loadModel(query, defaultGraphUris, namedGraphUris));
            }
            else
            {
                if (log.isDebugEnabled()) log.debug("Loading Dataset using CONSTRUCT/DESCRIBE query: {}", query);
                return getResponseBuilder(getEndpointAccessor().loadDataset(query, defaultGraphUris, namedGraphUris));
            }
        }
        
        if (log.isWarnEnabled()) log.warn("SPARQL endpoint received unknown type of query: {}", query);
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    /**
     * Returns response builder for the given RDF dataset.
     * 
     * @param dataset RDF dataset
     * @return response builder
     */
    public javax.ws.rs.core.Response.ResponseBuilder getResponseBuilder(Dataset dataset)
    {
        return new com.atomgraph.core.model.impl.Response(getRequest(),
                dataset,
                new EntityTag(Long.toHexString(com.atomgraph.core.model.impl.Response.hashDataset(dataset))),
                getMediaTypes().getWritable(dataset.getClass()),
                Collections.<Locale>emptyList(),
                Collections.<String>emptyList()).
            getResponseBuilder();
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
                new EntityTag(Long.toHexString(ModelUtils.hashModel(model))),
                getMediaTypes().getWritable(model.getClass()),
                Collections.<Locale>emptyList(),
                Collections.<String>emptyList()).
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
                new EntityTag(Long.toHexString(hash)),
                getMediaTypes().getWritable(resultSet.getClass()),
                Collections.<Locale>emptyList(),
                Collections.<String>emptyList()).
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