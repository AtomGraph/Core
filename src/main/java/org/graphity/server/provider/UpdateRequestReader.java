/*
 * Copyright (C) 2013 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graphity.server.provider;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.graphity.server.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads SPARQL Update from request body.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.server.ApplicationBase
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/update/UpdateRequest.html">Jena UpdateRequest</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyReader.html">JAX-RS MessageBodyReader</a>
 */
@Provider
@Consumes(MediaType.APPLICATION_SPARQL_UPDATE)
public class UpdateRequestReader implements MessageBodyReader<UpdateRequest>
{

    private static final Logger log = LoggerFactory.getLogger(UpdateRequestReader.class);

    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, javax.ws.rs.core.MediaType mt)
    {
        return type == UpdateRequest.class;
    }

    @Override
    public UpdateRequest readFrom(Class<UpdateRequest> type, Type type1, Annotation[] antns, javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream in) throws IOException, WebApplicationException
    {
	if (log.isTraceEnabled()) log.trace("Reading UpdateRequest with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
	return UpdateFactory.read(in);
    }
    
}
