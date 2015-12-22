/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.core;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import java.util.ArrayList;
import java.util.Arrays;
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
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class MediaTypes
{
    //private final List<javax.ws.rs.core.MediaType> modelMediaTypes;
    //private final List<javax.ws.rs.core.MediaType> resultSetMediaTypes;
    private final Map<Class, List<javax.ws.rs.core.MediaType>> classTypes;
    
    /**
     * Media types that can be used to represent of SPARQL result set
     * 
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-success">SPARQL 1.1 Protocol. 2.1.6 Success Responses</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSetRewindable.html">Jena ResultSetRewindable</a>
     */
    private static final javax.ws.rs.core.MediaType[] RESULT_SET_MEDIA_TYPES = new MediaType[]{MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE};
    
    public MediaTypes(Map<Class, List<javax.ws.rs.core.MediaType>> classTypes)
    {
        this.classTypes = classTypes;
    }

    public MediaTypes()
    {
        classTypes = new HashMap<>();

        List<javax.ws.rs.core.MediaType> list = new ArrayList<>();
        Map<String, String> utf8Param = new HashMap<>();
        utf8Param.put("charset", "UTF-8");

        // Model
        
        Iterator<javax.ws.rs.core.MediaType> it = getRegistered().iterator();
        while (it.hasNext())
        {
            javax.ws.rs.core.MediaType registered = it.next();
            list.add(new MediaType(registered.getType(), registered.getSubtype(), utf8Param));
        }
        
        MediaType rdfXml = new MediaType(org.graphity.core.MediaType.APPLICATION_RDF_XML_TYPE.getType(), org.graphity.core.MediaType.APPLICATION_RDF_XML_TYPE.getSubtype(), utf8Param);
        list.add(0, rdfXml); // first one becomes default
        
        classTypes.put(Model.class, Collections.unmodifiableList(list));

        // ResultSet
        
        list = new ArrayList<>();

        it = Arrays.asList(RESULT_SET_MEDIA_TYPES).iterator();
        while (it.hasNext())
        {
            javax.ws.rs.core.MediaType registered = it.next();
            list.add(new MediaType(registered.getType(), registered.getSubtype(), utf8Param));
        }

        classTypes.put(ResultSet.class, Collections.unmodifiableList(list));
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
    protected Map<Class, List<javax.ws.rs.core.MediaType>> getClassTypes()
    {
        return classTypes;
    }
    
    public List<javax.ws.rs.core.MediaType> forClass(Class clazz)
    {
        return getClassTypes().get(clazz);
    }
    
}
