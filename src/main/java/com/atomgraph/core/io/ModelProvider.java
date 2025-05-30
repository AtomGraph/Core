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

import jakarta.ws.rs.core.Context;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.StreamRDFLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for reading RDF model from request and writing it to response.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see org.apache.jena.rdf.model.Model
 * @see jakarta.ws.rs.ext.MessageBodyReader
 * @see jakarta.ws.rs.ext.MessageBodyWriter
 */
@Provider
public class ModelProvider implements MessageBodyReader<Model>, MessageBodyWriter<Model>
{    
    private static final Logger log = LoggerFactory.getLogger(ModelProvider.class);

    public static final String REQUEST_URI_HEADER = "X-Request-URI";
    public static final String RESPONSE_URI_HEADER = "X-Response-URI";
    
    @Context UriInfo uriInfo;
    
    // READER
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        if (lang == null) return false;
        return type == Model.class && RDFParserRegistry.isRegistered(lang) && RDFLanguages.isTriples(lang);
    }

    @Override
    public Model readFrom(Class<Model> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Reading Model with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
        
        Model model = ModelFactory.createDefaultModel();

        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString()); // cannot be null - isReadable() checks that
        if (log.isDebugEnabled()) log.debug("RDF language used to read Model: {}", lang);
        
        String baseURI = null;
        // attempt to retrieve base URI from a special-purpose header (workaround for JAX-RS 1.x limitation)
        if (httpHeaders.containsKey(REQUEST_URI_HEADER)) baseURI = httpHeaders.getFirst(REQUEST_URI_HEADER);
        if (getUriInfo() != null) baseURI = getUriInfo().getAbsolutePath().toString();

        return read(model, entityStream, lang, baseURI); // extract base URI from httpHeaders?
    }

    public Model read(Model model, InputStream is, Lang lang, String baseURI)
    {
        ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStrict; // throw exceptions on all parse errors
        return read(model, is, lang, baseURI, errorHandler);
    }
    
    public Model read(Model model, InputStream is, Lang lang, String baseURI, ErrorHandler errorHandler)
    {
        if (model == null) throw new IllegalArgumentException("Model must be not null");
        if (is == null) throw new IllegalArgumentException("InputStream must be not null");
        if (lang == null) throw new IllegalArgumentException("Lang must be not null");

        RDFParser parser = RDFParser.create().
            lang(lang).
            errorHandler(errorHandler).
            checking(true). // otherwise exceptions will not be thrown for invalid URIs!
            base(baseURI).
            source(is).
            build();
        
        parser.parse(StreamRDFLib.graph(model.getGraph()));
        
        return model;
    }
    
    // WRITER
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        if (lang == null) return false;
        return Model.class.isAssignableFrom(type) && RDFWriterRegistry.contains(lang) && RDFLanguages.isTriples(lang);
    }

    @Override
    public long getSize(Model model, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(Model model, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Writing Model with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);

        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString()); // cannot be null - isWritable() checks that
        if (log.isDebugEnabled()) log.debug("RDF language used to read Model: {}", lang);
        
        String baseURI = null;
        // attempt to retrieve base URI from a special-purpose header (workaround for JAX-RS 1.x limitation)
        if (httpHeaders.containsKey(RESPONSE_URI_HEADER)) baseURI = httpHeaders.getFirst(RESPONSE_URI_HEADER).toString();
        if (getUriInfo() != null) baseURI = getUriInfo().getAbsolutePath().toString();

        write(model, entityStream, lang, baseURI);
    }

    public Model write(Model model, OutputStream os, Lang lang, String baseURI)
    {
        if (model == null) throw new IllegalArgumentException("Model must be not null");
        if (os == null) throw new IllegalArgumentException("OutputStream must be not null");
        if (lang == null) throw new IllegalArgumentException("Lang must be not null");

        String syntax = lang.getName();
        if (log.isDebugEnabled()) log.debug("Syntax used to write Model: {}", syntax);
        
        return model.write(os, syntax);
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}
