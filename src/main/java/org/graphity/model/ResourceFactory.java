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
package org.graphity.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import org.graphity.model.impl.LinkedDataResourceImpl;
import org.graphity.model.query.ModelResource;
import org.graphity.model.query.ResultSetResource;
import org.graphity.model.query.impl.EndpointModelResourceImpl;
import org.graphity.model.query.impl.EndpointResultSetResourceImpl;
import org.graphity.model.query.impl.QueryModelModelResourceImpl;
import org.graphity.model.query.impl.QueryModelResultSetResourceImpl;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ResourceFactory
{
    public static LinkedDataResource getLinkedDataResourceResource(String uri)
    {
	return new LinkedDataResourceImpl(uri);
    }

    public static ModelResource getModelResource(String endpointUri, Query query)
    {
	return new EndpointModelResourceImpl(endpointUri, query);
    }

    public static ModelResource getModelResource(String endpointUri, String uri)
    {
	return new EndpointModelResourceImpl(endpointUri, uri);
    }

    public static ModelResource getModelResource(Model queryModel, Query query)
    {
	return new QueryModelModelResourceImpl(queryModel, query);
    }

    public static ModelResource getModelResource(Model queryModel, String uri)
    {
	return new QueryModelModelResourceImpl(queryModel, uri);
    }

    public static ResultSetResource getResultSetResource(String endpointUri, Query query)
    {
	return new EndpointResultSetResourceImpl(endpointUri, query);
    }

    public static ResultSetResource getResultSetResource(Model queryModel, Query query)
    {
	return new QueryModelResultSetResourceImpl(queryModel, query);
    }
    
}