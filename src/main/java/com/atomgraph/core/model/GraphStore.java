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

import org.apache.jena.rdf.model.Model;
import java.net.URI;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

/**
 * Generic SPARQL 1.1 Graph Store HTTP Protocol interface
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public interface GraphStore
{
    /**
     * Handles GET query request and returns result as response
     * 
     * @param defaultGraph indicates whether default graph is used
     * @param graphUri named graph URI
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-get">5.2 HTTP GET</a>
     */
    @GET Response get(@QueryParam("default") Boolean defaultGraph, @QueryParam("graph") URI graphUri);
    
    /**
     * Handles POST query request and returns result as response
     * 
     * @param model RDF payload model
     * @param defaultGraph indicates whether default graph is used
     * @param graphUri named graph URI
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-post">5.5 HTTP POST</a>
     */
    @POST Response post(Model model, @QueryParam("default") Boolean defaultGraph, @QueryParam("graph") URI graphUri);
    
    /**
     * Handles PUT query request and returns result as response
     * 
     * @param model RDF payload model
     * @param defaultGraph indicates whether default graph is used
     * @param graphUri named graph URI
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-put">5.3 HTTP PUT</a>
     */
    @PUT Response put(Model model, @QueryParam("default") Boolean defaultGraph, @QueryParam("graph") URI graphUri);
    
    /**
     * Handles DELETE query request and returns result as response
     * 
     * @param defaultGraph indicates whether default graph is used
     * @param graphUri named graph URI
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-delete">5.4 HTTP DELETE</a>
     */
    @DELETE Response delete(@QueryParam("default") Boolean defaultGraph, @QueryParam("graph") URI graphUri);

}