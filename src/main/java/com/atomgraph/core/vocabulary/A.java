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

import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.rdf.model.Property;

import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public final class A
{

    static
    {
        org.apache.jena.sys.JenaSystem.init(); // ensure Jena (RDFS vocab) is initialized before ontapi touches it
    }
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = OntModelFactory.createModel(OntSpecification.OWL1_FULL_MEM);
    
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
    public static final Property dataset = m_model.createDataProperty( NS + "dataset" );
    
    /** Graph Store URL property */
    public static final Property graphStore = m_model.createObjectProperty( NS + "graphStore" );
    
    /** Quad store URL property */
    public static final Property quadStore = m_model.createObjectProperty( NS + "quadStore" );
    
    /** <code>Cache-Control</code> property **/
    public static final Property cacheControl = m_model.createDataProperty( NS + "cacheControl" );

    /** Result limit property */
    public static final Property resultLimit = m_model.createDataProperty( NS + "resultLimit" );

    /** Cache models property */
    public static final Property cacheModelLoads = m_model.createDataProperty( NS + "cacheModelLoads" );
    
    /** Preemptive HTTP Basic auth property */
    public static final Property preemptiveAuth = m_model.createDataProperty( NS + "preemptiveAuth" );
    
    /** Max <code>GET</code> request size property */
    public static final Property maxGetRequestSize = m_model.createDataProperty( NS + "maxGetRequestSize" );
    
    /** HTTP Basic auth user property */
    public static final Property authUser = m_model.createDataProperty( NS + "authUser" );
    
    /** HTTP Basic auth password property */
    public static final Property authPwd = m_model.createDataProperty( NS + "authPwd" );

}