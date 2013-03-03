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
package org.graphity.platform.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.sun.jersey.api.core.ResourceConfig;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.platform.util.DataManager;
import org.graphity.util.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL endpoint resource, implementing ?query= access method
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/sparql")
public class SPARQLEndpointBase extends ResourceBase implements SPARQLEndpoint
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);

    public static List<Variant> RESULT_SET_VARIANTS = Variant.VariantListBuilder.newInstance().
			mediaTypes(org.graphity.platform.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    org.graphity.platform.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE).
			add().build();

    /**
     * Configuration property for ontology SPARQL endpoint (set in web.xml)
     * 
     */
    public static final String PROPERTY_QUERY_RESULT_LIMIT = "org.graphity.platform.query.result-limit";
    
    private final Query query;
    @Context ResourceConfig config;

    public SPARQLEndpointBase(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders httpHeaders,
	@Context ResourceConfig config,
	@QueryParam("limit") @DefaultValue("20") Long limit,
	@QueryParam("offset") @DefaultValue("0") Long offset,
	@QueryParam("order-by") String orderBy,
	@QueryParam("desc") Boolean desc,
	@QueryParam("query") Query query)
    {
	super(uriInfo, request, httpHeaders, config,
		limit, offset, orderBy, desc);	
	this.query = query;
    }

    @Override
    public Query getQuery()
    {
	return query;
    }

    @Override
    public Response getResponse() // can this be simplified by reusing LDP ResourceBase code?
    {
	return query(getQuery());
    }

    public ResultSetRewindable loadResultSetRewindable(Resource service, Query query)
    {
	if (service != null)
	{
	    com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(
		    ResourceFactory.createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
	    if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", getURI(), endpoint.getURI());

	    return DataManager.get().loadResultSet(endpoint.getURI(), query); // .getResultSetRewindable()
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its OntModel", getURI());
	    return DataManager.get().loadResultSet(getOntModel(), query); // .getResultSetRewindable();
	}
    }

    @Override
    public Response query(@QueryParam("query") Query query)
    {
	if (query == null) throw new WebApplicationException(Response.Status.BAD_REQUEST);

	if (query.isSelectType())
	{
	    if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing SELECT query: {}", query);
	    if (config.getProperty(PROPERTY_QUERY_RESULT_LIMIT) != null)
		query.setLimit(Long.parseLong(config.getProperty(PROPERTY_QUERY_RESULT_LIMIT).toString()));

	    ResultSetRewindable resultSet = loadResultSetRewindable(getEndpoint(), query);
	    EntityTag entityTag = new EntityTag(Long.toHexString(ResultSetUtils.hashResultSet(resultSet)));
	    resultSet.reset();
	    Response.ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);

	    if (rb != null)
	    {
		if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
		return rb.build();
	    }
	    else
	    {
		Variant variant = getRequest().selectVariant(RESULT_SET_VARIANTS);
		if (variant == null)
		{
		    if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, RESULT_SET_VARIANTS);
		    return Response.notAcceptable(RESULT_SET_VARIANTS).build();
		}	
		else
		{
		    if (log.isTraceEnabled()) log.trace("Generating SPARQL results Response with Variant: {} and EntityTag: {}", variant, entityTag);
		    return Response.ok(resultSet, variant).tag(entityTag).build(); // uses ResultSetWriter
		}
	    }
	}

	if (query.isConstructType() || query.isDescribeType())
	{
	    if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing CONSTRUCT/DESCRIBE query: {}", query);
	    return getResponse(loadModel(getEndpoint(), query));
	}

	if (log.isWarnEnabled()) log.warn("SPARQL endpoint received unknown type of query: {}", query);
	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

}