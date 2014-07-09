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

import javax.servlet.ServletContext;
import javax.ws.rs.core.Request;
import org.graphity.server.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory class for creating SPARQL endpoints.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class SPARQLEndpointFactory
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointFactory.class);

    /**
     * Creates new SPARQL endpoint from application configuration and request metadata.
     * 
     * @param uriInfo URI information of the request
     * @param request request
     * @param servletContext webapp context
     * @param dataManager RDF data manager for this endpoint
     * @return a new endpoint
     */
    /*
    public static SPARQLEndpointProxy createProxy(UriInfo uriInfo, Request request, ServletContext servletContext,
            DataManager dataManager)
    {
	return new SPARQLEndpointProxyBase(uriInfo, request, servletContext, dataManager);
    }
    */
    
    /**
     * Creates new SPARQL endpoint from explicit URI resource and request metadata.
     * 
     * @param endpoint endpoint resource
     * @param request request
     * @param servletContext webapp context
     * @param dataManager RDF data manager for this endpoint
     * @return a new endpoint
     */
    public static SPARQLEndpointProxy createProxy(Request request, ServletContext servletContext,
        DataManager dataManager)
    {
	return new SPARQLEndpointProxyBase(request, servletContext, dataManager);
    }

}