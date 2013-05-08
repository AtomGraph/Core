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

import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.core.ResourceConfig;
import javax.ws.rs.core.Request;

/**
 * A Factory class for creating new SPARQL Graph Store proxies.
 * Provides static convenience methods.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class GraphStoreFactory
{
    /**
     * Creates new GraphStore from application configuration.
     * 
     * @param request current request
     * @param resourceConfig webapp configuration
     * @return graph store instance
     */
    public static GraphStore createGraphStore(Request request, ResourceConfig resourceConfig)
    {
	return new GraphStoreBase(request, resourceConfig);
    }
    
    /**
     * Creates new GraphStore from explicit URI resource.
     * 
     * @param graphStore remote graph store resource
     * @param request current request
     * @return graph store instance
     */
    public static GraphStore createGraphStore(Resource graphStore, Request request)
    {
	return new GraphStoreBase(graphStore, request);
    }

}
