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
package org.graphity.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 * Extends standard JAX-RS media type with RDF media types
 * @see <a href="http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html">javax.ws.rs.core.MediaType</a>
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class MediaType extends javax.ws.rs.core.MediaType
{
    /** "application/rdf+xml" */
    public final static String APPLICATION_RDF_XML = Lang.RDFXML.getContentType().getContentType();
    /** "application/rdf+xml" */
    public final static MediaType APPLICATION_RDF_XML_TYPE = new MediaType(Lang.RDFXML.getContentType().getType(), Lang.RDFXML.getContentType().getSubType());

    /** "text/turtle" */
    public final static String TEXT_TURTLE = Lang.TURTLE.getContentType().getContentType();
    /** "text/turtle" */
    public final static MediaType TEXT_TURTLE_TYPE = new MediaType(Lang.TURTLE.getContentType().getType(), Lang.TURTLE.getContentType().getSubType());

    /** "text/trig" */
    public final static String TEXT_TRIG = Lang.TRIG.getContentType().getContentType();
    /** "text/trig" */
    public final static MediaType TEXT_TRIG_TYPE = new MediaType(Lang.TRIG.getContentType().getType(), Lang.TRIG.getContentType().getSubType());

    /** "application/n-triples" */
    public final static String TEXT_NTRIPLES = Lang.NTRIPLES.getContentType().getContentType();
    /** "application/n-triples" */
    public final static MediaType TEXT_NTRIPLES_TYPE = new MediaType(Lang.NTRIPLES.getContentType().getType(), Lang.NTRIPLES.getContentType().getSubType());

    /** "application/n-quads" */
    public final static String TEXT_NQUADS = Lang.NTRIPLES.getContentType().getContentType();
    /** "application/n-quads" */
    public final static MediaType TEXT_NQUADS_TYPE = new MediaType(Lang.NQUADS.getContentType().getType(), Lang.NQUADS.getContentType().getSubType());

    /** "application/sparql-results+xml" */
    public final static String APPLICATION_SPARQL_RESULTS_XML = "application/sparql-results+xml";
    /** "application/sparql-results+xml" */
    public final static MediaType APPLICATION_SPARQL_RESULTS_XML_TYPE = new MediaType("application","sparql-results+xml");

    /** "application/sparql-results+json" */
    public final static String APPLICATION_SPARQL_RESULTS_JSON = "application/sparql-results+json";
    /** "application/sparql-results+json" */
    public final static MediaType APPLICATION_SPARQL_RESULTS_JSON_TYPE = new MediaType("application","sparql-results+json");

    /** "application/sparql-query" */
    public final static String APPLICATION_SPARQL_QUERY = "application/sparql-query";
    /** "application/sparql-query" */
    public final static MediaType APPLICATION_SPARQL_QUERY_TYPE = new MediaType("application","sparql-query");

    /** "application/sparql-update" */
    public final static String APPLICATION_SPARQL_UPDATE = "application/sparql-update";
    /** "application/sparql-update" */
    public final static MediaType APPLICATION_SPARQL_UPDATE_TYPE = new MediaType("application","sparql-update");
    
    /** "application/ld+json" */
    public final static String APPLICATION_LD_JSON = "application/ld+json";
    /** "application/ld+json" */
    public final static MediaType APPLICATION_LD_JSON_TYPE = new MediaType("application","ld+json");
    
    public MediaType(String type, String subtype, Map<String, String> parameters)
    {
	super(type, subtype, parameters);
    }

    public MediaType(String type, String subtype)
    {
        super(type,subtype);
    }

    public MediaType()
    {
        super();
    }

    public static javax.ws.rs.core.MediaType[] getRegistered()
    {
        List<javax.ws.rs.core.MediaType> list = getRegisteredList();
        javax.ws.rs.core.MediaType[] array = new javax.ws.rs.core.MediaType[list.size()];
        list.toArray(array);
        return array;
    }
    
    public static List<javax.ws.rs.core.MediaType> getRegisteredList()
    {
        List<javax.ws.rs.core.MediaType> mediaTypes = new ArrayList<>();
        
        Iterator<Lang> it = RDFLanguages.getRegisteredLanguages().iterator();
        while (it.hasNext())
        {
            Lang lang = it.next();
            if (!lang.equals(Lang.RDFNULL))
            {
                ContentType ct = lang.getContentType();
                mediaTypes.add(new javax.ws.rs.core.MediaType(ct.getType(), ct.getSubType()));
            }
        }

        return mediaTypes;
    }

}
