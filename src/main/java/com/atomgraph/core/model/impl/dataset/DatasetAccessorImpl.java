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

package com.atomgraph.core.model.impl.dataset;

import com.atomgraph.core.model.DatasetAccessor;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for dataset-backed Graph Stores.
 * Implementation of Graph Store Protocol on Jena dataset.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public class DatasetAccessorImpl implements DatasetAccessor
{
    private static final Logger log = LoggerFactory.getLogger(DatasetAccessorImpl.class);

    private final Dataset dataset;
        
    public DatasetAccessorImpl(Dataset dataset)
    {
        if (dataset == null) throw new IllegalArgumentException("Dataset cannot be null");
        this.dataset = dataset;
    }

    @Override
    public Model getModel()
    {
        return getDataset().getDefaultModel();
    }
    
    @Override
    public Model getModel(String uri)
    {
        return getDataset().getNamedModel(uri);
    }

    @Override
    public boolean containsModel(String uri)
    {
        return getDataset().containsNamedModel(uri);
    }

    @Override
    public void putModel(Model model)
    {
        getDataset().setDefaultModel(model);
    }

    @Override
    public void putModel(String uri, Model model)
    {
        getDataset().replaceNamedModel(uri, model);
    }

    @Override
    public void deleteDefault()
    {
        getDataset().setDefaultModel(ModelFactory.createDefaultModel());
    }

    @Override
    public void deleteModel(String uri)
    {
        getDataset().removeNamedModel(uri);
    }

    @Override
    public void add(Model model)
    {
        getDataset().getDefaultModel().add(model);
    }

    @Override
    public void add(String uri, Model model)
    {
        getDataset().addNamedModel(uri, model);
    }

    public Dataset getDataset()
    {
        return dataset;
    }

}