/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.model;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;

/**
 * RDF resource, representation of which was queried from a SPARQL endpoint.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
 */
public interface QueriedResource
{
    /**
     * Returns SPARQL query that was used to retrieve the RDF representation (<code>DESCRIBE</code> or <code>CONSTRUCT</code>)
     * 
     * @return query object
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    Query getQuery();

    /**
     * Returns RDF description of this resource. It is retrieved by executing the query on the SPARQL endpoint.
     * 
     * @return description RDF model
     */
    Model describe();
    
}