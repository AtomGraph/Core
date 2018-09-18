/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.core.riot.lang;

import com.atomgraph.core.riot.RDFLanguages;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDFLib;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class RDFPostReaderTest
{
    public static final String ENC = StandardCharsets.UTF_8.name();
    
    public String validRDFPost;
    private Model validExpected;
    
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        RDFLanguages.register(RDFLanguages.RDFPOST);
        RDFParserRegistry.registerLangTriples(RDFLanguages.RDFPOST, new RDFPostReaderFactory());
    }
    
    @Before
    public void setUp() throws UnsupportedEncodingException
    {
        validRDFPost = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://dc.org/#title", ENC) + "&ol=" + URLEncoder.encode("title", ENC) + "&ll=da" +
        "&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&ou=" + URLEncoder.encode("http://object1", ENC) +
                                                        "&pu=" + URLEncoder.encode("http://predicate2", ENC) + "&ou=" + URLEncoder.encode("http://object2", ENC) +
                                                                                                            "&ou=" + URLEncoder.encode("http://object3", ENC) +
        "&su=" + URLEncoder.encode("http://subject2", ENC) + "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ol=" + URLEncoder.encode("literal1", ENC) +
        "&su=" + URLEncoder.encode("http://subject3", ENC) + "&pu=" + URLEncoder.encode("http://predicate4", ENC) + "&ol=" + URLEncoder.encode("literal2", ENC) + "&ll=da" +
        "&su=" + URLEncoder.encode("http://subject4", ENC) + "&pu=" + URLEncoder.encode("http://predicate5", ENC) + "&ol=" + URLEncoder.encode("literal3", ENC) + "&lt=" + URLEncoder.encode("http://type", ENC) +
                                                        "&pu=" + URLEncoder.encode("http://dct.org/#hasPart", ENC) + "&ob=" + URLEncoder.encode("b1", ENC) +
        "&sb=" + URLEncoder.encode("b1", ENC) + "&pu=" + URLEncoder.encode("http://rdf.org/#first", ENC) + "&ou=" + URLEncoder.encode("http://something/", ENC) +
                                            "&pu=" + URLEncoder.encode("http://rdf.org/#rest", ENC) + "&ou=" + URLEncoder.encode("http://rdf.org/#nil", ENC);    


        RDFDatatype datatype = TypeMapper.getInstance().getSafeTypeByName("http://type");

        validExpected = ModelFactory.createDefaultModel();
        validExpected.add(validExpected.createResource("http://subject1"), validExpected.createProperty("http://dc.org/#title"), validExpected.createLiteral("title", "da")).
                add(validExpected.createResource("http://subject1"), validExpected.createProperty("http://predicate1"), validExpected.createResource("http://object1")).
                add(validExpected.createResource("http://subject1"), validExpected.createProperty("http://predicate2"), validExpected.createResource("http://object2")).
                add(validExpected.createResource("http://subject1"), validExpected.createProperty("http://predicate2"), validExpected.createResource("http://object3")).
                add(validExpected.createResource("http://subject2"), validExpected.createProperty("http://predicate3"), validExpected.createLiteral("literal1")).
                add(validExpected.createResource("http://subject3"), validExpected.createProperty("http://predicate4"), validExpected.createLiteral("literal2", "da")).
                add(validExpected.createResource("http://subject4"), validExpected.createProperty("http://predicate5"), validExpected.createTypedLiteral("literal3", datatype)).
                add(validExpected.createResource("http://subject4"), validExpected.createProperty("http://dct.org/#hasPart"), validExpected.createResource(AnonId.create("b1"))).
                add(validExpected.createResource(AnonId.create("b1")), validExpected.createProperty("http://rdf.org/#first"), validExpected.createResource("http://something/")).
                add(validExpected.createResource(AnonId.create("b1")), validExpected.createProperty("http://rdf.org/#rest"), validExpected.createResource("http://rdf.org/#nil"));
    }

    /**
     * The simple parsing method only has this one test, others are for the streaming RIOT version
     * @throws URISyntaxException 
     */
    @Test
    public void testValidBodySimpleParse() throws URISyntaxException
    {
        Model parsed = new RDFPostReader().parse(validRDFPost, ENC);
        
        assertIsomorphic(validExpected, parsed);
    }
    
    public ReaderRIOT createRIOTParser()
    {
        ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStrict; // throw exceptions on all parse errors
        ParserProfile parserProfile = RiotLib.profile("http://base", true, true, errorHandler);
        ReaderRIOT parser = RDFDataMgr.createReader(RDFLanguages.RDFPOST);
        parser.setErrorHandler(errorHandler);
        parser.setParserProfile(parserProfile);
        return parser;
    }
    
    /**
     * Test of parse method, of class RDFPostReader.
     */
    @Test
    public void testValidBodyStreamingParse()
    {
        Model parsed = ModelFactory.createDefaultModel();

        createRIOTParser().read(new ByteArrayInputStream(validRDFPost.getBytes()), "http://base", null, StreamRDFLib.graph(parsed.getGraph()), null);
        
        assertIsomorphic(validExpected, parsed);
    }

    @Test
    public void testWithRandomParams() throws UnsupportedEncodingException
    {
        String random = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&x=123" + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&ol=" + URLEncoder.encode("literal", ENC) + "&ZZZ=pu" +
            "&su=" + URLEncoder.encode("http://subject2", ENC) +  "&q=42" + "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ol=" + URLEncoder.encode("literal1", ENC);
        Model randomParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(random.getBytes()), "http://base", null, StreamRDFLib.graph(randomParsed.getGraph()), null);
        
        assertTrue(randomParsed.isEmpty());
    }
    
    @Test
    public void testSkipMissingPredicate() throws UnsupportedEncodingException
    {
        String skipToNextSubject = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&ol=" + URLEncoder.encode("literal", ENC) +
            "&su=" + URLEncoder.encode("http://subject2", ENC) + "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ol=" + URLEncoder.encode("literal1", ENC);
        Model expected = ModelFactory.createDefaultModel();
        expected.add(expected.createResource("http://subject2"), expected.createProperty("http://predicate3"), expected.createLiteral("literal1"));
        Model parsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextSubject.getBytes()), "http://base", null, StreamRDFLib.graph(parsed.getGraph()), null);
        
        assertIsomorphic(expected, parsed);
    }
    
    @Test
    public void testSkipMissingPredicateLocalName() throws UnsupportedEncodingException
    {
        String skipToNextSubject = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pn=" + URLEncoder.encode("http://ns/", ENC) + "&ol=" + URLEncoder.encode("literal", ENC) +
            "&su=" + URLEncoder.encode("http://subject2", ENC) + "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ol=" + URLEncoder.encode("literal1", ENC);
        Model expected = ModelFactory.createDefaultModel();
        expected.add(expected.createResource("http://subject2"), expected.createProperty("http://predicate3"), expected.createLiteral("literal1"));
        Model parsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextSubject.getBytes()), "http://base", null, StreamRDFLib.graph(parsed.getGraph()), null);
        
        assertIsomorphic(expected, parsed);
    }
    
    @Test
    public void testSkipMissingObject() throws UnsupportedEncodingException
    {
        String skipToNextSubject = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://dc.org/#title", ENC) + 
            "&su=" + URLEncoder.encode("http://subject2", ENC) + "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ol=" + URLEncoder.encode("literal1", ENC);                
        Model skipToNextSubjectExp = ModelFactory.createDefaultModel();
        skipToNextSubjectExp.add(skipToNextSubjectExp.createResource("http://subject2"), skipToNextSubjectExp.createProperty("http://predicate3"), skipToNextSubjectExp.createLiteral("literal1"));
        Model skipToNextSubjectParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextSubject.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextSubjectParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextSubjectExp, skipToNextSubjectParsed);
        
        String skipToNextPredicate = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://dc.org/#title", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&ol=" + URLEncoder.encode("literal", ENC);
        Model skipToNextPredicateExp = ModelFactory.createDefaultModel();
        skipToNextPredicateExp.add(skipToNextPredicateExp.createResource("http://subject1"), skipToNextPredicateExp.createProperty("http://predicate1"), skipToNextPredicateExp.createLiteral("literal"));
        Model skipToNextPredicateParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextPredicate.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextPredicateParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextPredicateExp, skipToNextPredicateParsed);
    }
    
    @Test
    public void testSkipMissingObjectLocalName() throws UnsupportedEncodingException
    {
        String skipToNextSubject = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&on=" + URLEncoder.encode("http://ns/", ENC) +
            "&su=" + URLEncoder.encode("http://subject2", ENC) + "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ol=" + URLEncoder.encode("literal1", ENC);
        Model skipToNextSubjectExp = ModelFactory.createDefaultModel();
        skipToNextSubjectExp.add(skipToNextSubjectExp.createResource("http://subject2"), skipToNextSubjectExp.createProperty("http://predicate3"), skipToNextSubjectExp.createLiteral("literal1"));
        Model skipToNextSubjectParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextSubject.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextSubjectParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextSubjectExp, skipToNextSubjectParsed);

        String skipToNextPredicate = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&on=" + URLEncoder.encode("http://ns/", ENC) +
            "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ol=" + URLEncoder.encode("literal1", ENC);
        Model skipToNextPredicateExp = ModelFactory.createDefaultModel();
        skipToNextPredicateExp.add(skipToNextPredicateExp.createResource("http://subject1"), skipToNextPredicateExp.createProperty("http://predicate3"), skipToNextPredicateExp.createLiteral("literal1"));
        Model skipToNextPredicateParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextPredicate.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextPredicateParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextPredicateExp, skipToNextPredicateParsed);
    }
    
    @Test
    public void testSkipMissingDatatype() throws UnsupportedEncodingException
    {
        String skipToNextNonLiteralObject = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&lt=" + URLEncoder.encode("http://type", ENC) + "ll=da" +
            "&ou=" + URLEncoder.encode("http://object1", ENC);
        Model skipToNextNonLiteralObjectExp = ModelFactory.createDefaultModel();
        skipToNextNonLiteralObjectExp.add(skipToNextNonLiteralObjectExp.createResource("http://subject1"), skipToNextNonLiteralObjectExp.createProperty("http://predicate1"), skipToNextNonLiteralObjectExp.createResource("http://object1"));
        Model skipToNextNonLiteralObjectParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextNonLiteralObject.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextNonLiteralObjectParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextNonLiteralObjectExp, skipToNextNonLiteralObjectParsed);

        String skipToNextPredicate = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&lt=" + URLEncoder.encode("http://type", ENC) + "ll=da" +
            "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ou=" + URLEncoder.encode("http://object1", ENC);
        Model skipToNextPredicateExp = ModelFactory.createDefaultModel();
        skipToNextPredicateExp.add(skipToNextPredicateExp.createResource("http://subject1"), skipToNextPredicateExp.createProperty("http://predicate3"), skipToNextPredicateExp.createResource("http://object1"));
        Model skipToNextPredicateParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextPredicate.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextPredicateParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextPredicateExp, skipToNextPredicateParsed);
    }
    
    @Test
    public void testSkipMissingLangTag() throws UnsupportedEncodingException
    {
        String skipToNextNonLiteralObject = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&ll=da" + "&lt=" + URLEncoder.encode("http://type", ENC) +
            "&ou=" + URLEncoder.encode("http://object1", ENC);
        Model skipToNextNonLiteralObjectExp = ModelFactory.createDefaultModel();
        skipToNextNonLiteralObjectExp.add(skipToNextNonLiteralObjectExp.createResource("http://subject1"), skipToNextNonLiteralObjectExp.createProperty("http://predicate1"), skipToNextNonLiteralObjectExp.createResource("http://object1"));
        Model skipToNextNonLiteralObjectParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextNonLiteralObject.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextNonLiteralObjectParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextNonLiteralObjectExp, skipToNextNonLiteralObjectParsed);

        String skipToNextPredicate = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://predicate1", ENC) + "&ll=da" + "&lt=" + URLEncoder.encode("http://type", ENC) +
            "&pu=" + URLEncoder.encode("http://predicate3", ENC) + "&ou=" + URLEncoder.encode("http://object1", ENC);
        Model skipToNextPredicateExp = ModelFactory.createDefaultModel();
        skipToNextPredicateExp.add(skipToNextPredicateExp.createResource("http://subject1"), skipToNextPredicateExp.createProperty("http://predicate3"), skipToNextPredicateExp.createResource("http://object1"));
        Model skipToNextPredicateParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToNextPredicate.getBytes()), "http://base", null, StreamRDFLib.graph(skipToNextPredicateParsed.getGraph()), null);
        
        assertIsomorphic(skipToNextPredicateExp, skipToNextPredicateParsed);
    }

    @Test
    public void testSkipMissingObjectToEOF() throws UnsupportedEncodingException
    {
        String skipToEOF = "&rdf=&su=" + URLEncoder.encode("http://subject1", ENC) + "&pu=" + URLEncoder.encode("http://dc.org/#title", ENC) + "&lt=" + URLEncoder.encode("http://type", ENC);
        Model skipToEOFExp = ModelFactory.createDefaultModel();
        Model skiptToEOFParsed = ModelFactory.createDefaultModel();
        createRIOTParser().read(new ByteArrayInputStream(skipToEOF.getBytes()), "http://base", null, StreamRDFLib.graph(skiptToEOFParsed.getGraph()), null);
        
        assertIsomorphic(skipToEOFExp, skiptToEOFParsed);
    }
    
    public static void assertIsomorphic(Model wanted, Model got)
    {
        if (!wanted.isIsomorphicWith(got))
            fail("Models not isomorphic (not structurally equal))");
    }
    
}
