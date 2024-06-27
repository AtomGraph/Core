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
package com.atomgraph.core.io;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.apache.jena.riot.resultset.ResultSetWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for writing SPARQL result set to the response.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a>
 * @see org.apache.jena.query.ResultSet
 * @see jakarta.ws.rs.ext.MessageBodyReader
 * @see jakarta.ws.rs.ext.MessageBodyWriter
 */
@Provider
public class ResultSetProvider implements MessageBodyReader<ResultSetRewindable>, MessageBodyWriter<ResultSet>
{
    private static final Logger log = LoggerFactory.getLogger(ResultSetProvider.class);
    
    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, jakarta.ws.rs.core.MediaType mediaType)
    {
        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        if (lang == null) return false;
        return type == ResultSetRewindable.class && ResultSetReaderRegistry.isRegistered(lang);
    }

    @Override
    public ResultSetRewindable readFrom(Class<ResultSetRewindable> type, Type type1, Annotation[] antns, jakarta.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream in) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Reading ResultSet with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
        
        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString()); // cannot be null - isReadable() checks that
        if (log.isDebugEnabled()) log.debug("RDF language used to read ResultSet: {}", lang);

        // result set needs to be rewindable because results might be processed multiple times, e.g. to calculate hash and write response
        return ResultSetFactory.makeRewindable(ResultSetMgr.read(in, lang));
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        if (lang == null) return false;
        return ResultSet.class.isAssignableFrom(type) && ResultSetWriterRegistry.isRegistered(lang);
    }

    @Override
    public long getSize(ResultSet t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(ResultSet results, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Writing ResultSet with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);

        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString()); // cannot be null - isWritable() checks that
        if (log.isDebugEnabled()) log.debug("RDF language used to write ResultSet: {}", lang);
        
        ResultSetFormatter.output(entityStream, results, lang);
    }
    
}
