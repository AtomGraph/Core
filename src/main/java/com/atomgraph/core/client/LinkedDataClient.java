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
import java.net.URI;
import org.apache.jena.rdf.model.Model;

/**
 * Linked Data client.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class LinkedDataClient extends ClientBase
{
    
    protected LinkedDataClient(WebResource webResource, MediaTypes mediaTypes)
    {
        super(webResource, mediaTypes);
    }
    
    public static LinkedDataClient create(WebResource webResource, MediaTypes mediaTypes)
    {
        return new LinkedDataClient(webResource, mediaTypes);
    }

    @Override
    public LinkedDataClient addFilter(ClientFilter authFilter)
    {
        if (authFilter == null) throw new IllegalArgumentException("ClientFilter cannot be null");

        super.addFilter(authFilter);

        return this;
    }
    
    public Model get()
    {
        return get(getReadableMediaTypes(Model.class), null).getEntity(Model.class);
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
    
    public URI getWebResourceURI()
    {
        return getWebResource().getURI();
    }

    @Override
    public MediaType getDefaultMediaType()
    {
        return MediaType.TEXT_NTRIPLES_TYPE;
    }

}
