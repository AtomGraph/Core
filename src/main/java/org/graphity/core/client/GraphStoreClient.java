/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.core.client;

import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.graphity.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class GraphStoreClient
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLClient.class);

    private final WebResource webResource;

    protected GraphStoreClient(WebResource webResource)
    {
        this.webResource = webResource;
    }
    
    public WebResource getWebResource()
    {
        return webResource;
    }

    public static GraphStoreClient create(WebResource webResource)
    {
        return new GraphStoreClient(webResource);
    }

    public ClientResponse getModel(javax.ws.rs.core.MediaType[] acceptedTypes)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} default graph", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            accept(acceptedTypes).
            get(ClientResponse.class);
    }

    public ClientResponse getModel(javax.ws.rs.core.MediaType[] acceptedTypes, String uri)
    {
	if (log.isDebugEnabled()) log.debug("GET Model from Graph Store {} with named graph URI: {}", getWebResource().getURI(), uri);
	return getWebResource().queryParam("graph", uri).
            accept(acceptedTypes).
            get(ClientResponse.class);
    }

    public ClientResponse headNamed(String graphURI)
    {
	return getWebResource().queryParam("graph", graphURI).
            method("HEAD", ClientResponse.class);
    }
    
    public ClientResponse putModel(MediaType contentType, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} default graph", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            type(contentType).
            put(ClientResponse.class, model);
    }

    public ClientResponse putModel(MediaType contentType, String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("PUT Model to Graph Store {} with named graph URI {}", getWebResource().getURI(), uri);
	return getWebResource().queryParam("graph", uri).
            type(contentType).
            put(ClientResponse.class, model);
    }

    public ClientResponse deleteDefault()
    {
	if (log.isDebugEnabled()) log.debug("DELETE default graph from Graph Store {}", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            delete(ClientResponse.class);
    }

    public ClientResponse deleteModel(String uri)
    {
	if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {} from Graph Store {}", uri, getWebResource().getURI());
	return getWebResource().queryParam("graph", uri).
            delete(ClientResponse.class);
    }

    public ClientResponse add(MediaType contentType, Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} default graph", getWebResource().getURI());
	return getWebResource().queryParam("default", "").
            type(contentType).
            post(ClientResponse.class, model);
    }

    public ClientResponse add(MediaType contentType, String uri, Model model)
    {
	if (log.isDebugEnabled()) log.debug("POST Model to Graph Store {} with named graph URI: {}", getWebResource().getURI(), uri);
	return getWebResource().queryParam("graph", uri).
            type(contentType).
            post(ClientResponse.class, model);
    }
    
}
