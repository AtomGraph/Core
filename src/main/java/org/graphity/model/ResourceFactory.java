/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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