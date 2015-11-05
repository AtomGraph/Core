/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.core.util;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.LocationMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.graphity.core.MediaType;
import org.graphity.core.provider.DatasetProvider;
import org.graphity.core.provider.ModelProvider;
import org.graphity.core.provider.QueryWriter;
import org.graphity.core.provider.ResultSetWriter;
import org.graphity.core.provider.UpdateRequestReader;
import org.graphity.core.util.jena.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class DataClient extends DataManager
{
    private static final Logger log = LoggerFactory.getLogger(DataClient.class);
    
    private final ClientConfig cc = new DefaultClientConfig();
    private final Client client;
    
    public DataClient(LocationMapper mapper, Context context, boolean preemptiveAuth)
    {
        super(mapper, context, preemptiveAuth);

        cc.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
        cc.getSingletons().add(new ModelProvider());
        cc.getSingletons().add(new DatasetProvider());
        cc.getSingletons().add(new ResultSetWriter());
        cc.getSingletons().add(new QueryWriter());
        cc.getSingletons().add(new UpdateRequestReader());

        client = Client.create(cc);
    }
   
    public Client getClient()
    {
        return client;
    }
    
    public WebResource getEndpoint(String endpointURI, Query query, MultivaluedMap<String, String> params)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", endpointURI, query);
        
        WebResource endpoint = getClient().resource(URI.create(endpointURI));
        
	if (params != null)
	    for (Map.Entry<String, List<String>> entry : params.entrySet())
		if (!entry.getKey().equals("query")) // query param is handled separately
		    for (String value : entry.getValue())
		    {
			if (log.isTraceEnabled()) log.trace("Adding param to SPARQL request with name: {} and value: {}", entry.getKey(), value);
			endpoint.queryParam(entry.getKey(), value);
		    }
	
	return endpoint;
    }
    
    @Override
    public Model loadModel(String endpointURI, Query query, MultivaluedMap<String, String> params)
    {
	if (log.isDebugEnabled()) log.debug("Remote service {} Query: {} ", endpointURI, query);
	if (query == null) throw new IllegalArgumentException("Query must be not null");

	return getEndpoint(endpointURI, query, params).
            accept(MediaType.TEXT_NTRIPLES_TYPE, MediaType.APPLICATION_RDF_XML_TYPE).
            type(MediaType.APPLICATION_SPARQL_QUERY_TYPE).
            post(ClientResponse.class, query).
            getEntity(Model.class);
    }
    
}
