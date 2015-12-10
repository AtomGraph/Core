/**
 *  Copyright 2014 Martynas Jusevičius <martynas@graphity.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.graphity.core.model.impl;

import com.sun.jersey.api.client.WebResource;
import org.graphity.core.model.GraphStoreOrigin;

/**
 * Base class of SPARQL Graph Store origins.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class GraphStoreOriginBase extends OriginBase implements GraphStoreOrigin
{

    public GraphStoreOriginBase(WebResource webResource)
    {
        super(webResource);
    }
    
    /**
     * Constructs Graph Store origin from URI and HTTP authentication credentials.
     * 
     * @param uri origin URI
     * @param authUser authentication username
     * @param authPwd authentication password
     */
    /*
    public GraphStoreOriginBase(String uri, String authUser, byte[] authPwd)
    {
        super(uri, authUser, authPwd);
    }

    public GraphStoreOriginBase(String uri, String authUser, String authPwd)
    {
        super(uri, authUser, authPwd);
    }
    */
    /**
     * Constructs Graph Store origin from URI.
     * 
     * @param uri 
     */
    /*
    public GraphStoreOriginBase(String uri)
    {
        super(uri);
    }
    */
}
