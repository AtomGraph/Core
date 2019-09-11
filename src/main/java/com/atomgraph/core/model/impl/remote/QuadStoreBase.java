/*
 * Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>.
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
import com.atomgraph.core.client.QuadStoreClient;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class QuadStoreBase extends com.atomgraph.core.model.impl.QuadStoreBase implements com.atomgraph.core.model.remote.QuadStore
{
    private static final Logger log = LoggerFactory.getLogger(QuadStoreBase.class);

    private final QuadStoreClient quadStoreClient;

    /**
     * Constructs Quad Store proxy from request metadata and origin URI.
     * 
     * @param quadStoreClient SPARQL 1.1 Graph Store Protocol client extended with quad support
     * @param mediaTypes supported media types
     * @param request HTTP request
     */
    public QuadStoreBase(@Context QuadStoreClient quadStoreClient, @Context MediaTypes mediaTypes, @Context Request request)
    {
        super(request, mediaTypes);
        if (quadStoreClient == null) throw new IllegalArgumentException("QuadStoreClient cannot be null");
        
        this.quadStoreClient = quadStoreClient;
    }

    @Override
    public Dataset get()
    {
        return getQuadStoreClient().get();
    }

    @Override
    public void add(Dataset dataset)
    {
        getQuadStoreClient().add(dataset);
    }

    @Override
    public void replace(Dataset dataset)
    {
        getQuadStoreClient().replace(dataset);
    }

    @Override
    public void delete()
    {
        getQuadStoreClient().delete();
    }
    
    @Override
    public Model getModel()
    {
        return getQuadStoreClient().getModel();
    }

    @Override
    public Model getModel(String uri)
    {
        return getQuadStoreClient().getModel(uri);
    }

    @Override
    public boolean containsModel(String uri)
    {
        return getQuadStoreClient().containsModel(uri);
    }
    
    @Override
    public void putModel(Model model)
    {
        getQuadStoreClient().putModel(model);
    }

    @Override
    public void putModel(String uri, Model model)
    {
        getQuadStoreClient().putModel(uri, model);
    }

    @Override
    public void deleteDefault()
    {
        getQuadStoreClient().deleteDefault();
    }

    @Override
    public void deleteModel(String uri)
    {
        getQuadStoreClient().deleteModel(uri);
    }

    @Override
    public void add(Model model)
    {
        getQuadStoreClient().add(model);
    }

    @Override
    public void add(String uri, Model model)
    {
        getQuadStoreClient().add(uri, model);
    }

    @Override
    public String getURI()  // needs to align with Jena's Resource.getURI() which returns String
    {
        return getQuadStoreClient().getWebResource().getURI().toString();
    }
    
    @Override
    public QuadStoreClient getQuadStoreClient()
    {
        return quadStoreClient;
    }
    
}
