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

import org.graphity.core.riot.RDFLanguages;
import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Context;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.atlas.iterator.PeekIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ReaderRIOTBase;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import static org.apache.jena.riot.tokens.TokenType.DIRECTIVE;
import static org.apache.jena.riot.tokens.TokenType.EOF;
import static org.apache.jena.riot.tokens.TokenType.IRI;
import static org.apache.jena.riot.tokens.TokenType.NODE;
import static org.apache.jena.riot.tokens.TokenType.PREFIXED_NAME;
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
public class RDFPostReader extends ReaderRIOTBase // implements StreamRDF // implements ReaderRIOT
{    
    private static final Logger log = LoggerFactory.getLogger(RDFPostReader.class);

    //private static final StreamRDF streamRDF = new StreamRDFBase();

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
        Tokenizer tokens = new TokenizerRDFPost(PeekReader.makeUTF8(in));  
        
        try
        {
            runParser(tokens, RiotLib.profile(RDFLanguages.RDFPOST, baseURI), output);
        }
        /*
        catch (IOException ex)
        {
            
        }
        */
        finally
        {
            //finish();
            tokens.close();
        }
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected void runParser(Tokenizer tokens, ParserProfile profile, StreamRDF dest)
    {
        PeekIterator<Token> peekIter = new PeekIterator<>(tokens);

        if (moreTokens(peekIter))
        {
            Token t = peekToken(tokens, peekIter) ;
            if ( lookingAt(tokens, peekIter, DIRECTIVE) && !t.getImage().equals(TokenizerRDFPost.RDF))
                exception(peekToken(tokens, peekIter), "RDF/POST needs to start with 'rdf' query param (found '" + peekToken(tokens, peekIter) + "')") ;
            nextToken(tokens, peekIter);
       }
            
        while (moreTokens(peekIter)) {
            Token t = peekToken(tokens, peekIter) ;
            if ( lookingAt(tokens, peekIter, DIRECTIVE) &&
                    (t.getImage().equals(DEF_NS_DECL) || t.getImage().equals(NS_DECL)))
            {
                directive(tokens, peekIter, profile, dest) ;
                continue ;
            }

            triples(tokens, peekIter, profile, dest) ;

            //oneTopLevelElement() ;

            if ( lookingAt(tokens, peekIter, EOF))
                break ;
        }
    }
        
    private Token tokenEOF = null ;
    
    protected final Token peekToken(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        // Avoid repeating.
        if ( eof(tokens, peekIter) ) return tokenEOF ;
        return peekIter.peek() ;
    }
    
    protected final boolean eof(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        if ( tokenEOF != null )
            return true ;
        
        if ( ! moreTokens(peekIter) )
        {
            tokenEOF = new Token(tokens.getLine(), tokens.getColumn()) ;
            tokenEOF.setType(EOF) ;
            return true ;
        }
        return false ;
    }
    
    protected final boolean moreTokens(PeekIterator<Token> peekIter) 
    {
        return peekIter.hasNext() ;
    }
    
    protected final boolean lookingAt(Tokenizer tokens, PeekIterator<Token> peekIter, TokenType tokenType)
    {
        if ( eof(tokens, peekIter) )
            return tokenType == EOF ;
        if ( tokenType == NODE )
            return peekToken(tokens, peekIter).isNode() ;
        return peekToken(tokens, peekIter).hasType(tokenType) ;
    }
    
    protected long currLine = -1 ;
    protected long currCol = -1 ;
    
    protected final Token nextToken(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        if ( eof(tokens, peekIter) )
            return tokenEOF ;
        
        // Tokenizer errors appear here!
        try {
            Token t = peekIter.next() ;
            currLine = t.getLine() ;
            currCol = t.getColumn() ;
            return t ;
        } catch (RiotParseException ex)
        {
            // Intercept to log it.
            raiseException(ex) ;
            throw ex ;
        }
        catch (AtlasException ex)
        {
            // Bad I/O
            RiotParseException ex2 = new RiotParseException(ex.getMessage(), -1, -1) ;
            raiseException(ex2) ;
            throw ex2 ;
        }
    }
    
