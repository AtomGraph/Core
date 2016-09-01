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
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.NoReaderForLangException;
import org.apache.jena.shared.NoWriterForLangException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 * As class providing access to supported media types.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class MediaTypes
{
    
    /**
     * Media types that can be used to represent of SPARQL result set
     * 
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-success">SPARQL 1.1 Protocol. 2.1.6 Success Responses</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSetRewindable.html">Jena ResultSetRewindable</a>
     */
    private static final javax.ws.rs.core.MediaType[] RESULT_SET_MEDIA_TYPES = new MediaType[]{MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE};
    
    public static final Map<String, String> UTF8_PARAM = new HashMap<>();
    static
    {
        UTF8_PARAM.put("charset", "UTF-8");
    }
    
    private final Map<Class, List<javax.ws.rs.core.MediaType>> readable, writable;
    
    public MediaTypes(Map<Class, List<javax.ws.rs.core.MediaType>> readable, Map<Class, List<javax.ws.rs.core.MediaType>> writable)
    {
	if (readable == null) throw new IllegalArgumentException("Map of readable MediaTypes must be not null");        
	if (writable == null) throw new IllegalArgumentException("Map of writable MediaTypes must be not null");        

        this.readable = Collections.unmodifiableMap(readable);
        this.writable = Collections.unmodifiableMap(writable);
    }

    public MediaTypes()
    {
        this(RDFLanguages.getRegisteredLanguages(), UTF8_PARAM);
    }
    
    public MediaTypes(Collection<Lang> registered, Map<String, String> parameters)
    {
	if (registered == null) throw new IllegalArgumentException("Collection of Langs must be not null");        
        
        Map<Class, List<javax.ws.rs.core.MediaType>> readableMap = new HashMap<>(), writableMap = new HashMap<>();
        List<javax.ws.rs.core.MediaType> readableList = new ArrayList<>(), writableList = new ArrayList<>();

        // Model

        Iterator<Lang> modelLangIt = registered.iterator();
        while (modelLangIt.hasNext())
        {
            Lang lang = modelLangIt.next();
            // we ignore TriX for now because of Jena bug: https://issues.apache.org/jira/browse/JENA-1211
            if (!lang.equals(Lang.RDFNULL) && !lang.equals(Lang.TRIX))
            {
                try
                {
                    if (ModelFactory.createDefaultModel().getReader(lang.getName()) != null)
                    {
                        MediaType mt = new MediaType(lang, UTF8_PARAM);
                        // avoid adding duplicates. Cannot use Set because ordering is important
                        if (!readableList.contains(mt)) readableList.add(mt);
                    }
                }
                catch (NoReaderForLangException ex) {}
                
                try
                {
                    if (ModelFactory.createDefaultModel().getWriter(lang.getName()) != null)
                    {
                        MediaType mt = new MediaType(lang, UTF8_PARAM);                        
                        // avoid adding duplicates. Cannot use Set because ordering is important                        
                        if (!writableList.contains(mt)) writableList.add(mt);
                    }
                }
                catch (NoWriterForLangException ex) {}
            }
        }
        
        MediaType rdfXml = new MediaType(MediaType.APPLICATION_RDF_XML_TYPE.getType(), MediaType.APPLICATION_RDF_XML_TYPE.getSubtype(), parameters);
        readableList.add(0, rdfXml); // first one becomes default
        writableList.add(0, rdfXml); // first one becomes default
        
        readableMap.put(Model.class, Collections.unmodifiableList(readableList));
        writableMap.put(Model.class, Collections.unmodifiableList(writableList));
        
        // ResultSet
        
        readableList = new ArrayList<>();
        writableList = new ArrayList<>();

        Iterator<javax.ws.rs.core.MediaType> resultSetLangIt = Arrays.asList(RESULT_SET_MEDIA_TYPES).iterator();
        while (resultSetLangIt.hasNext())
        {
            javax.ws.rs.core.MediaType resultSetType = resultSetLangIt.next();
            readableList.add(new MediaType(resultSetType.getType(), resultSetType.getSubtype(), parameters));
            writableList.add(new MediaType(resultSetType.getType(), resultSetType.getSubtype(), parameters));            
        }

        readableMap.put(ResultSet.class, Collections.unmodifiableList(readableList));
        writableMap.put(ResultSet.class, Collections.unmodifiableList(writableList));

        // make maps unmodifiable
        
        readable = Collections.unmodifiableMap(readableMap);
        writable = Collections.unmodifiableMap(writableMap);        
    }

    public static List<javax.ws.rs.core.MediaType> getRegistered()
    {
        List<javax.ws.rs.core.MediaType> mediaTypes = new ArrayList<>();
        
        Iterator<Lang> it = RDFLanguages.getRegisteredLanguages().iterator();
        while (it.hasNext())
        {
            Lang lang = it.next();
            if (!lang.equals(Lang.RDFNULL)) mediaTypes.add(new MediaType(lang));
        }

        return mediaTypes;
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
    
}
