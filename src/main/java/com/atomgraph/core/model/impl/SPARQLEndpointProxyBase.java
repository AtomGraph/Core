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
import com.atomgraph.core.model.SPARQLEndpointOrigin;
import com.atomgraph.core.model.SPARQLEndpointProxy;
import com.atomgraph.core.vocabulary.A;
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

    private final SPARQLEndpointOrigin origin;
    private final SPARQLClient client;

    /**
     * Constructs SPARQL endpoint proxy from request metadata and origin.
     * 
     * @param request
     * @param servletConfig
     * @param origin
     * @param mediaTypes 
     */
    public SPARQLEndpointProxyBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context SPARQLEndpointOrigin origin)
    {
        super(request, servletConfig, mediaTypes);
        if (origin == null) throw new IllegalArgumentException("SPARQLEndpointOrigin cannot be null");
        this.origin = origin;
        
        Integer maxGetRequestSize = getMaxGetRequestSize(servletConfig, A.maxGetRequestSize);
        if (maxGetRequestSize != null) client = SPARQLClient.create(origin.getWebResource(), mediaTypes, maxGetRequestSize);
        else client = SPARQLClient.create(origin.getWebResource(), mediaTypes);
    }
    
    @Override
    public SPARQLEndpointOrigin getOrigin()
    {
        return origin;
    }
    
    @Override
    public SPARQLClient getClient()
    {
        return client;
    }
        
    @Override
    public Model loadModel(Query query)
    {
	return getClient().loadModel(query);
    }

    @Override
    public ResultSetRewindable select(Query query)
    {
        return getClient().select(query);
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
        return getClient().ask(query);
    }
    
    @Override
    public void update(UpdateRequest updateRequest)
    {
        getClient().update(updateRequest);
    }
    
    public final Integer getMaxGetRequestSize(ServletConfig servletConfig, DatatypeProperty property)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object sizeValue = servletConfig.getInitParameter(property.getURI());
        if (sizeValue != null) return Integer.parseInt(sizeValue.toString());

        return null;
    }
    
}