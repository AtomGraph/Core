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

import com.atomgraph.core.MediaTypes;
import java.net.URI;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.apache.jena.ext.com.google.common.collect.ImmutableMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelGetter;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public interface DataManager extends ModelGetter
{
    
    ImmutableMap<String, Model> getModelCache();
    
//    Client getClient();
    
//    MediaTypes getMediaTypes();
    
    //WebTarget getEndpoint(URI endpointURI);
    
    Response get(String uri, jakarta.ws.rs.core.MediaType[] acceptedTypes); // TO-DO: deprecate?
    
    boolean usePreemptiveAuth();
    
    Model loadModel(String uri);
    
}
