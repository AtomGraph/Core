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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.LocationMapper;
import java.net.URI;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.client.LinkedDataClient;
import java.net.URISyntaxException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelReader;
import org.apache.jena.util.FileManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Utility class for retrieval of RDF models from remote URLs.
*
* @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
* @see org.apache.jena.util.FileManager
* @see org.apache.jena.rdf.model.ModelGetter
* @see com.atomgraph.core.client.LinkedDataClient
*/

public class DataManagerImpl extends FileManagerImpl implements DataManager
{

    private static final Logger log = LoggerFactory.getLogger(DataManagerImpl.class);

    private final boolean preemptiveAuth;
    private final Client client;
    private final MediaTypes mediaTypes;
            
    /**
     * Creates data manager.
     * 
     * @param mapper location mapper
     * @param client HTTP client
     * @param mediaTypes supported readable and writable media types
     * @param preemptiveAuth if true, preemptive HTTP authentication will be used
     */
    public DataManagerImpl(LocationMapper mapper, Client client, MediaTypes mediaTypes, boolean preemptiveAuth)
    {
        super(mapper);
        if (client == null) throw new IllegalArgumentException("Client must be not null");
        if (mediaTypes == null) throw new IllegalArgumentException("MediaTypes must be not null");
        this.client = client;
        this.mediaTypes = mediaTypes;
        this.preemptiveAuth = preemptiveAuth;
        
        addLocatorFile() ;
        addLocatorURL() ;
        addLocatorClassLoader(getClass().getClassLoader()) ;
    }
    
    @Override
    public Client getClient()
    {
        return client;
    }

    @Override
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    @Override
    public WebTarget getEndpoint(URI endpointURI)
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
        
        return getClient().target(endpointURI.normalize());
    }
    
    @Override
    public Response get(String uri, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return LinkedDataClient.create(getEndpoint(URI.create(uri)), getMediaTypes()).
                get(acceptedTypes, null);
    }
    
    @Override
    public boolean usePreemptiveAuth()
    {
        return preemptiveAuth;
    }
        
    @Override
    public Model loadModel(String uri)
    {
        // only handle HTTP/HTTPS URIs, leave the rest to Jena
        if (!hasCachedModel(uri))
        {
            String mappedURI = mapURI(uri);
            if (mappedURI.startsWith("http") || mappedURI.startsWith("https"))
            {
                Model model = LinkedDataClient.create(getEndpoint(URI.create(uri)), getMediaTypes()).get();

                if (isCachingModels()) addCacheModel(uri, model) ;

                return model;
            }
        }
        
        return super.loadModel(uri);
    }
    
    @Override
    public Model getModel(String uri)
    {
        return loadModel(uri);
    }

    @Override
    public Model getModel(String uri, ModelReader loadIfAbsent)
    {
        Model model = getModel(uri);
        
        if (model == null) return loadIfAbsent.readModel(ModelFactory.createDefaultModel(), uri);
        
        return model;
    }
    
    @Override
    public Model readModel(Model model, String uri)
    {
        String mappedURI = mapURI(uri);
        if (mappedURI.startsWith("http") || mappedURI.startsWith("https"))
            return model.add(LinkedDataClient.create(getEndpoint(URI.create(uri)), getMediaTypes()).get());
        
        return super.readModel(model, uri);
    }

}