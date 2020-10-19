/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.core.client;

import com.atomgraph.core.MediaType;
import com.atomgraph.core.MediaTypes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL Graph Store Protocol client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public class GraphStoreClient extends ClientBase implements DatasetAccessor
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreClient.class);
    
    private static final String DEFAULT_PARAM_NAME = "default";
    private static final String GRAPH_PARAM_NAME = "graph";
    
    protected GraphStoreClient(WebTarget endpoint, MediaTypes mediaTypes)
    {
        super(endpoint, mediaTypes);
    }

    protected GraphStoreClient(WebTarget endpoint)
    {
        this(endpoint, new MediaTypes());
    }

    public static GraphStoreClient create(WebTarget endpoint, MediaTypes mediaTypes)
    {
        return new GraphStoreClient(endpoint, mediaTypes);
    }

    public static GraphStoreClient create(WebTarget endpoint)
    {
        return new GraphStoreClient(endpoint);
    }

    /**
     * Registers client filter.
     * Can cause performance problems with <code>ApacheConnector</code>.
     * 
     * @param filter client request filter
     * @return this SPARQL client
     * @see <a href="https://blogs.oracle.com/japod/how-to-use-jersey-client-efficiently">How To Use Jersey Client Efficiently</a>
     */
    @Override
    public GraphStoreClient register(ClientRequestFilter filter)
    {
        if (filter == null) throw new IllegalArgumentException("ClientRequestFilter cannot be null");

        super.register(filter);

        return this;
    }
    
    @Override
    public MediaType getDefaultMediaType()
    {
        return MediaType.APPLICATION_NTRIPLES_TYPE;
    }
    
    @Override
    public boolean containsModel(String uri)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, uri);

        try (Response cr = head(getReadableMediaTypes(Model.class), params))
        {
            return cr.getStatusInfo().
                getFamily().
                equals(Response.Status.Family.SUCCESSFUL);
        }
    }

    @Override
    public Model getModel()
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(DEFAULT_PARAM_NAME, Boolean.TRUE.toString());

        try (Response cr = get(getReadableMediaTypes(Model.class), params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
            
            return cr.readEntity(Model.class);
        }
    }

    @Override
    public Model getModel(String uri)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, uri);

        try (Response cr = get(getReadableMediaTypes(Model.class), params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
            
            return cr.readEntity(Model.class);
        }
    }
    
    @Override
    public void add(Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(DEFAULT_PARAM_NAME, Boolean.TRUE.toString());

        try (Response cr = post(model, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}, params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }
    
    @Override
    public void add(String uri, Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, uri);

        try (Response cr = post(model, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}, params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }
    
    @Override
    public void putModel(Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(DEFAULT_PARAM_NAME, Boolean.TRUE.toString());

        try (Response cr = put(model, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}, params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }

    @Override
    public void putModel(String uri, Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, uri);

        try (Response cr = put(model, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}, params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }
    
    @Override
    public void deleteDefault()
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(DEFAULT_PARAM_NAME, Boolean.TRUE.toString());

        try (Response cr = delete(new javax.ws.rs.core.MediaType[]{}, params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }

    @Override
    public void deleteModel(String uri)
    {
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.putSingle(GRAPH_PARAM_NAME, uri);

        try (Response cr = delete(new javax.ws.rs.core.MediaType[]{}, params))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }

}
