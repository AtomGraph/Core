/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.core.factory;

import com.atomgraph.core.MediaTypes;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for media type registry.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see com.atomgraph.core.MediaTypes
 * @see jakarta.ws.rs.core.Context
 */
public class MediaTypesFactory implements Factory<MediaTypes>
{

    private static final Logger log = LoggerFactory.getLogger(MediaTypesFactory.class);
    
    private final MediaTypes mediaTypes;
    
    public MediaTypesFactory(final MediaTypes mediaTypes)
    {
        this.mediaTypes = mediaTypes;
    }
    
    @Override
    public MediaTypes provide()
    {
        return getMediaTypes();
    }

    @Override
    public void dispose(MediaTypes t)
    {
    }

    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
}
