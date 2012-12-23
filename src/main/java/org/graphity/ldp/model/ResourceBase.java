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

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.sun.jersey.api.uri.UriTemplate;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import org.graphity.util.QueryBuilder;
import org.graphity.util.SelectBuilder;
import org.graphity.util.locator.PrefixMapper;
import org.graphity.util.manager.DataManager;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.LDP;
import org.graphity.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class ResourceBase extends LDPResourceBase //implements QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private final Long limit, offset;
    private final String orderBy;
    private final Boolean desc;

    public static OntModel getOntology(UriInfo uriInfo)
    {
	// ResourceConfig.getProperty()
	return getOntology(uriInfo.getBaseUriBuilder().path("ontology/").build().toString(), "org/graphity/ldp/vocabulary/graphity-ldp.ttl");
    }
    
    public static OntModel getOntology(String ontologyUri, String ontologyPath)
    {
	//if (!OntDocumentManager.getInstance().getFileManager().hasCachedModel(baseUri)) // not cached
	{	    
	    if (log.isDebugEnabled())
	    {
		log.debug("Ontology not cached, reading from file: {}", ontologyPath);
		log.debug("DataManager.get().getLocationMapper(): {}", DataManager.get().getLocationMapper());
		log.debug("Adding name/altName mapping: {} altName: {} ", ontologyUri, ontologyPath);
	    }
	    OntDocumentManager.getInstance().addAltEntry(ontologyUri, ontologyPath);

	    LocationMapper mapper = OntDocumentManager.getInstance().getFileManager().getLocationMapper();
	    if (log.isDebugEnabled()) log.debug("Adding prefix/altName mapping: {} altName: {} ", ontologyUri, ontologyPath);
	    ((PrefixMapper)mapper).addAltPrefixEntry(ontologyUri, ontologyPath);	    
	}
	//else
	    //if (log.isDebugEnabled()) log.debug("Ontology already cached, returning cached instance");

	OntModel ontModel = OntDocumentManager.getInstance().getOntology(ontologyUri, OntModelSpec.OWL_MEM_RDFS_INF);
	if (log.isDebugEnabled()) log.debug("Ontology size: {}", ontModel.size());
	return ontModel;
    }

    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders httpHeaders,
	    @QueryParam("limit") @DefaultValue("20") Long limit,
	    @QueryParam("offset") @DefaultValue("0") Long offset,
	    @QueryParam("order-by") String orderBy,
	    @QueryParam("desc") Boolean desc)
    {
	this(getOntology(uriInfo),
		uriInfo, request, httpHeaders, VARIANTS,
		limit, offset, orderBy, desc);
    }

    public static Resource getResource(OntResource ontResource,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	return new ResourceBase(ontResource,
		uriInfo, request, httpHeaders, variants,
		limit, offset, orderBy, desc);
    }
    
    protected ResourceBase(OntModel ontModel,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	this(ontModel.createOntResource(uriInfo.getRequestUri().toString()),
		uriInfo, request, httpHeaders, variants,
		limit, offset, orderBy, desc);
	
	if (log.isDebugEnabled()) log.debug("Constructing LDP ResourceBase");
    }

    protected ResourceBase(OntResource ontResource,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(ontResource, uriInfo, request, httpHeaders, variants);

	this.limit = limit;
	this.offset = offset;
	this.orderBy = orderBy;
	this.desc = desc;

	if (log.isDebugEnabled()) log.debug("Constructing ResourceBase");

	// in case this OntResource does not exist in the ontology OntModel
	if (!getOntModel().containsResource(getOntResource()))
	{
	    OntClass ontClass = matchOntClass();
	    if (ontClass != null)
	    {
		Individual individual = ontClass.createIndividual(getURI());
		if (log.isDebugEnabled()) log.debug("Individual {} created from resource OntClass {}", individual, ontClass);
		
		if (hasRDFType(LDP.Page))
		{
		    OntResource container = getOntModel().createOntResource(getUriInfo().getAbsolutePath().toString());
		    if (log.isDebugEnabled()) log.debug("Adding PageResource metadata: {} ldp:pageOf {}", getOntResource(), container);
		    setPropertyValue(LDP.pageOf, container);

		    if (log.isDebugEnabled())
		    {
			log.debug("OFFSET: {} LIMIT: {}", getOffset(), getLimit());
			log.debug("ORDER BY: {} DESC: {}", getOrderBy(), getDesc());
		    }

		    if (getOffset() >= getLimit())
		    {
			if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", getURI(), getPrevious().getURI());
			addProperty(XHV.prev, getPrevious());
		    }

		    // no way to know if there's a next page without counting results (either total or in current page)
		    //int subjectCount = describe().listSubjects().toList().size();
		    //log.debug("describe().listSubjects().toList().size(): {}", subjectCount);
		    //if (subjectCount >= getLimit())
		    {
			if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", getURI(), getNext().getURI());
			addProperty(XHV.next, getNext());
		    }
		}
	    }
	}
    }

    @Override
    public Response getResponse()
    {
	 // ldp:Container always redirects to first ldp:Page
	if (hasRDFType(LDP.Container))
	{
	    if (log.isDebugEnabled()) log.debug("OntResource is ldp:Container, redirecting to the first ldp:Page");
	    UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
		    replaceQueryParam("limit", getLimit()).
		    replaceQueryParam("offset", getOffset());
	    if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
	    if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());
	    
	    return Response.seeOther(uriBuilder.build()).build();
	}
	
	return super.getResponse();
    }

    @Override
    public Model describe()
    {
	Model description = super.describe();

	if (!description.isEmpty())
	{
	    if (asIndividual().listOntClasses(true).hasNext())
	    {
		//OntClass ontClass = asIndividual().getOntClass(true);
		OntClass ontClass = asIndividual().listOntClasses(true).next();
		if (ontClass.hasProperty(SPIN.constraint))
		{
		    RDFNode constraint = getModel().getResource(ontClass.getURI()).getProperty(SPIN.constraint).getObject();
		    TemplateCall call = SPINFactory.asTemplateCall(constraint);

		    QueryBuilder queryBuilder = QueryBuilder.fromQuery(getQuery(call), getModel());
		    queryBuilder.build(); // sets sp:text value
		    if (log.isDebugEnabled()) log.debug("OntResource {} gets explicit spin:query value {}", this, queryBuilder);
		    setPropertyValue(SPIN.query, queryBuilder);

		    RDFNode mode = getRestrictionHasValue(ontClass, Graphity.mode);
		    if (mode != null && mode.isURIResource())
		    {
			if (log.isDebugEnabled()) log.debug("OntResource {} gets explicit g:mode value {}", this, mode);
			setPropertyValue(Graphity.mode, mode);
		    }

		    description.add(loadModel(getService(ontClass), getQuery(call)));
		}
		
		if (hasRDFType(LDP.Page))
		{
		    // add description of ldp:Container
		    OntResource container = getPropertyResourceValue(LDP.pageOf).as(OntResource.class);
		    LinkedDataResource ldc = new LinkedDataResourceBase(container, getUriInfo(), getRequest(), getHttpHeaders(), getVariants());
		    description.add(ldc.describe());
		}
	    }
	    else
	    {
		QueryBuilder queryBuilder = QueryBuilder.fromDescribe(getURI(), getModel());
		queryBuilder.build(); // sets sp:text value
		if (log.isDebugEnabled()) log.debug("OntResource with URI {} gets explicit spin:query value {}", getURI(), queryBuilder);
		setPropertyValue(SPIN.query, queryBuilder);
	    }
	}
    
	return description;
    }
    
    public com.hp.hpl.jena.rdf.model.Resource getService(OntClass ontClass)
    {
	RDFNode hasValue = getRestrictionHasValue(ontClass, Graphity.service);
	if (hasValue != null && hasValue.isResource()) return hasValue.asResource();

	return null;
    }

    public Model loadModel(com.hp.hpl.jena.rdf.model.Resource service, Query query)
    {
	if (service != null)
	{
	    com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
	    if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", getURI(), endpoint.getURI());

	    return getModelResource(endpoint.getURI(), query).describe();
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its OntModel", getURI());
	    return getModelResource(getOntModel(), query).describe();
	}
    }

    public Query getQuery(TemplateCall call)
    {
	if (call == null) throw new IllegalArgumentException("TemplateCall cannot be null");
	String queryString = call.getQueryString();
	queryString = queryString.replace("?this", "<" + getURI() + ">"); // binds ?this to URI of current resource
	Query arqQuery = QueryFactory.create(queryString);
	
	if (hasRDFType(LDP.Page))
	{
	    if (!arqQuery.isSelectType()) throw new IllegalArgumentException("PageResource must have a SPIN Select query");

	    QueryBuilder queryBuilder ;
	    org.topbraid.spin.model.Query query = ARQ2SPIN.parseQuery(arqQuery.toString(), getModel());
	    SelectBuilder selectBuilder = SelectBuilder.fromSelect((Select)query).
		limit(getLimit()).offset(getOffset());
	    /*
	    if (orderBy != null)
	    {
		com.hp.hpl.jena.rdf.model.Resource modelVar = getOntology().createResource().addLiteral(SP.varName, "model");
		Property orderProperty = ResourceFactory.createProperty(getOrderBy();
		com.hp.hpl.jena.rdf.model.Resource orderVar = getOntology().createResource().addLiteral(SP.varName, orderProperty.getLocalName());

		selectBuilder.orderBy(orderVar, getDesc()).optional(modelVar, orderProperty, orderVar);
	    }
	    */
	    //QueryBuilder queryBuilder;
	    if (selectBuilder.getPropertyResourceValue(SP.resultVariables) != null)
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} has result variables: {}", selectBuilder, selectBuilder.getPropertyResourceValue(SP.resultVariables));
		queryBuilder = QueryBuilder.fromDescribe(selectBuilder.getPropertyResourceValue(SP.resultVariables)).
		    subQuery(selectBuilder);
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} does not have result variables, using wildcard", selectBuilder);
		queryBuilder = QueryBuilder.fromDescribe(selectBuilder.getModel()).subQuery(selectBuilder);
	    }
	    return queryBuilder.build();
	}
	
	return arqQuery;
    }
    
    public final OntClass matchOntClass()
    {
	StringBuilder path = new StringBuilder();
	path.append("/").append(getUriInfo().getPath(false));
	return matchOntClass(path);
    }
    
    public final OntClass matchOntClass(CharSequence path)
    {
	Property utProp = getOntModel().createProperty("http://purl.org/linked-data/api/vocab#uriTemplate");
	ExtendedIterator<Restriction> it = getOntModel().listRestrictions();

	while (it.hasNext())
	{
	    Restriction restriction = it.next();	    
	    if (restriction.canAs(HasValueRestriction.class)) // throw new IllegalArgumentException("Resource matching this URI template is not a HasValueRestriction");
	    {
		HasValueRestriction hvr = restriction.asHasValueRestriction();
		if (hvr.getOnProperty().equals(utProp))
		{
		    UriTemplate uriTemplate = new UriTemplate(hvr.getHasValue().toString());
		    HashMap<String, String> map = new HashMap<String, String>();

		    if (uriTemplate.match(path, map))
		    {
			if (log.isDebugEnabled()) log.debug("Path {} matched UriTemplate {}", path, uriTemplate);

			OntClass ontClass = hvr.listSubClasses(true).next(); //hvr.getSubClass();	    
			if (log.isDebugEnabled()) log.debug("Path {} matched endpoint OntClass {}", path, ontClass);
			return ontClass;
		    }
		    else
			if (log.isDebugEnabled()) log.debug("Path {} did not match UriTemplate {}", path, uriTemplate);
		}
	    }
	}

	if (log.isDebugEnabled()) log.debug("Path {} has no OntClass match in this OntModel", path);
	return null;
    }

    public final Long getLimit()
    {
	return limit;
    }

    public final Long getOffset()
    {
	return offset;
    }

    public final String getOrderBy()
    {
	return orderBy;
    }

    public final Boolean getDesc()
    {
	return desc;
    }

    public RDFNode getRestrictionHasValue(OntClass ontClass, OntProperty property)
    {
	ExtendedIterator<OntClass> it = ontClass.listSuperClasses(true);
	while (it.hasNext())
	{
	    OntClass superClass = it.next();
	    if (superClass.canAs(HasValueRestriction.class))
	    {
		HasValueRestriction restriction = superClass.asRestriction().asHasValueRestriction();
		if (restriction.getOnProperty().equals(property))
		    return restriction.getHasValue();
	    }
	}
	
	return null;
    }

    public final com.hp.hpl.jena.rdf.model.Resource getPrevious()
    {
	UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
	    replaceQueryParam("limit", getLimit()).
	    replaceQueryParam("offset", getOffset() - getLimit());

	if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
	if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());

	return getOntModel().createResource(uriBuilder.build().toString());
    }

    public final com.hp.hpl.jena.rdf.model.Resource getNext()
    {
	UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
	    replaceQueryParam("limit", getLimit()).
	    replaceQueryParam("offset", getOffset() + getLimit());

	if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
	if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());

	return getOntModel().createResource(uriBuilder.build().toString());
    }

}