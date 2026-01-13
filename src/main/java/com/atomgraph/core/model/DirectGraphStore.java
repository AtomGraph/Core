/*
 * Copyright 2026 martynas.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.core.model;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Response;
import org.apache.jena.rdf.model.Model;

/**
 * Directly-identified SPARQL 1.1 Graph Store HTTP Protocol interface (aka Linked Data)
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.w3.org/TR/sparql11-http-rdf-update/#direct-graph-identification">SPARQL 1.1 Graph Store HTTP Protocol</a>
 * @see <a href="https://en.wikipedia.org/wiki/Linked_data">Linked Data</a>
*/
public interface DirectGraphStore
{
    /**
     * Handles GET query request and returns result as response
     * 
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-get">5.2 HTTP GET</a>
     */
    @GET Response get();
    
    /**
     * Handles POST query request and returns result as response
     * 
     * @param model RDF payload model
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-post">5.5 HTTP POST</a>
     */
    @POST Response post(Model model);
    
    /**
     * Handles PUT query request and returns result as response
     * 
     * @param model RDF payload model
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-put">5.3 HTTP PUT</a>
     */
    @PUT Response put(Model model);
    
    /**
     * Handles DELETE query request and returns result as response
     * 
     * @return result response
     * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#http-delete">5.4 HTTP DELETE</a>
     */
    @DELETE Response delete();

}