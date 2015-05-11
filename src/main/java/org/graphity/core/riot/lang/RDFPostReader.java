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

package org.graphity.core.riot.lang;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.util.Context;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.atlas.lib.Tuple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ReaderRIOTBase;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.riot.tokens.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RDF/POST parser.
 * Reads RDF from RDF/POST-encoded string.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://www.lsrn.org/semweb/rdfpost.html">RDF/POST Encoding for RDF</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/Model.html">Model</a>
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class RDFPostReader extends ReaderRIOTBase implements StreamRDF // implements ReaderRIOT
{    
    private static final Logger log = LoggerFactory.getLogger(RDFPostReader.class);

    private static final StreamRDF streamRDF = new StreamRDFBase();

    public static final String RDF =            "rdf";
    
    public static final String DEF_NS_DECL =    "v";
    public static final String NS_DECL =        "n";

    public static final String BLANK_SUBJ =     "sb";
    public static final String URI_SUBJ =       "su";
    public static final String DEF_NS_SUBJ =    "sv";
    public static final String NS_SUBJ =        "sn";

    public static final String URI_PRED =       "pu";
    public static final String DEF_NS_PRED =    "pv";
    public static final String NS_PRED =        "pn";
    
    public static final String BLANK_OBJ =      "ob";
    public static final String URI_OBJ =        "ou";
    public static final String DEF_NS_OBJ =     "ov";
    public static final String NS_OBJ =         "on";
    public static final String LITERAL_OBJ =    "ol";

    public static final String TYPE =           "lt";
    public static final String LANG =           "ll";

    public Model parse(List<String> k, List<String> v) throws URISyntaxException
    {
	Model model = ModelFactory.createDefaultModel();

	Resource subject = null;
	Property property = null;
	RDFNode object = null;

	for (int i = 0; i < k.size(); i++)
	{
	    switch (k.get(i))
	    {
		case DEF_NS_DECL:
		    model.setNsPrefix("", v.get(i)); // default namespace
		    break;
		case NS_DECL:
		    if (i + 1 < k.size() && k.get(i + 1).equals(DEF_NS_DECL)) // if followed by "v" (if not out of bounds)
		    {
			model.setNsPrefix(v.get(i), v.get(i + 1)); // namespace with prefix
			i++; // skip the following "v"
		    }
		    break;
		    
		case BLANK_SUBJ:
		    subject = model.createResource(new AnonId(v.get(i))); // blank node
		    property = null;
		    object = null;
		    break;
		case URI_SUBJ:
                    URI subjectURI = new URI(v.get(i));
                    //if (!subjectURI.isAbsolute()) subjectURI = baseURI.resolve(subjectURI);
		    subject = model.createResource(subjectURI.toString()); // full URI
		    property = null;
		    object = null;
		    break;
		case DEF_NS_SUBJ:
		    subject = model.createResource(model.getNsPrefixURI("") + v.get(i)); // default namespace
		    property = null;
		    object = null;
		    break;
		case NS_SUBJ:
		    if (i + 1 < k.size() && k.get(i + 1).equals(DEF_NS_SUBJ)) // if followed by "sv" (if not out of bounds)
		    {
			subject = model.createResource(model.getNsPrefixURI(v.get(i)) + v.get(i + 1)); // ns prefix + local name
			property = null;
			object = null;
			i++; // skip the following "sv"
		    }
		    break;

		case URI_PRED:
                    URI propertyURI = new URI(v.get(i));
                    //if (!propertyURI.isAbsolute()) propertyURI = baseURI.resolve(propertyURI);
                    property = model.createProperty(propertyURI.toString());
		    object = null;
		    break;
		case DEF_NS_PRED:
		    property = model.createProperty(model.getNsPrefixURI(""), v.get(i));
		    object = null;
		    break;
		case NS_PRED:
		    if (i + 1 < k.size() && k.get(i + 1).equals(DEF_NS_PRED)) // followed by "pv" (if not out of bounds)
		    {
			property = model.createProperty(model.getNsPrefixURI(v.get(i)) + v.get(i + 1)); // ns prefix + local name
			object = null;
			i++; // skip the following "pv"
		    }
		    break;

		case BLANK_OBJ:
		    object = model.createResource(new AnonId(v.get(i))); // blank node
		    break;
		case URI_OBJ:
                    URI objectURI = new URI(v.get(i));
                    //if (!objectURI.isAbsolute()) objectURI = baseURI.resolve(objectURI);
                    object = model.createResource(objectURI.toString()); // full URI
		    break;
		case DEF_NS_OBJ:
		    object = model.createResource(model.getNsPrefixURI("") + v.get(i)); // default namespace
		    break;
		case NS_OBJ:
		    if (i + 1 < k.size() && k.get(i + 1).equals(DEF_NS_OBJ)) // followed by "ov" (if not out of bounds)
		    {
			object = model.createResource(model.getNsPrefixURI(v.get(i)) + v.get(i + 1)); // ns prefix + local name
			i++; // skip the following "ov"
		    }
		    break;
		case LITERAL_OBJ:
		    if (i + 1 < k.size()) // check if not out of bounds
			switch (k.get(i + 1))
			{
			    case TYPE:
				object = model.createTypedLiteral(v.get(i), new BaseDatatype(v.get(i + 1))); // typed literal (value+datatype)
				i++; // skip the following "lt"
				break;
			    case LANG:
				object = model.createLiteral(v.get(i), v.get(i + 1)); // literal with language (value+lang)
				i++; // skip the following "ll"
				break;
			    default:
				object = model.createLiteral(v.get(i)); // plain literal (if not followed by lang or datatype)
				break;
			}
		    else
			object = model.createLiteral(v.get(i)); // plain literal
		    break;
		case TYPE:
		    if (i + 1 < k.size() && k.get(i + 1).equals(LITERAL_OBJ)) // followed by "ol" (if not out of bounds)
		    {
			object = model.createTypedLiteral(v.get(i + 1), new BaseDatatype(v.get(i))); // typed literal (datatype+value)
			i++; // skip the following "ol"
		    }
		    break;
		case LANG:
		    if (i + 1 < k.size() && k.get(i + 1).equals(LITERAL_OBJ)) // followed by "ol" (if not out of bounds)
		    {
			model.createLiteral(v.get(i + 1), v.get(i)); // literal with language (lang+value)
			i++; // skip the following "ol"
		    }
		    break;
	    }

	    if (subject != null && property != null && object != null)
		model.add(model.createStatement(subject, property, object));
	}

	return model;
    }

    @Override
    public void read(InputStream in, String baseURI, Lang lang, StreamRDF output, Context context)
    {
        try
        {
            Tokenizer tokenizer = new TokenizerRDFPost(PeekReader.makeUTF8(in));
        }
        /*
        catch (IOException ex)
        {
            
        }
        */
        finally
        {
            finish();
        }
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void start()
    {
        streamRDF.start();
    }

    @Override
    public void triple(Triple triple)
    {
        streamRDF.triple(triple);
    }

    @Override
    public void quad(Quad quad)
    {
        streamRDF.quad(quad);
    }

    @Override
    public void tuple(Tuple<Node> tuple)
    {
        streamRDF.tuple(tuple);
    }

    @Override
    public void base(String base)
    {
        streamRDF.base(base);
    }

    @Override
    public void prefix(String prefix, String iri)
    {
        streamRDF.prefix(prefix, iri);
    }

    @Override
    public void finish()
    {
        streamRDF.finish();
    }

}
