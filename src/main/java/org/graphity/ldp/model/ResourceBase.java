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
import org.graphity.ldp.model.impl.PageResourceImpl;
import org.graphity.util.QueryBuilder;
import org.graphity.util.locator.PrefixMapper;
import org.graphity.util.manager.DataManager;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.LDP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.vocabulary.SPIN;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class ResourceBase extends LDPResourceBase
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
	    }
	}
    }

    @Override
    public Response getResponse()
    {
	 // ldp:Container always redirects to first ldp:Page
	if (hasRDFType(LDP.Container))
	{
	    UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
		    replaceQueryParam("limit", getLimit()).
		    replaceQueryParam("offset", getOffset());
	    if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
	    if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());
	    
	    return Response.seeOther(uriBuilder.build()).build();
	}
	if (hasRDFType(LDP.Page) && !(this instanceof PageResource))
	{
	    if (log.isDebugEnabled()) log.debug("OntResource is a page, returning PageResource Response");
	    PageResource page = new PageResourceImpl(getOntResource(),
		getUriInfo(), getRequest(), getHttpHeaders(), getVariants(),
		getLimit(), getOffset(), getOrderBy(), getDesc());

	    return page.getResponse();
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

		    description.add(loadModel(call));
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

    public Model loadModel(TemplateCall call)
    {
	if (call.hasProperty(Graphity.service))
	{
	    String endpointUri = null;
	    com.hp.hpl.jena.rdf.model.Resource service = call.getPropertyResourceValue(Graphity.service);
	    if (service != null) endpointUri = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		createProperty("http://www.w3.org/ns/sparql-service-description#endpoint")).getURI();

	    com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
	    if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", call.getURI(), endpoint.getURI());

	    return getModelResource(endpointUri, getQuery(call)).describe();
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its OntModel", getURI());
	    return getModelResource(getOntModel(), getQuery(call)).describe();
	}
    }
    
    public Query getQuery(TemplateCall call)
    {
	String queryString = call.getQueryString();
	queryString = queryString.replace("?this", "<" + getURI() + ">"); // binds ?this to URI of current resource
	return QueryFactory.create(queryString);
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

}