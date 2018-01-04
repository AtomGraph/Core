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

package com.atomgraph.core.riot.lang;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.Context;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.atlas.iterator.PeekIterator;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ReaderRIOTBase;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import static org.apache.jena.riot.tokens.TokenType.*;
import static com.atomgraph.core.riot.lang.TokenizerRDFPost.*;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RDF/POST parser.
 * Reads RDF from RDF/POST-encoded string.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="http://www.lsrn.org/semweb/rdfpost.html">RDF/POST Encoding for RDF</a>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Model.html">Model</a>
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class RDFPostReader extends ReaderRIOTBase // implements ReaderRIOT
{    
    private static final Logger log = LoggerFactory.getLogger(RDFPostReader.class);

    private ErrorHandler errorHandler = ErrorHandlerFactory.getDefaultErrorHandler();    
    private ParserProfile parserProfile = null;
        
    public Model parse(String body, String charsetName) throws URISyntaxException
    {
        List<String> keys = new ArrayList<>(), values = new ArrayList<>();

	String[] params = body.split("&");

	for (String param : params)
	{
	    if (log.isTraceEnabled()) log.trace("Parameter: {}", param);
	    
	    String[] array = param.split("=");
	    String key = null;
	    String value = null;

	    try
	    {
		key = URLDecoder.decode(array[0], charsetName);
		if (array.length > 1) value = URLDecoder.decode(array[1], charsetName);
	    } catch (UnsupportedEncodingException ex)
	    {
		if (log.isWarnEnabled()) log.warn("Unsupported encoding", ex);
	    }

            if (value != null) // && key != null
            {
                keys.add(key);
                values.add(value);
            }
	}

        return parse(keys, values);
    }

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
				object = model.createTypedLiteral(v.get(i), TypeMapper.getInstance().getSafeTypeByName(v.get(i + 1))); // typed literal (value+datatype)
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
			object = model.createTypedLiteral(v.get(i + 1), TypeMapper.getInstance().getSafeTypeByName(v.get(i))); // typed literal (datatype+value)
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

    public boolean skipEmptyLiterals()
    {
        return true;
    }

    @Override
    public void read(InputStream in, String baseURI, ContentType ct, StreamRDF output, Context context)
    {
        read(in, baseURI, RDFLanguages.contentTypeToLang(ct), output, context);
    }

    @Override
    public void read(InputStream in, String baseURI, Lang lang, StreamRDF output, Context context)
    {
        read(FileUtils.asBufferedUTF8(in), baseURI, lang, output, context);
    }
    
    @Override
    public void read(Reader in, String baseURI, ContentType ct, StreamRDF output, Context context)
    {
        read(in, baseURI, RDFLanguages.contentTypeToLang(ct), output, context);
    }
    
    public void read(Reader in, String baseURI, Lang lang, StreamRDF output, Context context)
    {
        ParserProfile profile = parserProfile;
        if (profile == null)
            profile = RiotLib.profile(baseURI, false, false, errorHandler); 
        if (errorHandler == null)
            setErrorHandler(profile.getHandler());
        
        Tokenizer tokens = new TokenizerRDFPost(PeekReader.make(in));  
        
        try
        {
            runParser(tokens, getParserProfile(), output);
        }
        finally
        {
            output.finish();
            tokens.close();
        }
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
            
        while (moreTokens(peekIter))
        {
            Token t = peekToken(tokens, peekIter) ;
            if ( lookingAt(tokens, peekIter, DIRECTIVE) &&
                    (t.getImage().equals(DEF_NS_DECL) || t.getImage().equals(NS_DECL)))
            {
                directive(tokens, peekIter, profile, dest) ;
                continue ;
            }

            triples(tokens, peekIter, profile, dest) ;

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
    }
    
    protected void predicateObjectList(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest, Node subject)
    {
        for (;;)
        {
            // pred is expected, but there is no &pv=, &pn=, or &pu= ahead: skip to the next subj (&sb=, &su=, &sv=, &sn=).

            if ( !lookingAt(tokens, peekIter, DIRECTIVE) ||
                peekToken(tokens, peekIter).getImage() == null ||
                    !peekToken(tokens, peekIter).getImage().startsWith("p"))
            {
                skipToSubject(tokens, peekIter);
                return; // return to subject
            }

            if (lookingAt(tokens, peekIter, EOF))
                return;
            
            predicateObjectItem(tokens, peekIter, profile, dest, subject) ;            
        }
    }
    
    protected void skipToSubject(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        while (moreTokens(peekIter))
        {
            if (lookingAt(tokens, peekIter, DIRECTIVE) &&
                    peekToken(tokens, peekIter).getImage() != null &&
                    peekToken(tokens, peekIter).getImage().startsWith("s"))
                return; // return to subject

            nextToken(tokens, peekIter);  // subject not seen yet - move on
        }
    }
    
    protected void predicateObjectItem(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest, Node subject)
    {        
        Node predicate = predicate(tokens, peekIter, profile) ;
        if (predicate == null) return; // if predicateNS() failed to find pv
        
        nextToken(tokens, peekIter) ;
        
        // we reached the end - there is no object for this predicate
        if (lookingAt(tokens, peekIter, EOF))
            return;
        
        objectList(tokens, peekIter, profile, dest, subject, predicate) ;
    }
    
    protected Node predicate(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile)
    {
        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;
        
        Token t = peekToken(tokens, peekIter) ;
        
        if (t.getImage().equals(NS_PRED)) // pn is a special case - requires following pv
            return predicateNS(tokens, peekIter, profile);
        
        Node n = node(tokens, peekIter, profile) ;
        if ( n == null || !n.isURI() )
            exception(t, "Expected IRI for predicate: got: %s", t) ;
        return n ;
    }

    protected Node predicateNS(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile)
    {
        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;            
        nextToken(tokens, peekIter); // skip pn

        if ( !lookingAt(tokens, peekIter, PREFIXED_NAME) && peekToken(tokens, peekIter).getImage() != null)
            exception(peekToken(tokens, peekIter), "'pn' requires a prefix (found '" + peekToken(tokens, peekIter) + "')") ;
        Token prefixToken = nextToken(tokens, peekIter);

        if ( !lookingAt(tokens, peekIter, DIRECTIVE) ||
            peekToken(tokens, peekIter).getImage() == null ||
                !peekToken(tokens, peekIter).getImage().equals(DEF_NS_PRED))
        {
            skipToSubject(tokens, peekIter);
            return null;
        }

        nextToken(tokens, peekIter); // skip pv

        if ( !lookingAt(tokens, peekIter, PREFIXED_NAME) && peekToken(tokens, peekIter).getImage2() != null)
            exception(peekToken(tokens, peekIter), "'pn' requires a prefix (found '" + peekToken(tokens, peekIter) + "')") ;
        Token prefixedName = peekToken(tokens, peekIter);

        prefixedName.setImage(prefixToken.getImage()); // set prefix // nextToken(tokens, peekIter)
        return tokenAsNode(profile, prefixedName);
    }
    
    protected final Node node(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile)
    {
        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;
        nextToken(tokens, peekIter) ; // move to the real value
        
        // Token to Node
        return tokenAsNode(profile, peekToken(tokens, peekIter)) ;
    }
    
    protected final void objectList(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, StreamRDF dest, Node subject, Node predicate)
    {
        for (;;)
        {
            Node object = object(tokens, peekIter, profile, subject, predicate) ;
            if (object == null) return; // if object() failed to find o*

            // do not emit empty literals. Override skippingEmptyLiterals() to change this behaviour
            if (!(object.isLiteral() && object.getLiteralLexicalForm().isEmpty() && skipEmptyLiterals()))
                emitTriple(profile, dest, subject, predicate, object) ;

            if ( !moreTokens(peekIter) )
                break ;
 
            if ( !lookingAt(tokens, peekIter, DIRECTIVE) )
                break ;
            // list continues - move over the directive
            String image = peekToken(tokens, peekIter).getImage() ;
            if (!(image.startsWith("o") || image.equals(TYPE) || image.equals(LANG)))
                break;
        }
    }
    
    protected Node object(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile, Node subject, Node predicate)
    {
        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;

        // obj is expected, but there is no &ob=, &ov=, &on=, &ou=, or &ol= ahead: skip to the next pred (&pu=, &pv=, or &pn=) or subj (&sb=, &su=, &sv=, &sn=), whichever comes first.

        Token t = peekToken(tokens, peekIter) ;
        String image = t.getImage() ;
        
        if (!image.startsWith("o"))
        {
            if (image.equals(TYPE) || image.equals(LANG)) // lt or ll
            {
                Node n = objectLiteral(tokens, peekIter, profile);
                if (n == null) skipToSubjectOrPredicateOrNonLiteralObject(tokens, peekIter);
                
                if (peekToken(tokens, peekIter).getImage().startsWith("o"))
                    return object(tokens, peekIter, profile, subject, predicate);
            }

            skipToSubjectOrPredicate(tokens, peekIter);
            return null;
        }
        
        // ol - a special case which checks for following lt and ll
        if (lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(LITERAL_OBJ))
            return objectLiteral(tokens, peekIter, profile);

        // on - a special case which checks for following ov
        if (lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(NS_OBJ))
            return objectNS(tokens, peekIter, profile);            
        
        Node n = node(tokens, peekIter, profile) ;
        nextToken(tokens, peekIter) ; // skip what?
        return n ;
    }

    protected void skipToSubjectOrPredicate(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        while (moreTokens(peekIter))
        {
            if (lookingAt(tokens, peekIter, DIRECTIVE) &&
                    peekToken(tokens, peekIter).getImage() != null &&
                    (peekToken(tokens, peekIter).getImage().startsWith("s") ||
                        peekToken(tokens, peekIter).getImage().startsWith("p")))
                return; // return to subject or predicate

            nextToken(tokens, peekIter);  // subject or predicate not seen yet - move on
        }
    }

    public Node objectLiteral(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile)
    {
        // &lt= or &ll= is seen, but there is no &ol= ahead: skip to the next non-literal obj (&ob=, &ou=, &ov=, or &on=), pred (&pu=, &pv=, or &pn=) or subj (&sb=, &su=, &sv=, &sn=), whichever comes first.

        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;
        
        if (!peekToken(tokens, peekIter).getImage().equals(LITERAL_OBJ)) // not ol
        {
            // type
            if (lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(TYPE)) // peekToken(tokens, peekIter).getImage().equals(LANG))
            {
                nextToken(tokens, peekIter);

                if ( !lookingAt(tokens, peekIter, IRI))
                    exception(peekToken(tokens, peekIter), "'lt' requires a URI (found '" + peekToken(tokens, peekIter) + "')") ;
                Token dtIriToken = nextToken(tokens, peekIter);
                
                // no ol follows lt
                if (!(lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(LITERAL_OBJ)))
                    return null;
                
                nextToken(tokens, peekIter); // skip ol

                if ( !lookingAt(tokens, peekIter, STRING))
                    exception(peekToken(tokens, peekIter), "'ol' requires a node (found '" + peekToken(tokens, peekIter) + "')") ;
                Token literal = peekToken(tokens, peekIter);
                
                literal.setType(LITERAL_DT);                
                literal.setSubToken2(dtIriToken);
                return tokenAsNode(profile, literal);
            }
            
            // lang
            if (lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(LANG))
            {
                nextToken(tokens, peekIter);

                if ( !lookingAt(tokens, peekIter, LITERAL_LANG))
                    exception(peekToken(tokens, peekIter), "'ol' requires a node (found '" + peekToken(tokens, peekIter) + "')") ;
                Token langToken = nextToken(tokens, peekIter);

                // no ol follows ll
                if (!(lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(LITERAL_OBJ)))
                    return null;

                nextToken(tokens, peekIter); // skip ol

                if ( !lookingAt(tokens, peekIter, STRING))
                    exception(peekToken(tokens, peekIter), "'ol' requires a node (found '" + peekToken(tokens, peekIter) + "')") ;
                Token literal = peekToken(tokens, peekIter);
                
                literal.setType(LITERAL_LANG);
                literal.setImage2(langToken.getImage2());
                return tokenAsNode(profile, literal);
            }            
        }
            
        nextToken(tokens, peekIter); // skip ol

        if ( !lookingAt(tokens, peekIter, STRING))
            exception(peekToken(tokens, peekIter), "'ol' requires a string (found '" + peekToken(tokens, peekIter) + "')") ;
        Token literal = nextToken(tokens, peekIter);
        
        Node literalNode = tokenAsNode(profile, literal);
        
        // type
        if (lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(TYPE))
        {
            Token tokenDT = type(tokens, peekIter);
            literal.setType(LITERAL_DT);
            literal.setSubToken2(tokenDT);
            return tokenAsNode(profile, literal);
        }
        // lang
        if (lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(LANG))
        {
            Token tokenLang = lang(tokens, peekIter);
            literal.setType(LITERAL_LANG);
            literal.setImage2(tokenLang.getImage2());
            return tokenAsNode(profile, literal);
        }
        
        return literalNode;
    }

    protected void skipToSubjectOrPredicateOrNonLiteralObject(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        while (moreTokens(peekIter))
        {
            if (lookingAt(tokens, peekIter, DIRECTIVE) &&
                peekToken(tokens, peekIter).getImage() != null &&
                    (peekToken(tokens, peekIter).getImage().startsWith("s") ||
                    peekToken(tokens, peekIter).getImage().startsWith("p") ||
                    (peekToken(tokens, peekIter).getImage().startsWith("o") &&
                        !peekToken(tokens, peekIter).getImage().equals(LITERAL_OBJ))))
                return; // return to subject or predicate

            nextToken(tokens, peekIter);  // subject or predicate not seen yet - move on
        }
    }
    
    public Token type(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        if (!(lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(TYPE)))
            exception(peekToken(tokens, peekIter), "Expected 'lt' (found '" + peekToken(tokens, peekIter) + "')") ;
        nextToken(tokens, peekIter); // skip lt

        if ( !lookingAt(tokens, peekIter, IRI))
            exception(peekToken(tokens, peekIter), "'lt' requires a URI (found '" + peekToken(tokens, peekIter) + "')") ;
     
        return nextToken(tokens, peekIter); // nextToken
    }

    public Token lang(Tokenizer tokens, PeekIterator<Token> peekIter)
    {
        if (!(lookingAt(tokens, peekIter, DIRECTIVE) && peekToken(tokens, peekIter).getImage().equals(LANG)))
            exception(peekToken(tokens, peekIter), "Expected 'll' (found '" + peekToken(tokens, peekIter) + "')") ;
        nextToken(tokens, peekIter); // skip ll

        if ( !lookingAt(tokens, peekIter, LITERAL_LANG))
            exception(peekToken(tokens, peekIter), "'ll' requires a language string (found '" + peekToken(tokens, peekIter) + "')") ;
     
        return nextToken(tokens, peekIter);        
    }

    protected Node objectNS(Tokenizer tokens, PeekIterator<Token> peekIter, ParserProfile profile)
    {
        if ( !lookingAt(tokens, peekIter, DIRECTIVE))
            exception(peekToken(tokens, peekIter), "Expected RDF/POST directive (found '" + peekToken(tokens, peekIter) + "')") ;            
        nextToken(tokens, peekIter); // skip on

        if ( !lookingAt(tokens, peekIter, PREFIXED_NAME) && peekToken(tokens, peekIter).getImage() != null)
            exception(peekToken(tokens, peekIter), "'on' requires a prefix (found '" + peekToken(tokens, peekIter) + "')") ;
        Token prefixToken = nextToken(tokens, peekIter);
        
        if ( !lookingAt(tokens, peekIter, DIRECTIVE) ||
            peekToken(tokens, peekIter).getImage() == null ||
                !peekToken(tokens, peekIter).getImage().equals(DEF_NS_OBJ))
        {
            skipToSubjectOrPredicate(tokens, peekIter); // skipToSubject(tokens, peekIter);
            return null;
        }

        nextToken(tokens, peekIter); // skip ov

        if ( !lookingAt(tokens, peekIter, PREFIXED_NAME) && peekToken(tokens, peekIter).getImage2() != null)
            exception(peekToken(tokens, peekIter), "'on' requires a prefix (found '" + peekToken(tokens, peekIter) + "')") ;
        Token prefixedName = peekToken(tokens, peekIter); // nextToken(tokens, peekIter);

        prefixedName.setImage(prefixToken.getImage());
        return tokenAsNode(profile, prefixedName);
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
        if (getErrorHandler() != null)
            getErrorHandler().fatal(ex.getOriginalMessage(), ex.getLine(), ex.getCol()) ;
        throw ex ;
    }

    @Override
    public ErrorHandler getErrorHandler()
    {
        return errorHandler;
    }
    
    @Override
    public void setErrorHandler(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    @Override    
    public ParserProfile getParserProfile()
    {
        return parserProfile;
    } 

    @Override
    public void setParserProfile(ParserProfile parserProfile)
    {
        this.parserProfile = parserProfile;
    }    

}
