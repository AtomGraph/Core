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

package com.atomgraph.core.model;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.impl.QueriedResourceBase;

/**
 * A factory class for creating Linked Data resources.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class ResourceFactory
{

    /**
     * Creates new Linked Data resource backed by a SPARQL endpoint.
     * 
     * @param uriInfo URI information of the current request
     * @param request current request object
     * @param servletConfig servlet config
     * @param mediaTypes supported media types
     * @param endpoint SPARQL endpoint backing the application
     * @return a new Linked Data resource
     */
    public static Resource create(UriInfo uriInfo, Request request, ServletConfig servletConfig, MediaTypes mediaTypes,
            SPARQLEndpoint endpoint)
    {
	return new QueriedResourceBase(uriInfo, request, servletConfig, mediaTypes, endpoint);
    }

}