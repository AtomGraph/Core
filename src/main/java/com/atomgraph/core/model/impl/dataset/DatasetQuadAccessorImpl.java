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
package com.atomgraph.core.model.impl.dataset;

import com.atomgraph.core.model.DatasetQuadAccessor;
import java.util.Iterator;
import jakarta.ws.rs.core.Context;
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

    private final Dataset dataset;
    
    public DatasetQuadAccessorImpl(@Context Dataset dataset)
    {
        if (dataset == null) throw new IllegalArgumentException("Dataset cannot be null");
        this.dataset = dataset;
    }

    @Override
    public Dataset get()
    {
        return getDataset();
    }

    @Override
    public void add(Dataset dataset)
    {
        getDataset().getDefaultModel().add(dataset.getDefaultModel());
        
        Iterator<String> it = dataset.listNames();
        while (it.hasNext())
        {
            String graphURI = it.next();
            getDataset().addNamedModel(graphURI, dataset.getNamedModel(graphURI));
        }
    }

    @Override
    public void replace(Dataset dataset)
    {
        delete();
        
        add(dataset);
    }

    @Override
    public void delete()
    {
        getDataset().getDefaultModel().removeAll();
        
        Iterator<String> it = getDataset().listNames();
        while (it.hasNext())
            getDataset().removeNamedModel(it.next());
    }

    @Override
    public void patch(Dataset dataset)
    {
        getDataset().getDefaultModel().removeAll();
        getDataset().getDefaultModel().add(dataset.getDefaultModel());
        
        Iterator<String> it = dataset.listNames();
        while (it.hasNext())
        {
            String graphURI = it.next();
            getDataset().replaceNamedModel(graphURI, dataset.getNamedModel(graphURI));
        }
    }

    public Dataset getDataset()
    {
        return dataset;
    }

}
