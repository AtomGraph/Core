/*
 * Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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