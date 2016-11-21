/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.core.model.impl.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.model.RemoteService;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class ServiceImpl implements RemoteService
{

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    
    private final org.apache.jena.rdf.model.Resource endpoint, graphStore;
    private final String authUser, authPwd;

    public ServiceImpl(org.apache.jena.rdf.model.Resource endpoint, org.apache.jena.rdf.model.Resource graphStore, String authUser, String authPwd)
    {
	if (endpoint == null) throw new IllegalArgumentException("SPARQL endpoint Resource must be not null");
	if (graphStore == null) throw new IllegalArgumentException("Graph Store Resource must be not null");
        
        this.endpoint = endpoint;
        this.graphStore = graphStore;
        this.authUser = authUser;
        this.authPwd = authPwd;
    }

    public ServiceImpl(org.apache.jena.rdf.model.Resource endpoint, org.apache.jena.rdf.model.Resource graphStore)
    {
        this(endpoint, graphStore, null, null);
    }
        
    @Override
    public org.apache.jena.rdf.model.Resource getSPARQLEndpoint()
    {
        return endpoint;
    }
    
    @Override
    public org.apache.jena.rdf.model.Resource getGraphStore()
    {
        return graphStore;
    }

    @Override
    public String getAuthUser()
    {
        return authUser;
    }
    
    @Override
    public String getAuthPwd()
    {
        return authPwd;
    }

}
