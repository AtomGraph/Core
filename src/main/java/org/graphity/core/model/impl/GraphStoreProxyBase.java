/**
 *  Copyright 2014 Martynas Jusevičius <martynas@graphity.org>
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

package org.graphity.core.model.impl;

import com.hp.hpl.jena.rdf.model.Model;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import org.graphity.core.MediaTypes;
import org.graphity.core.model.GraphStoreOrigin;
import org.graphity.core.model.GraphStoreProxy;
import org.graphity.core.model.Origin;
import org.graphity.core.util.jena.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of Graph Store.
 * This class forwards requests to a remote origin.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/service") // not standard
public class GraphStoreProxyBase extends GraphStoreBase implements GraphStoreProxy
{
    private static final Logger log = LoggerFactory.getLogger(GraphStoreProxyBase.class);

    private final Origin origin;
    private final DataManager dataManager;
    
    /**
     * Constructs Graph Store proxy from request metadata and origin.
     * 
     * @param request request
     * @param servletConfig servlet config
     * @param mediaTypes supported media types
     * @param origin graph store origin
     * @param dataManager data manager
     */
    public GraphStoreProxyBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context GraphStoreOrigin origin, @Context DataManager dataManager)
    {
        super(request, servletConfig, mediaTypes);
        if (origin == null) throw new IllegalArgumentException("GraphStoreOrigin cannot be null");
        if (dataManager == null) throw new IllegalArgumentException("DataManager cannot be null");
        this.origin = origin;
        this.dataManager = dataManager;
    }

     /**
     * Returns configured Graph Store resource.
     * This graph store is a proxy for the remote one.
     * 
     * @return graph store resource
     */
    @Override
    public Origin getOrigin()
    {
        return origin;
    }

    @Override
    public Model getModel()
    {
	return getDataManager().getModel(getOrigin().getURI());
    }

    @Override
    public Model getModel(String uri)
    {
        return getDataManager().getModel(getOrigin().getURI(), uri);
    }

    @Override
    public boolean containsModel(String uri)
    {
        return getDataManager().containsModel(getOrigin().getURI(), uri);
    }

    @Override
    public void putModel(Model model)
    {
        getDataManager().putModel(getOrigin().getURI(), model);
    }

    @Override
    public void putModel(String uri, Model model)
    {
        getDataManager().putModel(getOrigin().getURI(), uri, model);
    }

    @Override
    public void deleteDefault()
    {
        getDataManager().deleteDefault(getOrigin().getURI());
    }

    @Override
    public void deleteModel(String uri)
    {
        getDataManager().deleteModel(getOrigin().getURI(), uri);
    }

    @Override
    public void add(Model model)
    {
        getDataManager().addModel(getOrigin().getURI(), model);
    }

    @Override
    public void add(String uri, Model model)
    {
        getDataManager().addModel(getOrigin().getURI(), uri, model);
    }

    public DataManager getDataManager()
    {
        return dataManager;
    }

}
