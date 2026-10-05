/**
 *  Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>
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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.apache.jena.riot.resultset.ResultSetWriterRegistry;
import org.apache.jena.sparql.resultset.ResultsCompare;
import org.apache.jena.sparql.resultset.SPARQLResult;
import org.apache.jena.sparql.vocabulary.FOAF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A boolean and a result set through the provider, in every results format, and the formats a boolean
 * does not survive in, pinned so a Jena upgrade that changes them is noticed.
 */
public class SPARQLResultProviderTest
{

    public static final MediaType XML = mediaType(ResultSetLang.RS_XML);
    public static final MediaType JSON = mediaType(ResultSetLang.RS_JSON);
    public static final MediaType CSV = mediaType(ResultSetLang.RS_CSV);
    public static final MediaType TSV = mediaType(ResultSetLang.RS_TSV);
    public static final MediaType THRIFT = mediaType(ResultSetLang.RS_Thrift);
    public static final MediaType PROTOBUF = mediaType(ResultSetLang.RS_Protobuf);
    public static final MediaType TURTLE = MediaType.valueOf("text/turtle");

    private SPARQLResultProvider provider;

    private static MediaType mediaType(Lang lang)
    {
        return MediaType.valueOf(lang.getContentType().getContentTypeStr());
    }

    @BeforeEach
    public void init()
    {
        provider = new SPARQLResultProvider();
    }

    /** Plain strings and IRIs only, since CSV keeps neither datatypes nor language tags */
    public static ResultSetRewindable resultSet()
    {
        Model model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createResource("http://example/alice"), FOAF.name, "Alice");
        model.add(ResourceFactory.createResource("http://example/bob"), FOAF.name, "Bob");
        return ResultSetFactory.copyResults(QueryExecutionFactory.create("SELECT ?s ?name WHERE { ?s <" + FOAF.name + "> ?name } ORDER BY ?name", model).execSelect());
    }

    private byte[] write(SPARQLResult result, MediaType mediaType) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        provider.writeTo(result, SPARQLResult.class, null, null, mediaType, new MultivaluedHashMap<>(), out);
        return out.toByteArray();
    }

    private SPARQLResult read(byte[] bytes, MediaType mediaType) throws IOException
    {
        return provider.readFrom(SPARQLResult.class, null, null, mediaType, new MultivaluedHashMap<>(), new ByteArrayInputStream(bytes));
    }

    @Test
    public void testHandlesSPARQLResultInResultsFormatsOnly()
    {
        assertTrue(provider.isReadable(SPARQLResult.class, null, null, JSON));
        assertTrue(provider.isWriteable(SPARQLResult.class, null, null, JSON));
        assertTrue(provider.isWriteable(SPARQLResult.class, null, null, MediaType.valueOf("application/sparql-results+xml;charset=UTF-8")), "parameters are disregarded");

        assertFalse(provider.isReadable(ResultSet.class, null, null, JSON), "a result set is ResultSetProvider's");
        assertFalse(provider.isReadable(ResultSetRewindable.class, null, null, JSON));
        assertFalse(provider.isWriteable(ResultSet.class, null, null, JSON));
        assertFalse(provider.isReadable(SPARQLResult.class, null, null, TURTLE), "RDF is not a results format");
        assertFalse(provider.isWriteable(SPARQLResult.class, null, null, TURTLE));
    }

    @Test
    public void testBooleanRoundTrip() throws IOException
    {
        for (MediaType mediaType : List.of(XML, JSON, CSV))
            for (boolean value : new boolean[]{ true, false })
            {
                SPARQLResult result = read(write(new SPARQLResult(value), mediaType), mediaType);
                assertTrue(result.isBoolean(), mediaType + " reads back as a boolean");
                assertEquals(value, result.getBooleanResult(), mediaType.toString());
            }
    }

    @Test
    public void testBooleanBodies() throws IOException
    {
        assertTrue(new String(write(new SPARQLResult(true), JSON), StandardCharsets.UTF_8).contains("\"boolean\" : true") || new String(write(new SPARQLResult(true), JSON), StandardCharsets.UTF_8).contains("\"boolean\":true"));
        assertTrue(new String(write(new SPARQLResult(false), XML), StandardCharsets.UTF_8).contains("<boolean>false</boolean>"));
        assertTrue(new String(write(new SPARQLResult(true), CSV), StandardCharsets.UTF_8).startsWith("_askResult"));
    }

    @Test
    public void testBooleanWritesTSVButReadsBackAsResultSet() throws IOException
    {
        byte[] tsv = write(new SPARQLResult(true), TSV);
        assertTrue(new String(tsv, StandardCharsets.UTF_8).startsWith("?_askResult"));
        assertTrue(read(tsv, TSV).isResultSet(), "Jena's TSV reader never yields a boolean, so TSV is writable but not readable for one");
    }

    @Test
    public void testBooleanIsNotWriteableInBinaryFormats()
    {
        for (MediaType mediaType : List.of(THRIFT, PROTOBUF))
            assertThrows(RuntimeException.class, () -> write(new SPARQLResult(true), mediaType), mediaType + " has no boolean encoding in Jena");
    }

    @Test
    public void testBooleanLangSetsMatchJena()
    {
        for (Lang lang : SPARQLResultProvider.BOOLEAN_WRITABLE_LANGS) assertTrue(ResultSetWriterRegistry.isRegistered(lang), lang.toString());
        for (Lang lang : SPARQLResultProvider.BOOLEAN_READABLE_LANGS) assertTrue(ResultSetReaderRegistry.isRegistered(lang), lang.toString());

        assertTrue(SPARQLResultProvider.isBooleanWriteable(JSON));
        assertTrue(SPARQLResultProvider.isBooleanWriteable(TSV));
        assertFalse(SPARQLResultProvider.isBooleanReadable(TSV));
        assertFalse(SPARQLResultProvider.isBooleanWriteable(PROTOBUF));
        assertFalse(SPARQLResultProvider.isBooleanReadable(THRIFT));
        assertFalse(SPARQLResultProvider.isBooleanWriteable(MediaType.TEXT_HTML_TYPE));
        assertFalse(SPARQLResultProvider.isBooleanReadable(null));
    }

    @Test
    public void testResultSetRoundTrip() throws IOException
    {
        for (MediaType mediaType : List.of(XML, JSON, CSV, TSV, THRIFT, PROTOBUF))
        {
            SPARQLResult result = read(write(new SPARQLResult(resultSet()), mediaType), mediaType);
            assertTrue(result.isResultSet(), mediaType.toString());
            assertTrue(result.getResultSet() instanceof ResultSetRewindable, "read back rewindable: " + mediaType);
            if (mediaType.equals(CSV))
            {
                // CSV keeps the names and the rows but not the terms: an IRI comes back as a string
                assertEquals(List.of("s", "name"), result.getResultSet().getResultVars());
                assertEquals(2, ResultSetFactory.copyResults(result.getResultSet()).size());
            }
            else assertTrue(ResultsCompare.equalsByTerm(resultSet(), result.getResultSet()), mediaType.toString());
        }
    }

    @Test
    public void testModelResultIsRejected()
    {
        assertThrows(RuntimeException.class, () -> write(new SPARQLResult(ModelFactory.createDefaultModel()), JSON), "a model answers in RDF, through its own provider");
    }

}
