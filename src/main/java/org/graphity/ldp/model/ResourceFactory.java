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
package org.graphity.ldp.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.graphity.ldp.model.impl.EndpointPageResourceImpl;
import org.graphity.ldp.model.impl.LDPResourceBase;
import org.graphity.ldp.model.impl.QueryModelPageResourceImpl;
import org.graphity.ldp.model.query.impl.EndpointModelResourceImpl;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ResourceFactory extends org.graphity.model.ResourceFactory
{
    private static final Logger log = LoggerFactory.getLogger(ResourceFactory.class);

    public static LinkedDataResource getResource(String endpointUri, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	return new EndpointModelResourceImpl(endpointUri, query, uriInfo, req, mediaType);
    }
    
    public static LinkedDataResource getResource(Model queryModel, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	return new QueryModelModelResourceImpl(queryModel, query, uriInfo, req, mediaType);
    }

    public static LinkedDataResource getResource(Model queryModel, String uri,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	return new QueryModelModelResourceImpl(queryModel, uri, uriInfo, req, mediaType);
    }

    public static LinkedDataResource getResource(Model queryModel, Query query,
	    UriInfo uriInfo, Request request, MediaType mediaType,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	return new QueryModelPageResourceImpl(queryModel, query, uriInfo, request, mediaType,
		limit, offset, orderBy, desc);
    }

    public static LinkedDataResource getLinkedDataResource(String endpointUri, Query query,
	    UriInfo uriInfo, Request request, MediaType mediaType,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	return new EndpointPageResourceImpl(endpointUri, query, uriInfo, request, mediaType,
		limit, offset, orderBy, desc);
    }

    public static LDPResource getResource(LinkedDataResource resource)
    {
	return new LDPResourceBase(resource);
    }

}