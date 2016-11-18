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

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.net.URLEncoder;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@RunWith(JUnit4.class)
public class RDFPostReaderTest
{
    public static String POST_BODY = "&rdf=&su=" + URLEncoder.encode("http://subject1") + "&pu=" + URLEncoder.encode("http://dc.org/#title") + "&ol=" + URLEncoder.encode("title") + "&ll=da" +
    "&su=" + URLEncoder.encode("http://subject1") + "&pu=" + URLEncoder.encode("http://predicate1") + "&ou=" + URLEncoder.encode("http://object1") +
						    "&pu=" + URLEncoder.encode("http://predicate2") + "&ou=" + URLEncoder.encode("http://object2") +
													"&ou=" + URLEncoder.encode("http://object3") +
    "&su=" + URLEncoder.encode("http://subject2") + "&pu=" + URLEncoder.encode("http://predicate3") + "&ol=" + URLEncoder.encode("literal1") +
    "&su=" + URLEncoder.encode("http://subject3") + "&pu=" + URLEncoder.encode("http://predicate4") + "&ol=" + URLEncoder.encode("literal2") + "&ll=da" +
    "&su=" + URLEncoder.encode("http://subject4") + "&pu=" + URLEncoder.encode("http://predicate5") + "&ol=" + URLEncoder.encode("literal3") + "&lt=" + URLEncoder.encode("http://type") +
						    "&pu=" + URLEncoder.encode("http://dct.org/#hasPart") + "&ob=" + URLEncoder.encode("b1") +
    "&sb=" + URLEncoder.encode("b1") + "&pu=" + URLEncoder.encode("http://rdf.org/#first") + "&ou=" + URLEncoder.encode("http://something/") +
					"&pu=" + URLEncoder.encode("http://rdf.org/#rest") + "&ou=" + URLEncoder.encode("http://rdf.org/#nil");
    
    //public static Model MODEL = ModelFactory.createDefaultModel().
    //	    add(ResourceFactory.createResource("http://subject1"), ResourceFactory.createProperty("http://dc.org/#title"),
    
    public RDFPostReaderTest()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }
    
    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }

    /**
     * Test of parse method, of class RDFPostReader.
     */
    @Test
    @Ignore
    public void testParse() throws Exception
    {
	RDFPostReader instance = new RDFPostReader();
	Model expected = ModelFactory.createDefaultModel();
	expected.add(expected.createResource("http://subject1"), expected.createProperty("http://dc.org/#title"), expected.createLiteral("title", "da")).
		add(expected.createResource("http://subject1"), expected.createProperty("http://predicate1"), expected.createResource("http://object1")).
		add(expected.createResource("http://subject1"), expected.createProperty("http://predicate2"), expected.createResource("http://object2")).
		add(expected.createResource("http://subject1"), expected.createProperty("http://predicate2"), expected.createResource("http://object3")).
		add(expected.createResource("http://subject2"), expected.createProperty("http://predicate3"), expected.createLiteral("literal1")).
		add(expected.createResource("http://subject3"), expected.createProperty("http://predicate4"), expected.createLiteral("literal2", "da")).
		add(expected.createResource("http://subject4"), expected.createProperty("http://predicate5"), expected.createTypedLiteral("literal3", new BaseDatatype("http://type"))).
		add(expected.createResource("http://subject4"), expected.createProperty("http://dct.org/#hasPart"), expected.createResource(AnonId.create("b1"))).
		add(expected.createResource(AnonId.create("b1")), expected.createProperty("http://rdf.org/#first"), expected.createResource("http://something/")).
		add(expected.createResource(AnonId.create("b1")), expected.createProperty("http://rdf.org/#rest"), expected.createResource("http://rdf.org/#nil"));
	System.out.println("Expected Model");
	System.out.println(expected.listStatements().toList().toString());

	Model parsed = instance.parse(POST_BODY, "UTF-8");
	System.out.println("Parsed RDF/POST Model");
	System.out.println(parsed.listStatements().toList().toString());

	assertIsoModels(expected, parsed);
    }

    public static void assertIsoModels(Model wanted, Model got)
    {
	if (!wanted.isIsomorphicWith(got))
	    fail("Models not isomorphic (not structurally equal))");
    }
}
