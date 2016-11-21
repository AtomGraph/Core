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
package com.atomgraph.core.model.impl.dataset;

import com.atomgraph.core.model.DatasetService;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class ServiceImpl implements DatasetService
{

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    private final Dataset dataset;

    public ServiceImpl(Dataset dataset)
    {
	if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        
        this.dataset = dataset;
    }

    /*
    @Override
    public SPARQLEndpoint getSPARQLEndpoint(Request request, MediaTypes mediaTypes)
    {
        return new SPARQLEndpointBase(request, mediaTypes, getDataset());
    }

    @Override
    public GraphStore getGraphStore(Request request, MediaTypes mediaTypes)
    {
        return new GraphStoreBase(request, mediaTypes, getDataset());
    }
    */
    
    @Override
    public Dataset getDataset()
    {
        return dataset;
    }
    
}
