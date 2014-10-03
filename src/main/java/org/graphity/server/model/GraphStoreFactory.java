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

import org.graphity.server.model.impl.GraphStoreProxyBase;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Request;
import org.graphity.server.util.DataManager;

/**
 * A factory class for creating new SPARQL Graph Store proxies.
 * Provides static convenience methods.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class GraphStoreFactory
{
    
    /**
     * Creates new GraphStore from explicit URI resource.
     * 
     * @param request current request
     * @param servletContext servlet context
     * @param origin remote graph store origin
     * @param dataManager RDF data manager for this graph store
     * @return graph store instance
     */
    public static GraphStore createProxy(Request request, ServletContext servletContext, GraphStoreOrigin origin, DataManager dataManager)
    {
	return new GraphStoreProxyBase(request, servletContext, origin, dataManager);
    }

}
