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
package org.graphity.core.vocabulary;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public final class G
{
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://graphity.org/g#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI()
    {
	return NS;
    }
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    public static final ObjectProperty graphStore = m_model.createObjectProperty( NS + "graphStore" );
    
    public static final DatatypeProperty cacheControl = m_model.createDatatypeProperty( NS + "cacheControl" );

    public static final DatatypeProperty resultLimit = m_model.createDatatypeProperty( NS + "resultLimit" );

    public static final DatatypeProperty preemptiveAuth = m_model.createDatatypeProperty( NS + "preemptiveAuth" );
    
    public static final DatatypeProperty cacheModelLoads = m_model.createDatatypeProperty( NS + "cacheModelLoads" );

    public static final ObjectProperty baseUri = m_model.createObjectProperty( NS + "baseUri" );

    public static final ObjectProperty absolutePath = m_model.createObjectProperty( NS + "absolutePath" );

    public static final ObjectProperty requestUri = m_model.createObjectProperty( NS + "requestUri" );

    public static final DatatypeProperty httpHeaders = m_model.createDatatypeProperty( NS + "httpHeaders" );

    public static final DatatypeProperty maxGetRequestSize = m_model.createDatatypeProperty( NS + "maxGetRequestSize" );
    
}