    protected final void directive(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest)
    {
        // It's a directive ...
        Token t = peekToken(tokens, peekIter) ;
        String x = t.getImage() ;
        nextToken(tokens, peekIter) ;

        if ( x.equals(TokenizerRDFPost.DEF_NS_DECL) ) {
            directiveBase(tokens, peekIter, profile, dest) ;
            return ;
        }

        if ( x.equals(TokenizerRDFPost.NS_DECL) ) {
            directiveNSDecl(tokens, peekIter, profile, dest);
            return ;
        }
        
        exception(t, "Unrecognized directive: %s", x) ;
    }

    protected final void directiveBase(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest)
    {
        Token token = peekToken(tokens, peekIter) ;
        if ( !lookingAt(tokens, peekIter, IRI) )
            exception(token, "@base requires an IRI (found '" + token + "')") ;
        String baseStr = token.getImage() ;
        org.apache.jena.iri.IRI baseIRI = profile.makeIRI(baseStr, currLine, currCol) ;
        emitBase(baseIRI.toString(), dest);
        nextToken(tokens, peekIter);
        profile.getPrologue().setBaseURI(baseIRI) ;
    }
    
    protected void emitBase(String baseStr, StreamRDF dest)
    { 
        dest.base(baseStr);
    }
    
    protected final void directiveNSDecl(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest)
    {
        // Raw - unresolved prefix name.
        if ( !lookingAt(tokens, peekIter, PREFIXED_NAME) )
            exception(peekToken(tokens, peekIter), "'n' requires a prefix (found '" + peekToken(tokens, peekIter) + "')") ;
        //Token temp = peekToken(tokens, peekIter);
        //if ( peekToken(tokens, peekIter).getImage2().length() != 0 )
        //    exception(peekToken(tokens, peekIter), "@prefix or PREFIX requires a prefix with no suffix (found '" + peekToken(tokens, peekIter) + "')") ;
        String prefix = peekToken(tokens, peekIter).getImage() ;
        nextToken(tokens, peekIter) ;
        if ( !(lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(DEF_NS_DECL)))
            exception(peekToken(tokens, peekIter), "'n' requires a following 'v' (found '" + peekToken(tokens, peekIter) + "')") ;
        nextToken(tokens, peekIter) ;
        if ( !lookingAt(tokens, peekIter, IRI) )
            exception(peekToken(tokens, peekIter), "'v' requires a IRI (found '" + peekToken(tokens, peekIter) + "')") ;
        String iriStr = peekToken(tokens, peekIter).getImage() ;
        org.apache.jena.iri.IRI iri = profile.makeIRI(iriStr, currLine, currCol) ;
        profile.getPrologue().getPrefixMap().add(prefix, iri) ;
        emitPrefix(prefix, iri.toString(), dest) ;
        nextToken(tokens, peekIter) ;
    }
    
    private void emitPrefix(String prefix, String iriStr, StreamRDF dest)
    {
        dest.prefix(prefix, iriStr) ; 
    }
    
    protected void triples(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest)
    {
        // Looking at a node.
        Node subject = node(tokens, peekIter, profile) ;
        if ( subject == null )
            exception(peekToken(tokens, peekIter), "Not recognized: expected node: %s", peekToken(tokens, peekIter).text()) ;

        nextToken(tokens, peekIter) ;
        predicateObjectList(tokens, peekIter, profile, dest, subject) ;
        //expectEndOfTriples() ;
    }
    
    protected void predicateObjectList(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest, Node subject)
    {
        predicateObjectItem(tokens, peekIter, profile, dest, subject) ;

        for (;;)
        {
            if ( !lookingAt(tokens, peekIter, DIRECTIVE) )
                break ;
            Token t = peekToken(tokens, peekIter) ;
            String image = t.getImage() ;
            if (!image.startsWith("p"))
                break;

            /*
            Token t = nextToken(tokens, peekIter) ;
            if ( !peekPredicate() )
                // Trailing (pointless) SEMICOLONs, no following
                // predicate/object list.
                break ;
            */
            predicateObjectItem(tokens, peekIter, profile, dest, subject) ;            
        }
    }
    
