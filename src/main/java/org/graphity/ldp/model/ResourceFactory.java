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

import org.graphity.ldp.model.query.impl.QueryModelResultSetResourceImpl;
import org.graphity.ldp.model.query.impl.EndpointModelResourceImpl;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.graphity.ldp.model.query.impl.EndpointResultSetResourceImpl;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.graphity.ldp.model.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ResourceFactory
{
    private static final Logger log = LoggerFactory.getLogger(ResourceFactory.class);
    
    public static LDPResource getResource(OntModel ontology, UriInfo uriInfo, Request req)
    {
	log.debug("Creating Resource");
	    
	return new ResourceBase(ontology, uriInfo, req);
    }
 
    public static LDPResource getResource(OntModel ontology, UriInfo uriInfo, Request req,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	LDPResource resource = getResource(ontology, uriInfo, req);

	if (resource.isContainer())
	{
	    log.debug("Creating ContainerResource");
	    //return new PageResourceBase(resource, limit, offset, orderBy, desc);
	    return new PageResourceBase(ontology, uriInfo, req, limit, offset, orderBy, desc);
	}
	
	return resource;
    }

    public static Resource getResource(String endpointUri, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	if (query.isDescribeType() || query.isConstructType()) return new EndpointModelResourceImpl(endpointUri, query, uriInfo, req, mediaType);
	if (query.isSelectType()) return new EndpointResultSetResourceImpl(endpointUri, query, uriInfo, req, mediaType);

	return null;
    }
    
    public static Resource getResource(Model queryModel, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	if (query.isDescribeType() || query.isConstructType()) return new QueryModelModelResourceImpl(queryModel, query, uriInfo, req, mediaType);
	if (query.isSelectType()) return new QueryModelResultSetResourceImpl(queryModel, query, uriInfo, req, mediaType);

	return null;
    }

}