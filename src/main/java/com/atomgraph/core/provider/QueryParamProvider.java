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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
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
public class QueryParamProvider implements ParamConverterProvider // InjectableProvider<QueryParam, Query>
{
    private static final Logger log = LoggerFactory.getLogger(QueryParamProvider.class);

    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type type, Annotation[] antns)
    {
        if (rawType.equals(Query.class))
        {
            return new ParamConverter<T>()
            {

                @Override
                public T fromString(final String value)
                {
                    if (value == null) throw new IllegalArgumentException("Cannot parse Query from null String");
                    
                    try
                    {
                        return rawType.cast(QueryFactory.create(value));
                    }
                    catch (QueryException ex)
                    {
                        if (log.isWarnEnabled()) log.warn("Supplied SPARQL query string could not be parsed, check syntax: {}", value);
                        //throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                        return null;
                    }
                }

                @Override
                public String toString(final T query)
                {
                    return query.toString();
                }
            };
        }

        return null;
    }

} 