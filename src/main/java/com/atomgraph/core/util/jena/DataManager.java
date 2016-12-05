/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.util.jena;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.LocationMapper;
import java.net.URI;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import com.atomgraph.core.MediaTypes;
import java.net.URISyntaxException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Utility class for retrieval of SPARQL query results from local RDF models and remote endpoints.
*
* @author Martynas Jusevičius <martynas@atomgraph.com>
* @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/FileManager.html">Jena FileManager</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Jena Model</a>
* @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSet.html">ARQ ResultSet</a>
*/

public class DataManager extends FileManager
{

    private static final Logger log = LoggerFactory.getLogger(DataManager.class);

    private final boolean preemptiveAuth;
    private final Client client;
    private final MediaTypes mediaTypes;
    private final javax.ws.rs.core.MediaType[] modelMediaTypes;
    private final javax.ws.rs.core.MediaType[] resultSetMediaTypes;
            
    /**
     * Creates data manager.
     * 
     * @param mapper location mapper
     * @param client HTTP client
     * @param mediaTypes supported readable and writable media types
     * @param preemptiveAuth if true, preemptive HTTP authentication will be used
     */
    public DataManager(LocationMapper mapper, Client client, MediaTypes mediaTypes, boolean preemptiveAuth)
    {
	super(mapper);
	if (client == null) throw new IllegalArgumentException("Client must be not null");        
	if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes must be not null");
        this.client = client;
        this.mediaTypes = mediaTypes;
        this.preemptiveAuth = preemptiveAuth;
        List<javax.ws.rs.core.MediaType> modelMediaTypeList = mediaTypes.getReadable(Model.class);
        modelMediaTypes = modelMediaTypeList.toArray(new javax.ws.rs.core.MediaType[modelMediaTypeList.size()]);
        List<javax.ws.rs.core.MediaType> resultMediaTypeList = mediaTypes.getReadable(ResultSet.class);
        resultSetMediaTypes = resultMediaTypeList.toArray(new javax.ws.rs.core.MediaType[resultMediaTypeList.size()]);
    }
    
    public Client getClient()
    {
        return client;
    }

    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    public javax.ws.rs.core.MediaType[] getModelMediaTypes()
    {
        return modelMediaTypes;
    }

    public javax.ws.rs.core.MediaType[] getResultSetMediaTypes()
    {
        return resultSetMediaTypes;
    }
    
    public WebTarget getEndpoint(URI endpointURI, ClientRequestFilter authFilter, MultivaluedMap<String, String> params)
    {
	if (endpointURI == null) throw new IllegalArgumentException("Endpoint URI must be not null");

        try
        {
            // remove fragment and normalize
            endpointURI = new URI(endpointURI.getScheme(), endpointURI.getSchemeSpecificPart(), null).normalize();
        }
        catch (URISyntaxException ex)
        {
            // should not happen, this a URI to URI conversion
        }
        
        WebTarget webTarget = getClient().target(endpointURI.normalize());
        if (authFilter != null) webTarget.register(authFilter);

        return webTarget;
    }
    
    public Response get(String uri, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from URI: {}", uri);
	return getEndpoint(URI.create(uri), null, null).
            request(acceptedTypes).
            get();
    }
    
    public boolean usePreemptiveAuth()
    {
        return preemptiveAuth;
    }
        
}