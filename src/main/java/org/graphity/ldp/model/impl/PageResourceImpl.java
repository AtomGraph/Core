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
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.LinkedDataResource;
import org.graphity.ldp.model.LinkedDataResourceBase;
import org.graphity.ldp.model.PageResource;
import org.graphity.ldp.model.ResourceBase;
import org.graphity.util.QueryBuilder;
import org.graphity.util.SelectBuilder;
import org.graphity.vocabulary.LDP;
import org.graphity.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.vocabulary.SP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public final class PageResourceImpl extends ResourceBase implements PageResource
{
    private static final Logger log = LoggerFactory.getLogger(PageResourceImpl.class);
    
    public PageResourceImpl(OntResource ontResource,
	UriInfo uriInfo, Request request, HttpHeaders httpHeaders, List<Variant> variants,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(ontResource, uriInfo, request, httpHeaders, variants, limit, offset, orderBy, desc);
	if (limit == null) throw new IllegalArgumentException("LIMIT must be not null");
	if (offset == null) throw new IllegalArgumentException("OFFSET must be not null");

	OntResource container = getOntModel().createOntResource(getUriInfo().getAbsolutePath().toString());
	if (log.isDebugEnabled()) log.debug("Adding PageResource metadata: {} sioc:has_parent {}", getOntResource(), container);
	setPropertyValue(LDP.pageOf, container);

	if (log.isDebugEnabled())
	{
	    log.debug("OFFSET: {} LIMIT: {}", getOffset(), getLimit());
	    log.debug("ORDER BY: {} DESC: {}", getOrderBy(), getDesc());
	}

	if (getOffset() >= getLimit())
	{
	    if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", getURI(), getPrevious().getURI());
	    addProperty(XHV.prev, getPrevious());
	}

	// no way to know if there's a next page without counting results (either total or in current page)
	//int subjectCount = describe().listSubjects().toList().size();
	//log.debug("describe().listSubjects().toList().size(): {}", subjectCount);
	//if (subjectCount >= getLimit())
	{
	    if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", getURI(), getNext().getURI());
	    addProperty(XHV.next, getNext());
	}
    }

    @Override
    public Model describe()
    {
	Model description = super.describe();

	 // add description of ldp:Container
	OntResource container = getPropertyResourceValue(LDP.pageOf).as(OntResource.class);
	LinkedDataResource ldc = new LinkedDataResourceBase(container, getUriInfo(), getRequest(), getHttpHeaders(), getVariants());
	description.add(ldc.describe());

	return description;
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

    @Override
    public Query getQuery(TemplateCall call)
    {
	if (call == null) throw new IllegalArgumentException("TemplateCall cannot be null");
	QueryBuilder queryBuilder;
	org.topbraid.spin.model.Query query = ARQ2SPIN.parseQuery(call.getQueryString(), getModel());
	if (!(query instanceof Select)) throw new IllegalArgumentException("PageResource must have a SPIN Select query");
	SelectBuilder selectBuilder = SelectBuilder.fromSelect((Select)query).
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
	//QueryBuilder queryBuilder;
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
	return queryBuilder.build();
    }

}