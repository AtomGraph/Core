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
package org.graphity.core.util.jena;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.core.MultivaluedMap;
import org.graphity.core.provider.DatasetProvider;
import org.graphity.core.provider.MediaTypesProvider;
import org.graphity.core.provider.ModelProvider;
import org.graphity.core.provider.QueryWriter;
import org.graphity.core.provider.ResultSetProvider;
import org.graphity.core.provider.UpdateRequestReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Utility class for retrieval of SPARQL query results from local RDF models and remote endpoints.
* Uses portions of Jena code
* (c) Copyright 2010 Epimorphics Ltd.
* All rights reserved.
*
* @author Martynas Jusevičius <martynas@graphity.org>
* @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/FileManager.html">Jena FileManager</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/sparql/util/Context.html">ARQ Context</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Jena Model</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSet.html">ARQ ResultSet</a>
*/

public class DataManager extends FileManager
{

    private static final Logger log = LoggerFactory.getLogger(DataManager.class);

    private final Context context;
    private final boolean preemptiveAuth;
    private final ClientConfig clientConfig = new DefaultClientConfig();
    private final javax.ws.rs.core.MediaType[] modelMediaTypes;
    private final javax.ws.rs.core.MediaType[] resultSetMediaTypes;
            
    /**
     * Creates data manager from file manager and SPARQL context.
     * 
     * @param mapper location mapper
     * @param context SPARQL context
     * @param cacheModelLoads true if loaded models should be cached
     * @param preemptiveAuth if true, preemptive HTTP authentication will be used
     */
    public DataManager(LocationMapper mapper, Context context, boolean cacheModelLoads, boolean preemptiveAuth)
    {
	super(mapper);
	if (context == null) throw new IllegalArgumentException("Context cannot be null");
	this.context = context;
        setModelCaching(cacheModelLoads);
        this.preemptiveAuth = preemptiveAuth;
        List<javax.ws.rs.core.MediaType> modelMediaTypeList = new MediaTypesProvider().getMediaTypes().getModelMediaTypes();
        modelMediaTypes = modelMediaTypeList.toArray(new javax.ws.rs.core.MediaType[modelMediaTypeList.size()]);
        List<javax.ws.rs.core.MediaType> resultMediaTypeList = new MediaTypesProvider().getMediaTypes().getResultSetMediaTypes();
        resultSetMediaTypes = resultMediaTypeList.toArray(new javax.ws.rs.core.MediaType[resultMediaTypeList.size()]);
        
        clientConfig.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
        clientConfig.getSingletons().add(new ModelProvider());
        clientConfig.getSingletons().add(new DatasetProvider());
        clientConfig.getSingletons().add(new ResultSetProvider());
        clientConfig.getSingletons().add(new QueryWriter());
        clientConfig.getSingletons().add(new UpdateRequestReader()); // TO-DO: UpdateRequestProvider
    }
    
    public ClientConfig getClientConfig()
    {
        return clientConfig;
    }

    public javax.ws.rs.core.MediaType[] getModelMediaTypes()
    {
        return modelMediaTypes;
    }

    public javax.ws.rs.core.MediaType[] getResultSetMediaTypes()
    {
        return resultSetMediaTypes;
    }

    public ClientFilter getClientAuthFilter(String endpointURI)
    {
        return getClientAuthFilter(getServiceContext(endpointURI));
    }
        
    public ClientFilter getClientAuthFilter(Context serviceContext)
    {
        if (serviceContext != null)
        {
            String usr = serviceContext.getAsString(Service.queryAuthUser);
            String pwd = serviceContext.getAsString(Service.queryAuthPwd);

            if (usr != null || pwd != null)
            {
                usr = usr==null?"":usr;
                pwd = pwd==null?"":pwd;

                return new HTTPBasicAuthFilter(usr, pwd);
            }
        }
        
        return null;
    }

    public WebResource getEndpoint(String endpointURI)
    {
        return getEndpoint(endpointURI, getClientAuthFilter(endpointURI), null);
    }
    
    public WebResource getEndpoint(String endpointURI, MultivaluedMap<String, String> params)
    {
        return getEndpoint(endpointURI, getClientAuthFilter(endpointURI), params);
    }
    
    public WebResource getEndpoint(String endpointURI, ClientFilter authFilter, MultivaluedMap<String, String> params)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");
      
        Client client = Client.create(getClientConfig());
        if (authFilter != null) client.addFilter(authFilter);
        if (log.isDebugEnabled()) client.addFilter(new LoggingFilter(System.out));
        
