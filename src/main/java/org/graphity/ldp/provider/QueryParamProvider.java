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
package org.graphity.ldp.provider;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import java.lang.reflect.Type;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides HTTP query parameter with SPARQL string as injectable ARQ query
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/QueryParam.html">@QueryParam</a>
 * @see <a href="http://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/spi/inject/PerRequestTypeInjectableProvider.html">PerRequestTypeInjectableProvider</a>
 */
@Provider
public class QueryParamProvider extends PerRequestTypeInjectableProvider<QueryParam, Query> // implements InjectableProvider<QueryParam, Parameter>
{
    private static final Logger log = LoggerFactory.getLogger(QueryParamProvider.class);
    
    @Context HttpContext hc = null;

    public QueryParamProvider(Type t)
    {
	super(t);
    }

    @Override
    public Injectable<Query> getInjectable(ComponentContext ic, QueryParam qp)
    {
	final String paramName = qp.value();
	return new Injectable<Query>()
	{
	    @Override
	    public Query getValue()
	    {
		String value = hc.getUriInfo().getQueryParameters().getFirst(paramName);
		if (value == null || value.isEmpty()) return null;
		    
		if (log.isTraceEnabled()) log.trace("Providing Injectable<Query> with @QueryParam({}) and value: {}", paramName, value);
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
} 