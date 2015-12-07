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

import java.nio.charset.Charset;
import org.graphity.core.model.Origin;

/**
 * Base class for origin implementation.
 * Origins are used to indicate remote SPARQL and Graph Store endpoints.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class OriginBase implements Origin
{
    static private final Charset CHARACTER_SET = Charset.forName("iso-8859-1");
    
    private final String uri;
    private final String username;
    private final byte[] password;
    
    /**
     * Constructs origin from URI.
     * 
     * @param uri origin URI
     */
    public OriginBase(String uri)
    {
        this(uri, null, (byte[])null);
    }
    
    /**
     * Constructs origin from URI.
     * 
     * @param uri origin URI
     * @param username username
     * @param password password
     */
    public OriginBase(String uri, String username, byte[] password)
    {
        this.uri = uri;
        this.username = username;
        this.password = password;
    }
    
    public OriginBase(String uri, String username, String password)
    {
        this(uri, username, password.getBytes(CHARACTER_SET));
    }
    
    @Override
    public String getURI()
    {
        return uri;
    }

    @Override
    public String getUsername()
    {
        return username;
    }

    @Override
    public byte[] getPassword()
    {
        return password;
    }
    
    @Override
    public String toString()
    {
        return uri; // TO-DO: add username/password?
    }
    
}
