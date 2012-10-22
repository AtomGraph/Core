/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graphity.ldp.model.impl;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.LDPResource;
import org.graphity.ldp.model.LinkedDataResource;
import org.graphity.ldp.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LDPResourceBase implements LDPResource
{
    private static final Logger log = LoggerFactory.getLogger(LDPResourceBase.class);

    private LinkedDataResource resource = null;

    public LDPResourceBase(OntResource ontResource, UriInfo uriInfo, Request request,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	resource = ResourceFactory.getLinkedDataResource(ontResource, uriInfo, request, null, limit, offset, orderBy, desc);
	
	if (resource.getModel().isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("Loaded Model is empty");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
    }

    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML + ";qs=2;charset=UTF-8")
    public Response getXHTMLResponse()
    {
	if (log.isTraceEnabled()) log.trace("Returning XHTML Response");
	    
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(getEntityTag());
	if (rb != null) return rb.build();
	
	//GenericEntity<LinkedDataResource> entity = new GenericEntity<LinkedDataResource>(getResource()) { };
	return Response.ok(getResource()).tag(getEntityTag()).build(); // uses ResourceXHTMLWriter
    }

    @Override
    public Response getResponse()
    {
	return getResource().getResponse();
    }
    
    @Override
    public Response post(Model model)
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Response put(Model model)
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Response delete()
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Request getRequest()
    {
	return getResource().getRequest();
    }

    @Override
    public UriInfo getUriInfo()
    {
	return getResource().getUriInfo();
    }

    @Override
    public EntityTag getEntityTag()
    {
	return getResource().getEntityTag();
    }

    @Override
    public String getURI()
    {
	return getResource().getURI();
    }

    @Override
    public Model getModel()
    {
	return getResource().getModel();
    }

    public LinkedDataResource getResource()
    {
	return resource;
    }
    
}