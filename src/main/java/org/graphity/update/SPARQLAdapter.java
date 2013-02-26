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

package org.graphity.update;

import org.graphity.ldp.update.UpdateProcessRemote;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import java.io.ByteArrayOutputStream;
import org.openjena.riot.WebContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL Update implementation of Fuseki DatasetAccessor
 * @see <a href="http://jena.apache.org/documentation/javadoc/fuseki/org/apache/jena/fuseki/DatasetAccessor.html">Fuseki's DatasetAccessor</a>
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class SPARQLAdapter // implements org.openjena.fuseki.DatasetAccessor
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLAdapter.class);
    
    private String endpoint = null;
    
    /**
     * Constructs adapter based on SPARQL endpoint URI.
     * @param	endpoint    Absolute SPARQL endpoint URI
     */
    public SPARQLAdapter(String endpoint)
    {
	this.endpoint = endpoint;
    }

    /**
     * Returns SPARQL endpoint URI for this adapter.
     * @return	absolute SPARQL endpoint URI
     */
    public String getEndpoint()
    {
	return endpoint;
    }
    
    /**
     * Adds RDF Model to the default graph.
     * @param	data	RDF Model
     */
    public void add(Model model)
    {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	model.write(baos, WebContent.langNTriples);

	//UpdateDataInsert;
	// http://www.w3.org/TR/sparql11-update/#insertData
	UpdateRequest request = UpdateFactory.create("INSERT DATA { "
	    + baos.toString() +
	    "}", Syntax.syntaxSPARQL_11);

	UpdateProcessRemote process = new UpdateProcessRemote(request, getEndpoint());
	//process.setBasicAuthentication(getServiceApiKey(), "X".toCharArray());
	process.execute();	
    }
    
    /**
     * Adds RDF Model to specified named graph.
     * @param	graphUri    URI of the named graph
     * @param	model	    RDF Model
     */
    public void add(String graphUri, Model model)
    {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	model.write(baos, WebContent.langNTriples);

	//UpdateDataInsert;
	// http://www.w3.org/TR/sparql11-update/#insertData
	UpdateRequest request = UpdateFactory.create("CREATE GRAPH <" + graphUri + ">", Syntax.syntaxSPARQL_11).
	   add("INSERT DATA { GRAPH <" + graphUri + "> {"
	    + baos.toString() +
	    "} }");

	UpdateProcessRemote process = new UpdateProcessRemote(request, getEndpoint());
	//process.setBasicAuthentication(getServiceApiKey(), "X".toCharArray());
	process.execute();
    }
    
    /**
     * Checks if named graph exists.
     * @param	graphUri    URI of the named graph
     * @return	true if named graph exists
     */
    public boolean containsModel(String graphUri)
    {
	Query query = QueryFactory.create("ASK { <" + graphUri + "> ?p ?o }");
	QueryEngineHTTP request = QueryExecutionFactory.createServiceRequest(getEndpoint(), query);
	return request.execAsk();
    }
    
}