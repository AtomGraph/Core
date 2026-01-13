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
package com.atomgraph.core.model.impl;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.DatasetAccessor;
import com.atomgraph.core.model.DirectGraphStore;
import com.atomgraph.core.model.Service;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.apache.jena.rdf.model.Model;

/**
 * Directly-identified SPARQL Graph Stores implementation.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.w3.org/TR/sparql11-http-rdf-update/#indirect-graph-identification">SPARQL 1.1 Graph Store HTTP Protocol</a>
 * @see com.atomgraph.core.model.GraphStore
 */
public class DirectGraphStoreImpl extends GraphStoreBase implements DirectGraphStore
{

    private final UriInfo uriInfo;
    
    /**
     * Constructs Graph Store from request metadata.
     * 
     * @param request request
     * @param service SPARQL service
     * @param mediaTypes supported media types
     * @param uriInfo URI information
     */
    @Inject
    public DirectGraphStoreImpl(@Context Request request, Service service, MediaTypes mediaTypes, @Context UriInfo uriInfo)
    {
        this(request, service.getDatasetAccessor(), mediaTypes, uriInfo);
    }

    /**
     * Constructs Graph Store from request metadata and dataset accessor.
     *
     * @param request request
     * @param accessor dataset accessor
     * @param mediaTypes supported media types
     * @param uriInfo URI information
     */
    public DirectGraphStoreImpl(Request request, DatasetAccessor accessor, MediaTypes mediaTypes, @Context UriInfo uriInfo)
    {
        super(request, accessor, mediaTypes);
        this.uriInfo = uriInfo;
    }
    
    /**
     * Implements <code>GET</code> method of SPARQL Graph Store Protocol.
     * 
     * @return response
     */
    @GET
    @Override
    public Response get()
    {
        return super.get(false, getURI());
    }

    /**
     * Implements <code>POST</code> method of SPARQL Graph Store Protocol.
     * 
     * @param model RDF request body
     * @return response
     */
    @POST
    @Override
    public Response post(Model model)
    {
        return super.post(model, false, getURI());
    }

    /**
     * Implements <code>PUT</code> method of SPARQL Graph Store Protocol.
     * 
     * @param model RDF request body
     * @return response
     */    
    @PUT
    @Override
    public Response put(Model model)
    {
        return super.put(model, false, getURI());
    }

    /**
     * Implements <code>DELETE</code> method of SPARQL Graph Store Protocol.
     * 
     * @return response
     */
    @DELETE
    @Override
    public Response delete()
    {
        return super.delete(false, getURI());
    }

    /**
     * Returns the graph URI.
     *
     * @return the graph URI
     */
    public final URI getURI()
    {
        return getUriInfo().getAbsolutePath();
    }

    /**
     * Returns the URI information.
     *
     * @return URI information
     */
    public final UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}
