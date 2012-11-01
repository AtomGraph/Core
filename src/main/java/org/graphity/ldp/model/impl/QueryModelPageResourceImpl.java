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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.graphity.ldp.model.PageResource;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.graphity.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryModelPageResourceImpl extends QueryModelModelResourceImpl implements PageResource
{
    private static final Logger log = LoggerFactory.getLogger(QueryModelPageResourceImpl.class);
    
    private UriInfo uriInfo = null;
    private Long limit = null;
    private Long offset = null;
    private String orderBy = null;
    private Boolean desc = true;
    
    public QueryModelPageResourceImpl(Model queryModel, Query query,
	UriInfo uriInfo, Request request, MediaType mediaType,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(queryModel, query, request, mediaType);
	if (uriInfo == null) throw new IllegalArgumentException("UriInfo must be not null");
	if (limit == null) throw new IllegalArgumentException("LIMIT must be not null");
	if (offset == null) throw new IllegalArgumentException("OFFSET must be not null");
	if (orderBy == null) throw new IllegalArgumentException("ORDER BY must be not null");
	if (desc == null) throw new IllegalArgumentException("DESC must be not null");
	this.uriInfo = uriInfo;
	this.limit = limit;
	this.offset = offset;
	this.orderBy = orderBy;
	this.desc = desc;
	
	// add links to previous/next page (HATEOS)
	if (getOffset() >= getLimit()) getCurrent().addProperty(XHV.prev, getPrevious());
	if (getModel().size() >= getLimit()) getCurrent().addProperty(XHV.next, getNext());
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

    @Override
    public String getURI()
    {
	if (getOffset() == 0) return getUriInfo().getAbsolutePath().toString(); // hack

	return getUriInfo().getAbsolutePathBuilder().
		queryParam("limit", getLimit()).
		queryParam("offset", getOffset()).
		queryParam("order-by", getOrderBy()).
		queryParam("desc", getDesc()).
		build().toString();
    }

    public final com.hp.hpl.jena.rdf.model.Resource getCurrent()
    {
	return getModel().createResource(getUriInfo().getAbsolutePathBuilder().
		queryParam("limit", getLimit()).
		queryParam("offset", getOffset()).
		queryParam("order-by", getOrderBy()).
		queryParam("desc", getDesc()).
		build().toString());	
    }

    @Override
    public final com.hp.hpl.jena.rdf.model.Resource getPrevious()
    {
	return getModel().createResource(getUriInfo().getAbsolutePathBuilder().
		queryParam("limit", getLimit()).
		queryParam("offset", getOffset() - getLimit()).
		queryParam("order-by", getOrderBy()).
		queryParam("desc", getDesc()).
		build().toString());
    }

    @Override
    public final com.hp.hpl.jena.rdf.model.Resource getNext()
    {
	return getModel().createResource(getUriInfo().getAbsolutePathBuilder().
		queryParam("limit", getLimit()).
		queryParam("offset", getOffset() + getLimit()).
		queryParam("order-by", getOrderBy()).
		queryParam("desc", getDesc()).
		build().toString());
    }


    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

}