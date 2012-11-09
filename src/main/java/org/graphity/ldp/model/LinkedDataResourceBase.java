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
package org.graphity.ldp.model;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.graphity.ldp.Application;
import org.graphity.ldp.model.impl.EndpointPageResourceImpl;
import org.graphity.ldp.model.impl.QueryModelPageResourceImpl;
import org.graphity.ldp.model.query.ModelResource;
import org.graphity.ldp.model.query.impl.EndpointModelResourceImpl;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.graphity.model.ResourceFactory;
import org.graphity.model.query.QueriedResource;
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
public class LinkedDataResourceBase extends ResourceFactory implements LinkedDataResource, QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(LinkedDataResourceBase.class);

    private UriInfo uriInfo = null;
    private Request request = null;
    private HttpHeaders httpHeaders = null;
    private List<Variant> variants = null;
    private OntResource ontResource = null;
    private Query query = null;
    private ModelResource resource = null;
    private Long limit = null;
    private Long offset = null;
    private String orderBy = null;
    private Boolean desc = null;

    public final QueryBuilder getQueryBuilder(Long limit, Long offset, String orderBy, Boolean desc)
    {
	QueryBuilder queryBuilder;
	
	if (getOntResource().hasRDFType(SIOC.CONTAINER))
	{
	    if (!getOntResource().hasProperty(Graphity.selectQuery)) throw new IllegalArgumentException("Container Resource must have a SELECT query");

	    SelectBuilder selectBuilder = SelectBuilder.fromResource(getOntResource().getPropertyResourceValue(Graphity.selectQuery)).
		limit(limit).offset(offset);
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} is Container gets explicit SELECT Query Resource {}", getOntResource().getURI(), selectBuilder);
	    getOntResource().setPropertyValue(Graphity.selectQuery, selectBuilder);

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
		queryBuilder = QueryBuilder.fromDescribe(getOntResource().getModel()).subQuery(selectBuilder);
	    }
	}
	else
	{
	    if (getOntResource().hasProperty(Graphity.query))
	    {
		queryBuilder = QueryBuilder.fromResource(getOntResource().getPropertyResourceValue(Graphity.query));
		if (log.isDebugEnabled()) log.debug("OntResource with URI {} has Query Resource {}", getOntResource().getURI(), getOntResource().getPropertyResourceValue(Graphity.query));
	    }
	    else
	    {
		queryBuilder = QueryBuilder.fromDescribe(getURI(), getOntResource().getModel());
		if (log.isDebugEnabled()) log.debug("OntResource with URI {} gets explicit Query Resource {}", getOntResource().getURI(), queryBuilder);
	    }
	}
	
	getOntResource().setPropertyValue(Graphity.query, queryBuilder); // Resource alway get a g:query value

	return queryBuilder;
    }

    public LinkedDataResourceBase(@Context UriInfo uriInfo, @Context Request request,
	    @Context HttpHeaders httpHeaders, @Context List<Variant> variants,
	    @QueryParam("limit") @DefaultValue("20") Long limit,
	    @QueryParam("offset") @DefaultValue("0") Long offset,
	    String orderBy,
	    @QueryParam("desc") @DefaultValue("false") Boolean desc)
    {
	this(Application.getOntResource(uriInfo),
		uriInfo, request, httpHeaders, variants,
		limit, offset, orderBy, desc);
    }

    // intialize with default supported MediaTypes
    public LinkedDataResourceBase(OntResource ontResource, @Context UriInfo uriInfo, @Context Request request,
	    @Context HttpHeaders httpHeaders,
	    @QueryParam("limit") @DefaultValue("20") Long limit,
	    @QueryParam("offset") @DefaultValue("0") Long offset,
	    String orderBy,
	    @QueryParam("desc") @DefaultValue("false") Boolean desc)
    {
	this(ontResource, uriInfo, request, httpHeaders, 
	    Variant.VariantListBuilder.newInstance().
		mediaTypes(MediaType.APPLICATION_XHTML_XML_TYPE,
			org.graphity.ldp.MediaType.APPLICATION_RDF_XML_TYPE,
			org.graphity.ldp.MediaType.TEXT_TURTLE_TYPE).
		//languages(new Locale("en")).
		add().build(),
	    limit, offset, orderBy, desc);
    }

    public LinkedDataResourceBase(OntResource ontResource,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (ontResource == null) throw new IllegalArgumentException("OntResource cannot be null");
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (httpHeaders == null) throw new IllegalArgumentException("HttpHeaders cannot be null");
	if (variants == null) throw new IllegalArgumentException("Variants cannot be null");

	this.ontResource = ontResource;
	if (log.isDebugEnabled()) log.debug("Creating LinkedDataResource from OntResource with URI: {}", ontResource.getURI());

	this.uriInfo = uriInfo;
	this.request = request;
	this.httpHeaders = httpHeaders;
	this.variants = variants;

	this.limit = limit;
	this.offset = offset;
	this.orderBy = orderBy;
	this.desc = desc;
	
	query = getQueryBuilder(limit, offset, orderBy, desc).build();	
    }

    @GET
    @Override
    public Response getResponse()
    {
	// always prefer XHTML if it's acceptable
	if (getHttpHeaders().getAcceptableMediaTypes().contains(MediaType.APPLICATION_XHTML_XML_TYPE)
		&& (getRequest().evaluatePreconditions(getEntityTag()) == null)) // resource modified
	{
	    if (log.isTraceEnabled()) log.trace("Generating XHTML Response");
	    return Response.ok(this).tag(getEntityTag()).build(); // uses ResourceXHTMLWriter<Resource> FIX!
	}
	
	return getResource().getResponse(); // uses ModelWriter<Model>
    }

    public static OntClass matchOntClass(Class<?> cls, OntModel ontModel)
    {
	if (log.isDebugEnabled()) log.debug("Matching @Path annotation {} of Class {}", cls.getAnnotation(Path.class).value(), cls);
	return matchOntClass(cls.getAnnotation(Path.class).value(), ontModel);
    }
    
    public static OntClass matchOntClass(String uriTemplate, OntModel ontModel)
    {
	if (uriTemplate == null) throw new IllegalArgumentException("Item endpoint class must have a @Path annotation");
	
	if (log.isDebugEnabled()) log.debug("Matching URI template template {} against Model {}", uriTemplate, ontModel);	
	Property utProp = ontModel.createProperty("http://purl.org/linked-data/api/vocab#uriTemplate");
	ResIterator it = ontModel.listResourcesWithProperty(utProp, uriTemplate);

	if (it.hasNext())
	{
	    com.hp.hpl.jena.rdf.model.Resource match = it.next();
	    if (!match.canAs(OntClass.class)) throw new IllegalArgumentException("Resource matching this URI template is not an OntClass");
	    
	    if (log.isDebugEnabled()) log.debug("URI template {} matched endpoint OntClass {}", uriTemplate, match.as(OntClass.class));
	    return match.as(OntClass.class);
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("URI template {} has no endpoint match in Model {}", uriTemplate, ontModel);
	    return null;   
	}
    }

    public ModelResource getResource()
    {
	if (log.isDebugEnabled()) log.debug("List of Variants: {}", variants);
	
	if (resource == null) // lazy loading
	{
	    if (getOntResource().hasRDFType(SIOC.CONTAINER))
	    {
		if (getOntResource().hasProperty(Graphity.service))
		{
		    com.hp.hpl.jena.rdf.model.Resource service = getOntResource().getPropertyResourceValue(Graphity.service);
		    if (service == null) throw new IllegalArgumentException("SPARQL Service must be a Resource");

		    com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
			createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
		    if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

		    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", getOntResource().getURI(), endpoint.getURI());
		    resource = new EndpointPageResourceImpl(endpoint.getURI(), getQuery(), getUriInfo(), getRequest(), getVariants(),
			    getLimit(), getOffset(), getOrderBy(), getDesc());
		}
		else
		{
		    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its Model", getOntResource().getURI());
		    resource = new QueryModelPageResourceImpl(getOntResource().getModel(), getQuery(), getUriInfo(), getRequest(), getVariants(),
			    getLimit(), getOffset(), getOrderBy(), getDesc());
		}
	    }
	    else
	    {
		if (getOntResource().hasProperty(Graphity.service))
		{
		    com.hp.hpl.jena.rdf.model.Resource service = getOntResource().getPropertyResourceValue(Graphity.service);
		    if (service == null) throw new IllegalArgumentException("SPARQL Service must be a Resource");

		    com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
			createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
		    if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

		    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", getOntResource().getURI(), endpoint.getURI());
		    resource = new EndpointModelResourceImpl(endpoint.getURI(), getQuery(), getRequest(), getVariants());
		}
		else
		{
		    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its Model", getOntResource().getURI());
		    resource = new QueryModelModelResourceImpl(getOntResource().getModel(), getQuery(), getRequest(), getVariants());
		}
	    }

	    if (resource.getModel().isEmpty())
	    {
		if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
		throw new WebApplicationException(Response.Status.NOT_FOUND);
	    }

	}
	
	return resource;
    }

    @Override
    public String getURI()
    {
	//return getUriInfo().getAbsolutePath().toString();
	return getOntResource().getURI();
    }

    @Override
    public EntityTag getEntityTag()
    {
	return getResource().getEntityTag();
    }

    @Override
    public Model getModel()
    {
	return getResource().getModel();
    }

    @Override
    public Request getRequest()
    {
	return request;
    }

    public OntResource getOntResource()
    {
	return ontResource;
    }

    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    @Override
    public List<Variant> getVariants()
    {
	return variants;
    }

    public HttpHeaders getHttpHeaders()
    {
	return httpHeaders;
    }

    @Override
    public Query getQuery()
    {
	return query;
    }

    public Boolean getDesc()
    {
	return desc;
    }

    public Long getLimit()
    {
	return limit;
    }

    public Long getOffset()
    {
	return offset;
    }

    public String getOrderBy()
    {
	return orderBy;
    }

}