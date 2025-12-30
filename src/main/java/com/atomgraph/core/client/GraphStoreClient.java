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
import com.atomgraph.core.io.ModelProvider;
import com.atomgraph.core.model.DatasetAccessor;
import jakarta.ws.rs.NotFoundException;
import java.net.URI;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.uri.UriComponent;
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
    
    public static final String DEFAULT_PARAM_NAME = "default";
    public static final String GRAPH_PARAM_NAME = "graph";

    private final Client client;
    private final URI endpoint;
    private final List<Object> components = new ArrayList<>();
    
    protected GraphStoreClient(Client client, MediaTypes mediaTypes, URI endpoint)
    {
        super(mediaTypes);
        this.client = client;
        this.endpoint = endpoint;
    }
    
    protected GraphStoreClient(Client client, MediaTypes mediaTypes)
    {
        this(client, mediaTypes, null);
    }
    
    public static GraphStoreClient create(Client client, MediaTypes mediaTypes, URI endpoint)
    {
        return new GraphStoreClient(client, mediaTypes, endpoint);
    }
    
    public static GraphStoreClient create(Client client, MediaTypes mediaTypes)
    {
        return new GraphStoreClient(client, mediaTypes);
    }

    /**
     * Register a JAX-RS component (provider) instance to be applied to all requests.
     * Components are applied to each WebTarget created by this client.
     * Supports Features, Filters, Interceptors, and other JAX-RS providers.
     *
     * @param component the component instance to register
     * @return this GraphStoreClient instance for method chaining
     * @throws IllegalArgumentException if component is null
     */
    public GraphStoreClient register(Object component)
    {
        if (component == null) throw new IllegalArgumentException("Component cannot be null");

        components.add(component);

        return this;
    }

    /**
     * Register a JAX-RS component (provider) class to be applied to all requests.
     * Components are applied to each WebTarget created by this client.
     * Supports Features, Filters, Interceptors, and other JAX-RS providers.
     *
     * @param componentClass the component class to register
     * @return this GraphStoreClient instance for method chaining
     * @throws IllegalArgumentException if componentClass is null
     */
    public GraphStoreClient register(Class<?> componentClass)
    {
        if (componentClass == null) throw new IllegalArgumentException("Component class cannot be null");

        components.add(componentClass);

        return this;
    }

    // default graph is not supported
    
    @Override
    public Model getModel()
    {
        try (Response cr = get(null))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();

            return cr.readEntity(Model.class);
        }    
    }
    
    @Override
    public void add(Model model)
    {
        try (Response cr = post(null, Entity.entity(model, getDefaultMediaType()), new jakarta.ws.rs.core.MediaType[]{}))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }
    
    @Override
    public void deleteDefault()
    {
        try (Response cr = delete(null))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }
    
    @Override
    public void putModel(Model model)
    {
        try (Response cr = put(null, Entity.entity(model, getDefaultMediaType()), new jakarta.ws.rs.core.MediaType[]{}))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }
    
    protected WebTarget getWebTarget(URI uri)
    {
        WebTarget target;

        // indirect graph identification
        if (getEndpoint() != null)
        {
            if (uri == null) // default graph - only possible with endpoint
                target = getClient().target(getEndpoint()).queryParam(DEFAULT_PARAM_NAME, Boolean.TRUE.toString());
            else // named graph
                target = getClient().target(getEndpoint()).queryParam(UriComponent.encode(GRAPH_PARAM_NAME, UriComponent.Type.UNRESERVED),
                    UriComponent.encode(uri.toString(), UriComponent.Type.UNRESERVED));
        }
        // direct graph idntification
        else
        {
            if (uri == null)
                throw new UnsupportedOperationException("Default graph is not supported without endpoint -- all RDF graphs in Linked Data are named");

            target = getClient().target(uri);
        }

        // Apply all registered components to this WebTarget
        for (Object component : components)
            target = target.register(component);

        return target;
    }
    
    public Response head(URI uri)
    {
        return head(uri, null);
    }
    
    public Response head(URI uri, jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return head(uri, acceptedTypes, new MultivaluedHashMap());
    }

    public Response head(URI uri, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(getWebTarget(uri).request(acceptedTypes), headers).head();
    }
    
    @Override
    public boolean containsModel(String uri)
    {
        try (Response cr = head(URI.create(uri)))
        {
            return cr.getStatusInfo().
                getFamily().
                equals(Response.Status.Family.SUCCESSFUL);
        }
    }
    
    public Response get(URI uri)
    {
        return get(uri, getReadableMediaTypes(Model.class));
    }

    public Response get(URI uri, jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return get(uri, acceptedTypes, new MultivaluedHashMap());
    }
    
    public Response get(URI uri, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(getWebTarget(uri).request(acceptedTypes), headers).get();
    }
    
    @Override
    public Model getModel(String uri)
    {
        try (Response cr = get(URI.create(uri)))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();

            cr.getHeaders().putSingle(ModelProvider.REQUEST_URI_HEADER, uri); // provide a base URI hint to ModelProvider
            return cr.readEntity(Model.class);
        }
    }
    
    public Response post(URI uri, Model model)
    {
        return post(uri, Entity.entity(model, getDefaultMediaType()), new jakarta.ws.rs.core.MediaType[]{}, new MultivaluedHashMap());
    }
    
    public Response post(URI uri, Entity entity)
    {
        return post(uri, entity, new jakarta.ws.rs.core.MediaType[]{}, new MultivaluedHashMap());
    }
    
    public Response post(URI uri, Entity entity, jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return post(uri, entity, acceptedTypes, new MultivaluedHashMap());
    }
        
    public Response post(URI uri, Entity entity, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(getWebTarget(uri).request(acceptedTypes), headers).post(entity);
    }
    
    @Override
    public void add(String uri, Model model)
    {
        try (Response cr = post(URI.create(uri), Entity.entity(model, getDefaultMediaType())))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();
        }
    }

    public Response put(URI uri, Model model)
    {
        return put(uri, Entity.entity(model, getDefaultMediaType()), getReadableMediaTypes(Model.class), new MultivaluedHashMap());
    } 
    
    public Response put(URI uri, Entity entity)
    {
        return put(uri, entity, getReadableMediaTypes(Model.class), new MultivaluedHashMap());
    } 

    public Response put(URI uri, Entity entity, jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return put(uri, entity, acceptedTypes, new MultivaluedHashMap());
    }
    
    public Response put(URI uri, Entity entity, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(getWebTarget(uri).request(acceptedTypes), headers).put(entity);
    }
    
    @Override
    public void putModel(String uri, Model model)
    {
        try (Response cr = put(URI.create(uri), Entity.entity(model, getDefaultMediaType())))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();                
        }
    }

    public Response delete(URI uri)
    {
        return delete(uri, new jakarta.ws.rs.core.MediaType[]{}, new MultivaluedHashMap(), new MultivaluedHashMap());
    }
    
    public Response delete(URI uri, jakarta.ws.rs.core.MediaType[] acceptedTypes)
    {
        return delete(uri, acceptedTypes, new MultivaluedHashMap(), new MultivaluedHashMap());
    }
    
    public Response delete(URI uri, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params)
    {
        return delete(uri, acceptedTypes, params, new MultivaluedHashMap());
    }
    
    public Response delete(URI uri, jakarta.ws.rs.core.MediaType[] acceptedTypes, MultivaluedMap<String, String> params, MultivaluedMap<String, Object> headers)
    {
        return applyHeaders(getWebTarget(uri).request(acceptedTypes), headers).delete();
    }

    @Override
    public void deleteModel(String uri)
    {
        try (Response cr = delete(URI.create(uri)))
        {
            // some endpoints might include response body which will not cause NotFoundException in Jersey
            if (cr.getStatus() == Status.NOT_FOUND.getStatusCode()) throw new NotFoundException();                
        }
    }

    protected Invocation.Builder applyHeaders(Invocation.Builder builder, MultivaluedMap<String, Object> headers)
    {
        if (headers != null)
            for (Map.Entry<String, List<Object>> entry : headers.entrySet())
                for (Object value : entry.getValue())
                    builder = builder.header(entry.getKey(), value);
        
        return builder;
    }
    
    @Override
    public MediaType getDefaultMediaType()
    {
        return MediaType.APPLICATION_NTRIPLES_TYPE;
    }
    
    public Client getClient()
    {
        return client;
    }

    public URI getEndpoint()
    {
        return endpoint;
    }
    
}