    protected void predicateObjectItem(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest, Node subject)
    {
        Node predicate = predicate(tokens, peekIter, profile) ;
        nextToken(tokens, peekIter) ; // skip ob??
        objectList(tokens, peekIter, profile, dest, subject, predicate) ;
    }
    
    protected Node predicate(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile)
    {
        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;

        // pred is expected, but there is no &pv=, &pn=, or &pu= ahead: skip to the next subj (&sb=, &su=, &sv=, &sn=).
        
        Token t = peekToken(tokens, peekIter) ;
        String image = t.getImage() ;
        if ( !image.startsWith("p")) // DIRECTIVE?
            //exception(peekToken(tokens, peekIter), "Expected RDF predicate directive 'pu'/'pv'/'pn' (found '" + peekToken(tokens, peekIter) + "')") ;
            while (!(t.hasType(DIRECTIVE) && image.startsWith("s")))
            {
                t = nextToken(tokens, peekIter); // too much?
                image = t.getImage();
            }
        
        if (image.equals(NS_PRED)) // pn
        {
            nextToken(tokens, peekIter); // skip pn
            
            if ( !lookingAt(tokens, peekIter, PREFIXED_NAME) && peekToken(tokens, peekIter).getImage() != null)
                exception(peekToken(tokens, peekIter), "'pn' requires a prefix (found '" + peekToken(tokens, peekIter) + "')") ;
            Token prefixToken = nextToken(tokens, peekIter);
            
            if (!(lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(DEF_NS_PRED)))
                exception(peekToken(tokens, peekIter), "Expected 'pv' (found '" + peekToken(tokens, peekIter) + "')") ;
            nextToken(tokens, peekIter); // skip pv
            
            if ( !lookingAt(tokens, peekIter, PREFIXED_NAME) && peekToken(tokens, peekIter).getImage2() != null)
                exception(peekToken(tokens, peekIter), "'pn' requires a prefix (found '" + peekToken(tokens, peekIter) + "')") ;
            Token prefixedName = peekToken(tokens, peekIter); // nextToken(tokens, peekIter);
            
            prefixedName.setImage(prefixToken.getImage()); // set prefix // nextToken(tokens, peekIter)
            return tokenAsNode(profile, prefixedName);
        }
            
        /*
        if ( t.hasType(TokenType.KEYWORD) ) {
            boolean strict = profile.isStrictMode() ;
            Token tErr = peekToken() ;
            String image = peekToken().getImage() ;
            if ( image.equals(KW_A) )
                return NodeConst.nodeRDFType ;
            if ( !strict && image.equals(KW_SAME_AS) )
                return nodeSameAs ;
            if ( !strict && image.equals(KW_LOG_IMPLIES) )
                return NodeConst.nodeRDFType ;
            exception(tErr, "Unrecognized: " + image) ;
        }
        */
        
        Node n = node(tokens, peekIter, profile) ;
        if ( n == null || !n.isURI() )
            exception(t, "Expected IRI for predicate: got: %s", t) ;
        return n ;

        
        //return null;
    }
    
    protected final Node node(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile)
    {
        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;
        nextToken(tokens, peekIter) ; // move to the real value

        /*
        Token t = peekToken(tokens, peekIter) ;
        String image = t.getImage() ;        
        if ( !image.equals(URI_PRED) && !image.equals(DEF_NS_PRED) && !image.equals(NS_PRED))
            exception(peekToken(tokens, peekIter), "Expected RDF predicate directive 'su'/'sv'/'sn' (found '" + peekToken(tokens, peekIter) + "')") ;
        */
        
        // Token to Node
        return tokenAsNode(profile, peekToken(tokens, peekIter)) ;
    }
    
