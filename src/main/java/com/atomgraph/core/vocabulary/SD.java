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

/**
 * The SPARQL 1.1 Service Description vocabulary: what a SPARQL service says about itself when asked without a query.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.w3.org/TR/sparql11-service-description/">SPARQL 1.1 Service Description</a>
 */
public class SD
{

    static
    {
        org.apache.jena.sys.JenaSystem.init(); // ensure Jena (RDFS vocab) is initialized before ontapi touches it
    }

    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = OntModelFactory.createModel(OntSpecification.OWL2_FULL_MEM);

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

    /** An RDF Dataset: a default graph and zero or more named graphs */
    public static final OntClass Dataset = m_model.createOntClass( NS + "Dataset" );

    /** A SPARQL service: an endpoint and what it supports */
    public static final OntClass Service = m_model.createOntClass( NS + "Service" );

    /** An RDF graph in a dataset */
    public static final OntClass Graph = m_model.createOntClass( NS + "Graph" );

    /** A named graph: a graph and the name it has in its dataset */
    public static final OntClass NamedGraph = m_model.createOntClass( NS + "NamedGraph" );

    /** A SPARQL language a service can support */
    public static final OntClass Language = m_model.createOntClass( NS + "Language" );

    /** The service's SPARQL Protocol endpoint URL */
    public static final Property endpoint = m_model.createObjectProperty( NS + "endpoint" );

    /** The graph of a named graph */
    public static final Property graph = m_model.createObjectProperty( NS + "graph" );

    /** The name of a named graph in its dataset */
    public static final Property name = m_model.createObjectProperty( NS + "name" );

    /** The default graph of a dataset */
    public static final Property defaultGraph = m_model.createObjectProperty( NS + "defaultGraph" );

    /** A named graph of a dataset */
    public static final Property namedGraph = m_model.createObjectProperty( NS + "namedGraph" );

    /** A SPARQL language the service supports */
    public static final Property supportedLanguage = m_model.createObjectProperty( NS + "supportedLanguage" );

    /** A format the service can answer a query result in */
    public static final Property resultFormat = m_model.createObjectProperty( NS + "resultFormat" );

    /** A property function the service supports in a query, such as jena-text's text:query */
    public static final Property propertyFeature = m_model.createObjectProperty( NS + "propertyFeature" );

    /** The SPARQL 1.0 Query language */
    public static final Resource SPARQL10Query = m_model.createIndividual(NS + "SPARQL10Query", Language);

    /** The SPARQL 1.1 Query language */
    public static final Resource SPARQL11Query = m_model.createIndividual(NS + "SPARQL11Query", Language);

    /** The SPARQL 1.1 Update language */
    public static final Resource SPARQL11Update = m_model.createIndividual(NS + "SPARQL11Update", Language);

}
