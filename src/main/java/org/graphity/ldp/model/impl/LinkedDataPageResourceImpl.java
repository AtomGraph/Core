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

    private OntResource ontResource = null;
    private QueryBuilder queryBuilder = null;
    private SelectBuilder selectBuilder = null;
    
    public LinkedDataPageResourceImpl(OntResource container,
	UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(container, uriInfo, request, httpHeaders, variants, limit, offset, orderBy, desc);
	if (limit == null) throw new IllegalArgumentException("LIMIT must be not null");
	if (offset == null) throw new IllegalArgumentException("OFFSET must be not null");

	if (!container.hasProperty(Graphity.selectQuery)) throw new IllegalArgumentException("Container Resource must have a SELECT query");

	int subjectCount = describe().listSubjects().toList().size();	
	if (log.isDebugEnabled())
	{
	    log.debug("OFFSET: {} LIMIT: {}", getOffset(), getLimit());
	    log.debug("ORDER BY: {} DESC: {}", getOrderBy(), getDesc());
	    log.debug("describe().listSubjects().toList().size(): {}", subjectCount);
	}
	
	// add links to container, previous/next page etc (HATEOS)
	if (log.isDebugEnabled()) log.debug("Adding page metadata: {} sioc:has_container {}", getURI(), container.getURI());
	addProperty(SIOC.HAS_CONTAINER, container);

	if (getOffset() >= getLimit())
	{
	    if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", getURI(), getPrevious().getURI());
	    addProperty(XHV.prev, getPrevious());
	}
	if (subjectCount >= getLimit())
	{
	    if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", getURI(), getNext().getURI());
	    addProperty(XHV.next, getNext());
	}
    }

    @Override
    public OntResource getOntResource()
    {
	if (ontResource == null)
	{
	    // different URI from Container!
	    UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().
		    replaceQueryParam("limit", getLimit()).
		    replaceQueryParam("offset", getOffset());
	    if (getOrderBy() != null) uriBuilder.replaceQueryParam("order-by", getOrderBy());
	    if (getDesc() != null) uriBuilder.replaceQueryParam("desc", getDesc());

	    if (log.isDebugEnabled()) log.debug("Creating LinkedDataPageResource from OntResource with URI: {}", uriBuilder.build().toString());
	    ontResource = super.getOntResource().getOntModel().createOntResource(uriBuilder.build().toString());

	    ontResource.setPropertyValue(Graphity.query, getQueryBuilder()); // Resource alway get a g:query value
	    ontResource.setPropertyValue(Graphity.service, super.getOntResource().getPropertyResourceValue(Graphity.service));
	}
	
	return ontResource;
    }

    public SelectBuilder getSelectBuilder()
    {
	if (selectBuilder == null)
	{
	    Resource select = super.getOntResource().getPropertyResourceValue(Graphity.selectQuery);

	    selectBuilder = SelectBuilder.fromResource(select).
		limit(getLimit()).offset(getOffset());

	    /*
	    if (orderBy != null)
	    {
		com.hp.hpl.jena.rdf.model.Resource modelVar = getOntology().createResource().addLiteral(SP.varName, "model");
		Property orderProperty = ResourceFactory.createProperty(getOrderBy();
		com.hp.hpl.jena.rdf.model.Resource orderVar = getOntology().createResource().addLiteral(SP.varName, orderProperty.getLocalName());

		selectBuilder.orderBy(orderVar, getDesc()).optional(modelVar, orderProperty, orderVar);
	    }
	    */
	}
	
	return selectBuilder;
    }
    
    @Override
    public QueryBuilder getQueryBuilder()
    {
	if (queryBuilder == null)
	{
	    if (getSelectBuilder().getPropertyResourceValue(SP.resultVariables) != null)
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} has result variables: {}", getSelectBuilder(), getSelectBuilder().getPropertyResourceValue(SP.resultVariables));
		queryBuilder = QueryBuilder.fromDescribe(getSelectBuilder().getPropertyResourceValue(SP.resultVariables)).
		    subQuery(getSelectBuilder());
	    }
	    else
	    {
		if (log.isDebugEnabled()) log.debug("Query Resource {} does not have result variables, using wildcard", selectBuilder);
		queryBuilder = QueryBuilder.fromDescribe(getSelectBuilder().getModel()).subQuery(getSelectBuilder());
	    }
	}
	
	return queryBuilder;
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