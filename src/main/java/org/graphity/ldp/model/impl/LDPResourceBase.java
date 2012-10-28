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
package org.graphity.ldp.model.impl;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LDPResourceBase extends LinkedDataResourceBase implements org.graphity.ldp.model.LDPResource
{
    private static final Logger log = LoggerFactory.getLogger(LDPResourceBase.class);

    public LDPResourceBase(OntResource ontResource, UriInfo uriInfo, Request request, MediaType mediaType, Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(ontResource, uriInfo, request, mediaType, limit, offset, orderBy, desc);
    }
    
    @Override
    public Response post(Model model)
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Response put(Model model)
    {
	throw new WebApplicationException(405);
    }

    @Override
    public Response delete()
    {
	throw new WebApplicationException(405);
    }

}