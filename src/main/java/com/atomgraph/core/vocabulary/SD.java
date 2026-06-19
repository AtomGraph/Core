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

import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

/**
 *
 * @author Martynas
 */
public class SD
{

    static
    {
        org.apache.jena.sys.JenaSystem.init(); // ensure Jena (RDFS vocab) is initialized before ontapi touches it
    }

    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = OntModelFactory.createModel(OntSpecification.OWL1_FULL_MEM);

    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.w3.org/ns/sparql-service-description#";

    /** <p>The namespace of the vocabulary as a string</p>
     * @return namespace URI
     *  @see #NS */
    public static String getURI()
    {
        return NS;
    }

    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    public static final OntClass Dataset = m_model.createOntClass( NS + "Dataset" );

    public static final OntClass Service = m_model.createOntClass( NS + "Service" );

    public static final OntClass Graph = m_model.createOntClass( NS + "Graph" );

    public static final OntClass NamedGraph = m_model.createOntClass( NS + "NamedGraph" );

    public static final OntClass Language = m_model.createOntClass( NS + "Language" );

    public static final Property endpoint = m_model.createObjectProperty( NS + "endpoint" );

    public static final Property graph = m_model.createObjectProperty( NS + "graph" );

    public static final Property name = m_model.createObjectProperty( NS + "name" );

    public static final Property defaultGraph = m_model.createObjectProperty( NS + "defaultGraph" );

    public static final Property namedGraph = m_model.createObjectProperty( NS + "namedGraph" );

    public static final Property supportedLanguage = m_model.createObjectProperty( NS + "supportedLanguage" );

    // created as plain typed resources rather than via createIndividual(): the OWL1 profile (OWL1_FULL_MEM, needed
    // for rdfs:Class recognition) rejects named individuals — ontapi throws OntJenaException$Creation otherwise
    public static final Resource SPARQL10Query = m_model.createResource(NS + "SPARQL10Query").addProperty(RDF.type, Language);

    public static final Resource SPARQL11Query = m_model.createResource(NS + "SPARQL11Query").addProperty(RDF.type, Language);

    public static final Resource SPARQL11Update = m_model.createResource(NS + "SPARQL11Update").addProperty(RDF.type, Language);

}
