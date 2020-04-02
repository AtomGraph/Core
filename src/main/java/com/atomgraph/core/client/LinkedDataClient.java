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
import java.net.URI;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import org.apache.jena.rdf.model.Model;

/**
 * Linked Data client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class LinkedDataClient extends ClientBase
{
    
    protected LinkedDataClient(WebTarget webResource, MediaTypes mediaTypes)
    {
        super(webResource, mediaTypes);
    }
    
    public static LinkedDataClient create(WebTarget webResource, MediaTypes mediaTypes)
    {
        return new LinkedDataClient(webResource, mediaTypes);
    }

    @Override
    public LinkedDataClient register(ClientRequestFilter filter)
    {
        if (filter == null) throw new IllegalArgumentException("ClientRequestFilter cannot be null");

        super.register(filter);

        return this;
    }
    
    public Model get()
    {
        return get(getReadableMediaTypes(Model.class), null).readEntity(Model.class);
    }
    
    public void post(Model model)
    {
        post(model, getDefaultMediaType(), null, null);
    }

    public void put(Model model)
    {
        put(model, getDefaultMediaType(), null, null);
    }

    public void delete()
    {
        delete(null, null);
    }
    
    public URI getWebTargetURI()
    {
        return getWebTarget().getUri();
    }

    @Override
    public MediaType getDefaultMediaType()
    {
        return MediaType.TEXT_NTRIPLES_TYPE;
    }

}
