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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import java.lang.reflect.Type;
import javax.ws.rs.FormParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryFormParamProvider extends PerRequestTypeInjectableProvider<FormParam, Query>
{
    private static final Logger log = LoggerFactory.getLogger(QueryFormParamProvider.class);
    
    @Context HttpContext httpContext;
    
    public QueryFormParamProvider(Type t)
    {
	super(t);
    }

    @Override
    public Injectable<Query> getInjectable(ComponentContext cc, FormParam fp)
    {
	final String paramName = fp.value();
	return new Injectable<Query>()
	{
	    @Override
	    public Query getValue()
	    {
		String value = getHttpContext().getRequest().getFormParameters().getFirst(paramName);
		if (value == null || value.isEmpty()) return null;
		    
		if (log.isTraceEnabled()) log.trace("Providing Injectable<Query> with @FormParam({}) and value: {}", paramName, value);
		try
		{
		    return QueryFactory.create(value);
		}
		catch (Exception ex)
		{
		    if (log.isWarnEnabled()) log.warn("Supplied SPARQL query string could not be parsed, check syntax: {}", value);
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
