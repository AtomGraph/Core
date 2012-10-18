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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.LDPResource;
import org.graphity.model.ModelResource;
import org.graphity.model.QueriedResource;
import org.graphity.util.ModelUtils;
import org.graphity.util.QueryBuilder;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.SIOC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class ResourceBase implements LDPResource, ModelResource, QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private OntModel ontology = null;
    private UriInfo uriInfo = null;
    private Request req = null;

    public ResourceBase(OntModel ontology, UriInfo uriInfo, Request req)
    {
	this.ontology = ontology;
	this.uriInfo = uriInfo;
	this.req = req;
    }
    
    public OntModel getOntology()
    {
	return ontology;
    }
    
    @Override
    public Request getRequest()
    {
	return req;
    }

    @Override
    public UriInfo getUriInfo()
    {
	return uriInfo;
    }
    
    @Override
    public String getURI()
    {
	return getUriInfo().getAbsolutePath().toString();
    }

    public QueryBuilder getQueryBuilder()
    {
	if (getQueryResource() != null) return QueryBuilder.fromResource(getQueryResource()); // CONSTRUCT
	
	return QueryBuilder.fromDescribe(getURI()); // default DESCRIBE
    }
    
    @Override
    public Model getModel()
    {
	if (getEndpointURI() != null)
	    return org.graphity.model.ModelResourceFactory.getResource(getEndpointURI(), getQuery()).getModel();
	
	return org.graphity.model.ModelResourceFactory.getResource(getOntology(), getQuery()).getModel();
    }

    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML + ";qs=2;charset=UTF-8")
    //@Override
    public Response getXHTMLResponse()
    {
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null) return rb.build();

	Model model = getModel();
	if (model.isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
	else
	    return Response.ok(this).tag(getEntityTag()).build(); // uses ResourceXHTMLWriter
    }

    @Override
    public Response getResponse()
    {
	// check if resource was modified and return 304 Not Modified if not
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null) return rb.build();

	Model model = getModel();
	if (model.isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
	else
	    return Response.ok(getModel()).tag(getEntityTag()).build(); // uses ModelProvider
    }

    @Override
    public EntityTag getEntityTag()
    {
	return new EntityTag(Long.toHexString(ModelUtils.hashModel(getModel())));
    }

    public com.hp.hpl.jena.rdf.model.Resource getService()
    {
	if (getOntResource() != null)
	    return getOntResource().getPropertyResourceValue(Graphity.service);
	
	return null;
    }

    public String getEndpointURI()
    {
	if (getService() != null)
	    return getService().getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		createProperty("http://www.w3.org/ns/sparql-service-description#endpoint")).getURI();
	
	return null;
    }

    public OntResource getOntResource()
    {
	return getOntology().getOntResource(getURI());
    }

    public com.hp.hpl.jena.rdf.model.Resource getQueryResource()
    {
	if (getOntResource() != null)
	    return getOntResource().getPropertyResourceValue(Graphity.query);
	
	return null;
    }

    @Override
    public Query getQuery()
    {
	//if (query == null) query = getQueryBuilder().build();
	
	return getQueryBuilder().build();
	
	//return query;
    }

    @Override
    public boolean isContainer()
    {
	return getOntResource() != null && getOntResource().hasRDFType(SIOC.CONTAINER);
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