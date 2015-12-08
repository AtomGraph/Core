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
package org.graphity.core.model;

import javax.servlet.ServletConfig;
import org.graphity.core.model.impl.SPARQLEndpointProxyBase;
import javax.ws.rs.core.Request;
import org.graphity.core.MediaTypes;
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
     * Creates new SPARQL endpoint from request metadata.
     * 
     * @param request request
     * @param servletConfig servlet config
     * @param origin proxy origin
     * @param mediaTypes
     * @return a new endpoint
     */
    public static SPARQLEndpoint createProxy(Request request, ServletConfig servletConfig, MediaTypes mediaTypes, SPARQLEndpointOrigin origin)
    {
	return new SPARQLEndpointProxyBase(request, servletConfig, mediaTypes, origin);
    }

}