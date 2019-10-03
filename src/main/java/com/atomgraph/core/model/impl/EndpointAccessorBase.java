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
package com.atomgraph.core.model.impl;

import java.net.URI;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

/**
 * Abstract implementation of the endpoint accessor.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public abstract class EndpointAccessorBase implements com.atomgraph.core.model.EndpointAccessor
{

    /**
     * Convenience method for <pre>DESCRIBE</pre> queries.
     * 
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @link #loadModel(query)
     * @param query
     * @return RDF model
     */
    @Override
    public Model describe(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isDescribeType()) throw new IllegalArgumentException("Query must be DESCRIBE");
        
        return loadModel(query, defaultGraphUris, namedGraphUris);
    }

    /**
     * Convenience method for <pre>CONSTRUCT</pre> queries.
     * 
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @link #loadModel(query)
     * @param query
     * @return RDF model
     */
    @Override
    public Model construct(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (query == null) throw new IllegalArgumentException("Query must be not null");
        if (!query.isConstructType()) throw new IllegalArgumentException("Query must be CONSTRUCT");
        
        return loadModel(query, defaultGraphUris, namedGraphUris);
    }

    /**
     * Loads RDF dataset from the endpoint by executing a SPARQL query (<code>DESCRIBE</code> or <code>CONSTRUCT</code>)
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    @Override
    public abstract Dataset loadDataset(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris);

    /**
     * Loads RDF graph from the endpoint by executing a SPARQL query (<code>DESCRIBE</code> or <code>CONSTRUCT</code>)
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    @Override
    public abstract Model loadModel(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris);
    
    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<pre>SELECT</pre>)
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return SPARQL result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    @Override
    public abstract ResultSetRewindable select(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris);

    /**
     * Asks boolean result from the endpoint by executing a SPARQL query (<pre>ASK</pre>)
     * 
     * @param query SPARQL query
     * @param defaultGraphUris default graph URIs
     * @param namedGraphUris named graph URIs
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    @Override
    public abstract boolean ask(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris);

    /**
     * Execute SPARQL update request
     * 
     * @param updateRequest update request
     * @param usingGraphUris using graph URIs
     * @param usingNamedGraphUris using named graph URIs
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-update-20130321/">SPARQL 1.1 Update</a>
     */
    @Override
    public abstract void update(UpdateRequest updateRequest, List<URI> usingGraphUris, List<URI> usingNamedGraphUris);
    
}
