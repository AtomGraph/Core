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
import javax.ws.rs.core.CacheControl;

/**
 * A factory class for creating Linked Data resources.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LinkedDataResourceFactory
{
    /**
     * Creates new SPARQL endpoint from explicit URI resource.
     * 
     * @param resource RDF resource
     * @return a new resource
     */
    public static LinkedDataResource createResource(Resource resource)
    {
	return new LinkedDataResourceBase(resource, null);
    }

    /**
     * Creates new SPARQL endpoint from explicit URI resource.
     * 
     * @param resource RDF resource
     * @param cacheControl cache control (null equals none)
     * @return a new resource
     */
    public static LinkedDataResource createResource(Resource resource, CacheControl cacheControl)
    {
	return new LinkedDataResourceBase(resource, cacheControl);
    }

}