        return client.resource(URI.create(endpointURI));
    }
    
    public ClientResponse get(String uri, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from URI: {}", uri);
	return getEndpoint(uri, null).
            accept(acceptedTypes).
            get(ClientResponse.class);
    }
    
    @Override
    public Model loadModel(String uri)
    {
        return get(uri, getModelMediaTypes()).getEntity(Model.class);
    }
        
    public boolean usePreemptiveAuth(Property property)
    {
        return preemptiveAuth;
    }
    
    /**
     * Returns SPARQL context
     * 
     * @return global context
     */
    public Context getContext()
    {
	return context;
    }

    /**
     * Given a URI (e.g. with encoded SPARQL query string), finds matching SPARQL endpoint in the service
     * context map.
     * 
     * @param filenameOrURI SPARQL request URI
     * @return matching map entry, or null if none
     */
    public Entry<String, Context> findEndpoint(String filenameOrURI)
    {
	if (getServiceContextMap() != null)
	{
	    Iterator<Entry<String, Context>> it = getServiceContextMap().entrySet().iterator();

	    while (it.hasNext())
	    {
		Entry<String, Context> endpoint = it.next(); 
		if (filenameOrURI.startsWith(endpoint.getKey()))
		    return endpoint;
	    }
	}
	
	return null;
    }

    /**
     * Returns the service context map. Endpoint URIs are used as keys.
     * 
     * @return service context map
     */
    public Map<String,Context> getServiceContextMap()
    {
	if (!getContext().isDefined(Service.serviceContext))
	{
	    Map<String,Context> serviceContext = new HashMap<>();
	    getContext().put(Service.serviceContext, serviceContext);
	}
	
	return (Map<String,Context>)getContext().get(Service.serviceContext);
    }

    /**
     * Adds service context for a SPARQL endpoint.
     * 
     * @param endpointURI endpoint URI
     * @param context context
     */
    public void addServiceContext(String endpointURI, Context context)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");
	
	getServiceContextMap().put(endpointURI, context);
    }

    /**
     * Adds service context for a SPARQL endpoint.
     * 
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     * @param context context
     */
    public void addServiceContext(Resource endpoint, Context context)
    {
	if (endpoint == null) throw new IllegalArgumentException("Endpoint Resource must be not null");
	if (!endpoint.isURIResource()) throw new IllegalArgumentException("Endpoint Resource must be URI Resource (not a blank node)");
	
	getServiceContextMap().put(endpoint.getURI(), context);
    }

    /**
     * Adds empty service context for a SPARQL endpoint.
     * 
     * @param endpointURI endpoint URI
     */
    public void addServiceContext(String endpointURI)
    {
	addServiceContext(endpointURI, new Context());
    }

    /**
     * Adds empty service context for a SPARQL endpoint.
     *
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     */
    public void addServiceContext(Resource endpoint)
    {
	addServiceContext(endpoint, new Context());
    }

    /**
     * Returns service context of a SPARQL endpoint.
     * 
     * @param endpointURI endpoint URI
     * @return context of the endpoint
     */
    public Context getServiceContext(String endpointURI)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");

	return getServiceContextMap().get(endpointURI);
    }
    
    public Context putServiceContext(String endpointURI, Context context)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");
	if (context == null) throw new IllegalArgumentException("Context must be not null");

        return getServiceContextMap().put(endpointURI, context);
    }
    
    /**
     * Returns service context of a SPARQL endpoint.
     * 
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     * @return context of the endpoint
     */    
    public Context getServiceContext(Resource endpoint)
    {
	if (endpoint == null) throw new IllegalArgumentException("Endpoint Resource must be not null");
	if (!endpoint.isURIResource()) throw new IllegalArgumentException("Endpoint Resource must be URI Resource (not a blank node)");

	return getServiceContext(endpoint.getURI());
    }

    /**
     * Checks if SPARQL endpoint has service context.
     * 
     * @param endpointURI endpoint URI
     * @return true if endpoint URI is bound to a context, false otherwise
     */    
    public boolean hasServiceContext(String endpointURI)
    {
	return getServiceContext(endpointURI) != null;
    }

    /**
     * Checks if SPARQL endpoint has service context.
     * 
     * @param endpoint endpoint resource (must be URI resource, not a blank node)
     * @return true if endpoint resource is bound to a context, false otherwise
     */    
    public boolean hasServiceContext(Resource endpoint)
    {
	return getServiceContext(endpoint) != null;
    }

    /**
     * Configures HTTP Basic authentication for SPARQL service context
     * 
     * @param endpointURI endpoint or graph store URI
     * @param authUser username
     * @param authPwd password
     * @return service context
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/sparql/util/Context.html">Context</a>
     */
    public Context putAuthContext(String endpointURI, String authUser, String authPwd)
    {
	if (endpointURI == null) throw new IllegalArgumentException("SPARQL endpoint URI cannot be null");
	if (authUser == null) throw new IllegalArgumentException("SPARQL endpoint authentication username cannot be null");
	if (authPwd == null) throw new IllegalArgumentException("SPARQL endpoint authentication password cannot be null");

	if (log.isDebugEnabled()) log.debug("Setting username/password credentials for SPARQL endpoint: {}", endpointURI);
	Context queryContext = new Context();
	queryContext.put(Service.queryAuthUser, authUser);
	queryContext.put(Service.queryAuthPwd, authPwd);

        return putServiceContext(endpointURI, queryContext);
    }
    
}