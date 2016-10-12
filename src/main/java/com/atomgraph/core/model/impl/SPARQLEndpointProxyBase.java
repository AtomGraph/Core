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

package com.atomgraph.core.model.impl;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.SPARQLClient;
import com.atomgraph.core.model.Application;
import com.atomgraph.core.model.SPARQLEndpointProxy;
import com.atomgraph.core.vocabulary.A;
import com.sun.jersey.api.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of SPARQL endpoint.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Path("/sparql")
public class SPARQLEndpointProxyBase extends SPARQLEndpointBase implements SPARQLEndpointProxy
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProxyBase.class);

    //private final Application application;
    private final SPARQLClient sparqlClient;

    /**
     * Constructs SPARQL endpoint proxy from request metadata and origin.
     * 
     * @param request request
     * @param servletConfig servlet config
     * @param mediaTypes supported media types
     * @param client HTTP client
     * @param application LDT application
     */
    public SPARQLEndpointProxyBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context SPARQLClient sparqlClient)
    {
        super(request, servletConfig, mediaTypes);
        if (sparqlClient == null) throw new IllegalArgumentException("Application cannot be null");
        this.sparqlClient = sparqlClient;        
    }
        
    @Override
    public SPARQLClient getSPARQLClient()
    {
        return sparqlClient;
    }
    
    /*
    public Application getApplication()
    {
        return application;
    }
    */
    
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