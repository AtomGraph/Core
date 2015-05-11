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
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;

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
    private Token token = null;    
    //private final PeekReader reader;
    //private boolean finished = false ;    
    
    public TokenizerRDFPost(PeekReader reader)
    {
        //this.reader = reader;
        super(reader);
    }
    
    @Override
    protected Token parseToken()
    {
        token = new Token(getLine(), getColumn()) ;

        try
        {
            String keyOrValue = readKeyOrValue();

            int ch = getReader().peekChar();
            
            if (ch == CH_EQUALS)
            {
                getReader().readChar();

                switch (keyOrValue)
                {
                    case BLANK_SUBJ:
                    case BLANK_OBJ:
                        token.setImage(readKeyOrValue());
                        token.setType(TokenType.BNODE);
                        if ( Checking ) checkBlankNode(token.getImage());
                        return token;
                    case URI_SUBJ:
                    case URI_PRED:
                    case URI_OBJ:
                        token.setImage(readKeyOrValue()) ;
                        token.setType(TokenType.IRI) ;
                        if ( Checking ) checkURI(token.getImage());
                        return token;
                }
            }
            
            if (ch == CH_AMPERSAND) getReader().readChar();

            return token;
        }
        catch (UnsupportedEncodingException ex)
        {
            //exception("");
        }
        
        return token;
    }

    private String readKeyOrValue() throws UnsupportedEncodingException
    {
        stringBuilder.setLength(0);

        int ch;
        do ch = getReader().readChar();
        while (ch != CH_EQUALS && ch != CH_AMPERSAND);
       
        return URLDecoder.decode(stringBuilder.toString(), "UTF-8");
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
