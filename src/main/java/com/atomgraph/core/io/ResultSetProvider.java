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
package com.atomgraph.core.io;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.jena.sparql.resultset.CSVInput;
import org.apache.jena.sparql.resultset.TSVInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for writing SPARQL result set to the response.
 * Needs to be registered in the JAX-RS application.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a>
 * @see org.apache.jena.query.ResultSet
 * @see javax.ws.rs.ext.MessageBodyReader
 * @see javax.ws.rs.ext.MessageBodyWriter
 */
@Provider
public class ResultSetProvider implements MessageBodyReader<ResultSetRewindable>, MessageBodyWriter<ResultSet>
{
    private static final Logger log = LoggerFactory.getLogger(ResultSetProvider.class);
    
    public static final List<MediaType> RESULT_SET_TYPES = new ArrayList<>();
    static
    {
        RESULT_SET_TYPES.add(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE);
        RESULT_SET_TYPES.add(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE);
        RESULT_SET_TYPES.add(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_CSV_TYPE);
        RESULT_SET_TYPES.add(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_TSV_TYPE);
    }
    
    public static boolean isResultSetType(MediaType mediaType)
    {
        for (MediaType mt : RESULT_SET_TYPES)
            if (mediaType.isCompatible(mt)) return true;

        return false;
    }
    
    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, javax.ws.rs.core.MediaType mediaType)
    {
        return type == ResultSetRewindable.class && isResultSetType(mediaType);
    }

    @Override
    public ResultSetRewindable readFrom(Class<ResultSetRewindable> type, Type type1, Annotation[] antns, javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream in) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Reading ResultSet with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);
        // result set needs to be rewindable because results might be processed multiple times, e.g. to calculate hash and write response
        // TO-DO: construct Jena's ResultFormat and then pass to ResultSet.load(in, format)
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE))
            return ResultSetFactory.makeRewindable(ResultSetFactory.fromXML(in));
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE))
            return ResultSetFactory.makeRewindable(ResultSetFactory.fromJSON(in));
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_CSV_TYPE))
            return ResultSetFactory.makeRewindable(CSVInput.fromCSV(in));
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_CSV_TYPE))
            return ResultSetFactory.makeRewindable(TSVInput.fromTSV(in));
        
        throw new IllegalStateException("ResultSet MediaType should be readable but no Jena reader matched");
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return ResultSet.class.isAssignableFrom(type) && isResultSetType(mediaType);
    }

    @Override
    public long getSize(ResultSet t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(ResultSet results, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
    {
        if (log.isTraceEnabled()) log.trace("Writing ResultSet with HTTP headers: {} MediaType: {}", httpHeaders, mediaType);

        //  TO-DO: construct Jena's ResultFormat and then pass to ResultSetFormatter.output(outStream, resultSet, rFmt)
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE))
        {
            ResultSetFormatter.outputAsXML(entityStream, results);
            return;
        }
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE))
        {
            ResultSetFormatter.outputAsJSON(entityStream, results);
            return;
        }
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_CSV_TYPE))
        {
            ResultSetFormatter.outputAsCSV(entityStream, results);
            return;
        }
        if (mediaType.isCompatible(com.atomgraph.core.MediaType.APPLICATION_SPARQL_RESULTS_TSV_TYPE))
        {
            ResultSetFormatter.outputAsTSV(entityStream, results);
            return;
        }
        
        throw new IllegalStateException("ResultSet MediaType should be writable but no Jena writer matched");
    }
    
}
