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
import java.util.Map;
import java.util.stream.Stream;
import jakarta.ws.rs.core.*;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.io.SPARQLResultProvider;
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
import com.atomgraph.core.vocabulary.SD;
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
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.sparql.resultset.SPARQLResult;
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

    /**
     * <a href="https://www.w3.org/ns/formats/">W3C format IRIs</a> of the languages that have one.
     * A service description names its result formats by them.
     */
    public static final Map<Lang, String> FORMATS = Map.ofEntries(
        Map.entry(ResultSetLang.RS_XML, "http://www.w3.org/ns/formats/SPARQL_Results_XML"),
        Map.entry(ResultSetLang.RS_JSON, "http://www.w3.org/ns/formats/SPARQL_Results_JSON"),
        Map.entry(ResultSetLang.RS_CSV, "http://www.w3.org/ns/formats/SPARQL_Results_CSV"),
        Map.entry(ResultSetLang.RS_TSV, "http://www.w3.org/ns/formats/SPARQL_Results_TSV"),
        Map.entry(Lang.RDFXML, "http://www.w3.org/ns/formats/RDF_XML"),
        Map.entry(Lang.TURTLE, "http://www.w3.org/ns/formats/Turtle"),
        Map.entry(Lang.NTRIPLES, "http://www.w3.org/ns/formats/N-Triples"),
        Map.entry(Lang.N3, "http://www.w3.org/ns/formats/N3"),
        Map.entry(Lang.JSONLD, "http://www.w3.org/ns/formats/JSON-LD"),
        Map.entry(Lang.RDFJSON, "http://www.w3.org/ns/formats/RDF_JSON"));
    
    private final Request request;
    private final UriInfo uriInfo;
    private final EndpointAccessor accessor;
    private final MediaTypes mediaTypes;
    
    /**
     * Constructs SPARQL endpoint from request metadata.
     * 
     * @param request current request
     * @param uriInfo current request URI information
     * @param service SPARQL service
     * @param mediaTypes supported media types
     */
    @Inject
    public SPARQLEndpointImpl(@Context Request request, @Context UriInfo uriInfo, Service service, MediaTypes mediaTypes)
    {
        this(request, uriInfo, service.getEndpointAccessor(), mediaTypes);
    }
    
    public SPARQLEndpointImpl(Request request, UriInfo uriInfo, EndpointAccessor accessor, MediaTypes mediaTypes)
    {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (uriInfo == null) throw new IllegalArgumentException("UriInfo cannot be null");
        if (accessor == null) throw new IllegalArgumentException("EndpointAccessor cannot be null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");

        this.request = request;
        this.uriInfo = uriInfo;
        this.accessor = accessor;
        this.mediaTypes = mediaTypes;
        if (log.isDebugEnabled()) log.debug("Constructing SPARQLEndpointBase");
    }
    
    /**
     * Returns the result of the SPARQL query or, if no query is provided, the service description.
     * 
     * @param query SPARQL query (optional)
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return response
     * @see <a href="https://www.w3.org/TR/sparql11-service-description/#accessing">SPARQL 1.1 Service Description: Accessing a Service Description</a>
     */
    @Override
    @GET
    public Response get(@QueryParam(QUERY) Query query,
            @QueryParam(DEFAULT_GRAPH_URI) List<URI> defaultGraphUris, @QueryParam(NAMED_GRAPH_URI) List<URI> namedGraphUris)
    {
        if (query == null) return getResponseBuilder(getServiceDescription()).build();
        
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
            if (log.isDebugEnabled()) log.debug("Loading ResultSet using SELECT query: {}", query);
            return getResponseBuilder(getEndpointAccessor().select(query, defaultGraphUris, namedGraphUris));
        }
        if (query.isAskType())
        {
            if (log.isDebugEnabled()) log.debug("Loading boolean using ASK query: {}", query);
            return getResponseBuilder(new SPARQLResult(getEndpointAccessor().ask(query, defaultGraphUris, namedGraphUris)));
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
     * Returns the service description of this endpoint.
     * 
     * @return RDF model
     */
    public Model getServiceDescription()
    {
        Model model = ModelFactory.createDefaultModel();
        Resource service = model.createResource().
            addProperty(RDF.type, SD.Service).
            addProperty(SD.endpoint, model.createResource(getUriInfo().getAbsolutePath().toString()));
        getSupportedLanguages().forEach(language -> service.addProperty(SD.supportedLanguage, language));
        getResultFormats().forEach(format -> service.addProperty(SD.resultFormat, format));
        
        return model;
    }
    
    /**
     * Returns the query languages this endpoint supports.
     * 
     * @return list of <code>sd:Language</code> resources
     */
    public List<Resource> getSupportedLanguages()
    {
        return List.of(SD.SPARQL11Query, SD.SPARQL11Update);
    }
    
    /**
     * Returns the formats this endpoint writes results in: those of the writable result set and RDF graph media types
     * that have a W3C format IRI.
     * 
     * @return list of format resources
     * @see #FORMATS
     */
    public List<Resource> getResultFormats()
    {
        return Stream.concat(getWritableMediaTypes(ResultSet.class).stream(), getWritableMediaTypes(Model.class).stream()).
            flatMap(mediaType -> FORMATS.entrySet().stream().filter(format -> format.getKey().getContentType().getContentTypeStr().equals(mediaType.getType() + "/" + mediaType.getSubtype()))).
            map(format -> ResourceFactory.createResource(format.getValue())).
            distinct().
            toList();
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
     * Returns response builder for the boolean result of an ASK query.
     * The result set and model arms of {@link SPARQLResult} have their own overloads, since the entity tag of
     * a result set is a hash of its rows, which needs a rewindable result set, and a model answers in RDF.
     * 
     * @param result boolean result
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(SPARQLResult result)
    {
        if (!result.isBoolean()) throw new IllegalArgumentException("Only a boolean SPARQLResult is supported here: a result set goes through getResponseBuilder(ResultSetRewindable), a model through getResponseBuilder(Model)");

        return new com.atomgraph.core.model.impl.Response(getRequest(),
                result,
                null,
                new EntityTag(Long.toHexString(ResultSetUtils.hashBoolean(result.getBooleanResult()))),
                getWritableBooleanMediaTypes(),
                getLanguages(),
                getEncodings()).
            getResponseBuilder();
    }

    /**
     * The writable result set media types a boolean can be written in. Jena has no boolean encoding in
     * Thrift and Protobuf, and a type of no results language at all (HTML, where an application adds it to
     * the result set types) has no boolean writer either; leaving them out of the offer makes a request for
     * one of them not acceptable rather than a failure to write.
     * 
     * @return list of media types
     */
    public List<MediaType> getWritableBooleanMediaTypes()
    {
        return getWritableMediaTypes(ResultSet.class).stream().filter(SPARQLResultProvider::isBooleanWriteable).toList();
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
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
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