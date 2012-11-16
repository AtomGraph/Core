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
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.List;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.LinkedDataResourceBase;
import org.graphity.ldp.model.PageResource;
import org.graphity.util.QueryBuilder;
import org.graphity.util.SelectBuilder;
import org.graphity.vocabulary.Graphity;
import org.graphity.vocabulary.SIOC;
import org.graphity.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.vocabulary.SP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public final class LinkedDataPageResourceImpl extends LinkedDataResourceBase implements PageResource
{
    private static final Logger log = LoggerFactory.getLogger(LinkedDataPageResourceImpl.class);

    private final OntResource container;
    private final Long limit;
    private final Long offset;
    private final String orderBy;
    private final Boolean desc;

    public static SelectBuilder getSelectBuilder(Resource select, Long limit, Long offset, String orderBy, Boolean desc)
    {
	SelectBuilder selectBuilder = SelectBuilder.fromResource(select).
	    limit(limit).offset(offset);

	/*
	if (orderBy != null)
	{
	    com.hp.hpl.jena.rdf.model.Resource modelVar = getOntology().createResource().addLiteral(SP.varName, "model");
	    Property orderProperty = ResourceFactory.createProperty(orderBy);
	    com.hp.hpl.jena.rdf.model.Resource orderVar = getOntology().createResource().addLiteral(SP.varName, orderProperty.getLocalName());

	    selectBuilder.orderBy(orderVar, desc).optional(modelVar, orderProperty, orderVar);
	}
	*/

	return selectBuilder;
    }

    
    public static QueryBuilder getQueryBuilder(SelectBuilder selectBuilder)
    {
	QueryBuilder queryBuilder;

	if (selectBuilder.getPropertyResourceValue(SP.resultVariables) != null)
	{
	    if (log.isDebugEnabled()) log.debug("Query Resource {} has result variables: {}", selectBuilder, selectBuilder.getPropertyResourceValue(SP.resultVariables));
	    queryBuilder = QueryBuilder.fromDescribe(selectBuilder.getPropertyResourceValue(SP.resultVariables)).
		subQuery(selectBuilder);
	}
	else
	{
	    if (log.isDebugEnabled()) log.debug("Query Resource {} does not have result variables, using wildcard", selectBuilder);
	    queryBuilder = QueryBuilder.fromDescribe(selectBuilder.getModel()).subQuery(selectBuilder);
	}
	
	return queryBuilder;
    }
    
    public static OntResource getOntResource(OntResource container, UriInfo uriInfo,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (container == null) throw new IllegalArgumentException("Container must be not null");
	if (!container.hasProperty(Graphity.selectQuery)) throw new IllegalArgumentException("Container Resource must have a SELECT query");

	if (limit == null) throw new IllegalArgumentException("LIMIT must be not null");
	if (offset == null) throw new IllegalArgumentException("OFFSET must be not null");

	// different URI from Container!
	UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().
		replaceQueryParam("limit", limit).
		replaceQueryParam("offset", offset);
	if (orderBy != null) uriBuilder.replaceQueryParam("order-by", orderBy);
	if (desc != null) uriBuilder.replaceQueryParam("desc", desc);

	if (log.isDebugEnabled()) log.debug("Creating LinkedDataPageResource from OntResource with URI: {}", uriBuilder.build().toString());
	OntResource ontResource = container.getOntModel().createOntResource(uriBuilder.build().toString());
	
	if (log.isDebugEnabled()) log.debug("Locking ontResource.getOntModel() before QueryBuilder call");
	//synchronized (ontResource.getOntModel())
	{
	    SelectBuilder selectBuilder = getSelectBuilder(container.getPropertyResourceValue(Graphity.selectQuery),
		    limit, offset, orderBy, desc);

	    QueryBuilder queryBuilder = getQueryBuilder(selectBuilder);
	    ontResource.setPropertyValue(Graphity.query, queryBuilder); // Resource alway get a g:query value
	    
	    ontResource.setPropertyValue(Graphity.service, container.getPropertyResourceValue(Graphity.service));
	}
	if (log.isDebugEnabled()) log.debug("Unlocking ontResource.getModel()");
	
	return ontResource;
    }
    
    public LinkedDataPageResourceImpl(OntResource container,
	UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(getOntResource(container, uriInfo, limit, offset, orderBy, desc),
		getResource(getOntResource(container, uriInfo, limit, offset, orderBy, desc),
				    uriInfo, request, httpHeaders, VARIANTS,
				    limit, offset, orderBy, desc),
		uriInfo, request, httpHeaders, variants,
		limit, offset, orderBy, desc);
	if (limit == null) throw new IllegalArgumentException("LIMIT must be not null");
	if (offset == null) throw new IllegalArgumentException("OFFSET must be not null");

	this.container = container;

	this.limit = limit;
	this.offset = offset;
	this.orderBy = orderBy;
	this.desc = desc;
	
	int subjectCount = getModel().listSubjects().toList().size();
	
	if (log.isDebugEnabled())
	{
	    log.debug("OFFSET: {} LIMIT: {}", offset, limit);
	    log.debug("ORDER BY: {} DESC: {}", orderBy, desc);
	    log.debug("getModel().size(): {} getModel().listSubjects().toList().size(): {}", getModel().size(), subjectCount);
	}
	
	// add links to container, previous/next page etc (HATEOS)
	//synchronized (getOntModel())
	{
	    if (log.isDebugEnabled()) log.debug("Adding page metadata: {} sioc:has_container {}", getURI(), container.getURI());
	    getOntResource().addProperty(SIOC.HAS_CONTAINER, container);
	    	    
	    if (getOffset() >= getLimit())
	    {
		if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", getURI(), getPrevious().getURI());
		getOntResource().addProperty(XHV.prev, getPrevious());
	    }
	    if (subjectCount >= getLimit())
	    {
		if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", getURI(), getNext().getURI());
		getOntResource().addProperty(XHV.next, getNext());
	    }
	    
	    // combine page metadata with query results (then exclude it in XSLT transformation)
	    getModel().add(getOntResource().listProperties());
	}
    }
    
    @Override
    public final Long getLimit()
    {
	return limit;
    }

    @Override
    public final Long getOffset()
    {
	return offset;
    }

    @Override
    public final String getOrderBy()
    {
	return orderBy;
    }

    @Override
    public final Boolean getDesc()
    {
	return desc;
    }

    public final com.hp.hpl.jena.rdf.model.Resource getContainer()
    {
	return container;
    }
    
    @Override
    public final com.hp.hpl.jena.rdf.model.Resource getPrevious()
    {
	UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
	    replaceQueryParam("limit", getLimit()).
	    replaceQueryParam("offset", getOffset() - getLimit());

	if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
	if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());

	return getOntModel().createResource(uriBuilder.build().toString());
    }

    @Override
    public final com.hp.hpl.jena.rdf.model.Resource getNext()
    {
	UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
	    replaceQueryParam("limit", getLimit()).
	    replaceQueryParam("offset", getOffset() + getLimit());

	if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
	if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());

	return getOntModel().createResource(uriBuilder.build().toString());
    }

}