/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.auth;

import com.sun.jersey.api.container.MappableContainerException;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
// http://pastie.org/1530248
// http://java.net/projects/jersey/sources/svn/content/trunk/jersey/samples/https-clientserver-grizzly/src/main/java/com/sun/jersey/samples/https_grizzly/auth/SecurityFilter.java
public class HTTPBasicAuthFilter implements ResourceFilter, ContainerRequestFilter
{
    private static final Logger log = LoggerFactory.getLogger(HTTPBasicAuthFilter.class);
    
    private static final String REALM = "HTTPS Example authentication";

    @Context SecurityContext sc = null;
    
    @Override
    public ContainerRequestFilter getRequestFilter()
    {
	return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter()
    {
	return null;
    }

    @Override
    public ContainerRequest filter(ContainerRequest cr)
    {
	if (log.isDebugEnabled()) log.debug("ContainerRequest: {} SecurityContext: {}", cr, sc);

	authenticate(cr);
      
	return cr;
    }

    private void authenticate(ContainerRequest cr)
    {
	if (log.isInfoEnabled()) log.info("Authenticating request");
      
	// Extract authentication credentials
	String authentication = cr.getHeaderValue(HttpHeaders.AUTHORIZATION);

	if (authentication == null)
	    throw new MappableContainerException(
		new AuthenticationException("Authentication credentials are required", REALM));

	if (!authentication.startsWith("Basic "))
	{
	    if (log.isInfoEnabled()) log.info("Only HTTP Basic authentication is supported");
	    //return null;
	}

	authentication = authentication.substring("Basic ".length());
	String[] values = Base64.base64Decode(authentication).split(":");
	if (values.length < 2)
	{
	    if (log.isInfoEnabled()) log.info("Invalid syntax for username and password");
	    throw new MappableContainerException(
		new AuthenticationException("Invalid syntax for username and password", REALM));
	}

	String username = values[0];
	String password = values[1];
	if (username == null || password == null)
	{
	    if (log.isInfoEnabled()) log.info("Missing username or password");
	    throw new MappableContainerException(
		new AuthenticationException("Missing username or password", REALM));
	}
	
	//return user;
    }
}