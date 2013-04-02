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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.graphity.server.MediaType;
import org.openjena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads RDF from request body or writes RDF to response
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Model</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyReader.html">MessageBodyReader</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyWriter.html">MessageBodyWriter</a>
 */
@Provider
@Consumes({MediaType.APPLICATION_RDF_XML, MediaType.TEXT_TURTLE, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_RDF_XML, MediaType.TEXT_TURTLE, MediaType.TEXT_PLAIN})
public class ModelProvider implements MessageBodyReader<Model>, MessageBodyWriter<Model>
{
    
    /**
     * Supported RDF syntaxes
     * 
     * @see org.graphity.ldp.MediaType
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/org/openjena/riot/lang/package-summary.html">RIOT</a>
     */
    public static final Map<String, Lang> LANGS = new HashMap<String, Lang>();
    static
    {
        LANGS.put(MediaType.APPLICATION_RDF_XML, Lang.RDFXML);
        LANGS.put(MediaType.TEXT_TURTLE, Lang.TURTLE);
        LANGS.put(MediaType.TEXT_PLAIN, Lang.TURTLE);
    }    
    private static final Logger log = LoggerFactory.getLogger(ModelProvider.class);

    // READER
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
        return type == Model.class;
    }

    @Override
    public Model readFrom(Class<Model> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
    {
	if (log.isTraceEnabled()) log.trace("Reading Model with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
	
	Model model = ModelFactory.createDefaultModel();
	
	String syntax = null;
	Lang lang = langFromMediaType(mediaType);
	if (lang != null) syntax = lang.getName();
	if (log.isDebugEnabled()) log.debug("Syntax used to read Model: {}", syntax);

	// extract base URI from httpHeaders?
	return model.read(entityStream, null, syntax);
    }
    
    public static Lang langFromMediaType(javax.ws.rs.core.MediaType mediaType)
    { 
        if (mediaType == null) return null;
	if (log.isTraceEnabled()) log.trace("langFromMediaType({}): {}", mediaType.getType() + "/" + mediaType.getSubtype(), LANGS.get(mediaType.getType() + "/" + mediaType.getSubtype()));
        return LANGS.get(mediaType.getType() + "/" + mediaType.getSubtype());
    }

    // WRITER
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType)
    {
        return Model.class.isAssignableFrom(type);
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
	String syntax = null;
	Lang lang = langFromMediaType(mediaType);
	if (lang != null) syntax = lang.getName();
	if (log.isDebugEnabled()) log.debug("Syntax used to write Model: {}", syntax);

	model.write(entityStream, syntax);
    }
    
}
