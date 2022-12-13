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
import static com.atomgraph.core.client.SPARQLClient.parseBoolean;
import com.atomgraph.core.exception.BadGatewayException;
import com.atomgraph.core.model.EndpointAccessor;
import static com.atomgraph.core.model.SPARQLEndpoint.DEFAULT_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.NAMED_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_GRAPH_URI;
import static com.atomgraph.core.model.SPARQLEndpoint.USING_NAMED_GRAPH_URI;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
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

        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        
        defaultGraphUris.forEach(defaultGraphUri -> { params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString()); });
        namedGraphUris.forEach(namedGraphUri -> { params.add(NAMED_GRAPH_URI, namedGraphUri.toString()); });

        try (Response cr = getSPARQLClient().query(query, Dataset.class, params))
        {
            return cr.readEntity(Dataset.class);
        }
        catch (ClientErrorException ex)
        {
            throw new BadGatewayException(ex);
        }
    }
    
    @Override
    public Model loadModel(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");

        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        
        defaultGraphUris.forEach(defaultGraphUri -> { params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString()); });
        namedGraphUris.forEach(namedGraphUri -> { params.add(NAMED_GRAPH_URI, namedGraphUri.toString()); });

        try (Response cr = getSPARQLClient().query(query, Model.class, params))
        {
            return cr.readEntity(Model.class);
        }
        catch (ClientErrorException ex)
        {
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public ResultSetRewindable select(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        
        defaultGraphUris.forEach(defaultGraphUri -> { params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString()); });
        namedGraphUris.forEach(namedGraphUri -> { params.add(NAMED_GRAPH_URI, namedGraphUri.toString()); });
        
        try (Response cr = getSPARQLClient().query(query, ResultSet.class, params))
        {
            return cr.readEntity(ResultSetRewindable.class);
        }
        catch (ClientErrorException ex)
        {
            throw new BadGatewayException(ex);
        }
    }
  
    @Override
    public boolean ask(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        
        defaultGraphUris.forEach(defaultGraphUri -> { params.add(DEFAULT_GRAPH_URI, defaultGraphUri.toString()); });
        namedGraphUris.forEach(namedGraphUri -> { params.add(NAMED_GRAPH_URI, namedGraphUri.toString()); });
        
        try (Response cr = getSPARQLClient().query(query, ResultSet.class, params))
        {
            return parseBoolean(cr);
        }
        catch (IOException | ClientErrorException ex)
        {
            throw new BadGatewayException(ex);
        }
    }

    @Override
    public void update(UpdateRequest updateRequest, List<URI> usingGraphUris, List<URI> usingNamedGraphUris)
    {
        if (usingGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (usingNamedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        
        usingGraphUris.forEach(usingGraphUri -> { params.add(USING_GRAPH_URI, usingGraphUri.toString()); });
        usingNamedGraphUris.forEach(usingNamedGraphUri -> { params.add(USING_NAMED_GRAPH_URI, usingNamedGraphUri.toString()); });

        try
        {
            getSPARQLClient().update(updateRequest, params);
        }
        catch (ClientErrorException ex)
        {
            throw new BadGatewayException(ex);
        }
    }
    
    public SPARQLClient getSPARQLClient()
    {
        return sparqlClient;
    }
    
}
