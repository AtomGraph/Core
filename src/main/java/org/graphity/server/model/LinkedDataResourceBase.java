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
package org.graphity.server.model;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.core.ResourceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-only Linked Data resources
 * 
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Resource.html">Resource</a>
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class LinkedDataResourceBase implements LinkedDataResource
{
    private static final Logger log = LoggerFactory.getLogger(LinkedDataResourceBase.class);

    /**
     * Configuration property for default Cache-Control header value (set in web.xml)
     * 
     */
    public static final String PROPERTY_CACHE_CONTROL = "org.graphity.server.cache-control";

    private final Resource resource;
    private final SPARQLEndpointBase endpoint;
    private final CacheControl cacheControl;
    @Context UriInfo uriInfo;
    @Context ResourceConfig resourceConfig;
    @Context ResourceContext resourceContext;

    /** 
     * Constructs read-only LD resource from Jena's Resource and JAX-RS context
     * 
     * @param resource the current resource in the ontology
     * @param uriInfo URI information
     * @param request current request
     * @param httpHeaders current request headers
     * @param variants representation variants
     */
    public LinkedDataResourceBase(@Context UriInfo uriInfo, @Context ResourceConfig resourceConfig, @Context ResourceContext resourceContext)
    {
	this(ResourceFactory.createResource(uriInfo.getAbsolutePath().toString()),
		resourceContext.getResource(SPARQLEndpointBase.class),
		(resourceConfig.getProperty(PROPERTY_CACHE_CONTROL) == null) ? null : CacheControl.valueOf(resourceConfig.getProperty(PROPERTY_CACHE_CONTROL).toString()));
    }
    
    protected LinkedDataResourceBase(Resource resource, SPARQLEndpointBase endpoint, CacheControl cacheControl)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");	
	if (!resource.isURIResource()) throw new IllegalArgumentException("Resource must be URI Resource (not a blank node)");

	this.resource = resource;
	this.cacheControl = cacheControl;
	this.endpoint = endpoint;
	
	if (log.isDebugEnabled())
	{
	    log.debug("Creating LinkedDataResource from Resource with URI: {}", resource.getURI());
	    //log.debug("List of Variants: {}", variants);
	}
    }
    
    @GET
    @Override
    public Response getResponse()
    {
	Model model = getEndpoint().loadModel(getQuery());

	if (model.isEmpty())
	{
	    if (log.isTraceEnabled()) log.trace("DESCRIBE Model is empty; returning 404 Not Found");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
	if (log.isDebugEnabled()) log.debug("Returning @GET Response with {} statements in Model", model.size());
	return getResponseBuilder(model).build();

    }
    
    public ResponseBuilder getResponseBuilder(Model model)
    {
	return getEndpoint().getResponseBuilder(model).
		cacheControl(getCacheControl());
    }
    
    public Query getQuery()
    {
	return getQuery(getURI());
    }
    
    public Query getQuery(String uri)
    {
	return QueryFactory.create("DESCRIBE <" + uri + ">");
    }

    @Override
    public final String getURI()
    {
	return getResource().getURI();
    }
    
    public final Resource getResource()
    {
	return resource;
    }

    public SPARQLEndpointBase getEndpoint()
    {
	return endpoint;
    }

    @Override
    public final Model getModel()
    {
	return getResource().getModel();
    }
    
    public final UriInfo getUriInfo()
    {
	return uriInfo;
    }

    public final CacheControl getCacheControl()
    {
	return cacheControl;
    }

    public ResourceConfig getResourceConfig()
    {
	return resourceConfig;
    }

    public ResourceContext getResourceContext()
    {
	return resourceContext;
    }
    
    @Override
    public AnonId getId()
    {
	return getResource().getId();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource inModel(Model model)
    {
	return getResource().inModel(model);
    }

    @Override
    public boolean hasURI(String string)
    {
	return getResource().hasURI(string);
    }

    @Override
    public String getNameSpace()
    {
	return getResource().getNameSpace();
    }

    @Override
    public String getLocalName()
    {
	return getResource().getLocalName();
    }

    @Override
    public Statement getRequiredProperty(Property prprt)
    {
	return getResource().getRequiredProperty(prprt);
    }

    @Override
    public Statement getProperty(Property prprt)
    {
	return getResource().getProperty(prprt);
    }

    @Override
    public StmtIterator listProperties(Property prprt)
    {
	return getResource().listProperties(prprt);
    }

    @Override
    public StmtIterator listProperties()
    {
	return getResource().listProperties();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, boolean bln)
    {
	return getResource().addLiteral(prprt, bln);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, long l)
    {
	return getResource().addLiteral(prprt, l);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, char c)
    {
	return getResource().addLiteral(prprt, c);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, double d)
    {
	return getResource().addLiteral(prprt, d);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, float f)
    {
	return getResource().addLiteral(prprt, f);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, Object o)
    {
	return getResource().addLiteral(prprt, o);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, Literal ltrl)
    {
	return getResource().addLiteral(prprt, ltrl);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, String string)
    {
	return getResource().addLiteral(prprt, string);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, String string, String string1)
    {
	return getResource().addProperty(prprt, string, string1);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, String string, RDFDatatype rdfd)
    {
	return getResource().addProperty(prprt, prprt);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, RDFNode rdfn)
    {
	return getResource().addProperty(prprt, rdfn);
    }

    @Override
    public boolean hasProperty(Property prprt)
    {
	return getResource().hasProperty(prprt);
    }

    @Override
    public boolean hasLiteral(Property prprt, boolean bln)
    {
	return getResource().hasLiteral(prprt, bln);
    }

    @Override
    public boolean hasLiteral(Property prprt, long l)
    {
	return getResource().hasLiteral(prprt, l);
    }

    @Override
    public boolean hasLiteral(Property prprt, char c)
    {
	return getResource().hasLiteral(prprt, c);
    }

    @Override
    public boolean hasLiteral(Property prprt, double d)
    {
	return getResource().hasLiteral(prprt, d);
    }

    @Override
    public boolean hasLiteral(Property prprt, float f)
    {
	return getResource().hasLiteral(prprt, f);
    }

    @Override
    public boolean hasLiteral(Property prprt, Object o)
    {
	return getResource().hasLiteral(prprt, o);
    }

    @Override
    public boolean hasProperty(Property prprt, String string)
    {
	return getResource().hasProperty(prprt, string);
    }

    @Override
    public boolean hasProperty(Property prprt, String string, String string1)
    {
	return getResource().hasProperty(prprt, string, string1);
    }

    @Override
    public boolean hasProperty(Property prprt, RDFNode rdfn)
    {
	return getResource().hasProperty(prprt, rdfn);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource removeProperties()
    {
	return getResource().removeProperties();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource removeAll(Property prprt)
    {
	return getResource().removeAll(prprt);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource begin()
    {
	return getResource().begin();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource abort()
    {
	return getResource().abort();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource commit()
    {
	return getResource().commit();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource getPropertyResourceValue(Property prprt)
    {
	return getResource().getPropertyResourceValue(prprt);
    }

    @Override
    public boolean isAnon()
    {
	return getResource().isAnon();
    }

    @Override
    public boolean isLiteral()
    {
	return getResource().isLiteral();
    }

    @Override
    public boolean isURIResource()
    {
	return getResource().isURIResource();
    }

    @Override
    public boolean isResource()
    {
	return getResource().isResource();
    }

    @Override
    public <T extends RDFNode> T as(Class<T> type)
    {
	return getResource().as(type);
    }

    @Override
    public <T extends RDFNode> boolean canAs(Class<T> type)
    {
	return getResource().canAs(type);
    }

    @Override
    public Object visitWith(RDFVisitor rdfv)
    {
	return getResource().visitWith(rdfv);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource asResource()
    {
	return getResource().asResource();
    }

    @Override
    public Literal asLiteral()
    {
	return getResource().asLiteral();
    }

    @Override
    public Node asNode()
    {
	return getResource().asNode();
    }

    @Override
    public boolean equals(Object obj)
    {
	return getResource().equals(obj);
    }

    @Override
    public int hashCode()
    {
	return getResource().hashCode();
    }
    
}