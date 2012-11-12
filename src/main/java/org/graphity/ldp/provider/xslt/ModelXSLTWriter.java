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
package org.graphity.ldp.provider.xslt;

import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.spi.resource.Singleton;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import org.graphity.util.XSLTBuilder;
import org.openjena.riot.WebContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
@Singleton
@Produces({MediaType.APPLICATION_XHTML_XML})
public class ModelXSLTWriter implements MessageBodyWriter<Model>
{
    private static final Logger log = LoggerFactory.getLogger(ModelXSLTWriter.class);

    //private XSLTBuilder builder = null;
    private Source stylesheet = null;
    private URIResolver resolver = null;
	
    @Context private UriInfo uriInfo;
    
    public ModelXSLTWriter(Source stylesheet, URIResolver resolver) throws TransformerConfigurationException
    {
	if (stylesheet == null) throw new IllegalArgumentException("XSLT stylesheet Source cannot be null");
	if (resolver == null) throw new IllegalArgumentException("URIResolver cannot be null");
	this.stylesheet = stylesheet;
	this.resolver = resolver;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
	return Model.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Model model, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
	return -1;
    }

    @Override
    public void writeTo(Model model, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
    {
	if (log.isTraceEnabled()) log.trace("Writing Model with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);

	try
	{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    model.write(baos, WebContent.langRDFXML);

	    // create XSLTBuilder per request output to avoid document() caching
	    XSLTBuilder builder = XSLTBuilder.fromStylesheet(stylesheet).resolver(resolver);
	    //builder.getTransformer().clearParameters(); // remove previously set param values

	    builder.document(new ByteArrayInputStream(baos.toByteArray())).
		//parameter("uri", UriBuilder.fromUri(resource.getURI()).build()). // URI can be retrieved from OntResource
		parameter("base-uri", uriInfo.getBaseUri()).
		parameter("absolute-path", uriInfo.getAbsolutePath()).
		parameter("http-headers", httpHeaders.toString()).
		result(new StreamResult(entityStream));
	    
	    if (uriInfo.getQueryParameters().getFirst("lang") != null)
		builder.parameter("lang", uriInfo.getQueryParameters().getFirst("lang"));
	    if (uriInfo.getQueryParameters().getFirst("mode") != null)
		builder.parameter("mode", UriBuilder.fromUri(uriInfo.getQueryParameters().getFirst("mode")).build());
	    if (uriInfo.getQueryParameters().getFirst("query") != null)
		builder.parameter("query", uriInfo.getQueryParameters().getFirst("query"));

	    builder.transform();
	    baos.close();
	}
	catch (TransformerException ex)
	{
	    log.error("XSLT transformation failed", ex);
	    throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
	}
    }

}