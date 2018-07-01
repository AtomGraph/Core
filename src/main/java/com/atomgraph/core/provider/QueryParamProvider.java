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
package com.atomgraph.core.provider;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS query parameter provider for SPARQL string.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see org.apache.jena.query.Query
 * @see javax.ws.rs.QueryParam
 * @see javax.ws.rs.core.Context
 */
@Provider
public class QueryParamProvider extends PerRequestTypeInjectableProvider<QueryParam, Query>
{
    private static final Logger log = LoggerFactory.getLogger(QueryParamProvider.class);
    
    @Context HttpContext httpContext;

    public QueryParamProvider()
    {
	super(Query.class);
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
		String value = getHttpContext().getUriInfo().getQueryParameters().getFirst(paramName);
		if (value == null || value.isEmpty()) return null;
		    
		if (log.isTraceEnabled()) log.trace("Providing Injectable<Query> with @QueryParam({}) and value: {}", paramName, value);
		try
		{
		    return QueryFactory.create(value);
		}
		catch (Exception ex)
		{
		    if (log.isWarnEnabled()) log.warn("Supplied SPARQL query string could not be parsed, check syntax: {}", value);
		    //throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                    return null;
		}
	    }
	};
    }

    public HttpContext getHttpContext()
    {
	return httpContext;
    }

} 