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
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.sun.jersey.api.core.ResourceConfig;
import java.net.URI;
import javax.naming.ConfigurationException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import org.graphity.server.util.DataManager;
import org.graphity.server.vocabulary.GS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of SPARQL Graph Store.
 * This class does not natively manage the RDF store. It forwards the requests to a remote Graph Store service.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/service") // not standard
public class GraphStoreBase implements GraphStore
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreBase.class);

    private final Resource resource;
    private final Request request;
    private final ResourceConfig resourceConfig;

    public GraphStoreBase(@Context UriInfo uriInfo, @Context Request request, @Context ResourceConfig resourceConfig)
    {
	this(ResourceFactory.createResource(uriInfo.getBaseUriBuilder().
                path(GraphStoreBase.class).
                build().
                toString()),
	    request, resourceConfig);
    }

    protected GraphStoreBase(Resource graphStore, Request request, ResourceConfig resourceConfig)
    {
	if (graphStore == null) throw new IllegalArgumentException("Graph store Resource cannot be null");
	if (!graphStore.isURIResource()) throw new IllegalArgumentException("Graph store Resource must be URI Resource (not a blank node)");
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (resourceConfig == null) throw new IllegalArgumentException("ResourceConfig cannot be null");
	
	this.resource = graphStore;
	this.request = request;
        this.resourceConfig = resourceConfig;
    }

    /**
     * Returns response for a given RDF model.
     * 
     * @param model RDF model
     * @return response object
     */
    public Response getResponse(Model model)
    {
        return ModelResponse.fromRequest(getRequest()).
                getResponseBuilder(model).
                //cacheControl(getCacheControl()).
                build();
    }
    
     /**
     * Returns configured Graph Store resource.
     * 
     * @return graph store resource
     */
    public Resource getRemoteStore()
    {
        try
        {
            Resource graphStore = getRemoteStore(getResourceConfig());
            if (graphStore == null) throw new ConfigurationException("Graph Store not configured (gs:graphStore not set in web.xml)");
            return graphStore;
        }
        catch (ConfigurationException ex)
        {
            if (log.isErrorEnabled()) log.warn("Graph Store configuration error", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);            
        }
    }

     /**
     * Returns Graph Store for supplied webapp configuration.
     * Uses <code>gs:graphStore</code> parameter value from web.xml as graph store URI.
     * 
     * @param resourceConfig webapp config
     * @return graph store resource
     */
    public Resource getRemoteStore(ResourceConfig resourceConfig)
    {
        Object storeUri = resourceConfig.getProperty(GS.graphStore.getURI());
        if (storeUri != null)
        {
            String authUser = (String)resourceConfig.getProperty(Service.queryAuthUser.getSymbol());
            String authPwd = (String)resourceConfig.getProperty(Service.queryAuthPwd.getSymbol());
            if (authUser != null && authPwd != null)
                DataManager.get().putAuthContext(storeUri.toString(), authUser, authPwd);

            return ResourceFactory.createResource(storeUri.toString());
        }
        
        return null;
    }
    
    @GET
    @Override
    public Response get(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);

	if (defaultGraph)
	{
	    Model model = DataManager.get().getModel(getRemoteStore().getURI());
	    if (log.isDebugEnabled()) log.debug("GET Graph Store default graph, returning Model of size(): {}", model.size());
	    return getResponse(model);
	}
	else
	{
	    Model model = DataManager.get().getModel(getRemoteStore().getURI(), graphUri.toString());
	    if (model == null)
	    {
		if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} not found", graphUri);
		return Response.status(Status.NOT_FOUND).build();
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} found, returning Model of size(): {}", graphUri, model.size());
		return getResponse(model);
	    }
	}
    }

    @POST
    @Override
    public Response post(Model model, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);
	if (log.isDebugEnabled()) log.debug("POST Graph Store request with RDF payload: {} payload size(): {}", model, model.size());
	
	if (model.isEmpty()) return Response.noContent().build();
	
	if (defaultGraph)
	{
	    if (log.isDebugEnabled()) log.debug("POST Model to default graph");
	    DataManager.get().addModel(getRemoteStore().getURI(), model);
	    return Response.ok().build();
	}
	else
	{
	    boolean existingGraph = DataManager.get().containsModel(getRemoteStore().getURI(), graphUri.toString());

	    // is this implemented correctly? The specification is not very clear.
	    if (log.isDebugEnabled()) log.debug("POST Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
	    DataManager.get().addModel(getRemoteStore().getURI(), graphUri.toString(), model);
	    
	    if (existingGraph) return Response.ok().build();
	    else return Response.created(graphUri).build();
	}
    }

    @PUT
    @Override
    public Response put(Model model, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);
	if (log.isDebugEnabled()) log.debug("PUT Graph Store request with RDF payload: {} payload size(): {}", model, model.size());
	
	if (defaultGraph)
	{
	    if (log.isDebugEnabled()) log.debug("PUT Model to default graph");
	    DataManager.get().putModel(getRemoteStore().getURI(), model);
	    return Response.ok().build();
	}
	else
	{
	    boolean existingGraph = DataManager.get().containsModel(getRemoteStore().getURI(), graphUri.toString());
	    
	    if (log.isDebugEnabled()) log.debug("PUT Model to named graph with URI: {} Did it already exist? {}", graphUri, existingGraph);
	    DataManager.get().putModel(getRemoteStore().getURI(), graphUri.toString(), model);
	    
	    if (existingGraph) return Response.ok().build();
	    else return Response.created(graphUri).build();
	}	
    }

    @DELETE
    @Override
    public Response delete(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
	if (!defaultGraph && graphUri == null) throw new WebApplicationException(Status.BAD_REQUEST);
	
	if (defaultGraph)
	{
	    DataManager.get().deleteDefault(getRemoteStore().getURI());
	    if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store");	    
	    return Response.noContent().build();
	}
	else
	{
	    if (!DataManager.get().containsModel(getRemoteStore().getURI(), graphUri.toString()))
	    {
		if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {}: not found", graphUri);
		return Response.status(Status.NOT_FOUND).build();
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("DELETE named graph with URI: {}", graphUri);
		DataManager.get().deleteModel(getRemoteStore().getURI(), graphUri.toString());
		return Response.noContent().build();
	    }
	}
    }

    public Resource getResource()
    {
	return resource;
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

    public Request getRequest()
    {
	return request;
    }

    public ResourceConfig getResourceConfig()
    {
        return resourceConfig;
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