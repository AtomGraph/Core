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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.NoReaderForLangException;
import org.apache.jena.shared.NoWriterForLangException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDFLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for reading RDF model from request and writing it to response.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Jena Model</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyReader.html">JAX-RS MessageBodyReader</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyWriter.html">JAX-RS MessageBodyWriter</a>
 */
@Provider
public class ModelProvider implements MessageBodyReader<Model>, MessageBodyWriter<Model>
{    
    private static final Logger log = LoggerFactory.getLogger(ModelProvider.class);

    public boolean isRDFMediaType(MediaType mediaType)
    {
        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        return RDFLanguages.contentTypeToLang(formatType.toString()) != null;
    }
    
    // READER
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return type == Model.class && isRDFMediaType(mediaType);
    }

    @Override
    public Model readFrom(Class<Model> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException
    {
	if (log.isTraceEnabled()) log.trace("Reading Model with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
	
	Model model = ModelFactory.createDefaultModel();	

        MediaType formatType = new MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param        
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        if (lang == null)
        {
            if (log.isErrorEnabled()) log.error("MediaType '{}' not supported by Jena", formatType);
            throw new NoReaderForLangException("MediaType not supported: " + formatType);
        }
	if (log.isDebugEnabled()) log.debug("RDF language used to read Model: {}", lang);
        
        return read(model, entityStream, lang, null); // extract base URI from httpHeaders?
    }

    public Model read(Model model, InputStream is, Lang lang, String baseURI)
    {
        ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStrict; // throw exceptions on all parse errors
        ParserProfile parserProfile = RiotLib.profile(baseURI, true, true, errorHandler);
        return read(model, is, lang, baseURI, errorHandler, parserProfile);
    }
    
    public Model read(Model model, InputStream is, Lang lang, String baseURI, ErrorHandler errorHandler, ParserProfile parserProfile)
    {
	if (model == null) throw new IllegalArgumentException("Model must be not null");        
	if (is == null) throw new IllegalArgumentException("InputStream must be not null");        
	if (lang == null) throw new IllegalArgumentException("Lang must be not null");        

        ReaderRIOT parser = RDFDataMgr.createReader(lang);
        parser.setErrorHandler(errorHandler);
        parser.setParserProfile(parserProfile);
        parser.read(is, baseURI, null, StreamRDFLib.graph(model.getGraph()), null);
        
        return model;
    }
    
    // WRITER
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return Model.class.isAssignableFrom(type) && isRDFMediaType(mediaType);
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
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        if (lang == null)
        {
            if (log.isErrorEnabled()) log.error("MediaType '{}' not supported by Jena", formatType);
            throw new NoWriterForLangException("MediaType not supported: " + formatType);
        }
	if (log.isDebugEnabled()) log.debug("RDF language used to read Model: {}", lang);
        
	write(model, entityStream, lang, null);
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
    
}
