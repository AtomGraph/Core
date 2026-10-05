/**
 *  Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>
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

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.apache.jena.riot.resultset.ResultSetWriterRegistry;
import org.apache.jena.sparql.resultset.ResultsReader;
import org.apache.jena.sparql.resultset.ResultsWriter;
import org.apache.jena.sparql.resultset.SPARQLResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for a SPARQL query result in the SPARQL results formats: a boolean, which an ASK
 * query answers with, or a result set. Jena's {@link SPARQLResult} is the union over everything a
 * query can return, and this provider takes the two arms that the results formats carry; a model or
 * a dataset answers in an RDF format and has a provider of its own.
 * <p>
 * Not every results format carries a boolean. Jena writes one in XML, JSON, CSV and TSV and refuses
 * in Thrift and Protobuf, and reads one back only from XML, JSON and CSV, since its TSV, Thrift and
 * Protobuf readers always produce a row set. {@link #BOOLEAN_WRITABLE_LANGS} and
 * {@link #BOOLEAN_READABLE_LANGS} record that, so an endpoint can offer and a client can ask for the
 * formats a boolean survives in.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see org.apache.jena.sparql.resultset.SPARQLResult
 * @see jakarta.ws.rs.ext.MessageBodyReader
 * @see jakarta.ws.rs.ext.MessageBodyWriter
 */
@Provider
public class SPARQLResultProvider implements MessageBodyReader<SPARQLResult>, MessageBodyWriter<SPARQLResult>
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLResultProvider.class);

    /** The results formats Jena can write a boolean in; its Thrift and Protobuf writers throw on one */
    public static final Set<Lang> BOOLEAN_WRITABLE_LANGS = Set.of(ResultSetLang.RS_XML, ResultSetLang.RS_JSON, ResultSetLang.RS_CSV, ResultSetLang.RS_TSV);
    /** The results formats Jena reads a boolean back from; its TSV, Thrift and Protobuf readers always produce a row set */
    public static final Set<Lang> BOOLEAN_READABLE_LANGS = Set.of(ResultSetLang.RS_XML, ResultSetLang.RS_JSON, ResultSetLang.RS_CSV);

    /**
     * The results language of a media type, with its parameters (such as the charset) disregarded.
     *
     * @param mediaType media type
     * @return language, or null when the media type is not a registered one
     */
    public static Lang getLang(MediaType mediaType)
    {
        if (mediaType == null) return null;

        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        return RDFLanguages.contentTypeToLang(formatType.toString());
    }

    /**
     * Whether a boolean result can be written in the given media type.
     *
     * @param mediaType media type
     * @return true if Jena has a boolean writer for it
     */
    public static boolean isBooleanWriteable(MediaType mediaType)
    {
        Lang lang = getLang(mediaType);
        return lang != null && BOOLEAN_WRITABLE_LANGS.contains(lang);
    }

    /**
     * Whether a boolean result can be read from the given media type.
     *
     * @param mediaType media type
     * @return true if Jena's reader for it yields a boolean
     */
    public static boolean isBooleanReadable(MediaType mediaType)
    {
        Lang lang = getLang(mediaType);
        return lang != null && BOOLEAN_READABLE_LANGS.contains(lang);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        Lang lang = getLang(mediaType);
        if (lang == null) return false;
        return type == SPARQLResult.class && ResultSetReaderRegistry.isRegistered(lang);
    }

    @Override
    public SPARQLResult readFrom(Class<SPARQLResult> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream in) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Reading SPARQLResult with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);

        Lang lang = getLang(mediaType); // cannot be null - isReadable() checks that
        if (log.isDebugEnabled()) log.debug("Results language used to read SPARQLResult: {}", lang);

        SPARQLResult result = ResultsReader.create().lang(lang).build().readAny(in);
        // the stream closes after this method, so the rows are read now and can be processed more than once
        if (result.isResultSet()) return new SPARQLResult(ResultSetFactory.makeRewindable(result.getResultSet()));
        return result;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        Lang lang = getLang(mediaType);
        if (lang == null) return false;
        return SPARQLResult.class.isAssignableFrom(type) && ResultSetWriterRegistry.isRegistered(lang);
    }

    @Override
    public long getSize(SPARQLResult result, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(SPARQLResult result, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Writing SPARQLResult with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);

        Lang lang = getLang(mediaType); // cannot be null - isWriteable() checks that
        if (log.isDebugEnabled()) log.debug("Results language used to write SPARQLResult: {}", lang);

        if (result.isBoolean())
        {
            // the endpoint offers only the formats a boolean can be written in; this is for a caller that did not ask it
            if (!BOOLEAN_WRITABLE_LANGS.contains(lang)) throw new NotAcceptableException("A boolean result cannot be written as " + lang);
            ResultsWriter.create().lang(lang).build().write(entityStream, result.getBooleanResult());
            return;
        }
        if (result.isResultSet())
        {
            ResultsWriter.create().lang(lang).build().write(entityStream, result.getResultSet());
            return;
        }

        throw new WebApplicationException("A SPARQLResult holding a model, a dataset or JSON is not written in a SPARQL results format; a model and a dataset have providers of their own",
            Response.Status.INTERNAL_SERVER_ERROR);
    }

}
