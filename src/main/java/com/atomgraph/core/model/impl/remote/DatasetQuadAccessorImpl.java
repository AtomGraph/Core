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

import com.atomgraph.core.client.QuadStoreClient;
import com.atomgraph.core.model.DatasetQuadAccessor;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class DatasetQuadAccessorImpl implements DatasetQuadAccessor
{
    private static final Logger log = LoggerFactory.getLogger(DatasetQuadAccessorImpl.class);

    private final QuadStoreClient quadStoreClient;

    /**
     * Constructs Quad Store proxy from request metadata and origin URI.
     * 
     * @param quadStoreClient SPARQL 1.1 Graph Store Protocol client extended with quad support
     */
    public DatasetQuadAccessorImpl(QuadStoreClient quadStoreClient)
    {
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
    public void patch(Dataset dataset)
    {
        getQuadStoreClient().patch(dataset);
    }
    
    public QuadStoreClient getQuadStoreClient()
    {
        return quadStoreClient;
    }

}
