/*
 * Copyright (C) 2014 Martynas
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

package org.graphity.server.provider;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import org.graphity.server.model.SPARQLEndpoint;
import org.graphity.server.model.SPARQLEndpointBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas
 */
public class DescriptionProvider extends PerRequestTypeInjectableProvider<Context, Model> implements ContextResolver<Model>
{
    private static final Logger log = LoggerFactory.getLogger(DescriptionProvider.class);

    @Context UriInfo uriInfo;
    @Context ResourceContext resourceContext;

    public DescriptionProvider()
    {
	super(Model.class);
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }

    public ResourceContext getResourceContext()
    {
        return resourceContext;
    }
    
    @Override
    public Injectable<Model> getInjectable(ComponentContext cc, Context context)
    {
	return new Injectable<Model>()
	{
	    @Override
	    public Model getValue()
	    {
		return getDescription();
	    }

	};
    }

    @Override
    public Model getContext(Class<?> type)
    {
        return getDescription();
    }
    
    /**
     * Returns RDF description of this resource.
     * The description is the result of a query executed on the SPARQL endpoint of this resource.
     * By default, the query is <code>DESCRIBE</code> with URI of this resource.
     * 
     * @return RDF description
     * @see getQuery()
     */
    public Model getDescription()
    {
	Model description = getEndpoint().loadModel(getQuery());
	
        if (description.isEmpty())
	{
	    if (log.isDebugEnabled()) log.debug("Description Model is empty; returning 404 Not Found");
	    throw new WebApplicationException(Response.Status.NOT_FOUND);
	}
        
        return description;
    }

    /**
     * Returns SPARQL endpoint of this resource.
     * Query is executed on this endpoint to retrieve RDF representation of this resource.
     * 
     * @return SPARQL endpoint resource
     */
    //@Override
    public SPARQLEndpoint getEndpoint()
    {
	return getResourceContext().getResource(SPARQLEndpointBase.class);
    }

    /**
     * Returns query used to retrieve RDF description of this resource
     * 
     * @return query object
     */
    //@Override
    public Query getQuery()
    {
	return getQuery(getUriInfo().getAbsolutePath().toString());
    }

    /**
     * Given a resource URI, returns query that can be used to retrieve its RDF description
     * 
     * @param uri resource URI
     * @return query object
     */
    public Query getQuery(String uri)
    {
	return QueryFactory.create("DESCRIBE <" + uri + ">");
    }

}
