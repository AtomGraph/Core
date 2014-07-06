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

package org.graphity.server.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.update.UpdateRequest;
import javax.naming.ConfigurationException;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.graphity.server.util.DataManager;
import org.graphity.server.vocabulary.GS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas
 */
@Path("/sparql")
public class SPARQLEndpointProxyBase extends SPARQLEndpointBase implements SPARQLEndpointProxy
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProxyBase.class);

    private final DataManager dataManager;

    public SPARQLEndpointProxyBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletContext servletContext, @Context DataManager dataManager)
    {
        super(uriInfo, request, servletContext);
	if (dataManager == null) throw new IllegalArgumentException("DataManager cannot be null");
        this.dataManager = dataManager;
    }

    protected SPARQLEndpointProxyBase(Resource endpoint, Request request, ServletContext servletContext, DataManager dataManager)
    {
        super(endpoint, request, servletContext);
	if (dataManager == null) throw new IllegalArgumentException("DataManager cannot be null");
        this.dataManager = dataManager;
    }
    
    /**
     * Returns configured SPARQL endpoint resource.
     * This endpoint is a proxy for the remote endpoint.
     * 
     * @return endpoint resource
     */
    @Override
    public Resource getOrigin()
    {
        return getOrigin(getServletContext());
    }

    /**
     * Returns SPARQL endpoint resource for supplied webapp context configuration.
     * Uses <code>gs:endpoint</code> context parameter value as endpoint URI.
     * 
     * @param servletContext context config
     * @return endpoint resource
     */
    public Resource getOrigin(ServletContext servletContext)
    {
        if (servletContext == null) throw new IllegalArgumentException("ServletContext cannot be null");

        try
        {
            Object endpointUri = servletContext.getInitParameter(GS.endpoint.getURI());
            if (endpointUri == null) throw new ConfigurationException("SPARQL endpoint not configured (gs:endpoint not set in web.xml)");

            String authUser = (String)servletContext.getInitParameter(Service.queryAuthUser.getSymbol());
            String authPwd = (String)servletContext.getInitParameter(Service.queryAuthPwd.getSymbol());
            if (authUser != null && authPwd != null)
                getDataManager().putAuthContext(endpointUri.toString(), authUser, authPwd);

            return ResourceFactory.createResource(endpointUri.toString());
        }
        catch (ConfigurationException ex)
        {
            if (log.isErrorEnabled()) log.warn("SPARQL endpoint configuration error", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);            
        }
    }

    @Override
    public Model loadModel(Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading Model from SPARQL endpoint: {} using Query: {}", getOrigin(), query);
	return getDataManager().loadModel(getOrigin().getURI(), query);
    }

    @Override
    public ResultSetRewindable select(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isSelectType()) throw new IllegalArgumentException("Query must be SELECT");
        
	if (log.isDebugEnabled()) log.debug("Loading ResultSet from SPARQL endpoint: {} using Query: {}", getOrigin().getURI(), query);
	return getDataManager().loadResultSet(getOrigin().getURI(), query); // .getResultSetRewindable()
    }

    @Override
    public boolean ask(Query query)
    {
	if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isAskType()) throw new IllegalArgumentException("Query must be ASK");
        
	return getDataManager().ask(getOrigin().getURI(), query);
    }

    @Override
    public void update(UpdateRequest updateRequest)
    {
	if (log.isDebugEnabled()) log.debug("Executing update on SPARQL endpoint: {} using UpdateRequest: {}", getOrigin(), updateRequest);
	getDataManager().executeUpdateRequest(getOrigin().getURI(), updateRequest);
    }

    public DataManager getDataManager()
    {
        return dataManager;
    }

}
