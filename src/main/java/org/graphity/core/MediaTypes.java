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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 * As class providing access to supported media types.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class MediaTypes
{

    /**
     * Media types that can be used to represent of SPARQL result set
     * 
     * @see <a href="http://www.w3.org/TR/sparql11-protocol/#query-success">SPARQL 1.1 Protocol. 2.1.6 Success Responses</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/ResultSetRewindable.html">Jena ResultSetRewindable</a>
     */
    public static final javax.ws.rs.core.MediaType[] RESULT_SET_MEDIA_TYPES = new javax.ws.rs.core.MediaType[]{org.graphity.core.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    org.graphity.core.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE};

    // private MediaTypes() { }

    protected List<javax.ws.rs.core.MediaType> getRegistered() // static
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

    /**
     * Returns supported RDF model media types.
     * 
     * @return list of media types
     */
    public List<javax.ws.rs.core.MediaType> getModelMediaTypes()
    {
        List<javax.ws.rs.core.MediaType> list = new ArrayList<>();
        Map<String, String> utf8Param = new HashMap<>();
        utf8Param.put("charset", "UTF-8");
        
        Iterator<javax.ws.rs.core.MediaType> it = getRegistered().iterator();
        while (it.hasNext())
        {
            javax.ws.rs.core.MediaType registered = it.next();
            list.add(new MediaType(registered.getType(), registered.getSubtype(), utf8Param));
        }
        
        MediaType rdfXml = new MediaType(org.graphity.core.MediaType.APPLICATION_RDF_XML_TYPE.getType(), org.graphity.core.MediaType.APPLICATION_RDF_XML_TYPE.getSubtype(), utf8Param);
        list.add(0, rdfXml); // first one becomes default
        
        return list;
    }
    
    /**
     * Returns supported SPARQL result set media types.
     * 
     * @return list of media types
     */
    public List<javax.ws.rs.core.MediaType> getResultSetMediaTypes()
    {
        List<javax.ws.rs.core.MediaType> list = new ArrayList<>();
        Map<String, String> utf8Param = new HashMap<>();
        utf8Param.put("charset", "UTF-8");

        Iterator<javax.ws.rs.core.MediaType> it = Arrays.asList(RESULT_SET_MEDIA_TYPES).iterator();
        while (it.hasNext())
        {
            javax.ws.rs.core.MediaType registered = it.next();
            list.add(new javax.ws.rs.core.MediaType(registered.getType(), registered.getSubtype(), utf8Param));
        }

        return list;
    }

}
