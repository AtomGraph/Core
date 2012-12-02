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

package org.graphity.model.query.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import org.graphity.model.query.EndpointResource;
import org.graphity.model.query.ModelResource;
import org.graphity.util.manager.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class EndpointModelResourceImpl implements EndpointResource, ModelResource
{
    private static final Logger log = LoggerFactory.getLogger(EndpointModelResourceImpl.class);

    private String endpointUri = null;
    private Query query = null;
    private Model model = null;

    public EndpointModelResourceImpl(String endpointUri, Query query)
    {
	if (endpointUri == null) throw new IllegalArgumentException("Endpoint URI must be not null");
	if (query == null) throw new IllegalArgumentException("Query must be not null");
	this.endpointUri = endpointUri;
	this.query = query;
	
	if (log.isDebugEnabled()) log.debug("Querying remote service: {} with Query: {}", endpointUri, query);
	model = DataManager.get().loadModel(endpointUri, query);

	if (log.isDebugEnabled()) log.debug("Number of Model stmts read: {}", model.size());
    }
   
    public EndpointModelResourceImpl(String endpointUri, String uri)
    {
	this(endpointUri, QueryFactory.create("DESCRIBE <" + uri + ">"));
    }

    @Override
    public Model describe()
    {
	return model;
    }

    @Override
    public String getEndpointURI()
    {
	return endpointUri;
    }

    @Override
    public Query getQuery()
    {
	return query;
    }

}