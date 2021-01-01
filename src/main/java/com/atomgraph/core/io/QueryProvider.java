/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atomgraph.core.io;

import org.apache.jena.query.Query;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import com.atomgraph.core.MediaType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyReader;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for reading/writing SPARQL queries.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see org.apache.jena.query.Query
 */ 
@Provider
@Consumes(MediaType.APPLICATION_SPARQL_QUERY)
@Produces(MediaType.APPLICATION_SPARQL_QUERY)
public class QueryProvider implements MessageBodyReader<Query>, MessageBodyWriter<Query>
{
    private static final Logger log = LoggerFactory.getLogger(QueryProvider.class);

    // READER
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
        return type == Query.class;
    }

    @Override
    public Query readFrom(Class<Query> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
    {
        try
        {
            return QueryFactory.create(IOUtils.toString(entityStream, StandardCharsets.UTF_8));
        }
        catch (QueryParseException ex)
        {
            throw new BadRequestException(ex);
        }
    }

    // WRITER
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
        return type == Query.class;
    }

    @Override
    public long getSize(Query query, Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(Query query, Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
    {
        entityStream.write(query.toString().getBytes(Charset.forName("UTF-8")));
    }

}
