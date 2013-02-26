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
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.uri.UriTemplate;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.ldp.query.InsertDataBuilder;
import org.graphity.ldp.query.QueryBuilder;
import org.graphity.ldp.query.SelectBuilder;
import org.graphity.ldp.vocabulary.*;
import org.graphity.util.locator.PrefixMapper;
import org.graphity.util.manager.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.vocabulary.SPIN;

/**
 * Base class of generic read-write Graphity LDP resources
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see PageResource
 */
@Path("{path: .*}")
public class ResourceBase extends LDPResourceBase implements PageResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private final Long limit, offset;
    private final String orderBy;
    private final Boolean desc;
    private final OntClass matchedOntClass;
    private final com.hp.hpl.jena.rdf.model.Resource dataset, endpoint, service;
    private final Query query;
    private final QueryBuilder queryBuilder;

    /**
     * Reads ontology from configured file and resolves against base URI of the request
     * @param uriInfo JAX-RS URI info
     * @param config configuration from web.xml
     * @return ontology Model
     * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceConfig.html">ResourceConfig</a>
     */
    public static OntModel getOntology(UriInfo uriInfo, ResourceConfig config)
    {
	if (log.isDebugEnabled()) log.debug("web.xml properties: {}", config.getProperties());
	Object ontologyLocation = config.getProperty(org.graphity.ldp.Application.PROPERTY_ONTOLOGY_LOCATION);
	Object ontologyPath = config.getProperty(org.graphity.ldp.Application.PROPERTY_ONTOLOGY_PATH);
	Object ontologyEndpoint = config.getProperty(org.graphity.ldp.Application.PROPERTY_ONTOLOGY_ENDPOINT);
	Object ontologyUri = config.getProperty(org.graphity.ldp.Application.PROPERTY_ONTOLOGY_URI);

	if (ontologyUri != null)
	{
	    if (ontologyEndpoint != null)
	    {
		if (log.isDebugEnabled()) log.debug("Reading ontology from named graph {} in SPARQL endpoint {}", ontologyUri, ontologyEndpoint);
		Query query = QueryFactory.create("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } }");
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF,
			DataManager.get().loadModel(ontologyEndpoint.toString(), query));
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("Reading ontology from remote file with URI: {}", ontologyUri);
		return OntDocumentManager.getInstance().getOntology(ontologyUri.toString(), OntModelSpec.OWL_MEM_RDFS_INF);
	    }
	}
	else
	{
	    if (ontologyEndpoint != null)
	    {
		if (log.isDebugEnabled()) log.debug("Reading ontology from default graph in SPARQL endpoint {}", ontologyEndpoint);
		Query query = QueryFactory.create("CONSTRUCT WHERE { ?s ?p ?o }");
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF,
			DataManager.get().loadModel(ontologyEndpoint.toString(), query));
	    }
	    else
	    {
		if (ontologyLocation == null || ontologyPath == null) throw new IllegalStateException("Ontology for this Graphity LDP Application is not configured properly. Check ResourceConfig and/or web.xml");
		if (log.isDebugEnabled()) log.debug("Reading ontology from local file");
		return getOntology(uriInfo.getBaseUriBuilder().path(ontologyPath.toString()).build().toString(), ontologyLocation.toString());
	    }
	}
    }
    
    public static OntModel getOntology(String ontologyUri, String ontologyLocation)
    {
	//if (!OntDocumentManager.getInstance().getFileManager().hasCachedModel(baseUri)) // not cached
	{	    
	    if (log.isDebugEnabled())
	    {
		log.debug("Ontology not cached, reading from file: {}", ontologyLocation);
		log.debug("DataManager.get().getLocationMapper(): {}", DataManager.get().getLocationMapper());
		log.debug("Adding name/altName mapping: {} altName: {} ", ontologyUri, ontologyLocation);
	    }
	    OntDocumentManager.getInstance().addAltEntry(ontologyUri, ontologyLocation);

	    LocationMapper mapper = OntDocumentManager.getInstance().getFileManager().getLocationMapper();
	    if (log.isDebugEnabled()) log.debug("Adding prefix/altName mapping: {} altName: {} ", ontologyUri, ontologyLocation);
	    ((PrefixMapper)mapper).addAltPrefixEntry(ontologyUri, ontologyLocation);	    
	}
	//else
	    //if (log.isDebugEnabled()) log.debug("Ontology already cached, returning cached instance");

	OntModel ontModel = OntDocumentManager.getInstance().getOntology(ontologyUri, OntModelSpec.OWL_MEM_RDFS_INF);
	if (log.isDebugEnabled()) log.debug("Ontology size: {}", ontModel.size());
	return ontModel;
    }

    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders httpHeaders,
	    @Context ResourceConfig config,
	    @QueryParam("limit") @DefaultValue("20") Long limit,
	    @QueryParam("offset") @DefaultValue("0") Long offset,
	    @QueryParam("order-by") String orderBy,
	    @QueryParam("desc") Boolean desc)
    {
	this(getOntology(uriInfo, config),
		uriInfo, request, httpHeaders, VARIANTS,
		(config.getProperty(org.graphity.ldp.Application.PROPERTY_CACHE_CONTROL) == null) ? null : CacheControl.valueOf(config.getProperty(org.graphity.ldp.Application.PROPERTY_CACHE_CONTROL).toString()),
		limit, offset, orderBy, desc);
    }
    
    protected ResourceBase(OntModel ontModel,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants, CacheControl cacheControl,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	this(ontModel.createOntResource(uriInfo.getRequestUri().toString()),
		uriInfo, request, httpHeaders, variants, cacheControl,
		limit, offset, orderBy, desc);
	
	if (log.isDebugEnabled()) log.debug("Constructing LDP ResourceBase");
    }

    protected ResourceBase(OntResource ontResource,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants, CacheControl cacheControl,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(ontResource, uriInfo, request, httpHeaders, variants, cacheControl);

	this.limit = limit;
	this.offset = offset;
	this.orderBy = orderBy;
	this.desc = desc;
	
	matchedOntClass = matchOntClass(getRealURI());
	if (matchedOntClass == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
	if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with matched OntClass: {}", matchedOntClass);
	
	if (matchedOntClass.hasSuperClass(LDP.Page)) //if (hasRDFType(LDP.Page))
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
	
	query = getQuery(matchedOntClass, getRealURI());
	queryBuilder = QueryBuilder.fromQuery(query, getModel());
	if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with Query: {} and QueryBuilder: {}", query, queryBuilder);
	
	dataset = getDataset(matchedOntClass);
	if (dataset == null) throw new IllegalArgumentException("Resource OntClass must be a subclass of void:inDataset HasValueRestriction");
	if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with Dataset: {}", dataset);
	
	endpoint = dataset.getPropertyResourceValue(VoID.sparqlEndpoint);
	if (endpoint != null) service = getService(endpoint);
	else service = null;
	if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with SPARQL endpoint: {} and sd:Service: {}", endpoint, service);
    }

    @Override
    public Response getResponse()
    {
	 // ldp:Container always redirects to first ldp:Page
	if (hasRDFType(LDP.Container))
	{
	    if (log.isDebugEnabled()) log.debug("OntResource is ldp:Container, redirecting to the first ldp:Page");
	    //if (log.isDebugEnabled()) log.debug("Encoded order-by URI: {}", UriComponent.encode(getOrderBy(), UriComponent.Type.QUERY));

	    UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
		queryParam("limit", getLimit()).
		queryParam("offset", getOffset());
	    //if (getOrderBy() != null) uriBuilder.queryParam("order-by", UriComponent.encode(getOrderBy(), UriComponent.Type.QUERY));
	    //if (getDesc() != null) uriBuilder.queryParam("desc", getDesc());
	    
	    return Response.seeOther(uriBuilder.buildFromEncoded()).build();
	}

	return super.getResponse();
    }

    @Override
    public Model describe()
    {
	Model description = ModelFactory.createDefaultModel().
		add(loadModel(getEndpoint(), getQuery()));

	if (hasRDFType(LDP.Page))
	{
	    if (log.isDebugEnabled()) log.debug("Adding description of the ldp:Page");
	    description.add(super.describe());
	    
	    if (log.isDebugEnabled()) log.debug("Adding description of the ldp:Container");
	    OntResource container = getPropertyResourceValue(LDP.pageOf).as(OntResource.class);
	    Resource ldc = new ResourceBase(container, getUriInfo(), getRequest(), getHttpHeaders(), getVariants(), getCacheControl(),
		    getLimit(), getOffset(), getOrderBy(), getDesc());
	    description.add(ldc.describe());
	}

	// set metadata properties after description query is executed
	if (log.isDebugEnabled()) log.debug("OntResource {} gets explicit spin:query value {}", this, getQueryBuilder());
	setPropertyValue(SPIN.query, getQueryBuilder());

	RDFNode mode = getRestrictionHasValue(getMatchedOntClass(), Graphity.mode);
	if (mode != null && mode.isURIResource())
	{
	    if (log.isDebugEnabled()) log.debug("OntResource {} gets explicit g:mode value {}", this, mode);
	    setPropertyValue(Graphity.mode, mode);
	}

	return description;
    }
    
    public final com.hp.hpl.jena.rdf.model.Resource getDataset(OntClass ontClass)
    {
	RDFNode hasValue = getRestrictionHasValue(ontClass, VoID.inDataset);
	if (hasValue != null && hasValue.isResource()) return hasValue.asResource();

	return null;
    }

    public final com.hp.hpl.jena.rdf.model.Resource getService(com.hp.hpl.jena.rdf.model.Resource endpoint)
    {
	ResIterator it = getModel().listResourcesWithProperty(SD.endpoint, endpoint);
	
	if (it.hasNext()) return it.next();

	return null;
    }
    
    public Model loadModel(com.hp.hpl.jena.rdf.model.Resource endpoint, Query query)
    {
	if (endpoint != null && endpoint.getURI() != null)
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has explicit SPARQL endpoint: {}", getURI(), endpoint.getURI());

	    return DataManager.get().loadModel(endpoint.getURI(), query);
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("OntResource with URI: {} has no explicit SPARQL endpoint, querying its OntModel", getURI());
	    return DataManager.get().loadModel(getOntModel(), query);
	}
    }
    
    public QueryBuilder getQueryBuilder(org.topbraid.spin.model.Query query)
    {
	QueryBuilder qb = QueryBuilder.fromQuery(query);
	if (qb.getSubSelectBuilder() == null) throw new IllegalArgumentException("The SPIN query for ldp:Page class does not have a SELECT subquery");
	
	SelectBuilder selectBuilder = qb.getSubSelectBuilder().
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

	return qb;
    }

    public final Query getQuery(OntClass ontClass, URI thisUri)
    {
	return getQuery(getTemplateCall(ontClass), thisUri);
    }
    
    public TemplateCall getTemplateCall(OntClass ontClass)
    {
	if (!ontClass.hasProperty(SPIN.constraint))
	    throw new IllegalArgumentException("Resource OntClass must have a SPIN constraint Template");	    

	RDFNode constraint = getModel().getResource(ontClass.getURI()).getProperty(SPIN.constraint).getObject();
	return SPINFactory.asTemplateCall(constraint);
    }
    
    public Query getQuery(TemplateCall call, URI thisUri)
    {
	if (call == null) throw new IllegalArgumentException("TemplateCall cannot be null");
	String queryString = call.getQueryString();
	queryString = queryString.replace("?this", "<" + thisUri.toString() + ">"); // binds ?this to URI of current resource
	Query arqQuery = QueryFactory.create(queryString);
	
	if (hasRDFType(LDP.Page))
	{
	    if (log.isDebugEnabled()) log.debug("OntResource is an ldp:Page, making QueryBuilder from Query: {}", arqQuery);
	    return getQueryBuilder(ARQ2SPIN.parseQuery(arqQuery.toString(), getModel())).build();
	}
	
	return arqQuery;
    }

    public final OntClass matchOntClass(URI uri)
    {
	StringBuilder path = new StringBuilder();
	//path.append("/").append(getUriInfo().getPath(false));
	// instead of path, include query string by relativizing request URI against base URI
	path.append("/").append(getUriInfo().getBaseUri().relativize(uri));
	return matchOntClass(path);
    }
    
    public final OntClass matchOntClass(CharSequence path)
    {
	Property utProp = getOntModel().createProperty("http://purl.org/linked-data/api/vocab#uriTemplate");
	ExtendedIterator<Restriction> it = getOntModel().listRestrictions();
	TreeMap<UriTemplate, OntClass> matchedClasses = new TreeMap<UriTemplate,OntClass>(UriTemplate.COMPARATOR);
		
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
			//return ontClass;
			matchedClasses.put(uriTemplate, ontClass);
		    }
		    else
			if (log.isDebugEnabled()) log.debug("Path {} did not match UriTemplate {}", path, uriTemplate);
		}
	    }
	}

	if (!matchedClasses.isEmpty())
	{
	    if (log.isDebugEnabled()) log.debug("Matched UriTemplate: {} OntClass: {}", matchedClasses.firstKey(), matchedClasses.firstEntry().getValue());
	    return matchedClasses.firstEntry().getValue(); //matchedClasses.lastEntry().getValue();
	}
	
	if (log.isDebugEnabled()) log.debug("Path {} has no OntClass match in this OntModel", path);
	return null;
    }

    @Override
    public final Long getLimit()
    {
	return limit;
    }

    @Override
    public final Long getOffset()
    {
	return offset;
    }

    @Override
    public final String getOrderBy()
    {
	return orderBy;
    }

    @Override
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

    @Override
    public final com.hp.hpl.jena.rdf.model.Resource getPrevious()
    {
	UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
	    queryParam("limit", getLimit()).
	    queryParam("offset", getOffset() - getLimit());
	//if (getOrderBy() != null) uriBuilder.queryParam("order-by", getOrderBy());
	//if (getDesc() != null) uriBuilder.queryParam("desc", getDesc());

	return getOntModel().createResource(uriBuilder.build().toString());
    }

    @Override
    public final com.hp.hpl.jena.rdf.model.Resource getNext()
    {
	UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
	    queryParam("limit", getLimit()).
	    queryParam("offset", getOffset() + getLimit());
	//if (getOrderBy() != null) uriBuilder.queryParam("order-by", getOrderBy());
	//if (getDesc() != null) uriBuilder.queryParam("desc", getDesc());

	return getOntModel().createResource(uriBuilder.build().toString());
    }

    public final UriBuilder getUriBuilder()
    {
	return UriBuilder.fromUri(getURI());
    }
    
    public final URI getRealURI()
    {
	return getUriBuilder().build();
    }

    public OntClass getMatchedOntClass()
    {
	return matchedOntClass;
    }

    public com.hp.hpl.jena.rdf.model.Resource getDataset()
    {
	return dataset;
    }

    public com.hp.hpl.jena.rdf.model.Resource getEndpoint()
    {
	return endpoint;
    }

    public com.hp.hpl.jena.rdf.model.Resource getService()
    {
	return service;
    }

    public Query getQuery()
    {
	return query;
    }

    public final QueryBuilder getQueryBuilder()
    {
	return queryBuilder;
    }

    @Override
    public Response post(Model model)
    {
	if (log.isDebugEnabled()) log.debug("Returning @POST Response of the POSTed Model");
	
	InsertDataBuilder insertBuilder = InsertDataBuilder.fromData(model);
	if (log.isDebugEnabled()) log.debug("INSERT DATA generated from the POSTed Model: {}", insertBuilder);
	
	//getQueryBuilder()
	
	return getResponse(model);
    }

}