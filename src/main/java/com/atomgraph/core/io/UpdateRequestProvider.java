/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.io;

import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import com.atomgraph.core.MediaType;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.jena.query.QueryParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for reading SPARQL Update from request body.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see org.apache.jena.update.UpdateRequest
 * @see jakarta.ws.rs.ext.MessageBodyReader
 */
@Provider
@Consumes(MediaType.APPLICATION_SPARQL_UPDATE)
@Produces(MediaType.APPLICATION_SPARQL_UPDATE)
public class UpdateRequestProvider implements MessageBodyReader<UpdateRequest>, MessageBodyWriter<UpdateRequest>
{

    private static final Logger log = LoggerFactory.getLogger(UpdateRequestProvider.class);

    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, jakarta.ws.rs.core.MediaType mt)
    {
        return type == UpdateRequest.class;
    }

    @Override
    public UpdateRequest readFrom(Class<UpdateRequest> type, Type type1, Annotation[] antns, jakarta.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream in) throws IOException, WebApplicationException
    {
        if (log.isTraceEnabled()) log.trace("Reading UpdateRequest with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
        try
        {
            return UpdateFactory.read(in);
        }
        catch (QueryParseException ex)
        {
            if (log.isWarnEnabled()) log.warn("Supplied SPARQL update string could not be parsed, check syntax");
            throw new BadRequestException(ex);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType)
    {
        return UpdateRequest.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(UpdateRequest updateRequest, Class<?> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
    {
        if (log.isTraceEnabled()) log.trace("Writing UpdateRequest with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
        new OutputStreamWriter(entityStream, StandardCharsets.UTF_8).write(updateRequest.toString());
    }
    
}
