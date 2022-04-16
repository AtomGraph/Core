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
import com.atomgraph.core.client.LinkedDataClient;
import java.net.URISyntaxException;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.jena.ext.com.google.common.collect.ImmutableMap;
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
    private final LinkedDataClient ldc;
    private boolean cacheModelLoads;
    private final Map<String, Model> modelCache;

    /**
     * Creates data manager.
     * 
     * @param mapper location mapper
     * @param modelCache model cache map
     * @param ldc Linked Data client
     * @param cacheModelLoads if true, cache models after loading, using locations as keys
     * @param preemptiveAuth if true, preemptive HTTP authentication will be used
     */
    public DataManagerImpl(LocationMapper mapper, Map<String, Model> modelCache,
            LinkedDataClient ldc,
            boolean cacheModelLoads, boolean preemptiveAuth)
    {
        super(mapper);
        if (modelCache == null) throw new IllegalArgumentException("Model cache Map must be not null");
        this.modelCache = modelCache;
        this.cacheModelLoads = cacheModelLoads;
        this.preemptiveAuth = preemptiveAuth;
        this.ldc = ldc;
        
        addLocatorFile() ;
        addLocatorURL() ;
        addLocatorClassLoader(getClass().getClassLoader()) ;
    }
    
    public URI getEndpoint(URI endpointURI)
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
        
        return endpointURI.normalize();
    }
    
    @Override
    public Response get(String uri, javax.ws.rs.core.MediaType[] acceptedTypes)
    {
        return getLinkedDataClient().get(getEndpoint(URI.create(uri)), acceptedTypes);
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
                Model model = getLinkedDataClient().getModel(getEndpoint(URI.create(uri)).toString());

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
            return model.add(getLinkedDataClient().getModel(getEndpoint(URI.create(uri)).toString()));
        
        return super.readModel(model, uri);
    }

    /**
     * Returns an immutable copy of the model cache
     * 
     * @return immutable cache map
     */
    @Override
    public ImmutableMap<String, Model> getModelCache()
    {
        return ImmutableMap.copyOf(modelCache);
    }
    
    /**
     * Reset the model cache
     */
    @Override
    public void resetCache()
    {
        modelCache.clear() ;
    }
    
    /**
     * Change the state of model cache : does not clear the cache.
     * Deprecated - use constructor argument instead.
     * 
     * @param state true to enable caching
     */ 
    @Override
    @Deprecated
    public void setModelCaching(boolean state)
    {
        this.cacheModelLoads = state;
    }

    /**
     * Return whether caching is on of off
     * 
     * @return true if caching is enabled
     */
    @Override
    public boolean isCachingModels()
    {
        return cacheModelLoads;
    }
    
    /**
     * Read out of the cache - return null if not in the cache
     * 
     * @param filenameOrURI the location to load model from
     * @return model instance or null if it's not cached or caching is off
     */
    @Override
    public Model getFromCache(String filenameOrURI)
    { 
        if (!isCachingModels()) return null; 
        
        return modelCache.get(filenameOrURI);
    }
    
    /**
     * Check if model is cached for a given URI
     * 
     * @param filenameOrURI model location
     * @return true if cached, if it's not cached or caching is off
     */
    @Override
    public boolean hasCachedModel(String filenameOrURI)
    { 
        if (!isCachingModels()) return false;
        
        return modelCache.containsKey(filenameOrURI) ;
    }
    
    /**
     * Add model to cache using given URI as key
     * 
     * @param uri model URI (cache key)
     * @param m the model
     */
    @Override
    public void addCacheModel(String uri, Model m)
    { 
        if (isCachingModels()) modelCache.put(uri, m);
    }

    /**
     * Remove cache from model using given URI key
     * 
     * @param uri cache key
     */
    @Override
    public void removeCacheModel(String uri)
    { 
        if (isCachingModels()) modelCache.remove(uri) ;
    }
    
    @Override
    public boolean usePreemptiveAuth()
    {
        return preemptiveAuth;
    }
    
    /**
     * Returns Linked Data client.
     * 
     * @return client instance
     */
    public LinkedDataClient getLinkedDataClient()
    {
        return ldc;
    }
    
}