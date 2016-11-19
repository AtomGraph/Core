/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.vocabulary;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas
 */
public class SD
{
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.w3.org/ns/sparql-service-description#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI()
    {
	return NS;
    }
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    public static final OntClass Dataset = m_model.createClass( NS + "Dataset" );

    public static final OntClass Service = m_model.createClass( NS + "Service" );

    public static final OntClass Graph = m_model.createClass( NS + "Graph" );
    
    public static final OntClass NamedGraph = m_model.createClass( NS + "NamedGraph" );
    
    public static final ObjectProperty endpoint = m_model.createObjectProperty( NS + "endpoint" );

    public static final ObjectProperty graph = m_model.createObjectProperty( NS + "graph" );

    public static final ObjectProperty name = m_model.createObjectProperty( NS + "name" );

    public static final ObjectProperty defaultGraph = m_model.createObjectProperty( NS + "defaultGraph" );

    public static final ObjectProperty namedGraph = m_model.createObjectProperty( NS + "namedGraph" );

}
