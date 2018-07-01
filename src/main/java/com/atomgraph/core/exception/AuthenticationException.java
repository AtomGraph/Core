/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.exception;

/**
 * Exception that can be thrown when authentication fails.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class AuthenticationException extends RuntimeException
{
    private String realm = null;
    
    // TO-DO: make more specific constructors with UserAccount
    public AuthenticationException(String message, String realm)
    {
	super(message);
	this.realm = realm;
    }
    
    public String getRealm()
    {
	return realm;
    }
}
