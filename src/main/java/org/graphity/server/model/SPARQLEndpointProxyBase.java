/**
 *  Copyright 2014 Martynas Jusevičius <martynas@graphity.org>
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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateRequest;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Request;
import org.graphity.server.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas
 */
//@Path("/sparql")
public class SPARQLEndpointProxyBase extends SPARQLEndpointBase
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProxyBase.class);

    private final Origin origin;
    private final DataManager dataManager;

    /*
    public SPARQLEndpointProxyBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletContext servletContext, @Context DataManager dataManager)
    {
        this(ResourceFactory.createResource(uriInfo.getBaseUriBuilder().
                path(SPARQLEndpointProxyBase.class).
                build().
                toString()), request, servletContext, dataManager);
    }
    */
    
    protected SPARQLEndpointProxyBase(Request request, ServletContext servletContext, SPARQLEndpointOrigin origin, DataManager dataManager)
    {
        super(request, servletContext);
	if (origin == null) throw new IllegalArgumentException("Origin cannot be null");
	if (dataManager == null) throw new IllegalArgumentException("DataManager cannot be null");
        this.origin = origin;
        this.dataManager = dataManager;
    }
    
    /**
     * Returns configured SPARQL endpoint resource.
     * This endpoint is a proxy for the remote endpoint.
     * 
     * @return endpoint resource
     */
    public Origin getOrigin()
    {
        //return getOrigin(getServletContext());
        return origin;
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
