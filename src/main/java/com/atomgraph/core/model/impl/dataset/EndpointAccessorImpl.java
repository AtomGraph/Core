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
package com.atomgraph.core.model.impl.dataset;

import com.atomgraph.core.model.EndpointAccessor;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.sparql.core.DynamicDatasets;
import org.apache.jena.sparql.vocabulary.ResultSetGraphVocab;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class EndpointAccessorImpl implements EndpointAccessor
{
    
    private static final Logger log = LoggerFactory.getLogger(EndpointAccessorImpl.class);

    private final Dataset dataset;
    
    public EndpointAccessorImpl(Dataset dataset)
    {
        if (dataset == null) throw new IllegalArgumentException("Dataset cannot be null");
        this.dataset = dataset;
    }
    
    @Override
    public Dataset loadDataset(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (log.isDebugEnabled()) log.debug("Loading Model from Dataset using Query: {}", query);
        return loadDataset(specifyDataset(getDataset(), defaultGraphUris, namedGraphUris), query);
    }
    
    @Override
    public Model loadModel(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (log.isDebugEnabled()) log.debug("Loading Model from Dataset using Query: {}", query);
        return loadModel(specifyDataset(getDataset(), defaultGraphUris, namedGraphUris), query);
    }

    /**
     * Loads RDF dataset from an RDF dataset using a SPARQL query.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    public Dataset loadDataset(Dataset dataset, Query query)
    {
        if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
        if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");
        
        try (QueryExecution qex = QueryExecution.create(query, dataset))
        {        
            if (query.isConstructType()) return DatasetFactory.create(qex.execConstruct()); // subject to change if/when SPARQL can return quads
            if (query.isDescribeType()) return DatasetFactory.create(qex.execDescribe());
        
            throw new QueryExecException("Query to load Model must be CONSTRUCT or DESCRIBE");
        }
        catch (QueryExecException ex)
        {
            if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
            throw ex;
        }
    }
    
    /**
     * Loads RDF graph from an RDF dataset using a SPARQL query.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    public Model loadModel(Dataset dataset, Query query)
    {
        if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
        if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");
        
        try (QueryExecution qex = QueryExecution.create(query, dataset))
        {
            if (query.isConstructType()) return qex.execConstruct();
            if (query.isDescribeType()) return qex.execDescribe();
        
            throw new QueryExecException("Query to load Model must be CONSTRUCT or DESCRIBE");
        }
        catch (QueryExecException ex)
        {
            if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
            throw ex;
        }
    }
    
    @Override
    public ResultSetRewindable select(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (log.isDebugEnabled()) log.debug("Loading ResultSet from Dataset using Query: {}", query);
        return loadResultSet(specifyDataset(getDataset(), defaultGraphUris, namedGraphUris), query);
    }

    /**
     * Loads result set from an RDF dataset using a SPARQL query.
     * Only <code>SELECT</code> queries can be used with this method.
     * 
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    public ResultSetRewindable loadResultSet(Dataset dataset, Query query)
    {
        if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
        if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");
        
        try (QueryExecution qex = QueryExecution.create(query, dataset))
        {
            if (query.isSelectType()) return ResultSetFactory.copyResults(qex.execSelect());
            if (query.isAskType())
            {
                Model model = ModelFactory.createDefaultModel();
                model.createResource().
                    addProperty(RDF.type, ResultSetGraphVocab.ResultSet).
                    addLiteral(ResultSetGraphVocab.p_boolean, qex.execAsk());
                
                return ResultSetFactory.copyResults(ResultSetFactory.makeResults(model));
            }
            
            throw new QueryExecException("Query to load ResultSet must be SELECT or ASK");
        }
        catch (QueryExecException ex)
        {
            if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
            throw ex;
        }
    }
    
    @Override
    public boolean ask(Query query, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (log.isDebugEnabled()) log.debug("Loading Model from Dataset using Query: {}", query);
        return ask(specifyDataset(getDataset(), defaultGraphUris, namedGraphUris), query);
    }

    /**
     * Returns boolean result from an RDF dataset using a SPARQL query.
     * Only <code>ASK</code> queries can be used with this method.
     *
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public boolean ask(Dataset dataset, Query query)
    {
        if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
        if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");

        try (QueryExecution qex = QueryExecution.create(query, dataset))
        {
            if (query.isAskType()) return qex.execAsk();

            throw new QueryExecException("Query to load ResultSet must be SELECT");
        }
        catch (QueryExecException ex)
        {
            if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
            throw ex;
        }
    }
    
    /**
     * Executes update on dataset.
     * 
     * @param updateRequest update request
     * @param usingGraphUris default graph URIs
     * @param usingNamedGraphUris named graph URIs
     */
    @Override
    public void update(UpdateRequest updateRequest, List<URI> usingGraphUris, List<URI> usingNamedGraphUris)
    {
        if (log.isDebugEnabled()) log.debug("Attempting to update local Dataset, discarding UpdateRequest: {}", updateRequest);
    }

    // TO-DO: rewrite using Java 8 streams/lambdas
    public Dataset specifyDataset(Dataset dataset, List<URI> defaultGraphUris, List<URI> namedGraphUris)
    {
        if (defaultGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        if (namedGraphUris == null) throw new IllegalArgumentException("List<URI> cannot be null");
        
        if (!defaultGraphUris.isEmpty() || !namedGraphUris.isEmpty())
        {
            List<String> defaultGraphUriStrings = new ArrayList<>();
                for (URI defaultGraphUri : defaultGraphUris)
                    defaultGraphUriStrings.add(defaultGraphUri.toString());

            List<String> namedGraphUriStrings = new ArrayList<>();
                for (URI namedGraphUri : namedGraphUris)
                    namedGraphUriStrings.add(namedGraphUri.toString());
                
            DatasetDescription desc = DatasetDescription.create(defaultGraphUriStrings, namedGraphUriStrings);
            return DynamicDatasets.dynamicDataset(desc, dataset, false);
        }
            
        return dataset;
    }
    
    public Dataset getDataset()
    {
        return dataset;
    }
    
}
