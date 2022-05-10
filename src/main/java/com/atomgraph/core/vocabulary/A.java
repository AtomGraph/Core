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
package com.atomgraph.core.vocabulary;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public final class A
{
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "https://w3id.org/atomgraph/core#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     * @return namespace URI
     *  @see #NS */
    public static String getURI()
    {
        return NS;
    }
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    /** Dataset file property */
    public static final DatatypeProperty dataset = m_model.createDatatypeProperty( NS + "dataset" );
    
    /** Graph Store URL property */
    public static final ObjectProperty graphStore = m_model.createObjectProperty( NS + "graphStore" );
    
    /** Quad store URL property */
    public static final ObjectProperty quadStore = m_model.createObjectProperty( NS + "quadStore" );
    
    /** <code>Cache-Control</code> property **/
    public static final DatatypeProperty cacheControl = m_model.createDatatypeProperty( NS + "cacheControl" );

    /** Result limit property */
    public static final DatatypeProperty resultLimit = m_model.createDatatypeProperty( NS + "resultLimit" );

    /** Cache models property */
    public static final DatatypeProperty cacheModelLoads = m_model.createDatatypeProperty( NS + "cacheModelLoads" );
    
    /** Preemptive HTTP Basic auth property */
    public static final DatatypeProperty preemptiveAuth = m_model.createDatatypeProperty( NS + "preemptiveAuth" );
    
    /** Max <code>GET</code> request size property */
    public static final DatatypeProperty maxGetRequestSize = m_model.createDatatypeProperty( NS + "maxGetRequestSize" );
    
    /** HTTP Basic auth user property */
    public static final DatatypeProperty authUser = m_model.createDatatypeProperty( NS + "authUser" );
    
    /** HTTP Basic auth password property */
    public static final DatatypeProperty authPwd = m_model.createDatatypeProperty( NS + "authPwd" );

}