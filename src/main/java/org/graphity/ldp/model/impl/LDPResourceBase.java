/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graphity.ldp.model.impl;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.LDPResource;
import org.graphity.ldp.model.LinkedDataResource;
import org.graphity.ldp.model.ResourceFactory;
import org.graphity.util.QueryBuilder;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.SIOC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.vocabulary.SP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LDPResourceBase implements LDPResource
{
    private static final Logger log = LoggerFactory.getLogger(LDPResourceBase.class);

    private LinkedDataResource resource = null;
    
    public static QueryBuilder getQueryBuilder(OntResource ontResource,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (ontResource == null) throw new IllegalArgumentException("OntResource cannot be null");
	
	com.hp.hpl.jena.rdf.model.Resource queryResource = null;
	if (ontResource.hasProperty(Graphity.query))
	{
	    queryResource = ontResource.getPropertyResourceValue(Graphity.query);
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} has explicit Query Resource {}", ontResource.getURI(), queryResource);
	}
	else
	{
	    queryResource = QueryBuilder.fromDescribe(ontResource.getURI()).buildSPIN();
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} gets implicit Query Resource {}", ontResource.getURI(), queryResource);
	}
	
	if (ontResource.hasRDFType(SIOC.CONTAINER))
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} is a Container, building Query from SELECT", ontResource.getURI());
	    if (!(SPINFactory.asQuery(queryResource) instanceof Select))
		throw new IllegalArgumentException("Container QueryBuilder must get a SELECT query");

	    QueryBuilder selectBuilder = QueryBuilder.fromResource(queryResource).
		limit(limit).offset(offset);
	    /*
	    if (orderBy != null)
	    {
		com.hp.hpl.jena.rdf.model.Resource modelVar = getOntology().createResource().addLiteral(SP.varName, "model");
		Property orderProperty = ResourceFactory.createProperty(orderBy);
		com.hp.hpl.jena.rdf.model.Resource orderVar = getOntology().createResource().addLiteral(SP.varName, orderProperty.getLocalName());

		sb.orderBy(orderVar, desc).optional(modelVar, orderProperty, orderVar);
	    }
	    */

	    if (queryResource.getPropertyResourceValue(SP.resultVariables) != null)
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} has result variables: {}", queryResource, queryResource.getPropertyResourceValue(SP.resultVariables));
		return QueryBuilder.fromDescribe(queryResource.getPropertyResourceValue(SP.resultVariables)).
		    subQuery(selectBuilder);
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} does not have result variables, using all variables (*)", queryResource);
		return QueryBuilder.fromDescribe().subQuery(selectBuilder);
	    }
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} is a not a Container, building Query from DESCRIBE/CONSTRUCT", ontResource.getURI());
	    return QueryBuilder.fromResource(queryResource);
	}
    }
    
    public static LinkedDataResource getLinkedDataResource(OntResource ontResource, Query query,
	UriInfo uriInfo, Request request, MediaType mediaType,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (ontResource == null) throw new IllegalArgumentException("OntResource cannot be null");
	if (query == null) throw new IllegalArgumentException("Query cannot be null");
	
	if (log.isDebugEnabled()) log.debug("Creating LinkedDataResource from OntResource with URI: {}", ontResource.getURI());
	
	if (ontResource.hasProperty(Graphity.service))
	{
	    com.hp.hpl.jena.rdf.model.Resource service = ontResource.getPropertyResourceValue(Graphity.service);
	    if (service == null) throw new IllegalArgumentException("SPARQL Service must be a Resource");
	    
	    com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
	    if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");
	    
	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", ontResource.getURI(), endpoint.getURI());
	
	    return ResourceFactory.getLinkedDataResource(endpoint.getURI(), query, uriInfo, request, mediaType,
			limit, offset, orderBy, desc);
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its Model", ontResource.getURI());

	    return ResourceFactory.getLinkedDataResource(ontResource.getModel(), query, uriInfo, request, mediaType,
		    limit, offset, orderBy, desc);
	}
    }
    
    public LDPResourceBase(LinkedDataResource resource)
    {	
	if (resource.getModel().isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
	
	this.resource = resource;
    }

    public LDPResourceBase(OntResource ontResource, UriInfo uriInfo, Request request,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	this(getLinkedDataResource(ontResource,
		getQueryBuilder(ontResource, limit, offset, orderBy, desc).build(),
		uriInfo, request, MediaType.WILDCARD_TYPE, limit, offset, orderBy, desc));
    }

    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML + ";qs=2;charset=UTF-8")
    public Response getXHTMLResponse()
    {
	if (log.isTraceEnabled()) log.trace("Returning XHTML Response");
	    
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null) return rb.build();
	
	return Response.ok(getResource()).tag(getEntityTag()).build(); // uses ResourceXHTMLWriter
    }

    @Override
    public Response getResponse()
    {
	return getResource().getResponse();
    }
    
    @Override
    public Response post(Model model)
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Response put(Model model)
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Response delete()
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Request getRequest()
    {
	return getResource().getRequest();
    }

    @Override
    public UriInfo getUriInfo()
    {
	return getResource().getUriInfo();
    }

    @Override
    public EntityTag getEntityTag()
    {
	return getResource().getEntityTag();
    }

    @Override
    public String getURI()
    {
	//return getResource().getURI();
	return getUriInfo().getAbsolutePath().toString();
    }

    @Override
    public Model getModel()
    {
	return getResource().getModel();
    }

    public LinkedDataResource getResource()
    {
	return resource;
    }
    
}