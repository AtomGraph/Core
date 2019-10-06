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
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
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
    
    protected GraphStoreClient(WebResource webResource, MediaTypes mediaTypes)
    {
        super(webResource, mediaTypes);
    }

    protected GraphStoreClient(WebResource webResource)
    {
        this(webResource, new MediaTypes());
    }

    public static GraphStoreClient create(WebResource webResource, MediaTypes mediaTypes)
    {
        return new GraphStoreClient(webResource, mediaTypes);
    }

    public static GraphStoreClient create(WebResource webResource)
    {
        return new GraphStoreClient(webResource);
    }

    @Override
    public GraphStoreClient addFilter(ClientFilter authFilter)
    {
        if (authFilter == null) throw new IllegalArgumentException("ClientFilter cannot be null");

        super.addFilter(authFilter);

        return this;
    }
    
    @Override
    public MediaType getDefaultMediaType()
    {
        return MediaType.TEXT_NTRIPLES_TYPE;
    }
    
    @Override
    public boolean containsModel(String uri)
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("graph", uri);

        return head(Model.class, getReadableMediaTypes(Model.class), uri, params, null).
                getStatusInfo().
                getFamily().
                equals(Response.Status.Family.SUCCESSFUL);
    }

    @Override
    public Model getModel()
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("default", "");

        return get(getReadableMediaTypes(Model.class), params).getEntity(Model.class);
    }

    @Override
    public Model getModel(String uri)
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("graph", uri);

        return get(getReadableMediaTypes(Model.class), params).getEntity(Model.class);
    }
    
    @Override
    public void add(Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("default", "");

        post(model, getDefaultMediaType(), null, params);
    }
    
    @Override
    public void add(String uri, Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("graph", uri);

        post(model, getDefaultMediaType(), null, params);
    }
    
    @Override
    public void putModel(Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("default", "");

        put(model, getDefaultMediaType(), null, params);
    }

    @Override
    public void putModel(String uri, Model model)
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("graph", uri);

        put(model, getDefaultMediaType(), null, params);
    }
    
    @Override
    public void deleteDefault()
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("default", "");

        delete(null, params);
    }

    @Override
    public void deleteModel(String uri)
    {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("graph", uri);

        delete(null, params);
    }

}
