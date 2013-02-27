/*
 * Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.platform.provider;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes SPARQL result set to the response
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSet.html">ResultSet</a>
 * @see <a href="http://jsr311.java.net/nonav/javadoc/javax/ws/rs/ext/MessageBodyWriter.html">MessageBodyWriter</a>
 */
@Provider
@Produces({org.graphity.platform.MediaType.APPLICATION_SPARQL_RESULTS_XML, org.graphity.platform.MediaType.APPLICATION_SPARQL_RESULTS_JSON})
public class ResultSetWriter implements MessageBodyWriter<ResultSet>
{
    private static final Logger log = LoggerFactory.getLogger(ResultSetWriter.class);
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return (ResultSet.class.isAssignableFrom(type));
    }

    @Override
    public long getSize(ResultSet t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
	return -1;
    }

    @Override
    public void writeTo(ResultSet results, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
    {
	if (mediaType.equals(org.graphity.platform.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE))
	    ResultSetFormatter.outputAsJSON(entityStream, results);
	else
	    ResultSetFormatter.outputAsXML(entityStream, results);
    }
    
}
