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
package com.atomgraph.core.client;

import com.atomgraph.core.MediaTypes;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common base class for all HTTP-based protocol clients.
 * Note that <code>Response</code> objects returned by methods of this class <em>are not</em> closed.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public abstract class ClientBase
{
    
    private static final Logger log = LoggerFactory.getLogger(ClientBase.class);

    private final MediaTypes mediaTypes;
    
    protected ClientBase(MediaTypes mediaTypes)
    {
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes cannot be null");
        this.mediaTypes = mediaTypes;
    }
    
    public abstract MediaType getDefaultMediaType();
    
    public MediaType[] getReadableMediaTypes(Class clazz)
    {
        return getMediaTypes().getReadable(clazz).toArray(javax.ws.rs.core.MediaType[]::new);
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
}
