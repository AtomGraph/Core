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
package org.graphity.ldp.model.impl;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.LinkedDataResource;
import org.graphity.ldp.model.XHTMLResource;
import org.graphity.ldp.model.query.ModelResource;
import org.graphity.ldp.model.query.impl.EndpointModelResourceImpl;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.graphity.model.ResourceFactory;
import org.graphity.util.QueryBuilder;
import org.graphity.util.SelectBuilder;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.SIOC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.vocabulary.SP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LinkedDataResourceBase extends ResourceFactory implements LinkedDataResource, XHTMLResource
{
    private static final Logger log = LoggerFactory.getLogger(LinkedDataResourceBase.class);

    private OntResource ontResource = null;
    private UriInfo uriInfo = null;
    //private Model model = null;
    private ModelResource resource = null;
	
    public final QueryBuilder getQueryBuilder(Long limit, Long offset, String orderBy, Boolean desc)
    {
	QueryBuilder queryBuilder;
	
	if (ontResource.hasRDFType(SIOC.CONTAINER))
	{
	    if (!ontResource.hasProperty(Graphity.selectQuery)) throw new IllegalArgumentException("Container Resource must have a SELECT query");

	    SelectBuilder selectBuilder = SelectBuilder.fromResource(ontResource.getPropertyResourceValue(Graphity.selectQuery)).
		limit(limit).offset(offset);
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} is Container gets explicit SELECT Query Resource {}", ontResource.getURI(), selectBuilder);
	    ontResource.setPropertyValue(Graphity.selectQuery, selectBuilder);

	    /*
	    if (orderBy != null)
	    {
		com.hp.hpl.jena.rdf.model.Resource modelVar = getOntology().createResource().addLiteral(SP.varName, "model");
		Property orderProperty = ResourceFactory.createProperty(orderBy);
		com.hp.hpl.jena.rdf.model.Resource orderVar = getOntology().createResource().addLiteral(SP.varName, orderProperty.getLocalName());

		selectBuilder.orderBy(orderVar, desc).optional(modelVar, orderProperty, orderVar);
	    }
	    */
	    if (selectBuilder.getPropertyResourceValue(SP.resultVariables) != null)
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} has result variables: {}", selectBuilder, selectBuilder.getPropertyResourceValue(SP.resultVariables));
		queryBuilder = QueryBuilder.fromDescribe(selectBuilder.getPropertyResourceValue(SP.resultVariables)).
		    subQuery(selectBuilder);
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} does not have result variables, using wildcard", selectBuilder);
		queryBuilder = QueryBuilder.fromDescribe(ontResource.getModel()).subQuery(selectBuilder);
	    }
	}
	else
	{
	    if (ontResource.hasProperty(Graphity.query))
	    {
		queryBuilder = QueryBuilder.fromResource(ontResource.getPropertyResourceValue(Graphity.query));
		if (log.isDebugEnabled()) log.debug("OntResource with URI {} has Query Resource {}", ontResource.getURI(), ontResource.getPropertyResourceValue(Graphity.query));
	    }
	    else
	    {
		queryBuilder = QueryBuilder.fromDescribe(ontResource.getURI(), ontResource.getModel());
		if (log.isDebugEnabled()) log.debug("OntResource with URI {} gets explicit Query Resource {}", ontResource.getURI(), queryBuilder);
	    }
	}
	
	ontResource.setPropertyValue(Graphity.query, queryBuilder); // Resource alway get a g:query value

	return queryBuilder;
    }

    public LinkedDataResourceBase(OntResource ontResource,
	UriInfo uriInfo, Request request, MediaType mediaType,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (ontResource == null) throw new IllegalArgumentException("OntResource cannot be null");

	this.uriInfo = uriInfo;
	this.ontResource = ontResource;

	if (log.isDebugEnabled()) log.debug("Creating LinkedDataResource from OntResource with URI: {}", ontResource.getURI());

	Query query = getQueryBuilder(limit, offset, orderBy, desc).build();	

	if (ontResource.hasRDFType(SIOC.CONTAINER))
	{
	    if (ontResource.hasProperty(Graphity.service))
	    {
		com.hp.hpl.jena.rdf.model.Resource service = ontResource.getPropertyResourceValue(Graphity.service);
		if (service == null) throw new IllegalArgumentException("SPARQL Service must be a Resource");

		com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		    createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
		if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

		if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", ontResource.getURI(), endpoint.getURI());
		resource = new EndpointPageResourceImpl(endpoint.getURI(), query, uriInfo, request, mediaType,
			limit, offset, orderBy, desc);
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its Model", ontResource.getURI());
		resource = new QueryModelPageResourceImpl(ontResource.getModel(), query, uriInfo, request, mediaType,
			limit, offset, orderBy, desc);
	    }
	}
	else
	{
	    if (ontResource.hasProperty(Graphity.service))
	    {
		com.hp.hpl.jena.rdf.model.Resource service = ontResource.getPropertyResourceValue(Graphity.service);
		if (service == null) throw new IllegalArgumentException("SPARQL Service must be a Resource");

		com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		    createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
		if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

		if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", ontResource.getURI(), endpoint.getURI());
		resource = new EndpointModelResourceImpl(endpoint.getURI(), query, request, mediaType);
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its Model", ontResource.getURI());
		resource = new QueryModelModelResourceImpl(ontResource.getModel(), query, request, mediaType);
	    }
	}
	
	if (resource.getModel().isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
    }
    
    public static com.hp.hpl.jena.rdf.model.Resource matchEndpoint(Class<?> cls, Model model)
    {
	if (log.isDebugEnabled()) log.debug("Matching @Path annotation {} of Class {}", cls.getAnnotation(Path.class).value(), cls);
	return matchEndpoint(cls.getAnnotation(Path.class).value(), model);
    }
    
    public static com.hp.hpl.jena.rdf.model.Resource matchEndpoint(String uriTemplate, Model model)
    {
	if (uriTemplate == null) throw new IllegalArgumentException("Item endpoint class must have a @Path annotation");
	
	if (log.isDebugEnabled()) log.debug("Matching URI template template {} against Model {}", uriTemplate, model);	
	Property utProp = model.createProperty("http://purl.org/linked-data/api/vocab#uriTemplate");
	ResIterator it = model.listResourcesWithProperty(utProp, uriTemplate);
	
	if (it.hasNext())
	{
	    com.hp.hpl.jena.rdf.model.Resource match = it.next();
	    if (log.isDebugEnabled()) log.debug("URI template {} matched endpoint Resource {}", uriTemplate, match);	
	    return match;
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("URI template {} has no endpoint match in Model {}", uriTemplate, model);	
	    return null;   
	}
    }

    public ModelResource getResource()
    {
	return resource;
    }

    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    @Override
    public String getURI()
    {
	return getUriInfo().getAbsolutePath().toString();
    }

    @Override
    public EntityTag getEntityTag()
    {
	return getResource().getEntityTag();
    }

    @Override
    public Response getResponse()
    {
	return getResource().getResponse();
    }

    @Override
    public Request getRequest()
    {
	return getResource().getRequest();
    }

    @Override
    public Model getModel()
    {
	return getResource().getModel();
    }

    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML + ";qs=2;charset=UTF-8")
    @Override
    public Response getXHTMLResponse()
    {	    
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb.build();
	}
	else
	{
	    if (log.isTraceEnabled()) log.trace("Generating XHTML Response");
	    return Response.ok(this).tag(getEntityTag()).build(); // uses ResourceXHTMLWriter
	}
    }

}