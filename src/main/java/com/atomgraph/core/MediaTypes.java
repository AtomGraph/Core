/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atomgraph.core;

import java.nio.charset.StandardCharsets;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.RDFWriterRegistry;
import static org.apache.jena.riot.lang.extra.TurtleJCC.TTLJCC;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;

/**
 * As class providing access to supported media types.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class MediaTypes
{
    
    public static final Map<String, String> UTF8_PARAM = new HashMap<>();
    static
    {
        UTF8_PARAM.put(jakarta.ws.rs.core.MediaType.CHARSET_PARAMETER, StandardCharsets.UTF_8.name());
    }
    
    private final Map<Class, List<jakarta.ws.rs.core.MediaType>> readable, writable;
    
    public static boolean isTriples(jakarta.ws.rs.core.MediaType mediaType)
    {
        jakarta.ws.rs.core.MediaType formatType = new jakarta.ws.rs.core.MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        return lang != null && RDFLanguages.isTriples(lang);
    }
    
    public static boolean isQuads(jakarta.ws.rs.core.MediaType mediaType)
    {
        jakarta.ws.rs.core.MediaType formatType = new jakarta.ws.rs.core.MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        return lang != null && RDFLanguages.isQuads(lang);
    }
    
    public MediaTypes(Map<Class, List<jakarta.ws.rs.core.MediaType>> readable, Map<Class, List<jakarta.ws.rs.core.MediaType>> writable)
    {
        if (readable == null) throw new IllegalArgumentException("Map of readable MediaTypes must be not null");
        if (writable == null) throw new IllegalArgumentException("Map of writable MediaTypes must be not null");

        this.readable = Collections.unmodifiableMap(readable);
        this.writable = Collections.unmodifiableMap(writable);
    }
    
    public MediaTypes()
    {
        Map<Class, List<jakarta.ws.rs.core.MediaType>> readableMap = new HashMap<>(), writableMap = new HashMap<>();

        // Model/Dataset

        List<jakarta.ws.rs.core.MediaType> readableModelList = new ArrayList<>(), writableModelList = new ArrayList<>(),
                readableDatasetList = new ArrayList<>(), writableDatasetList = new ArrayList<>();

        for (Lang lang : RDFParserRegistry.registeredLangTriples())
        {
            if (lang.equals(Lang.RDFNULL)) continue;
            if (lang.equals(TTLJCC)) continue;
            
            final MediaType mt;
            // prioritize reading RDF Thrift and N-Triples because they're most efficient
            // don't add charset=UTF-8 param on readable types
            if (lang.equals(RDFLanguages.RDFTHRIFT)) mt = new MediaType(lang.getContentType()); // q=1
            else
            {
                if (lang.equals(RDFLanguages.NTRIPLES))
                {
                    Map<String, String> qParams = new HashMap<>();
                    qParams.put("q", "0.9");
                    mt = new MediaType(lang.getContentType(), qParams);
                }
                else
                {
                    Map<String, String> qParams = new HashMap<>();
                    qParams.put("q", "0.8");
                    mt = new MediaType(lang.getContentType(), qParams);
                }
            }

            // avoid adding duplicates. Cannot use Set because ordering is important
            if (!readableModelList.contains(mt)) readableModelList.add(mt);
        }
        
        for (Lang lang : RDFParserRegistry.registeredLangQuads())
        {
            if (lang.equals(Lang.RDFNULL)) continue;
            if (lang.equals(TTLJCC)) continue;
            
            final MediaType mt;
            // prioritize reading RDF Thrift and N-Triples because they're most efficient
            // don't add charset=UTF-8 param on readable types
            if (lang.equals(RDFLanguages.RDFTHRIFT)) mt = new MediaType(lang.getContentType()); // q=1
            else
            {
                if (lang.equals(RDFLanguages.NQUADS))
                {
                    Map<String, String> qParams = new HashMap<>();
                    qParams.put("q", "0.9");
                    mt = new MediaType(lang.getContentType(), qParams);
                }
                else
                {
                    Map<String, String> qParams = new HashMap<>();
                    qParams.put("q", "0.8");
                    mt = new MediaType(lang.getContentType(), qParams);
                }
            }

            // avoid adding duplicates. Cannot use Set because ordering is important
            if (!readableDatasetList.contains(mt)) readableDatasetList.add(mt);
        }
        
        for (Lang lang : RDFWriterRegistry.registeredLangs())
        {
            MediaType mtUTF8 = new MediaType(lang.getContentType(), UTF8_PARAM);
            // avoid adding duplicates. Cannot use Set because ordering is important
            if (RDFLanguages.isTriples(lang) && !writableModelList.contains(mtUTF8)) writableModelList.add(mtUTF8);
            if (RDFLanguages.isQuads(lang) && !writableDatasetList.contains(mtUTF8)) writableDatasetList.add(mtUTF8);
        }
        
//        // first MediaType becomes default:
//        readableModelList.add(0, MediaType.APPLICATION_RDF_XML_TYPE); // don't add charset=UTF-8 param on readable types
//        MediaType rdfXmlUtf8 = new MediaType(MediaType.APPLICATION_RDF_XML_TYPE.getType(), MediaType.APPLICATION_RDF_XML_TYPE.getSubtype(), UTF8_PARAM);
//        writableModelList.add(0, rdfXmlUtf8);
        
        readableMap.put(Model.class, Collections.unmodifiableList(readableModelList));
        writableMap.put(Model.class, Collections.unmodifiableList(writableModelList));
        readableMap.put(Dataset.class, Collections.unmodifiableList(readableDatasetList));
        writableMap.put(Dataset.class, Collections.unmodifiableList(writableDatasetList));
        
        // ResultSet
        
        List<jakarta.ws.rs.core.MediaType> readableResultSetList = new ArrayList<>();
        List<jakarta.ws.rs.core.MediaType> writableResultSetList = new ArrayList<>();

        for (Lang lang : ResultSetReaderRegistry.registered())
        {
            if (lang.equals(ResultSetLang.RS_None)) continue;
            
            jakarta.ws.rs.core.MediaType resultSetType = new MediaType(lang.getContentType());
            readableResultSetList.add(new MediaType(resultSetType.getType(), resultSetType.getSubtype())); // don't add charset=UTF-8 param on readable types
            writableResultSetList.add(new MediaType(resultSetType.getType(), resultSetType.getSubtype(), UTF8_PARAM));
        }

        readableMap.put(ResultSet.class, Collections.unmodifiableList(readableResultSetList));
        writableMap.put(ResultSet.class, Collections.unmodifiableList(writableResultSetList));

        // make maps unmodifiable
        
        readable = Collections.unmodifiableMap(readableMap);
        writable = Collections.unmodifiableMap(writableMap);
    }

    /**
     * Returns Java class to JAX-RS media type map.
     * 
     * @return class/type map
     */
    public Map<Class, List<jakarta.ws.rs.core.MediaType>> getReadable()
    {
        return readable;
    }

    public Map<Class, List<jakarta.ws.rs.core.MediaType>> getWritable()
    {
        return writable;
    }
    
    public List<jakarta.ws.rs.core.MediaType> getReadable(Class clazz)
    {
        return getReadable().get(clazz);
    }

    public List<jakarta.ws.rs.core.MediaType> getWritable(Class clazz)
    {
        return getWritable().get(clazz);
    }
    
    @Override
    public String toString()
    {
        return "Readable: " + getReadable().toString() + " Writable: " + getWritable().toString();
    }
    
}
