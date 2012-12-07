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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.impl.PageResourceImpl;
import org.graphity.model.query.QueriedResource;
import org.graphity.util.QueryBuilder;
import org.graphity.util.locator.PrefixMapper;
import org.graphity.util.manager.DataManager;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.SIOC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class ResourceBase extends LDPResourceBase implements QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private final Long limit, offset;
    private final String orderBy;
    private final Boolean desc;
    private final QueryBuilder describeQuery;
    private Model description = null;

    public static OntModel getOntology(UriInfo uriInfo)
    {
	// ResourceConfig.getProperty()
	return getOntology(uriInfo.getBaseUri().toString(), "org/graphity/ldp/vocabulary/graphity-ldp.ttl");
    }
    
    public static OntModel getOntology(String baseUri, String ontologyPath)
    {
	//synchronized (OntDocumentManager.getInstance())
	{
	    //if (!OntDocumentManager.getInstance().getFileManager().hasCachedModel(baseUri)) // not cached
	    {	    
		if (log.isDebugEnabled())
		{
		    log.debug("Ontology not cached, reading from file: {}", ontologyPath);
		    log.debug("DataManager.get().getLocationMapper(): {}", DataManager.get().getLocationMapper());
		    log.debug("Adding name/altName mapping: {} altName: {} ", baseUri, ontologyPath);
		}
		OntDocumentManager.getInstance().addAltEntry(baseUri, ontologyPath);

		LocationMapper mapper = OntDocumentManager.getInstance().getFileManager().getLocationMapper();
		if (log.isDebugEnabled()) log.debug("Adding prefix/altName mapping: {} altName: {} ", baseUri, ontologyPath);
		((PrefixMapper)mapper).addAltPrefixEntry(baseUri, ontologyPath);	    
	    }
	    //else
		//if (log.isDebugEnabled()) log.debug("Ontology already cached, returning cached instance");

	    OntModel ontModel = OntDocumentManager.getInstance().getOntology(baseUri, OntModelSpec.OWL_MEM_RDFS_INF);
	    if (log.isDebugEnabled()) log.debug("Ontology size: {}", ontModel.size());
	    return ontModel;
	}
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

    protected ResourceBase(OntModel ontModel,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	this(ontModel.createOntResource(uriInfo.getAbsolutePath().toString()),
		uriInfo, request, httpHeaders, variants,
		limit, offset, orderBy, desc);
	
	if (log.isDebugEnabled()) log.debug("Constructing LDP ResourceBase");
    }

    protected ResourceBase(OntResource ontResource,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(ontResource, uriInfo, request, httpHeaders, variants, limit, offset, orderBy, desc);

	this.limit = limit;
	this.offset = offset;
	this.orderBy = orderBy;
	this.desc = desc;

	if (log.isDebugEnabled()) log.debug("Constructing LDP ResourceBase");

	describeQuery = QueryBuilder.fromDescribe(ontResource.getURI(), ontResource.getModel());
	describeQuery.build(); // sets sp:text value
	
	if (!ontResource.hasProperty(Graphity.query)) // Resource always gets a g:query value
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} gets explicit Query Resource {}", ontResource.getURI(), describeQuery);
	    ontResource.setPropertyValue(Graphity.query, describeQuery);
	}
    }

    public QueryBuilder getDescribeQuery()
    {
	return describeQuery;
    }
    
    @Override
    public Model describe()
    {
	//if (description == null)
	{
	    description = super.describe();

	    if (hasRDFType(SIOC.CONTAINER))
	    {
		UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
			replaceQueryParam("limit", getLimit()).
			replaceQueryParam("offset", getOffset());
		if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
		if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());

		//OntClass pageOntClass = matchOntClass();
		//OntClass pageOntClass = getOntModel().getOntClass("http://localhost:8080/ontology/InstancesPageResource");
		OntClass pageOntClass = matchOntClass(getOntResource());
		if (pageOntClass == null) throw new IllegalArgumentException("Container must have a matching PageResource owl:Class");

		if (log.isDebugEnabled()) log.debug("Creating PageResource from with URI: {} ant OntClass: {}", uriBuilder.build().toString(), pageOntClass);
		//OntResource pageOntResource = getOntModel().createOntResource(uriBuilder.build().toString());
		Individual pageInd = pageOntClass.createIndividual(uriBuilder.build().toString());
		
		pageInd.setPropertyValue(SIOC.HAS_CONTAINER, this);
		pageInd.setPropertyValue(Graphity.selectQuery, getSelectQuery(pageInd));
		pageInd.setPropertyValue(Graphity.service, getService(pageInd));
		
		if (log.isDebugEnabled()) log.debug("OntResource is a container, adding PageResource description");
		PageResource page = new PageResourceImpl(pageInd,
		    getUriInfo(), getRequest(), getHttpHeaders(), getVariants(),
		    getLimit(), getOffset(), getOrderBy(), getDesc());

		description.add(page.describe());
	    }

	    if (hasProperty(Graphity.service))
	    {
		com.hp.hpl.jena.rdf.model.Resource service = getPropertyResourceValue(Graphity.service);
		if (service == null) throw new IllegalArgumentException("SPARQL Service must be a Resource");

		com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		    createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
		if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

		if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", getURI(), endpoint.getURI());

		// query endpoint whenever g:service is present
		description.add(getModelResource(endpoint.getURI(), getQuery()).describe());
	    }
	    else
	    {
		// don't do default DESCRIBE on OntModel again - super.describe() does it
		// compare on the SPARQL syntax level - equals() on objects doesn't seem to work
		//if (!getQueryBuilder().equals(getDescribeQuery()))
		if (!getQueryBuilder().toString().equals(getDescribeQuery().toString()))
		{
		    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its OntModel", getURI());
		    description.add(getModelResource(getOntModel(), getQuery()).describe());
		}
	    }
	}
	
	return description;
    }

    @Override
    public Query getQuery()
    {
	return getQueryBuilder().build();
    }

    public QueryBuilder getQueryBuilder()
    {
	if (log.isDebugEnabled()) log.debug("OntResource with URI {} has Query Resource {}", getURI(), getPropertyResourceValue(Graphity.query));
	return QueryBuilder.fromResource(getPropertyResourceValue(Graphity.query));
    }

    public OntClass matchOntClass(OntResource container)
    {
	if (container == null) throw new IllegalArgumentException("Container must not be null");	
	//container.getOntModel().listRestrictions();
	//ExtendedIterator<OntClass> it = container.getOntModel().listClasses();
	ExtendedIterator<Restriction> it = container.getOntModel().listRestrictions();
	
	while (it.hasNext())
	{
	    //OntClass ontClass = it.next();
	    Restriction restriction = it.next();
	    if (restriction.canAs(HasValueRestriction.class))
	    {
		HasValueRestriction hvr = restriction.asHasValueRestriction();
		if (hvr.getOnProperty().asResource().equals(SIOC.HAS_CONTAINER.asResource())
			&& hvr.getHasValue().equals(container))
		    return hvr.getSubClass();
	    }
	}
	
	return null;
    }

    public OntClass matchOntClass(OntModel ontModel)
    {
	if (log.isDebugEnabled()) log.debug("Matching @Path annotation {} of Class {}", getClass().getAnnotation(Path.class).value(), getClass());
	return matchOntClass(getClass().getAnnotation(Path.class).value(), ontModel);
    }

    public OntClass matchOntClass(Class<?> cls, OntModel ontModel)
    {
	if (log.isDebugEnabled()) log.debug("Matching @Path annotation {} of Class {}", cls.getAnnotation(Path.class).value(), cls);
	return matchOntClass(cls.getAnnotation(Path.class).value(), ontModel);
    }
    
    public OntClass matchOntClass(String uriTemplate, OntModel ontModel)
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
	    if (log.isDebugEnabled()) log.debug("URI template {} has no OntClass match in OntModel {}", uriTemplate, ontModel);
	    return null;   
	}
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

    public static com.hp.hpl.jena.rdf.model.Resource getSelectQuery(OntClass ontClass)
    {
	ExtendedIterator<OntClass> it = ontClass.listSuperClasses(true);
	while (it.hasNext())
	{
	    OntClass superClass = it.next();
	    if (superClass.canAs(HasValueRestriction.class))
	    {
		HasValueRestriction restriction = superClass.asRestriction().asHasValueRestriction();
		if (restriction.getOnProperty().equals(Graphity.selectQuery))
		    return restriction.getHasValue().asResource();
	    }
	}
	
	return null;
    }

    public com.hp.hpl.jena.rdf.model.Resource getService(Individual ind)
    {
	ExtendedIterator<OntClass> it = ind.listOntClasses(false);
	while (it.hasNext())
	{
	    OntClass ontClass = it.next();
	    if (ontClass.canAs(HasValueRestriction.class))
	    {
		HasValueRestriction restriction = ontClass.asRestriction().asHasValueRestriction();
		if (restriction.getOnProperty().equals(Graphity.service))
		    return restriction.getHasValue().asResource();
	    }
	}
	
	return null;
    }

    public com.hp.hpl.jena.rdf.model.Resource getSelectQuery(Individual ind)
    {
	ExtendedIterator<OntClass> it = ind.listOntClasses(false);
	while (it.hasNext())
	{
	    OntClass ontClass = it.next();
	    if (ontClass.canAs(HasValueRestriction.class))
	    {
		HasValueRestriction restriction = ontClass.asRestriction().asHasValueRestriction();
		if (restriction.getOnProperty().equals(Graphity.selectQuery))
		    return restriction.getHasValue().asResource();
	    }
	}
	
	return null;
    }

    public static com.hp.hpl.jena.rdf.model.Resource getService(OntClass ontClass)
    {
	ExtendedIterator<OntClass> it = ontClass.listSuperClasses(true);
	while (it.hasNext())
	{
	    OntClass superClass = it.next();
	    if (superClass.canAs(HasValueRestriction.class))
	    {
		HasValueRestriction restriction = superClass.asRestriction().asHasValueRestriction();
		if (restriction.getOnProperty().equals(Graphity.service))
		    return restriction.getHasValue().asResource();
	    }
	}
	
	return null;
    }
    
}