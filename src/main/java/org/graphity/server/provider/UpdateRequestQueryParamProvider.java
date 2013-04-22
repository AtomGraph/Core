/*
 * Copyright (C) 2013 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.provider;

import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides HTTP update parameter with SPARQL string as injectable ARQ update request.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.server.Application
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/QueryParam.html">JAX-RS @QueryParam</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/update/UpdateRequest.html">Jena UpdateRequest</a>
 * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/spi/inject/PerRequestTypeInjectableProvider.html">PerRequestTypeInjectableProvider</a>
 */
@Provider
public class UpdateRequestQueryParamProvider extends PerRequestTypeInjectableProvider<QueryParam, UpdateRequest>
{
    private static final Logger log = LoggerFactory.getLogger(UpdateRequestQueryParamProvider.class);
    
    @Context HttpContext httpContext;
    
    public UpdateRequestQueryParamProvider()
    {
	super(UpdateRequest.class);
    }

    @Override
    public Injectable<UpdateRequest> getInjectable(ComponentContext cc, QueryParam qp)
    {
	final String paramName = qp.value();
	return new Injectable<UpdateRequest>()
	{
	    @Override
	    public UpdateRequest getValue()
	    {
		String value = getHttpContext().getUriInfo().getQueryParameters().getFirst(paramName);
		if (value == null || value.isEmpty()) return null;
		    
		if (log.isTraceEnabled()) log.trace("Providing Injectable<UpdateRequest> with @QueryParam({}) and value: {}", paramName, value);
		try
		{
		    return UpdateFactory.create(value);
		}
		catch (Exception ex)
		{
		    if (log.isWarnEnabled()) log.warn("Supplied SPARQL update string could not be parsed, check syntax: {}", value);
		    throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
		}
	    }
	};
    }

    public HttpContext getHttpContext()
    {
	return httpContext;
    }

}
