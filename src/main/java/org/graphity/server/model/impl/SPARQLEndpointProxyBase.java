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

package org.graphity.server.model.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateRequest;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import org.graphity.server.model.Origin;
import org.graphity.server.model.SPARQLEndpointOrigin;
import org.graphity.server.model.SPARQLEndpointProxy;
import org.graphity.server.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/sparql")
public class SPARQLEndpointProxyBase extends SPARQLEndpointBase implements SPARQLEndpointProxy
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProxyBase.class);

    private final Origin origin;
    private final DataManager dataManager;
    
    public SPARQLEndpointProxyBase(@Context Request request, @Context ServletConfig servletConfig, @Context SPARQLEndpointOrigin origin, @Context DataManager dataManager)
    {
        super(request, servletConfig);
        if (origin == null) throw new IllegalArgumentException("SPARQLEndpointOrigin cannot be null");
        if (dataManager == null) throw new IllegalArgumentException("DataManager cannot be null");
        this.origin = origin;
        this.dataManager = dataManager;
    }
    
    @Override
    public Origin getOrigin()
    {
        return origin;
    }

    @Override
    public Model loadModel(Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading Model from SPARQL endpoint: {} using Query: {}", getOrigin().getURI(), query);
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
	if (log.isDebugEnabled()) log.debug("Executing update on SPARQL endpoint: {} using UpdateRequest: {}", getOrigin().getURI(), updateRequest);
	getDataManager().executeUpdateRequest(getOrigin().getURI(), updateRequest);
    }

    public DataManager getDataManager()
    {
        return dataManager;
    }

}