    /*
    protected final void predicateObjectItem(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, Node subject)
    {
        Node predicate = predicate(tokens, peekIter, profile) ;
        nextToken(tokens, peekIter) ;
        objectList(tokens, peekIter, profile, subject, predicate) ;
    }
    */
    
    protected final void objectList(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest, Node subject, Node predicate)
    {
        // obj is expected, but there is no &ob=, &ov=, &on=, &ou=, or &ol= ahead: skip to the next pred (&pu=, &pv=, or &pn=) or subj (&sb=, &su=, &sv=, &sn=), whichever comes first.

        Token t = peekToken(tokens, peekIter) ;
        String image = t.getImage() ;
        if ( !image.startsWith("o"))
            while (!(t.hasType(DIRECTIVE) && (image.startsWith("s") || image.startsWith("p"))))
            {
                t = nextToken(tokens, peekIter);
                image = t.getImage();
            }

        for (;;) {
            Node object = triplesNode(tokens, peekIter, profile, subject, predicate) ;
            emitTriple(profile, dest, subject, predicate, object) ;

            if ( !moreTokens(peekIter) )
                break ;
 
            if ( !lookingAt(tokens, peekIter, DIRECTIVE) )
                break ;
            // list continues - move over the directive
            t = peekToken(tokens, peekIter) ;
            
            image = t.getImage() ;
            if (!image.startsWith("o"))
                break;
        }
    }
    
    protected final Node triplesNode(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, Node subject, Node predicate)
    {
        //if ( lookingAt(NODE) ) {
            Node n = node(tokens, peekIter, profile) ;
            nextToken(tokens, peekIter) ;
            return n ;
        //}

        /*
        // Special words.
        if ( lookingAt(TokenType.KEYWORD) ) {
            Token tErr = peekToken() ;
            // Location independent node words
            String image = peekToken().getImage() ;
            nextToken() ;
            if ( image.equals(KW_TRUE) )
                return NodeConst.nodeTrue ;
            if ( image.equals(KW_FALSE) )
                return NodeConst.nodeFalse ;
            if ( image.equals(KW_A) )
                exception(tErr, "Keyword 'a' not legal at this point") ;

            exception(tErr, "Unrecognized keyword: " + image) ;
        }

        return triplesNodeCompound() ;
        */
    }
    
    protected final void emitTriple(ParserProfile profile, StreamRDF dest, Node subject, Node predicate, Node object)
    {
        emit(profile, dest, subject, predicate, object) ;
    }
    
    protected void emit(ParserProfile profile, StreamRDF dest, Node subject, Node predicate, Node object)
    {
        Triple t = profile.createTriple(subject, predicate, object, currLine, currCol) ;
        dest.triple(t) ;
    }
    
    protected final Node tokenAsNode(ParserProfile profile, Token token)
    {
        return profile.create(null, token) ; // return profile.create(currentGraph, token) ;
    }
    
    /*
    protected final void predicateObjectList(Node subject) {
        predicateObjectItem(subject) ;

        for (;;) {
            if ( !lookingAt(SEMICOLON) )
                break ;
            // predicatelist continues - move over all ";"
            while (lookingAt(SEMICOLON))
                nextToken() ;
            if ( !peekPredicate() )
                // Trailing (pointless) SEMICOLONs, no following
                // predicate/object list.
                break ;
            predicateObjectItem(subject) ;
        }
    }
    */
    
    protected final void exception(Token token, String msg, Object... args)
    { 
        if ( token != null )
            exceptionDirect(String.format(msg, args), token.getLine(), token.getColumn()) ;
        else
            exceptionDirect(String.format(msg, args), -1, -1) ;
    }

    protected final void exceptionDirect(String msg, long line, long col)
    { 
        raiseException(new RiotParseException(msg, line, col)) ;
    }
    
    protected final void raiseException(RiotParseException ex)
    { 
        ErrorHandler errorHandler = null; // profile.getHandler() ; 
        if ( errorHandler != null )
            errorHandler.fatal(ex.getOriginalMessage(), ex.getLine(), ex.getCol()) ;
        throw ex ;
    }    

}
