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

import com.hp.hpl.jena.ontology.OntModel;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.graphity.ldp.model.impl.PageResourceBase;
import org.graphity.ldp.model.impl.ResourceBase;
import org.graphity.vocabulary.SIOC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ResourceFactory extends org.graphity.model.ResourceFactory
{
    private static final Logger log = LoggerFactory.getLogger(ResourceFactory.class);
    
    public static Resource getResource(OntModel ontology, UriInfo uriInfo, Request req)
    {
	log.debug("Creating Resource");
	    
	return new ResourceBase(ontology, uriInfo, req);
    }
 
    public static Resource getResource(OntModel ontology, UriInfo uriInfo, Request req,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	Resource resource = getResource(ontology, uriInfo, req);

	if (resource.getOntResource() != null && resource.getOntResource().hasRDFType(SIOC.CONTAINER))
	{
	    log.debug("Creating ContainerResource");
	    return new PageResourceBase(resource, limit, offset, orderBy, desc);
	}
	
	return resource;
    }

}