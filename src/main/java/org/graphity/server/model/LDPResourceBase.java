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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.core.ResourceContext;
import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.graphity.server.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-write Linked Data resources.
 * RDF representations are queried from, and stored into, SPARQL endpoints.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://www.w3.org/TR/2013/WD-ldp-20130307/">Linked Data Platform 1.0</a>
 */
@Path("{path: .*}")
public class LDPResourceBase extends QueriedResourceBase implements LDPResource
{    
    private static final Logger log = LoggerFactory.getLogger(LDPResourceBase.class);

    /**
     * JAX-RS-compatible resource constructor with injected initialization objects.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the request
     * @param resourceConfig webapp configuration
     * @param resourceContext resource context
     */
    public LDPResourceBase(@Context UriInfo uriInfo, @Context ResourceConfig resourceConfig, @Context ResourceContext resourceContext)
    {
	super(uriInfo, resourceConfig, resourceContext);
    }

    /**
     * Protected constructor. Not suitable for JAX-RS but can be used when subclassing.
     * 
     * @param resource this resource as RDF resource
     * @param endpoint SPARQL endpoint of this resource
     * @param cacheControl cache control config
     */
    protected LDPResourceBase(Resource resource, SPARQLEndpoint endpoint, CacheControl cacheControl)
    {
	super(resource, endpoint, cacheControl);
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
	throw new WebApplicationException(405);
	
	//getOntResource().getOntModel().add(model);
	
	//return Response.created(null).build();
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
	// curl -X PUT -H "Content-Type: text/turtle" -d @vilnius-jug.ttl http://localhost:8080/
	if (log.isDebugEnabled()) log.debug("PUT request with RDF payload: {} payload size(): {}", model, model.size());
	com.hp.hpl.jena.sparql.util.Context queryContext = DataManager.get().getServiceContext(getEndpoint());
	String endpointURI = getEndpoint().getURI().replace("/sparql", "/service");
	DataManager.get().addServiceContext(endpointURI, queryContext);
	
	//if (DataManager.get().)
	DataManager.get().putModel(endpointURI, model);
	
	return Response.ok().build();
    }

    @Override
    public Response put(Model model, @QueryParam("graph") URI graphUri)
    {
	com.hp.hpl.jena.sparql.util.Context queryContext = DataManager.get().getServiceContext(getEndpoint());
	String endpointURI = getEndpoint().getURI().replace("/sparql", "/service");
	DataManager.get().addServiceContext(endpointURI, queryContext);

	DataManager.get().putModel(endpointURI, graphUri.toString(), model);
	
	return Response.ok().build();
    }

    /**
     * Handles DELETE method, deletes the RDF representation of this resource frrom the SPARQL endpoint, and
     * returns response.
     * 
     * @return response
     */
    @Override
    public Response delete()
    {
	throw new WebApplicationException(405);

	// if (getService() != null) DataManager.get().deleteModel(endpointUri, getUriInfo().getAbsolutePath()
	
	//getOntResource().remove();
	
	//return Response.noContent().build(); // 204 No Content
	// 410 Gone if provenance shows previous versions: http://www.w3.org/TR/chips/#cp4.2
    }

}