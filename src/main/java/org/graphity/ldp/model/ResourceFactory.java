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

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.graphity.ldp.model.impl.EndpointPageResourceImpl;
import org.graphity.ldp.model.impl.QueryModelPageResourceImpl;
import org.graphity.ldp.model.query.impl.EndpointModelResourceImpl;
import org.graphity.ldp.model.query.impl.EndpointResultSetResourceImpl;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.graphity.ldp.model.query.impl.QueryModelResultSetResourceImpl;
import org.graphity.util.QueryBuilder;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.SIOC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.vocabulary.SP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ResourceFactory
{
    private static final Logger log = LoggerFactory.getLogger(ResourceFactory.class);

    public static LinkedDataResource getLinkedDataResource(String endpointUri, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	return new EndpointModelResourceImpl(endpointUri, query, uriInfo, req, mediaType);
    }
    
    public static LinkedDataResource getLinkedDataResource(Model queryModel, Query query,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	return new QueryModelModelResourceImpl(queryModel, query, uriInfo, req, mediaType);
    }

    public static LinkedDataResource getLinkedDataResource(Model queryModel, String uri,
	    UriInfo uriInfo, Request req,
	    MediaType mediaType)
    {
	return new QueryModelModelResourceImpl(queryModel, uri, uriInfo, req, mediaType);
    }

    public static LinkedDataResource getLinkedDataResource(Model queryModel, Query query,
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

    public static LinkedDataResource getLinkedDataResource(OntResource ontResource,
	    UriInfo uriInfo, Request request, MediaType mediaType)
    {
	if (ontResource == null) throw new IllegalArgumentException("Resource cannot be null");
	com.hp.hpl.jena.rdf.model.Resource queryResource = ontResource.getPropertyResourceValue(Graphity.query);
	com.hp.hpl.jena.rdf.model.Resource service = ontResource.getPropertyResourceValue(Graphity.service);
	
	QueryBuilder queryBuilder = null;
	if (queryResource != null) queryBuilder = QueryBuilder.fromResource(queryResource); // CONSTRUCT
	else queryBuilder = QueryBuilder.fromDescribe(ontResource.getURI()); // default DESCRIBE	
	Query query = queryBuilder.build();

	if (service != null)
	{
	    String endpointUri = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		createProperty("http://www.w3.org/ns/sparql-service-description#endpoint")).getURI();
	    	return getLinkedDataResource(endpointUri, query, uriInfo, request, mediaType);
	}
	else
	    return getLinkedDataResource(ontResource.getModel(), query, uriInfo, request, mediaType);
    }
 
    public static LinkedDataResource getLinkedDataResource(OntResource ontResource,
	UriInfo uriInfo, Request request, MediaType mediaType,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (ontResource == null) throw new IllegalArgumentException("Resource cannot be null");
	if (ontResource.hasRDFType(SIOC.CONTAINER))
	{
	    com.hp.hpl.jena.rdf.model.Resource queryResource = ontResource.getPropertyResourceValue(Graphity.query);
	    com.hp.hpl.jena.rdf.model.Resource service = ontResource.getPropertyResourceValue(Graphity.service);

	    if (queryResource == null || !(SPINFactory.asQuery(queryResource) instanceof Select))
		throw new IllegalArgumentException("PageResource must have a SELECT query");

	    QueryBuilder selectBuilder = QueryBuilder.fromResource(queryResource).
		limit(limit).offset(offset);
	    /*
	    if (orderBy != null)
	    {
		com.hp.hpl.jena.rdf.model.Resource modelVar = getOntology().createResource().addLiteral(SP.varName, "model");
		Property orderProperty = ResourceFactory.createProperty(orderBy);
		com.hp.hpl.jena.rdf.model.Resource orderVar = getOntology().createResource().addLiteral(SP.varName, orderProperty.getLocalName());

		sb.orderBy(orderVar, desc).optional(modelVar, orderProperty, orderVar);
	    }
	    */

	    QueryBuilder queryBuilder = null;
	    if (queryResource.getPropertyResourceValue(SP.resultVariables) != null)
		queryBuilder = QueryBuilder.fromDescribe(queryResource.getPropertyResourceValue(SP.resultVariables)).
		    subQuery(selectBuilder);
	    else
		queryBuilder = QueryBuilder.fromDescribe().subQuery(selectBuilder);
	    Query query = queryBuilder.build();

	    if (service != null)
	    {
		String endpointUri = service.getPropertyResourceValue(com.hp.hpl.jena.rdf.model.ResourceFactory.
		    createProperty("http://www.w3.org/ns/sparql-service-description#endpoint")).getURI();
		    return getLinkedDataResource(endpointUri, query, uriInfo, request, mediaType,
			    limit, offset, orderBy, desc);
	    }
	    else
		return getLinkedDataResource(ontResource.getModel(), query, uriInfo, request, mediaType,
			limit, offset, orderBy, desc);
	}
	
	return getLinkedDataResource(ontResource, uriInfo, request, mediaType);
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