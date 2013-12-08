/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.provider;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.NoReaderForLangException;
import com.hp.hpl.jena.shared.NoWriterForLangException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads RDF from request body or writes RDF to response.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.server.ApplicationBase
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Jena Model</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyReader.html">JAX-RS MessageBodyReader</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyWriter.html">JAX-RS MessageBodyWriter</a>
 */
@Provider
//@Consumes({MediaType.APPLICATION_RDF_XML, MediaType.TEXT_TURTLE, MediaType.TEXT_PLAIN})
//@Produces({MediaType.APPLICATION_RDF_XML, MediaType.TEXT_TURTLE, MediaType.TEXT_PLAIN})
public class ModelProvider implements MessageBodyReader<Model>, MessageBodyWriter<Model>
{    
    private static final Logger log = LoggerFactory.getLogger(ModelProvider.class);

    public Lang getCompatibleLang(MediaType mediaType)
    {
        Iterator<Lang> it = RDFLanguages.getRegisteredLanguages().iterator();
        
        while (it.hasNext())
        {
            Lang lang = it.next();
            ContentType ct = lang.getContentType();
            if (MediaType.valueOf(ct.getContentType()).isCompatible(mediaType)) return lang;
        }
        
        return null;
    }
    
    public boolean isMediaTypeCompatible(MediaType mediaType)
    {
        return getCompatibleLang(mediaType) != null;
    }
    
    // READER
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
        return type == Model.class && RDFLanguages.contentTypeToLang(mediaType.toString()) != null;
        //&& isMediaTypeCompatible(mediaType);
    }

    @Override
    public Model readFrom(Class<Model> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
    {
	if (log.isTraceEnabled()) log.trace("Reading Model with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
	
	Model model = ModelFactory.createDefaultModel();	

        Lang lang = RDFLanguages.contentTypeToLang(mediaType.toString());
        if (lang == null)
        {
            Throwable ex = new NoReaderForLangException("Media type not supported");
            if (log.isErrorEnabled()) log.error("MediaType {} not supported by Jena", mediaType);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
	String syntax = lang.getName();
	if (log.isDebugEnabled()) log.debug("Syntax used to read Model: {}", syntax);

	// extract base URI from httpHeaders?
	return model.read(entityStream, null, syntax);
    }
    
    // WRITER
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
        return Model.class.isAssignableFrom(type) && RDFLanguages.contentTypeToLang(mediaType.toString()) != null;
                //&& isMediaTypeCompatible(mediaType);
    }

    @Override
    public long getSize(Model model, Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
	return -1;
    }

    @Override
    public void writeTo(Model model, Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
    {
	if (log.isTraceEnabled()) log.trace("Writing Model with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);

        Lang lang = RDFLanguages.contentTypeToLang(mediaType.toString());
        if (lang == null)
        {
            Throwable ex = new NoWriterForLangException("Media type not supported");
            if (log.isErrorEnabled()) log.error("MediaType {} not supported by Jena", mediaType);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
	String syntax = lang.getName();
	if (log.isDebugEnabled()) log.debug("Syntax used to write Model: {}", syntax);

	model.write(entityStream, syntax);
    }
    
}
