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
        
    private final String endpointURI, graphStoreURI;
    private final String authUser, authPwd;

    public ServiceImpl(String endpointURI, String graphStoreURI, String authUser, String authPwd)
    {
	if (endpointURI == null) throw new IllegalArgumentException("SPARQLEndpoint URI must be not null");
	if (graphStoreURI == null) throw new IllegalArgumentException("Graph Store URI must be not null");
        
        this.endpointURI = endpointURI;
        this.graphStoreURI = graphStoreURI;
        this.authUser = authUser;
        this.authPwd = authPwd;
    }

    public ServiceImpl(String endpointURI, String graphStoreURI)
    {
        this(endpointURI, graphStoreURI, null, null);
    }
    
    @Override
    public String getSPARQLEndpointURI()
    {
        return endpointURI;
    }
    
    @Override
    public String getGraphStoreURI()
    {
        return graphStoreURI;
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
