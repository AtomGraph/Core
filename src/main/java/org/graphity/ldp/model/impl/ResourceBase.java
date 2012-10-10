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
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.Resource;
import org.graphity.model.QueriedResource;
import org.graphity.util.ModelUtils;
import org.graphity.util.QueryBuilder;
import org.graphity.vocabulary.Graphity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
//@Path("/")
public class ResourceBase implements Resource, QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);

    private OntModel ontology = null;
    private UriInfo uriInfo = null;
    private Request req = null;
    //private com.hp.hpl.jena.rdf.model.Resource resource = null, query = null;
    //private Model model = null;
    private Query query = null;

    public ResourceBase(OntModel ontology, UriInfo uriInfo, Request req)
    {
	this.ontology = ontology;
	this.uriInfo = uriInfo;
	this.req = req;
    }
    
    @Override
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
	Model model = null;
	
	if (getService() != null)
	{
	    String endpointUri = getService().
		getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		    createProperty("http://www.w3.org/ns/sparql-service-description#endpoint")).getURI();
	
	    if (endpointUri != null)
		model = org.graphity.model.ResourceFactory.getResource(endpointUri, getQuery()).getModel();
	}
	else
	    model = org.graphity.model.ResourceFactory.getResource(getOntology(), getQuery()).getModel();
	
	if (model.isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}

	return model;
    }

    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML + ";qs=2;charset=UTF-8")
    //@Override
    public Response getXHTMLResponse()
    {
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

    @Override
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
	if (query == null) query = getQueryBuilder().build();
	
	return query;
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