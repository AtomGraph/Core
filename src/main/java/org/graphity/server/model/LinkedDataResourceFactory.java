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

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.graphity.server.model.impl.QueriedResourceBase;

/**
 * A factory class for creating Linked Data resources.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LinkedDataResourceFactory
{

    /**
     * Creates new Linked Data resource backed by a SPARQL endpoint.
     * 
     * @param uriInfo URI information of the current request
     * @param request current request object
     * @param servletConfig webapp context
     * @param endpoint SPARQL endpoint backing the application
     * @return a new Linked Data resource
     */
    public static LinkedDataResource create(UriInfo uriInfo, Request request, ServletConfig servletConfig,
            SPARQLEndpoint endpoint)
    {
	return new QueriedResourceBase(uriInfo, request, servletConfig, endpoint);
    }

}