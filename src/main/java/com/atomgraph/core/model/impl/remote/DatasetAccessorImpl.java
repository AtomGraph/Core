/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
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

package com.atomgraph.core.model.impl.remote;

import org.apache.jena.rdf.model.Model;
import com.atomgraph.core.client.GraphStoreClient;
import com.atomgraph.core.exception.BadGatewayException;
import com.atomgraph.core.model.DatasetAccessor;
import jakarta.ws.rs.ClientErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of Graph Store.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class DatasetAccessorImpl implements DatasetAccessor
{
    private static final Logger log = LoggerFactory.getLogger(DatasetAccessorImpl.class);

    private final GraphStoreClient graphStoreClient;
    
    /**
     * Constructs Graph Store proxy from request metadata and origin URI.
     * 
     * @param graphStoreClient SPARQL 1.1 Graph Store Protocol client
     */
    public DatasetAccessorImpl(GraphStoreClient graphStoreClient)
    {
        if (graphStoreClient == null) throw new IllegalArgumentException("GraphStoreClient cannot be null");
        this.graphStoreClient = graphStoreClient;
    }
    
    @Override
    public Model getModel()
    {
        try
        {
            return getGraphStoreClient().getModel();
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public Model getModel(String uri)
    {
        try
        {
            return getGraphStoreClient().getModel(uri);
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public boolean containsModel(String uri)
    {
        try
        {
            return getGraphStoreClient().containsModel(uri);
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }
    
    @Override
    public void putModel(Model model)
    {
        try
        {
            getGraphStoreClient().putModel(model);
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public void putModel(String uri, Model model)
    {
        try
        {
            getGraphStoreClient().putModel(uri, model);
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public void deleteDefault()
    {
        try
        {
            getGraphStoreClient().deleteDefault();
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public void deleteModel(String uri)
    {
        try
        {
            getGraphStoreClient().deleteModel(uri);
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public void add(Model model)
    {
        try
        {
            getGraphStoreClient().add(model);
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public void add(String uri, Model model)
    {
        try
        {
            getGraphStoreClient().add(uri, model);
        }
        catch (ClientErrorException ex)
        {
            if (log.isDebugEnabled()) log.debug("Graph Store backend client error", ex);
            throw new BadGatewayException(ex);
        }
    }

    public String getURI()  // needs to align with Jena's Resource.getURI() which returns String
    {
        return getGraphStoreClient().getEndpoint().getUri().toString();
    }
    
    public GraphStoreClient getGraphStoreClient()
    {
        return graphStoreClient;
    }
    
}