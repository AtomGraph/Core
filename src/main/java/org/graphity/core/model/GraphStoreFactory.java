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
import org.graphity.core.model.impl.GraphStoreProxyBase;
import javax.ws.rs.core.Request;
import org.graphity.core.MediaTypes;
import org.graphity.core.util.DataManager;

/**
 * A factory class for creating new SPARQL Graph Store proxies.
 * Provides static convenience methods.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class GraphStoreFactory
{
    
    /**
     * Creates new GraphStore from request metadata.
     * 
     * @param request current request
     * @param servletConfig servlet config
     * @param origin remote graph store origin
     * @param dataManager RDF data manager for this graph store
     * @param mediaTypes
     * @return graph store instance
     */
    public static GraphStore createProxy(Request request, ServletConfig servletConfig, MediaTypes mediaTypes, GraphStoreOrigin origin, DataManager dataManager)
    {
	return new GraphStoreProxyBase(request, servletConfig, mediaTypes, origin, dataManager);
    }

}
