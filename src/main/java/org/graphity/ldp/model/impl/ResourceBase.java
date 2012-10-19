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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.graphity.ldp.model.LDPResource;
import org.graphity.ldp.model.XHTMLResource;
import org.graphity.model.query.ModelResource;
import org.graphity.model.query.QueriedResource;
import org.graphity.util.ModelUtils;
import org.graphity.util.QueryBuilder;
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
public class ResourceBase implements LDPResource, ModelResource, QueriedResource, XHTMLResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private OntModel ontology = null;
    private UriInfo uriInfo = null;
    private Request request = null;
    private String uri = null;
    private OntResource ontResource = null;
    private com.hp.hpl.jena.rdf.model.Resource queryResource, service = null;
    private QueryBuilder queryBuilder = null;
    private Query query = null;
    private String endpointUri = null;
    private Model model = null;
    private EntityTag entityTag = null;
    private boolean isContainer = false;

    public ResourceBase(OntModel ontology, UriInfo uriInfo, Request request)
    {
	this.ontology = ontology;
	this.uriInfo = uriInfo;
	this.request = request;

	uri = uriInfo.getAbsolutePath().toString();
	ontResource = ontology.getOntResource(uri);
	
	if (ontResource != null)
	{
	    queryResource = ontResource.getPropertyResourceValue(Graphity.query);
	    service = ontResource.getPropertyResourceValue(Graphity.service);
	    isContainer = ontResource.hasRDFType(SIOC.CONTAINER);
	}
	
	if (queryResource != null) queryBuilder = QueryBuilder.fromResource(queryResource); // CONSTRUCT
	else queryBuilder = QueryBuilder.fromDescribe(uri); // default DESCRIBE	
	query = queryBuilder.build();	

	if (service != null)
	    endpointUri = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		createProperty("http://www.w3.org/ns/sparql-service-description#endpoint")).getURI();

	if (endpointUri != null) model = DataManager.get().loadModel(endpointUri, query);
	else model = DataManager.get().loadModel(ontology, query);
	if (model.isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}

	entityTag = new EntityTag(Long.toHexString(ModelUtils.hashModel(model)));
    }
    
    public OntModel getOntology()
    {
	return ontology;
    }
    
    @Override
    public Request getRequest()
    {
	return request;
    }

    @Override
    public UriInfo getUriInfo()
    {
	return uriInfo;
    }
    
    @Override
    public String getURI()
    {
	return uri;
    }

    public OntResource getOntResource()
    {
	return ontResource;
    }

    public com.hp.hpl.jena.rdf.model.Resource getQueryResource()
    {
	return queryResource;
    }

    public QueryBuilder getQueryBuilder()
    {
	return queryBuilder;
    }
    
    @Override
    public Model getModel()
    {
	return model;
    }

    @Override
    public Response getXHTMLResponse()
    {
	// check if resource was modified and return 304 Not Modified if not
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null) return rb.build();
	
	return Response.ok(this).tag(getEntityTag()).build(); // uses ResourceXHTMLWriter
    }

    @Override
    public Response getResponse()
    {
	// check if resource was modified and return 304 Not Modified if not
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null) return rb.build();
	
	return Response.ok(getModel()).tag(getEntityTag()).build(); // uses ResourceXHTMLWriter
    }

    @Override
    public EntityTag getEntityTag()
    {
	return entityTag;
    }

    public com.hp.hpl.jena.rdf.model.Resource getService()
    {	
	return service;
    }

    public String getEndpointURI()
    {
	return endpointUri;
    }

    @Override
    public Query getQuery()
    {
	return query;
    }

    @Override
    public boolean isContainer()
    {
	return isContainer;
    }

    @Override
    public Response post(Model model)
    {
	throw new WebApplicationException(405); // method not allowed
    }

    @Override
    public Response put(Model model)
    {
	throw new WebApplicationException(405); // method not allowed
    }

    @Override
    public Response delete()
    {
	throw new WebApplicationException(405); // method not allowed
    }

}