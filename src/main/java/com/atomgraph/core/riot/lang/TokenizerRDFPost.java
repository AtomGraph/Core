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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.atlas.lib.Chars;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import java.util.NoSuchElementException;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.riot.tokens.TokenChecker;

/**
 * RDF/POST tokenizer.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class TokenizerRDFPost implements Tokenizer // extends TokenizerText 
{
    
    public static boolean Checking = false ;

    public static final char CH_EQUALS =        '=';
    public static final char CH_AMPERSAND =     '&';
    
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
        
    private final PeekReader reader;
    private final TokenChecker tokenChecker;
    private final StringBuilder stringBuilder = new StringBuilder(200);
    private boolean finished = false ;
    private Token token = null;
    private String key, prevKey = null;
    
    public TokenizerRDFPost(PeekReader reader)
    {
        this(reader, null);
    }
    
    public TokenizerRDFPost(PeekReader reader, TokenChecker tokenChecker)
    {
        this.reader = reader;
        this.tokenChecker = tokenChecker;
    }
    
    protected Token parseToken()
    {
        token = new Token(getLine(), getColumn());
        String suffixOrIri = null;
        
        try
        {
            int ch = getReader().peekChar();
            if (ch == CH_EQUALS || ch == CH_AMPERSAND) getReader().readChar();

            if (key != null)
                switch (key)
                {
                    case DEF_NS_DECL: // v
                    case URI_SUBJ: // su
                    case URI_PRED: // pu
                    case URI_OBJ: // ou
                        token.setType(TokenType.IRI);
                        token.setImage(readUntilDelimiter()); // namespace URI
                        if ( Checking ) checkURI(token.getImage());
                        prevKey = key;
                        key = null;
                        return token;
                    case NS_DECL: // n
                        token.setType(TokenType.PREFIXED_NAME) ;
                        token.setImage(readUntilDelimiter()); // namespace prefix
                        prevKey = key;
                        key = null;
                        return token;
                    case NS_SUBJ: // sn
                    case NS_PRED: // pn
                    case NS_OBJ: // on
                        token.setType(TokenType.PREFIXED_NAME) ;
                        token.setImage(readUntilDelimiter()); // prefix
                        prevKey = key;
                        key = null;
                        return token;
                    case DEF_NS_SUBJ: // sv
                        if (prevKey != null && prevKey.equals(NS_SUBJ))
                            token.setType(TokenType.PREFIXED_NAME) ;
                        else
                            token.setType(TokenType.IRI) ;
                        suffixOrIri = readUntilDelimiter();
                        token.setImage(suffixOrIri);  // relative URI
                        token.setImage2(suffixOrIri); // suffix
                        prevKey = key;
                        key = null;
                        return token;
                    case DEF_NS_PRED: // pv
                        if (prevKey != null && prevKey.equals(NS_PRED))
                            token.setType(TokenType.PREFIXED_NAME) ;
                        else
                            token.setType(TokenType.IRI) ;
                        suffixOrIri = readUntilDelimiter();
                        token.setImage(suffixOrIri);  // relative URI
                        token.setImage2(suffixOrIri); // suffix
                        prevKey = key;
                        key = null;
                        return token;
                    case DEF_NS_OBJ: // ov
                        if (prevKey != null && prevKey.equals(NS_OBJ))
                            token.setType(TokenType.PREFIXED_NAME) ;
                        else
                            token.setType(TokenType.IRI) ;
                        suffixOrIri = readUntilDelimiter();
                        token.setImage(suffixOrIri);  // relative URI?
                        token.setImage2(suffixOrIri); // suffix
                        prevKey = key;
                        key = null;
                        return token;
                    case BLANK_SUBJ: // sb
                    case BLANK_OBJ: // ob
                        token.setType(TokenType.BNODE);
                        token.setImage(readUntilDelimiter());
                        if ( Checking ) checkBlankNode(token.getImage());
                        prevKey = key;
                        key = null;
                        return token;
                    case LITERAL_OBJ: // ol
                        token.setType(TokenType.STRING);
                        token.setImage(readUntilDelimiter());
                        prevKey = key;
                        key = null;
                        return token;
                    case TYPE: // lt
                        token.setType(TokenType.IRI);
                        token.setImage(readUntilDelimiter());
                        prevKey = key;
                        key = null;
                        return token;
                    case LANG: // ll
                        token.setType(TokenType.LITERAL_LANG);
                        token.setImage2(readUntilDelimiter());
                        prevKey = key;
                        key = null;
                        return token;
                }
            
            key = readUntilDelimiter();
            token.setImage(key);
            switch (key) // key switch
            {
                case RDF:
                    getReader().readChar(); //  read '=' preceding the empty value
                case DEF_NS_DECL: // v
                case NS_DECL: // n
                case BLANK_SUBJ: // sb
                case URI_SUBJ: // su
                case DEF_NS_SUBJ: // sv
                case NS_SUBJ: // sn
                case URI_PRED: // pu
                case DEF_NS_PRED: // pv
                case NS_PRED: // pn
                case BLANK_OBJ: // ob
                case URI_OBJ: // ou
                case DEF_NS_OBJ: // ov
                case NS_OBJ: // on
                case LITERAL_OBJ: // ol
                case TYPE: // lt
                case LANG: // ll
                    token.setType(TokenType.DIRECTIVE);
                    return token;
            }

            //exception("Failed to find an RDF/POST directive: %c(%d;0x%04X)", ch, ch, ch) ;
        }
        catch (UnsupportedEncodingException ex)
        {
            //exception("");
        }
        
        return token;
    }

    protected String readUntilDelimiter() throws UnsupportedEncodingException
    {
        stringBuilder.setLength(0);

        for (;;)
        {
            int ch = getReader().peekChar();
            if (ch  == Chars.EOF || ch == CH_EQUALS || ch == CH_AMPERSAND) break;
            
            stringBuilder.append((char)getReader().readChar());
        }
       
        return URLDecoder.decode(stringBuilder.toString(), "UTF-8");
    }

    private void exception(String message, Object... args) {
        exception$(message, getReader().getLineNum(), getReader().getColNum(), args) ;
    }

    private static void exception(PeekReader reader, String message, Object... args) {
        exception$(message, reader.getLineNum(), reader.getColNum(), args) ;
    }

    private static void exception$(String message, long line, long col, Object... args) {
        throw new RiotParseException(String.format(message, args), line, col) ;
    }
    
    public PeekReader getReader()
    {
        return reader;
    }

    private void checkBlankNode(String image)
    {
       getTokenChecker().checkBlankNode(image);
    }

    private void checkURI(String image)
    {
        getTokenChecker().checkURI(image);
    }
   
    @Override
    public boolean hasNext()
    {
        if ( finished )
            return false ;
        if ( token != null )
            return true ;

        try
        {
            //skip() ;
            if (getReader().eof())
            {
                //close() ;
                finished = true ;
                return false ;
            }
            token = parseToken() ;
            if ( token == null )
            {
                //close() ;
                finished = true ;
                return false ;
            }
            return true ;
        }
        catch (AtlasException ex)
        {
            if ( ex.getCause() != null )
            {
                if ( ex.getCause().getClass() == java.nio.charset.MalformedInputException.class )
                    throw new RiotParseException("Bad character encoding", getReader().getLineNum(), getReader().getColNum()) ;
                throw new RiotParseException("Bad input stream ["+ex.getCause()+"]", getReader().getLineNum(), getReader().getColNum()) ;
            }
            throw new RiotParseException("Bad input stream", getReader().getLineNum(), getReader().getColNum()) ;
        }
    }    
    
    
    @Override
    public boolean eof()
    {
        return hasNext() ;
    }

    @Override
    public Token next()
    {
        if ( ! hasNext() ) 
            throw new NoSuchElementException() ;
        Token t = token ;
        token = null ;
        return t ;
    }
    
    @Override
    public Token peek()
    {
        if ( ! hasNext() ) return null ;
        return token ; 
    }

    @Override
    public long getColumn()
    {
        return getReader().getColNum() ;
    }

    @Override
    public long getLine()
    {
        return getReader().getLineNum() ;
    }

    @Override
    public void close()
    { 
        IO.close(getReader()) ;
    }
 
    public TokenChecker getTokenChecker()
    {
        return tokenChecker;
    }
    
}
