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
package com.atomgraph.core.model.impl.remote;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.core.model.RemoteService;
import com.atomgraph.core.model.SPARQLEndpoint;
import com.sun.jersey.api.client.Client;
import javax.ws.rs.core.Request;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ServiceImpl implements RemoteService
{

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    
    private final org.apache.jena.rdf.model.Resource endpoint, graphStore;
    private final Client client;
    private final MediaTypes mediaTypes;
    private final String authUser, authPwd;
    private final Integer maxGetRequestSize;

    public ServiceImpl(Client client, MediaTypes mediaTypes, org.apache.jena.rdf.model.Resource endpoint, org.apache.jena.rdf.model.Resource graphStore, String authUser, String authPwd,
            Integer maxGetRequestSize)
    {
        if (client == null) throw new IllegalArgumentException("Client must be not null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes must be not null");
        if (endpoint == null) throw new IllegalArgumentException("SPARQL endpoint Resource must be not null");
        if (!endpoint.isURIResource()) throw new IllegalArgumentException("SPARQL endpoint Resource must be URI resource");
        if (graphStore == null) throw new IllegalArgumentException("Graph Store Resource must be not null");
        if (!graphStore.isURIResource()) throw new IllegalArgumentException("Graph Store Resource must be URI resource");
            
        this.client = client;
        this.mediaTypes = mediaTypes;
        this.endpoint = endpoint;
        this.graphStore = graphStore;
        this.authUser = authUser;
        this.authPwd = authPwd;
        this.maxGetRequestSize = maxGetRequestSize;
    }

    public ServiceImpl(Client client, MediaTypes mediaTypes, org.apache.jena.rdf.model.Resource endpoint, org.apache.jena.rdf.model.Resource graphStore)
    {
        this(client, mediaTypes, endpoint, graphStore, null, null, null);
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

    public Client getClient()
    {
        return client;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    @Override
    public String getAuthUser() // protected?
    {
        return authUser;
    }
    
    @Override
    public String getAuthPwd()  // protected?
    {
        return authPwd;
    }
    
    public Integer getMaxGetRequestSize()
    {
        return maxGetRequestSize;
    }
    
    @Override
    public SPARQLEndpoint getSPARQLEndpoint(Request request)
    {
        return new SPARQLEndpointBase(getClient(), getMediaTypes(), getMaxGetRequestSize(), getSPARQLEndpoint().getURI(), getAuthUser(), getAuthPwd(), request);
    }

    @Override
    public GraphStore getGraphStore(Request request)
    {
        return new GraphStoreBase(getClient(), getMediaTypes(), getGraphStore().getURI(), getAuthUser(), getAuthPwd(), request);
    }
    
}
