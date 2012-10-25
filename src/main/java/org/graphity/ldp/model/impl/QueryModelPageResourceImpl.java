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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.graphity.ldp.model.PageResource;
import org.graphity.ldp.model.query.impl.QueryModelModelResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryModelPageResourceImpl extends QueryModelModelResourceImpl implements PageResource
{
    private static final Logger log = LoggerFactory.getLogger(QueryModelPageResourceImpl.class);
    
    private Long limit = null;
    private Long offset = null;
    private String orderBy = null;
    private Boolean desc = true;
    
    public QueryModelPageResourceImpl(Model queryModel, Query query,
	UriInfo uriInfo, Request request, MediaType mediaType,
	Long limit, Long offset, String orderBy, Boolean desc)
    {
	super(queryModel, query, uriInfo, request, mediaType);
	this.limit = limit;
	this.offset = offset;
	this.orderBy = orderBy;
	this.desc = desc;
    }

    /*
    public PageResourceBase(Resource resource,
	    Long limit, Long offset, String orderBy, Boolean desc)
    {
	this(resource.getOntology(), resource.getUriInfo(), resource.getRequest(),
		limit, offset, orderBy, desc);
    }
    */
    
    /*
    @Override
    public Model getModel()
    {
	Model model = super.getModel();
	
	// http://code.google.com/p/linked-data-api/wiki/API_Viewing_Resources#Page_Description
	if (getPreviousResource() != null) model.add(getOntResource(), XHV.prev, getPreviousResource());
	if (getNextResource() != null) model.add(getOntResource(), XHV.prev, getNextResource());

	return model;
    }
    */
    
    @Override
    public Long getLimit()
    {
	return limit;
    }

    @Override
    public Long getOffset()
    {
	return offset;
    }

    @Override
    public String getOrderBy()
    {
	return orderBy;
    }

    @Override
    public Boolean getDesc()
    {
	return desc;
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource getPrevious()
    {
	if (getOffset() >= getLimit())
	    return getModel().getResource(UriBuilder.fromUri(getURI()).
		queryParam("limit", getLimit()).
		queryParam("offset", getOffset() - getLimit()).
		queryParam("order-by", getOrderBy()).
		queryParam("desc", getDesc()).
		build().toString());
	else
	    return null;
    }
    
    @Override
    public com.hp.hpl.jena.rdf.model.Resource getNext()
    {
	if (getModel().size() < getLimit())
	    return null;
	else
	    return getModel().getResource(UriBuilder.fromUri(getURI()).
		queryParam("limit", getLimit()).
		queryParam("offset", getOffset() + getLimit()).
		queryParam("order-by", getOrderBy()).
		queryParam("desc", getDesc()).
		build().toString());
    }

}