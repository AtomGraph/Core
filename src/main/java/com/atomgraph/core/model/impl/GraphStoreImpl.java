/**
 *  Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.model.impl;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.DatasetAccessor;
import com.atomgraph.core.model.GraphStore;
import com.atomgraph.core.model.Service;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indirectly-identified SPARQL Graph Stores implementation.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.w3.org/TR/sparql11-http-rdf-update/#indirect-graph-identification">SPARQL 1.1 Graph Store HTTP Protocol</a>
 * @see com.atomgraph.core.model.GraphStore
 */
public class GraphStoreImpl extends GraphStoreBase implements GraphStore
{
    
    private static final Logger log = LoggerFactory.getLogger(GraphStoreImpl.class);

    /**
     * Constructs Graph Store from request metadata.
     * 
     * @param request request
     * @param service SPARQL service
     * @param mediaTypes supported media types
     */
    @Inject
    public GraphStoreImpl(@Context Request request, Service service, MediaTypes mediaTypes)
    {
        super(request, service.getDatasetAccessor(), mediaTypes);
    }

    /**
     * Constructs Graph Store from request metadata and dataset accessor.
     *
     * @param request request
     * @param accessor dataset accessor
     * @param mediaTypes supported media types
     */
    public GraphStoreImpl(Request request, DatasetAccessor accessor, MediaTypes mediaTypes)
    {
        super(request, accessor, mediaTypes);
    }
    
    /**
     * Implements <code>GET</code> method of SPARQL Graph Store Protocol.
     * 
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */
    @GET
    @Override
    public Response get(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        return super.get(defaultGraph, graphUri);
    }

    /**
     * Implements <code>POST</code> method of SPARQL Graph Store Protocol.
     * 
     * @param model RDF request body
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */
    @POST
    @Override
    public Response post(Model model, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        return super.post(model, defaultGraph, graphUri);
    }

    /**
     * Implements <code>PUT</code> method of SPARQL Graph Store Protocol.
     * 
     * @param model RDF request body
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */    
    @PUT
    @Override
    public Response put(Model model, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        return super.put(model, defaultGraph, graphUri);
    }

    /**
     * Implements <code>DELETE</code> method of SPARQL Graph Store Protocol.
     * 
     * @param defaultGraph true if default graph is requested
     * @param graphUri named graph URI
     * @return response
     */
    @DELETE
    @Override
    public Response delete(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        return super.delete(defaultGraph, graphUri);
    }
    
}
