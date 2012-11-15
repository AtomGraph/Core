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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.impl.LinkedDataPageResourceImpl;
import org.graphity.ldp.model.query.ModelResource;
import org.graphity.ldp.model.query.impl.EndpointModelResourceImpl;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.graphity.model.ResourceFactory;
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
public class LinkedDataResourceBase extends ResourceFactory implements LinkedDataResource // QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(LinkedDataResourceBase.class);

    public static List<Variant> VARIANTS = Variant.VariantListBuilder.newInstance().
		mediaTypes(MediaType.APPLICATION_XHTML_XML_TYPE,
			org.graphity.ldp.MediaType.APPLICATION_RDF_XML_TYPE,
			org.graphity.ldp.MediaType.TEXT_TURTLE_TYPE).
		//languages(new Locale("en")).
		add().build();

    private final UriInfo uriInfo;
    private final Request request;
    private final HttpHeaders httpHeaders;
    private final List<Variant> variants;
    private final OntResource ontResource;
    
    private ModelResource resource = null; // the only mutable field

    public static OntModel getOntology(UriInfo uriInfo)
    {
	return getOntology(uriInfo.getBaseUri().toString(), "org/graphity/ldp/vocabulary/graphity-ldp.ttl");
    }
    
    // avoid (double-checked) locking and make state changes transactional with synchronized
    // http://www.ibm.com/developerworks/java/library/j-dcl/index.html
    // https://open.med.harvard.edu/svn/eagle-i-dev/apps/tags/1.0-MS4.0/common/model-jena/src/main/java/org/eaglei/model/jena/EagleIOntDocumentManager.java
    public static OntModel getOntology(String baseUri, String ontologyPath)
    {
	synchronized (OntDocumentManager.getInstance())
	{
	    if (!OntDocumentManager.getInstance().getFileManager().hasCachedModel(baseUri)) // not cached
	    {	    
		if (log.isDebugEnabled()) log.debug("Ontology not cached, reading from file: {}", ontologyPath);
		if (log.isDebugEnabled()) log.debug("DataManager.get().getLocationMapper(): {}", DataManager.get().getLocationMapper());

		if (log.isDebugEnabled()) log.debug("Adding name/altName mapping: {} altName: {} ", baseUri, ontologyPath);
		OntDocumentManager.getInstance().addAltEntry(baseUri, ontologyPath);

		LocationMapper mapper = OntDocumentManager.getInstance().getFileManager().getLocationMapper();
		if (log.isDebugEnabled()) log.debug("Adding prefix/altName mapping: {} altName: {} ", baseUri, ontologyPath);
		((PrefixMapper)mapper).addAltPrefixEntry(baseUri, ontologyPath);	    
	    }
	    else
		if (log.isDebugEnabled()) log.debug("Ontology already cached, returning cached instance");

	    OntModel ontModel = OntDocumentManager.getInstance().getOntology(baseUri, OntModelSpec.OWL_MEM_RDFS_INF);
	    if (log.isDebugEnabled()) log.debug("Ontology size: {}", ontModel.size());
	    return ontModel;
	}
    }

    private QueryBuilder getQueryBuilder()
    {
	QueryBuilder queryBuilder;
	
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

	return queryBuilder;
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

    public LinkedDataResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders httpHeaders,
	    @QueryParam("limit") Long limit,
	    @QueryParam("offset") Long offset,
	    @QueryParam("order-by") String orderBy,
	    @QueryParam("desc") Boolean desc)
    {
	this(getOntology(uriInfo).createOntResource(uriInfo.getAbsolutePath().toString()),
		uriInfo, request, httpHeaders, VARIANTS,
		limit, offset, orderBy, desc);
    }
    
    protected LinkedDataResourceBase(OntResource ontResource,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (ontResource == null) throw new IllegalArgumentException("OntResource cannot be null");
	if (uriInfo == null) throw new IllegalArgumentException("UriInfo cannot be null");
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (httpHeaders == null) throw new IllegalArgumentException("HttpHeaders cannot be null");
	if (variants == null) throw new IllegalArgumentException("Variants cannot be null");

	if (!ontResource.isURIResource()) throw new IllegalArgumentException("OntResource must be URI Resource (not a blank node)");
	this.ontResource = ontResource;
	if (log.isDebugEnabled()) log.debug("Creating LinkedDataResource from OntResource with URI: {}", ontResource.getURI());

	this.uriInfo = uriInfo;
	this.request = request;
	this.httpHeaders = httpHeaders;
	this.variants = variants;
	if (log.isDebugEnabled()) log.debug("List of Variants: {}", variants);

	if (getOntResource().hasRDFType(SIOC.CONTAINER))
	{
	    if (log.isDebugEnabled()) log.debug("OntResource is a container, returning page Resource");
	    resource = new LinkedDataPageResourceImpl(getOntResource(), getUriInfo(), getRequest(), getHttpHeaders(), getVariants(),
		limit, offset, orderBy, desc);
	    
	    // EXPERIMENTAL!				
	    resource.getModel().add(getOntResource().listProperties());
	}
    }

    @GET
    @Override
    // http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-21#section-5.3.1
    public Response getResponse()
    {
	// Content-Location http://www.w3.org/TR/chips/#cp5.2
	// http://www.w3.org/wiki/HR14aCompromise

	// redirect to the first page
	/*
	if (getOntResource().hasRDFType(SIOC.CONTAINER))
	    return Response.seeOther(UriBuilder.fromUri(getURI()).
		    replaceQueryParam("limit", limit).
		    replaceQueryParam("offset", offset).
		    replaceQueryParam("order-by", orderBy).
		    replaceQueryParam("desc", desc).
		    build()).
		build();
	*/
	
	if (log.isDebugEnabled()) log.debug("Returning @GET Response");
	return getResource().getResponse();
    }

    private ModelResource getResource()
    {	
	if (resource == null) // lazy loading - Response might not need queried Model
	{
	    QueryBuilder queryBuilder;
	    Query query;

	    if (log.isDebugEnabled()) log.debug("Locking ontResource.getModel() before QueryBuilder call");
	    synchronized (getOntResource().getModel())
	    {
		queryBuilder = getQueryBuilder();
		query = queryBuilder.build();
	    }
	    synchronized (getOntResource().getOntModel())
	    {
		getOntResource().setPropertyValue(Graphity.query, queryBuilder); // Resource alway get a g:query value		
	    }
	    if (log.isDebugEnabled()) log.debug("Unlocking ontResource.getModel()");

	    if (getOntResource().hasProperty(Graphity.service))
	    {
		com.hp.hpl.jena.rdf.model.Resource service = getOntResource().getPropertyResourceValue(Graphity.service);
		if (service == null) throw new IllegalArgumentException("SPARQL Service must be a Resource");

		com.hp.hpl.jena.rdf.model.Resource endpoint = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		    createProperty("http://www.w3.org/ns/sparql-service-description#endpoint"));
		if (endpoint == null || endpoint.getURI() == null) throw new IllegalArgumentException("SPARQL Service endpoint must be URI Resource");

		if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", getOntResource().getURI(), endpoint.getURI());

		resource = new EndpointModelResourceImpl(endpoint.getURI(), query, getRequest(), getVariants());
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its Model", getOntResource().getURI());
		if (log.isDebugEnabled()) log.debug("Locking getOntResource.getModel() before SPARQL query");
		synchronized (getOntModel())
		{
		    resource = new QueryModelModelResourceImpl(getOntModel(), query, getRequest(), getVariants());
		}
		if (log.isDebugEnabled()) log.debug("Unlocking getOntResource.getModel()");
	    }
	}
	    
	if (resource.getModel().isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
	
	return resource;
    }

    @Override
    public final String getURI()
    {
	return getOntResource().getURI();
    }

    @Override
    public final EntityTag getEntityTag()
    {
	return getResource().getEntityTag();
    }

    @Override
    public final Model getModel()
    {
	return getResource().getModel();
    }

    @Override
    public final Request getRequest()
    {
	return request;
    }

    public final OntResource getOntResource()
    {
	return ontResource;
    }

    public final OntModel getOntModel()
    {
	return getOntResource().getOntModel();
    }
    
    public final UriInfo getUriInfo()
    {
	return uriInfo;
    }

    @Override
    public final List<Variant> getVariants()
    {
	return variants;
    }

    public final HttpHeaders getHttpHeaders()
    {
	return httpHeaders;
    }

}