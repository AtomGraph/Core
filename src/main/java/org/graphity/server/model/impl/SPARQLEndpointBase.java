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
package org.graphity.server.model.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.*;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.graphity.server.model.SPARQLEndpoint;
import org.graphity.server.vocabulary.GS;
import org.graphity.util.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for SPARQL endpoints.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.server.model.SPARQLEndpoint
 */
public abstract class SPARQLEndpointBase implements SPARQLEndpoint
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);

    /**
     * Media types that can be used for representation of SPARQL result set
     * 
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSetRewindable.html">Jena ResultSetRewindable</a>
     */
    public static final List<Variant> RESULT_SET_VARIANTS = Variant.VariantListBuilder.newInstance().
			mediaTypes(org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE).
			add().build();
    
    private final Request request;
    private final ServletContext servletContext;
    
    /**
     * Protected constructor with explicit endpoint resource.
     * Not suitable for JAX-RS but can be used when subclassing.
     * 
     * @param request current request
     * @param servletContext webapp context
     */
    public SPARQLEndpointBase(@Context Request request, @Context ServletContext servletContext)
    {
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (servletContext == null) throw new IllegalArgumentException("ServletContext cannot be null");

	this.request = request;
	this.servletContext = servletContext;
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
     * @see org.graphity.server.provider.QueryParamProvider
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
    @Consumes(org.graphity.server.MediaType.APPLICATION_SPARQL_QUERY)
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
    @Consumes(org.graphity.server.MediaType.APPLICATION_FORM_URLENCODED)
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
    @Consumes(org.graphity.server.MediaType.APPLICATION_SPARQL_UPDATE)
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
            if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing SELECT query: {}", query);
            if (getServletContext().getInitParameter(GS.resultLimit.getURI()) != null)
                query.setLimit(Long.parseLong(getServletContext().getInitParameter(GS.resultLimit.getURI()).toString()));

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
     * Returns response builder for a given RDF model.
     * 
     * @param model RDF model
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Model model)
    {
        return ModelResponse.fromRequest(getRequest()).
                getResponseBuilder(model, getVariants());
                //cacheControl(getCacheControl()).
    }
    
    /**
     * Builds a list of acceptable response variants/
     * 
     * @return supported variants
     */
    public List<Variant> getVariants()
    {
        List<Variant> variants = new ArrayList<>();
        Iterator<Lang> it = RDFLanguages.getRegisteredLanguages().iterator();
        
        // RDF/XML as the default one - the first one gets selected by selectVariant()
        variants.add(new Variant(new MediaType(Lang.RDFXML.getContentType().getType(),
                Lang.RDFXML.getContentType().getSubType()),
            null, null));

        while (it.hasNext())
        {
            Lang lang = it.next();
            if (!lang.equals(Lang.RDFNULL) && !lang.equals(Lang.RDFXML))
            {
                ContentType ct = lang.getContentType();
                //List<String> altTypes = lang.getAltContentTypes();
                MediaType mediaType = new MediaType(ct.getType(), ct.getSubType()); // MediaType.valueOf(ct.getContentType()
                variants.add(new Variant(mediaType, null, null));
            }
        }
        
        return variants;
    }

    public EntityTag getEntityTag(ResultSet resultSet)
    {
        return new EntityTag(Long.toHexString(ResultSetUtils.hashResultSet(resultSet)));
    }
    
    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet)
    {
	return getResponseBuilder(resultSet, RESULT_SET_VARIANTS);
    }
    
    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet, List<Variant> variants)
    {
        resultSet.reset();
        EntityTag entityTag = getEntityTag(resultSet);
	resultSet.reset(); // ResultSet needs to be rewinded back to the beginning
	return getResponseBuilder(entityTag, resultSet, variants);
    }
    
    public ResponseBuilder getResponseBuilder(EntityTag entityTag, Object entity, List<Variant> variants)
    {	
        Variant variant = getRequest().selectVariant(variants);
        if (variant == null)
        {
            if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, variants);
            return Response.notAcceptable(variants);
        }

        ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb.variant(variant); // Jersey doesn't seem to set "Vary" header
	}
	else
	{
            if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", variant, entityTag);
            return Response.ok(entity, variant).
                    tag(entityTag);
	}	
    }

    /*
    */
    
    @Override
    public Model describe(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isDescribeType()) throw new IllegalArgumentException("Query must be DESCRIBE");
        
	return loadModel(query);
    }

    @Override
    public Model construct(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isConstructType()) throw new IllegalArgumentException("Query must be CONSTRUCT");
        
	return loadModel(query);
    }

    public Request getRequest()
    {
	return request;
    }

    public ServletContext getServletContext()
    {
	return servletContext;
    }
    
}