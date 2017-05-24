/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
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

package com.atomgraph.core.model.impl.proxy;

import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import com.atomgraph.core.MediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.client.SPARQLClient;

/**
 * Proxy implementation of SPARQL endpoint.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
// @Path("/sparql")
public class SPARQLEndpointBase extends com.atomgraph.core.model.impl.SPARQLEndpointBase
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);

    private final SPARQLClient sparqlClient;

    /**
     * Constructs SPARQL endpoint proxy from request metadata and origin.
     * 
     * @param request request
     * @param mediaTypes supported media types
     * @param sparqlClient SPARQL client
     */
    public SPARQLEndpointBase(@Context Request request, @Context MediaTypes mediaTypes, @Context SPARQLClient sparqlClient)
    {
        super(request, mediaTypes);
        if (sparqlClient == null) throw new IllegalArgumentException("SPARQLClient cannot be null");
        this.sparqlClient = sparqlClient;        
    }
        
    public SPARQLClient getSPARQLClient()
    {
        return sparqlClient;
    }
    
    @Override
    public Model loadModel(Query query)
    {
	return getSPARQLClient().loadModel(query);
    }

    @Override
    public ResultSetRewindable select(Query query)
    {
        return getSPARQLClient().select(query);
    }

    /**
     * Returns boolean result from a remote SPARQL endpoint using a query and optional request parameters.
     * Only <code>ASK</code> queries can be used with this method.
     * 
     * @param query query object
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */    
    @Override
    public boolean ask(Query query)
    {
        return getSPARQLClient().ask(query);
    }
    
    @Override
    public void update(UpdateRequest updateRequest)
    {
        getSPARQLClient().update(updateRequest);
    }
    
}