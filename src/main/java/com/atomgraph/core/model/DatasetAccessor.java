/*
 * Copyright 2021 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.core.model;

import org.apache.jena.rdf.model.Model;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public interface DatasetAccessor
{
    /** Get the default model of a Dataset
     * @return  model*/ 
    public Model getModel(); 
    
    /** Get a named model of a Dataset
     * @param graphUri graph name
     * @return  model */
    public Model getModel(String graphUri);

    /**
     * Does the Dataset contain a named graph?
     * @param graphURI graph name
     * @return true if graph exists
     */
    public boolean containsModel(String graphURI);
    
    /**
     * Put (replace) the default model of a Dataset
     * @param data payload model
     */
    public void putModel(Model data);
    
    /**
     * Put (create/replace) a named model of a Dataset
     * @param graphUri graph name
     * @param data payload model
     */
    public void putModel(String graphUri, Model data);

    /**
     * Delete (which means clear) the default model of a Dataset
     */
    public void deleteDefault() ;
    
    /**
     * Delete a named model of a Dataset
     * @param graphUri graph name
     */
    public void deleteModel(String graphUri);

    /**
     * Add statements to the default model of a Dataset
     * @param data payload model
     */
    public void add(Model data);
    
    /**
     * Add statements to a named model of a Dataset
     * @param graphUri graph name
     * @param data payload model
     */
    public void add(String graphUri, Model data);
    
}