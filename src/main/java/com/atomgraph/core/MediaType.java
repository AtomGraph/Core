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
package com.atomgraph.core;

import java.util.Map;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.resultset.ResultSetLang;

/**
 * Extends standard JAX-RS media type with RDF media types
 * 
 * @see <a href="https://jakarta.ee/specifications/restful-ws/3.0/apidocs/jakarta/ws/rs/core/mediatype">jakarta.ws.rs.core.MediaType</a>
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class MediaType extends jakarta.ws.rs.core.MediaType
{
    
    /** "application/rdf+xml" */
    public final static String APPLICATION_RDF_XML = Lang.RDFXML.getContentType().getContentTypeStr();
    /** "application/rdf+xml" */
    public final static MediaType APPLICATION_RDF_XML_TYPE = new MediaType(Lang.RDFXML.getContentType().getType(), Lang.RDFXML.getContentType().getSubType());

    /** "text/turtle" */
    public final static String TEXT_TURTLE = Lang.TURTLE.getContentType().getContentTypeStr();
    /** "text/turtle" */
    public final static MediaType TEXT_TURTLE_TYPE = new MediaType(Lang.TURTLE.getContentType().getType(), Lang.TURTLE.getContentType().getSubType());

    /** "text/trig" */
    public final static String TEXT_TRIG = Lang.TRIG.getContentType().getContentTypeStr();
    /** "text/trig" */
    public final static MediaType TEXT_TRIG_TYPE = new MediaType(Lang.TRIG.getContentType().getType(), Lang.TRIG.getContentType().getSubType());

    /** "application/n-triples" */
    public final static String APPLICATION_NTRIPLES = Lang.NTRIPLES.getContentType().getContentTypeStr();
    /** "application/n-triples" */
    public final static MediaType APPLICATION_NTRIPLES_TYPE = new MediaType(Lang.NTRIPLES.getContentType().getType(), Lang.NTRIPLES.getContentType().getSubType());

    /** "application/n-quads" */
    public final static String TEXT_NQUADS = Lang.NQUADS.getContentType().getContentTypeStr();
    /** "application/n-quads" */
    public final static MediaType TEXT_NQUADS_TYPE = new MediaType(Lang.NQUADS.getContentType().getType(), Lang.NQUADS.getContentType().getSubType());

    /** "application/ld+json" */
    public final static String APPLICATION_LD_JSON = Lang.JSONLD.getContentType().getContentTypeStr();
    /** "application/ld+json" */
    public final static MediaType APPLICATION_LD_JSON_TYPE = new MediaType(Lang.JSONLD.getContentType().getType(), Lang.JSONLD.getContentType().getSubType());

    /** "application/sparql-results+xml" */
    public final static String APPLICATION_SPARQL_RESULTS_XML = ResultSetLang.RS_XML.getContentType().getContentTypeStr();
    /** "application/sparql-results+xml" */
    public final static MediaType APPLICATION_SPARQL_RESULTS_XML_TYPE = new MediaType(ResultSetLang.RS_XML.getContentType().getType(), ResultSetLang.RS_XML.getContentType().getSubType());

    /** "application/sparql-results+json" */
    public final static String APPLICATION_SPARQL_RESULTS_JSON = ResultSetLang.RS_JSON.getContentType().getContentTypeStr();
    /** "application/sparql-results+json" */
    public final static MediaType APPLICATION_SPARQL_RESULTS_JSON_TYPE = new MediaType(ResultSetLang.RS_JSON.getContentType().getType(), ResultSetLang.RS_JSON.getContentType().getSubType());

    /** "text/csv" */
    public final static String APPLICATION_SPARQL_RESULTS_CSV = ResultSetLang.RS_CSV.getContentType().getContentTypeStr();
    /** "text/csv" */
    public final static MediaType APPLICATION_SPARQL_RESULTS_CSV_TYPE = new MediaType(ResultSetLang.RS_CSV.getContentType().getType(), ResultSetLang.RS_CSV.getContentType().getSubType());
    
    /** "text/tab-separated-values" */
    public final static String APPLICATION_SPARQL_RESULTS_TSV = ResultSetLang.RS_TSV.getContentType().getContentTypeStr();
    /** "text/tab-separated-values" */
    public final static MediaType APPLICATION_SPARQL_RESULTS_TSV_TYPE = new MediaType(ResultSetLang.RS_TSV.getContentType().getType(), ResultSetLang.RS_TSV.getContentType().getSubType());

    /** "application/sparql-query" */
    public final static String APPLICATION_SPARQL_QUERY = "application/sparql-query";
    /** "application/sparql-query" */
    public final static MediaType APPLICATION_SPARQL_QUERY_TYPE = new MediaType("application", "sparql-query");

    /** "application/sparql-update" */
    public final static String APPLICATION_SPARQL_UPDATE = "application/sparql-update";
    /** "application/sparql-update" */
    public final static MediaType APPLICATION_SPARQL_UPDATE_TYPE = new MediaType("application", "sparql-update");
    
    /** "application/rdf+x-www-form-urlencoded" */
    public final static String APPLICATION_RDF_URLENCODED = "application/rdf+x-www-form-urlencoded";
    /** "application/rdf+x-www-form-urlencoded" */
    public final static MediaType APPLICATION_RDF_URLENCODED_TYPE = new MediaType("application","rdf+x-www-form-urlencoded");

    public MediaType(Lang lang)
    {
        this(lang.getContentType());
    }

    public MediaType(Lang lang, Map<String, String> parameters)
    {
        this(lang.getContentType(), parameters);
    }
    
    public MediaType(ContentType ct)
    {
        this(ct.getType(), ct.getSubType());
    }

    public MediaType(ContentType ct, Map<String, String> parameters)
    {
        this(ct.getType(), ct.getSubType(), parameters);
    }
    
    public MediaType(String type, String subtype, Map<String, String> parameters)
    {
        super(type, subtype, parameters);
    }

    public MediaType(String type, String subtype)
    {
        super(type, subtype);
    }

    public MediaType()
    {
        super();
    }

}
