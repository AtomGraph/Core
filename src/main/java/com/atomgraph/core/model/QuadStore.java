/*
 * Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>.
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
import org.apache.jena.query.Dataset;

/**
 * SPARQL 1.1 Graph Store HTTP Protocol interface extended to support quads
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public interface QuadStore
{

    /**
     * Response with dataset.
     * 
     * @return result response
     */
    @GET Response get();
    
    /**
     * Appends request dataset entity.
     * 
     * @param dataset RDF payload dataset
     * @return result response
     */
    @POST Response post(Dataset dataset);
    
    /**
     * Replaces the dataset with request dataset entity.
     * 
     * @param dataset RDF payload dataset
     * @return result response
     */
    @PUT Response put(Dataset dataset);
    
    /**
     * Handles DELETE query request and returns result as response
     * 
     * @return result response
     */
    @DELETE Response delete();

}
