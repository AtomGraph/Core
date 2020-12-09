/*
 * Copyright 2020 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.core.util.jena;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.jena.sys.JenaSystem;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class StartupListener implements ServletContextListener
{
    
    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        JenaSystem.init();
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        JenaSystem.shutdown();
    }
    
}
