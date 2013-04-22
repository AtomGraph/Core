/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

/**
 * Extended SPARQL endpoint interface, includes query and update as well as JAX-RS helper methods.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Response.ResponseBuilder.html">JAX-RS ResponseBuilder</a>
 * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/javax/ws/rs/core/Variant.html">JAX-RS Variant</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSetRewindable.html">ARQ ResultSetRewindable</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Jena Model</a>
 */
public interface SPARQLEndpoint extends SPARQLQueryEndpoint, SPARQLUpdateEndpoint
{
    /**
     * Executes query and returns response builder initialized with its result
     * 
     * @param query SPARQL query object
     * @return response builder for the query result
     */
    ResponseBuilder getResponseBuilder(Query query);
    
    /**
     * Returns response builder initialized with RDF model
     * 
     * @param model RDF model
     * @return response builder for the model
     */
    ResponseBuilder getResponseBuilder(Model model);
    
    /**
     * Returns response builder initialized with RDF model and representation variants
     * 
     * @param model RDF model
     * @param variants list of representation variants
     * @return response builder for the model
     */
    ResponseBuilder getResponseBuilder(Model model, List<Variant> variants);
    
    /**
     * Returns response builder initialized with SPARQL result set
     * 
     * @param resultSet SPARQL result set
     * @return response builder for the result set
     */
    ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet);
    
    /**
     * Returns response builder initialized with SPARQL result set and representation variants
     * 
     * @param resultSet SPARQL result set
     * @param variants list of representation variants
     * @return response builder for the result set
     */
    ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet, List<Variant> variants);
    
    /**
     * Returns response builder initialized with entity tag, response entity, and representation variants
     * 
     * @param entityTag ETag value of the response entity
     * @param entity response entity
     * @param variants list of representation variants
     * @return response builder for the entity
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/EntityTag.html">EntityTag</a>
     */
    ResponseBuilder getResponseBuilder(EntityTag entityTag, Object entity, List<Variant> variants);
    
    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<code>DESCRIBE</code> or <code>CONSTRUCT</code>)
     * 
     * @param query SPARQL query
     * @return RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    Model loadModel(Query query);
 
    /**
     * Loads RDF model from the endpoint by executing a SPARQL query (<pre>SELECT</pre>)
     * 
     * @param query
     * @return SPARQL result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    ResultSetRewindable loadResultSetRewindable(Query query);

}