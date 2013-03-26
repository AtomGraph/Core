/*
 * Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.sun.jersey.api.core.ResourceConfig;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.graphity.server.util.DataManager;
import org.graphity.util.ModelUtils;
import org.graphity.util.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL endpoint resource, implementing SPARQL HTTP protocol
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/sparql")
public class SPARQLEndpointBase implements SPARQLEndpoint
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);

    public static final List<Variant> MODEL_VARIANTS = Variant.VariantListBuilder.newInstance().
		mediaTypes(org.graphity.server.MediaType.APPLICATION_RDF_XML_TYPE,
			org.graphity.server.MediaType.TEXT_TURTLE_TYPE).
		add().build();

    public static final List<Variant> RESULT_SET_VARIANTS = Variant.VariantListBuilder.newInstance().
			mediaTypes(org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE).
			add().build();

    public static final List<Variant> VARIANTS;
    static
    {
	List<Variant> variants = new ArrayList<Variant>();
	variants.addAll(MODEL_VARIANTS);
	variants.addAll(RESULT_SET_VARIANTS);
	VARIANTS = variants;
    }

    /**
     * Configuration property for remote origin SPARQL endpoint (set in web.xml)
     * 
     */
    public static final String PROPERTY_ENDPOINT_URI = "org.graphity.server.endpoint-uri";


    /**
     * Configuration property for maximum SELECT query results (set in web.xml)
     * 
     */
    public static final String PROPERTY_QUERY_RESULT_LIMIT = "org.graphity.server.query.result-limit";
    
    private final Resource endpoint;
    @Context UriInfo uriInfo;
    @Context Request request;
    @Context HttpHeaders httpHeaders;
    @Context ResourceConfig resourceConfig;

    public SPARQLEndpointBase(@Context ResourceConfig resourceConfig)
    {
	this((resourceConfig.getProperty(PROPERTY_ENDPOINT_URI) == null) ?
		null :
		ResourceFactory.createResource(resourceConfig.getProperty(PROPERTY_ENDPOINT_URI).toString()));
    }
    
    protected SPARQLEndpointBase(Resource endpoint)
    {
	if (endpoint == null) throw new IllegalArgumentException("Endpoint cannot be null");
	
	this.endpoint = endpoint;
	if (log.isDebugEnabled()) log.debug("Constructing SPARQLEndpointBase with endpoint: {}", endpoint);
    }

    @Override
    @GET
    public Response query(@QueryParam("query") Query query)
    {
	return getResponseBuilder(query).build();
    }

    public ResponseBuilder getResponseBuilder(Query query)
    {
	if (query == null) throw new WebApplicationException(Response.Status.BAD_REQUEST);

	if (query.isSelectType())
	{
	    if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing SELECT query: {}", query);
	    if (getResourceConfig().getProperty(PROPERTY_QUERY_RESULT_LIMIT) != null)
		query.setLimit(Long.parseLong(getResourceConfig().getProperty(PROPERTY_QUERY_RESULT_LIMIT).toString()));

	    return getResponseBuilder(loadResultSetRewindable(getEndpoint(), query));
	}

	if (query.isConstructType() || query.isDescribeType())
	{
	    if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing CONSTRUCT/DESCRIBE query: {}", query);
	    return getResponseBuilder(loadModel(getEndpoint(), query));
	}

	if (log.isWarnEnabled()) log.warn("SPARQL endpoint received unknown type of query: {}", query);
	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    public ResponseBuilder getResponseBuilder(Model model)
    {	
	EntityTag entityTag = new EntityTag(Long.toHexString(ModelUtils.hashModel(model)));
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb;
	}
	else
	{
	    Variant variant = getRequest().selectVariant(MODEL_VARIANTS);
	    if (variant == null)
	    {
		if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, getVariants());
		return Response.notAcceptable(MODEL_VARIANTS);
	    }	
	    else
	    {
		if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", variant, entityTag);
		return Response.ok(model, variant).
			tag(entityTag); // uses ModelXSLTWriter/ModelWriter
	    }
	}	
    }

    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet)
    {
	EntityTag entityTag = new EntityTag(Long.toHexString(ResultSetUtils.hashResultSet(resultSet)));
	resultSet.reset(); // ResultSet needs to be rewinded back to the beginning
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);

	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb;
	}
	else
	{
	    Variant variant = getRequest().selectVariant(RESULT_SET_VARIANTS);
	    if (variant == null)
	    {
		if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, RESULT_SET_VARIANTS);
		return Response.notAcceptable(RESULT_SET_VARIANTS);
	    }	
	    else
	    {
		if (log.isTraceEnabled()) log.trace("Generating SPARQL results Response with Variant: {} and EntityTag: {}", variant, entityTag);
		return Response.ok(resultSet, variant).
			tag(entityTag); // uses ResultSetWriter
	    }
	}	
    }

    public ResultSetRewindable loadResultSetRewindable(Resource endpoint, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading ResultSet from SPARQL endpoint: {} using Query: {}", endpoint.getURI(), query);
	return DataManager.get().loadResultSet(endpoint.getURI(), query); // .getResultSetRewindable()
    }

    public ResultSetRewindable loadResultSetRewindable(Query query)
    {
	return loadResultSetRewindable(getEndpoint(), query);
    }
    
    public Model loadModel(Resource endpoint, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading Model from SPARQL endpoint: {} using Query: {}", endpoint, query);
	return DataManager.get().loadModel(endpoint.getURI(), query);
    }

    public Model loadModel(Query query)
    {
	return loadModel(getEndpoint(), query);
    }

    public List<Variant> getVariants()
    {
	return VARIANTS;
    }

    public Resource getEndpoint()
    {
	return endpoint;
    }

    public HttpHeaders getHttpHeaders()
    {
	return httpHeaders;
    }

    public Request getRequest()
    {
	return request;
    }

    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    public ResourceConfig getResourceConfig()
    {
	return resourceConfig;
    }

}