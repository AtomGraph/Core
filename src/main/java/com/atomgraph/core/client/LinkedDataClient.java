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
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.jena.rdf.model.Model;

/**
 * Linked Data client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class LinkedDataClient extends ClientBase
{
    
    private final Client client;
    
    protected LinkedDataClient(Client client, MediaTypes mediaTypes)
    {
        super(mediaTypes);
        this.client = client;
    }
    
    public static LinkedDataClient create(Client client, MediaTypes mediaTypes)
    {
        return new LinkedDataClient(client, mediaTypes);
    }

    /**
     * Registers client filter.
     * Can cause performance problems with <code>ApacheConnector</code>.
     * 
     * @param filter client request filter
     * @return this SPARQL client
     * @see <a href="https://blogs.oracle.com/japod/how-to-use-jersey-client-efficiently">How To Use Jersey Client Efficiently</a>
     */
    public LinkedDataClient register(ClientRequestFilter filter)
    {
        if (filter == null) throw new IllegalArgumentException("ClientRequestFilter cannot be null");

        getClient().register(filter);

        return this;
    }
    
    public WebTarget getWebTarget(URI uri)
    {
        return getClient().target(uri);
    }
    
    public Response head(URI uri, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return getWebTarget(uri).request(acceptedTypes).head();
    }
    
    public Response get(URI uri, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return getWebTarget(uri).request(acceptedTypes).get();
    }
    
    public Model get(URI uri)
    {
        try (Response cr = get(uri, getReadableMediaTypes(Model.class)))
        {
            cr.getHeaders().putSingle(ModelProvider.REQUEST_URI_HEADER, uri.toString()); // provide a base URI hint to ModelProvider
            return cr.readEntity(Model.class);
        }
    }
    
    public Response post(URI uri, javax.ws.rs.core.MediaType[] acceptedTypes, Object body, javax.ws.rs.core.MediaType contentType)
    {
        return getWebTarget(uri).request(acceptedTypes).post(Entity.entity(body, contentType));
    }
    
    public void post(URI uri, Model model)
    {
        post(uri, getReadableMediaTypes(Model.class), model, getDefaultMediaType()).close();
    }

    public Response put(URI uri, javax.ws.rs.core.MediaType[] acceptedTypes, Object body, javax.ws.rs.core.MediaType contentType)
    {
        return getWebTarget(uri).request(acceptedTypes).put(Entity.entity(body, contentType));
    }
    
    public void put(URI uri, Model model)
    {
        put(uri, getReadableMediaTypes(Model.class), model, getDefaultMediaType()).close();
    }

    public Response delete(URI uri, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return getWebTarget(uri).request(getReadableMediaTypes(Model.class)).delete();
    }
    
    public void delete(URI uri)
    {
        delete(uri, getReadableMediaTypes(Model.class)).close();
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

}
