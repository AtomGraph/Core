/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.model.impl;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import java.net.URI;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.SPARQLClient;
import com.atomgraph.core.exception.NotFoundException;
import com.atomgraph.core.model.Application;
import com.atomgraph.core.model.QueriedResource;
import com.sun.jersey.api.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-only Linked Data resources with RDF representations queried from SPARQL endpoints.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.atomgraph.core.model.SPARQLEndpoint
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
 */
@Path("{path: .*}")
public class QueriedResourceBase extends ResourceBase implements QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(QueriedResourceBase.class);
    
    private final Client client;
    private final Application application;
    private final SPARQLClient sparqlClient;

    /**
     * JAX-RS-compatible resource constructor with injected initialization objects.
     * The URI of the resource being created is the absolute path of the current request URI.
     * 
     * @param uriInfo URI information of the request
     * @param request current request object
     * @param servletConfig webapp context
     * @param application LDT application
     * @param client
     * @param mediaTypes supported media types
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/UriInfo.html">JAX-RS UriInfo</a>
     * @see <a href="http://docs.oracle.com/javaee/7/api/javax/servlet/ServletContext.html">ServletContext</a>
     * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/core/ResourceContext.html">Jersey ResourceContext</a>
     */
    public QueriedResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context Client client, @Context Application application)
    {
	super(uriInfo, request, servletConfig, mediaTypes);
	if (application == null) throw new IllegalArgumentException("Application cannot be null");
        this.client = client;
	this.application = application;
        this.sparqlClient = SPARQLClient.create(application.getService().getSPARQLEndpointOrigin(client));
    }
    
    /**
     * Returns RDF description of this resource.
     * The description is the result of a query executed on the SPARQL endpoint of this resource.
     * By default, the query is <code>DESCRIBE</code> with URI of this resource.
     * 
     * @return RDF description
     * @see getQuery()
     */
    @Override
    public Model describe()
    {
        return getSPARQLClient().loadModel(getQuery());
    }
    
    /**
     * Handles GET request and returns response with RDF description of this resource.
     * 
     * @return response with RDF description
     */
    @GET
    @Override
    public Response get()
    {
	Model description = describe();
        
	if (description.isEmpty())
	{
	    if (log.isDebugEnabled()) log.debug("Query result Model is empty; returning 404 Not Found");
	    throw new NotFoundException("Query result Model is empty");
	}
        
	if (log.isDebugEnabled()) log.debug("Returning @GET Response with {} statements in Model", description.size());
	return getResponse(description);
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
	if (log.isWarnEnabled()) log.warn("POST request with RDF payload: {}. AtomGraph Core is read-only!  Only GET is supported", model);
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
	if (log.isWarnEnabled()) log.warn("PUT request with RDF payload: {}. AtomGraph Core is read-only! Only GET is supported", model);
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
	if (log.isWarnEnabled()) log.warn("DELETE request with RDF payload: {}. AtomGraph Core is read-only! Only GET is supported");
	throw new WebApplicationException(405);
    }

    /**
     * Returns query used to retrieve RDF description of this resource
     * 
     * @return query object
     */
    @Override
    public Query getQuery()
    {
	return getQuery(getURI());
    }
    
    /**
     * Given a resource URI, returns query that can be used to retrieve its RDF description
     * 
     * @param uri resource URI
     * @return query object
     */
    public Query getQuery(URI uri)
    {
	if (uri == null) throw new IllegalArgumentException("URI cannot be null");        
	return QueryFactory.create("DESCRIBE <" + uri.toString() + ">");
    }

    public Client getClient()
    {
        return client;
    }
    
    public SPARQLClient getSPARQLClient()
    {
        return sparqlClient;
    }
    
    public Application getApplication()
    {
	return application;
    }

}