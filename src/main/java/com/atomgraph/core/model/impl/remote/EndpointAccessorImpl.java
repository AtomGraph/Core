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

import com.atomgraph.core.client.SPARQLClient;
import com.atomgraph.core.model.EndpointAccessor;
import static com.atomgraph.core.model.SPARQLEndpoint.DEFAULT_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.NAMED_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_NAMED_GRAPH_URI;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.net.URI;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class EndpointAccessorImpl implements EndpointAccessor
{
    
    private final SPARQLClient sparqlClient;
    
    public EndpointAccessorImpl(SPARQLClient sparqlClient)
    {
        if (sparqlClient == null) throw new IllegalArgumentException("SPARQLClient cannot be null");
        this.sparqlClient = sparqlClient;
    }
    
    @Override
    public Dataset loadDataset(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        
        for (URI defaultGraphUri : defaultGraphUris)
            params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString());
        for (URI namedGraphUri : namedGraphUris)
            params.add(NAMED_GRAPH_URI, namedGraphUri.toString());

        return getSPARQLClient().query(query, Dataset.class, params).getEntity(Dataset.class);
    }
    
    @Override
    public Model loadModel(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        
        for (URI defaultGraphUri : defaultGraphUris)
            params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString());
        for (URI namedGraphUri : namedGraphUris)
            params.add(NAMED_GRAPH_URI, namedGraphUri.toString());

        return getSPARQLClient().query(query, Model.class, params).getEntity(Model.class);
    }

    @Override
    public ResultSetRewindable select(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        
        for (URI defaultGraphUri : defaultGraphUris)
            params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString());
        for (URI namedGraphUri : namedGraphUris)
            params.add(NAMED_GRAPH_URI, namedGraphUri.toString());
        
        return getSPARQLClient().query(query, ResultSet.class, params).getEntity(ResultSetRewindable.class);
    }
  
    @Override
    public boolean ask(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        
        for (URI defaultGraphUri : defaultGraphUris)
            params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString());
        for (URI namedGraphUri : namedGraphUris)
            params.add(NAMED_GRAPH_URI, namedGraphUri.toString());
        
        return SPARQLClient.parseBoolean(getSPARQLClient().query(query, ResultSet.class, params));
    }

    @Override
    public void update(UpdateRequest updateRequest, List<URI> usingGraphUris, List<URI> usingNamedGraphUris)
    {
        if (usingGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (usingNamedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        
        for (URI usingGraphUri : usingGraphUris)
            params.add(USING_GRAPH_URI, usingGraphUri.toString());
        for (URI usingNamedGraphUri : usingNamedGraphUris)
            params.add(USING_NAMED_GRAPH_URI, usingNamedGraphUri.toString());

        getSPARQLClient().update(updateRequest, params);
    }
    
    public SPARQLClient getSPARQLClient()
    {
        return sparqlClient;
    }
    
}
