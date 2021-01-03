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

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 * As class providing access to supported media types.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class MediaTypes
{
    
    /**
     * Registry of readable/writable RDF model and SPARQL result set media types
     */
    private static final javax.ws.rs.core.MediaType[] RESULT_SET_MEDIA_TYPES = new MediaType[]{MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
                            MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE};
    
    public static final Map<String, String> UTF8_PARAM = new HashMap<>();
    static
    {
        UTF8_PARAM.put("charset", "UTF-8");
    }
    
    private final Map<Class, List<javax.ws.rs.core.MediaType>> readable, writable;
    
    public static boolean isTriples(javax.ws.rs.core.MediaType mediaType)
    {
        javax.ws.rs.core.MediaType formatType = new javax.ws.rs.core.MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        return lang != null && RDFLanguages.isTriples(lang);
    }
    
    public static boolean isQuads(javax.ws.rs.core.MediaType mediaType)
    {
        javax.ws.rs.core.MediaType formatType = new javax.ws.rs.core.MediaType(mediaType.getType(), mediaType.getSubtype()); // discard charset param
        Lang lang = RDFLanguages.contentTypeToLang(formatType.toString());
        return lang != null && RDFLanguages.isQuads(lang);
    }
    
    public MediaTypes()
    {
        this(RDFLanguages.getRegisteredLanguages());
    }
    
    public MediaTypes(Map<Class, List<javax.ws.rs.core.MediaType>> readable, Map<Class, List<javax.ws.rs.core.MediaType>> writable)
    {
        if (readable == null) throw new IllegalArgumentException("Map of readable MediaTypes must be not null");
        if (writable == null) throw new IllegalArgumentException("Map of writable MediaTypes must be not null");

        this.readable = Collections.unmodifiableMap(readable);
        this.writable = Collections.unmodifiableMap(writable);
    }
    
    protected MediaTypes(Collection<Lang> registered)
    {
        if (registered == null) throw new IllegalArgumentException("Collection of Langs must be not null");
        
        Map<Class, List<javax.ws.rs.core.MediaType>> readableMap = new HashMap<>(), writableMap = new HashMap<>();

        // Model/Dataset

        List<javax.ws.rs.core.MediaType> readableModelList = new ArrayList<>(), writableModelList = new ArrayList<>(),
                readableDatasetList = new ArrayList<>(), writableDatasetList = new ArrayList<>();

        Iterator<Lang> langIt = registered.iterator();
        while (langIt.hasNext())
        {
            Lang lang = langIt.next();
            if (!lang.equals(Lang.RDFNULL))
            {
                if (RDFLanguages.isTriples(lang))
                {
                    final MediaType mt;
                    // prioritize reading RDF Thrift and N-Triples because they're most efficient
                    // don't add charset=UTF-8 param on readable types
                    if (lang.equals(RDFLanguages.RDFTHRIFT)) mt = new MediaType(lang); // q=1
                    else
                    {
                        if (lang.equals(RDFLanguages.NTRIPLES))
                        {
                            Map<String, String> qParams = new HashMap<>();
                            qParams.put("q", "0.9");
                            mt = new MediaType(lang, qParams);
                        }
                        else
                        {
                            Map<String, String> qParams = new HashMap<>();
                            qParams.put("q", "0.8");
                            mt = new MediaType(lang, qParams);
                        }
                    }
                    
                    // avoid adding duplicates. Cannot use Set because ordering is important
                    if (!readableModelList.contains(mt)) readableModelList.add(mt);

                    MediaType mtUTF8 = new MediaType(lang, UTF8_PARAM);
                    // avoid adding duplicates. Cannot use Set because ordering is important
                    if (!writableModelList.contains(mtUTF8)) writableModelList.add(mtUTF8);
                }
                
                if (RDFLanguages.isQuads(lang))
                {
                    final MediaType mt;
                    // prioritize reading RDF Thrift and N-Triples because they're most efficient
                    // don't add charset=UTF-8 param on readable types
                    if (lang.equals(RDFLanguages.RDFTHRIFT)) mt = new MediaType(lang); // q=1
                    else
                    {
                        if (lang.equals(RDFLanguages.NQUADS))
                        {
                            Map<String, String> qParams = new HashMap<>();
                            qParams.put("q", "0.9");
                            mt = new MediaType(lang, qParams);
                        }
                        else
                        {
                            Map<String, String> qParams = new HashMap<>();
                            qParams.put("q", "0.8");
                            mt = new MediaType(lang, qParams);
                        }
                    }
                    
                    // avoid adding duplicates. Cannot use Set because ordering is important
                    if (!readableDatasetList.contains(mt)) readableDatasetList.add(mt);
                    
                    MediaType mtUTF8 = new MediaType(lang, UTF8_PARAM);
                    // avoid adding duplicates. Cannot use Set because ordering is important
                    if (!writableDatasetList.contains(mtUTF8)) writableDatasetList.add(mtUTF8);
                }
            }
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
        
        List<javax.ws.rs.core.MediaType> readableResultSetList = new ArrayList<>();
        List<javax.ws.rs.core.MediaType> writableResultSetList = new ArrayList<>();

        Iterator<javax.ws.rs.core.MediaType> resultSetLangIt = Arrays.asList(RESULT_SET_MEDIA_TYPES).iterator();
        while (resultSetLangIt.hasNext())
        {
            javax.ws.rs.core.MediaType resultSetType = resultSetLangIt.next();
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
    public Map<Class, List<javax.ws.rs.core.MediaType>> getReadable()
    {
        return readable;
    }

    public Map<Class, List<javax.ws.rs.core.MediaType>> getWritable()
    {
        return writable;
    }
    
    public List<javax.ws.rs.core.MediaType> getReadable(Class clazz)
    {
        return getReadable().get(clazz);
    }

    public List<javax.ws.rs.core.MediaType> getWritable(Class clazz)
    {
        return getWritable().get(clazz);
    }
    
    @Override
    public String toString()
    {
        return "Readable: " + getReadable().toString() + " Writable: " + getWritable().toString();
    }
    
}
