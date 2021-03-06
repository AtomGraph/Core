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
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import org.apache.jena.rdf.model.Model;

/**
 * Linked Data client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class LinkedDataClient extends ClientBase
{
    
    protected LinkedDataClient(WebTarget endpoint, MediaTypes mediaTypes)
    {
        super(endpoint, mediaTypes);
    }
    
    public static LinkedDataClient create(WebTarget endpoint, MediaTypes mediaTypes)
    {
        return new LinkedDataClient(endpoint, mediaTypes);
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
    public LinkedDataClient register(ClientRequestFilter filter)
    {
        if (filter == null) throw new IllegalArgumentException("ClientRequestFilter cannot be null");

        super.register(filter);

        return this;
    }
    
    public Model get()
    {
        try (Response cr = get(getReadableMediaTypes(Model.class), new MultivaluedHashMap()))
        {
            return cr.readEntity(Model.class);
        }
    }
    
    public void post(Model model)
    {
        post(model, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}).close();
    }

    public void put(Model model)
    {
        put(model, getDefaultMediaType(), new javax.ws.rs.core.MediaType[]{}).close();
    }

    public void delete()
    {
        delete(new javax.ws.rs.core.MediaType[]{}).close();
    }

    @Override
    public MediaType getDefaultMediaType()
    {
        return MediaType.APPLICATION_NTRIPLES_TYPE;
    }

}
