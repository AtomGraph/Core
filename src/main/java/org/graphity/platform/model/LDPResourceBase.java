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
package org.graphity.platform.model;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-write Linked Data resources
 * 
 * @see LDPResource
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LDPResourceBase extends LinkedDataResourceBase implements LDPResource
{    
    private static final Logger log = LoggerFactory.getLogger(LDPResourceBase.class);
    
    /**
     * Constructs read-write LD resource from Jena's OntResource and JAX-RS context
     * 
     * @param resource the current resource in the ontology
     * @param uriInfo URI information
     * @param request current request
     * @param httpHeaders current request headers
     * @param variants representation variants
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Resource.html">Resource</a>
     */
    public LDPResourceBase(Resource resource,
	    UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants, CacheControl cacheControl)
    {
	super(resource, uriInfo, request, httpHeaders, variants, cacheControl);
    }

    @Override
    /**
     * @link <a href="http://lists.w3.org/Archives/Public/public-ldp-wg/2012Oct/0181.html">What is the document base URI of a POSTed document?</a>
     * 
     */
    public Response post(Model model)
    {
	throw new WebApplicationException(405);
	
	//getOntResource().getOntModel().add(model);
	
	//return Response.created(null).build();
    }

    @Override
    public Response put(Model model)
    {
	throw new WebApplicationException(405);
	
	// if (getService() != null) DataManager.get().putModel(endpointUri, getUriInfo().getAbsolutePath(), model);

	//getOntResource().getOntModel().add(model);
	
	//return Response.created(getUriInfo().getAbsolutePath()).build();
    }

    @Override
    public Response delete()
    {
	throw new WebApplicationException(405);

	// if (getService() != null) DataManager.get().deleteModel(endpointUri, getUriInfo().getAbsolutePath()
	
	//getOntResource().remove();
	
	//return Response.noContent().build(); // 204 No Content
	// 410 Gone if provenance shows previous versions: http://www.w3.org/TR/chips/#cp4.2
    }

}