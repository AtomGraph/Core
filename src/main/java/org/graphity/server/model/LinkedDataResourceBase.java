/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.graphity.server.model;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.*;
import com.sun.jersey.api.core.ResourceConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.graphity.server.vocabulary.GS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-only Linked Data resources.
 * 
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Resource.html">Jena Resource</a>
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("{path: .*}")
public class LinkedDataResourceBase implements LinkedDataResource
{
    private static final Logger log = LoggerFactory.getLogger(LinkedDataResourceBase.class);

    private final Resource resource;
    private final Request request;
    private final ResourceConfig resourceConfig;

    /** 
     * JAX-RS-compatible resource constructor with injected initialization objects.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the 
     * @param request current request
     * @param resourceConfig webapp configuration
     * @param description description of this resource
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/UriInfo.html#getAbsolutePath()">JAX-RS UriInfo.getAbsolutePath()</a>
     */
    public LinkedDataResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context ResourceConfig resourceConfig,
            @Context Model description)
    {
	this(description.createResource(uriInfo.getAbsolutePath().toString()), request, resourceConfig);
    }
    
    /**
     * Protected constructor. Not suitable for JAX-RS but can be used when subclassing.
     * 
     * @param resource This resource as RDF resource (must be URI resource, not a blank node)
     * @param request current request
     * @param resourceConfig Resource config of this resource
     */
    protected LinkedDataResourceBase(Resource resource, Request request, ResourceConfig resourceConfig)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (!resource.isURIResource()) throw new IllegalArgumentException("Resource must be URI Resource (not a blank node)");
	if (resourceConfig == null) throw new IllegalArgumentException("ResourceConfig cannot be null");

	this.resource = resource;
        this.request = request;
	this.resourceConfig = resourceConfig;
	
	if (log.isDebugEnabled()) log.debug("Creating LinkedDataResource from Resource with URI: {}", resource.getURI());
    }

    /**
     * Handles GET request and returns response with RDF description of this resource.
     * By default, the whole model of this resource is returned. If the description is empty, 404 Not Found is
     * returned.
     * 
     * @return response with RDF description
     */
    @GET
    @Override
    public Response get()
    {
	if (log.isDebugEnabled()) log.debug("Returning @GET Response with {} statements in Model", getModel().size());
	return getResponse(getModel());
    }

    /**
     * Handles POST method, stores the submitted RDF model in the SPARQL endpoint, and returns response.
     * 
     * @param model RDF payload
     * @return response
     */
    @Override
    public Response post(Model model)
    {
	if (log.isWarnEnabled()) log.warn("POST request with RDF payload: {}. Graphity Server is read-only!  Only GET is supported");
	throw new WebApplicationException(405);
    }

    /**
     * Handles PUT method, stores the submitted RDF model in the SPARQL endpoint, and returns response.
     * 
     * @param model RDF payload
     * @return response
     */
    @Override
    public Response put(Model model)
    {
	if (log.isWarnEnabled()) log.warn("PUT request with RDF payload: {}. Graphity Server is read-only!  Only GET is supported");
	throw new WebApplicationException(405);
    }

    /**
     * Handles DELETE method, deletes the RDF representation of this resource from the SPARQL endpoint, and
     * returns response.
     * 
     * @return response
     */
    @Override
    public Response delete()
    {
	if (log.isWarnEnabled()) log.warn("DELETE request with RDF payload: {}. Graphity Server is read-only! Only GET is supported");
	throw new WebApplicationException(405);
    }

    /**
     * Returns response for the given RDF model.
     * 
     * @param model RDF model
     * @return response object
     */
    public Response getResponse(Model model)
    {
        return getResponseBuilder(model).build();
    }

    /**
     * Returns response builder for the given RDF model.
     * 
     * @param model RDF model
     * @return response builder
     */
    public ResponseBuilder getResponseBuilder(Model model)
    {
        return ModelResponse.fromRequest(getRequest()).
                getResponseBuilder(model, getVariants()).
                cacheControl(getCacheControl());
    }

    /**
     * Builds a list of acceptable response variants/
     * 
     * @return supported variants
     */
    public List<Variant> getVariants()
    {
        List<Variant> variants = new ArrayList<>();
        Iterator<Lang> it = RDFLanguages.getRegisteredLanguages().iterator();
        
        // RDF/XML as the default one - the first one gets selected by selectVariant()
        variants.add(new Variant(new MediaType(Lang.RDFXML.getContentType().getType(),
                Lang.RDFXML.getContentType().getSubType()),
            null, null));

        while (it.hasNext())
        {
            Lang lang = it.next();
            if (!lang.equals(Lang.RDFNULL) && !lang.equals(Lang.RDFXML))
            {
                ContentType ct = lang.getContentType();
                //List<String> altTypes = lang.getAltContentTypes();
                MediaType mediaType = new MediaType(ct.getType(), ct.getSubType()); // MediaType.valueOf(ct.getContentType()
                variants.add(new Variant(mediaType, null, null));
            }
        }
        
        return variants;
    }

    /**
     * Returns URI of this resource
     * 
     * @return URI of this resource
     */
    @Override
    public final String getURI()
    {
	return getResource().getURI();
    }
    
    /**
     * Returns this resource as Jena RDF resource
     * 
     * @return RDF resource
     */
    private Resource getResource()
    {
	return resource;
    }

    /**
     * Returns RDF model of this resource
     * 
     * @return RDF model of this resource
     */
    @Override
    public final Model getModel()
    {
	return getResource().getModel();
    }

    /**
     * Returns current request.
     * 
     * @return request object
     */
    public Request getRequest()
    {
	return request;
    }

    /**
     * Returns configuration for this web application (including parameters specified in web.xml).
     * 
     * @return webapp configuration
     */
    public ResourceConfig getResourceConfig()
    {
	return resourceConfig;
    }

    /**
     * Returns <code>Cache-Control</code> header configuration for this resource
     * 
     * @return cache control of this resource
     */
    public CacheControl getCacheControl()
    {
	if (getResourceConfig().getProperty(GS.cacheControl.getURI()) == null) return null;
        
        return CacheControl.valueOf(getResourceConfig().getProperty(GS.cacheControl.getURI()).toString());
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
    public String toString()
    {
	return getResource().toString();
    }

}