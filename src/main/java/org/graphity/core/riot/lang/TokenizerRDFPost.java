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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import static org.graphity.core.riot.lang.TokenizerText.Checking;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class TokenizerRDFPost extends TokenizerText implements Tokenizer
{
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
        
    private final StringBuilder stringBuilder = new StringBuilder(200);    
    private Token token, directive = null;    
    
    public TokenizerRDFPost(PeekReader reader)
    {
        super(reader);
    }
    
    @Override
    protected Token parseToken()
    {
        token = new Token(getLine(), getColumn());

        try
        {
            int ch = getReader().peekChar();            
            if (ch == CH_EQUALS || ch == CH_AMPERSAND) getReader().readChar();

            if (directive != null)
                switch (directive.getImage())
                {
                    case DEF_NS_DECL: // v
                        token.setImage(readUntilDelimiter());
                        token.setType(TokenType.IRI);
                        if ( Checking ) checkURI(token.getImage());
                        directive = null;
                        return token;
                    case NS_DECL: // n
                    case DEF_NS_SUBJ: // sv
                    case DEF_NS_PRED: // pv
                    case DEF_NS_OBJ: // ov
                        token.setImage(readUntilDelimiter());
                        token.setType(TokenType.PREFIXED_NAME) ;
                        directive = null;
                        return token;
                    case BLANK_SUBJ: // sb
                    case BLANK_OBJ: // ob
                        token.setImage(readUntilDelimiter());
                        token.setType(TokenType.BNODE);
                        if ( Checking ) checkBlankNode(token.getImage());
                        directive = token;
                        return token;
                    case LITERAL_OBJ: // ol
                        token.setImage(readUntilDelimiter());

                        //Token next = peek();

                        token.setType(TokenType.STRING);
                        directive = token;
                        return token;
                }
                
            String key = readUntilDelimiter();
            switch (key) // key switch
            {
                case RDF:
                    getReader().readChar(); //  read '=' preceding the empty value
                    token.setImage(key);
                    token.setType(TokenType.DIRECTIVE);
                    directive = token;
                    return token;
                case DEF_NS_DECL: // v
                case NS_DECL: // n
                case DEF_NS_SUBJ: // "v
                case DEF_NS_PRED: // pv
                case DEF_NS_OBJ: // ov
                case BLANK_SUBJ: // sb
                case BLANK_OBJ: // ob
                case URI_SUBJ: // su
                case URI_PRED: // pu
                case URI_OBJ: // ou
                    token.setImage(key);
                    token.setType(TokenType.DIRECTIVE);
                    directive = token;
                    return token;
            }

            return token;
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
            if (ch == CH_EQUALS || ch == CH_AMPERSAND) break;
            
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
}
