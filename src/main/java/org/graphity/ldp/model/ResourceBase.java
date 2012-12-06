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
    private final QueryBuilder describeQuery, queryBuilder;
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
	
	if (!ontResource.hasProperty(Graphity.query)) // Resource always gets a g:query value
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI {} gets explicit Query Resource {}", ontResource.getURI(), describeQuery);
	    ontResource.setPropertyValue(Graphity.query, describeQuery);
	}
	
	if (log.isDebugEnabled()) log.debug("OntResource with URI {} has Query Resource {}", ontResource.getURI(), ontResource.getPropertyResourceValue(Graphity.query));
	queryBuilder = QueryBuilder.fromResource(ontResource.getPropertyResourceValue(Graphity.query));
    }

    public QueryBuilder getDescribeQuery()
    {
	return describeQuery;
    }
    
    @Override
    public Model describe()
    {
	if (description == null)
	{
	    description = super.describe();

	    if (hasRDFType(SIOC.CONTAINER))
	    {
		if (log.isDebugEnabled()) log.debug("OntResource is a container, adding PageResource description");
		PageResource page = new PageResourceImpl(this,
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
	return queryBuilder;
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

}