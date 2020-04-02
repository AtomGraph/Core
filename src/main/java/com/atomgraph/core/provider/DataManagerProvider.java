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

package com.atomgraph.core.provider;

import com.atomgraph.core.util.jena.DataManager;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for data manager.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see com.atomgraph.core.util.jena.DataManager
 * @see javax.ws.rs.core.Context
 */
public class DataManagerProvider implements Factory<DataManager>
{

    private static final Logger log = LoggerFactory.getLogger(DataManagerProvider.class);

    private final DataManager dataManager;
        
    public DataManagerProvider(final DataManager dataManager)
    {
        this.dataManager = dataManager;
    }

    @Override
    public DataManager provide()
    {
        return getDataManager();
    }

    @Override
    public void dispose(DataManager dataManager)
    {
    }
    
    /**
     * Returns default data manager instance.
     * @return data manager instance
     */
    public DataManager getDataManager()
    {
        return dataManager;
    }

}