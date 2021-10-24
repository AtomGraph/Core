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

import com.atomgraph.core.riot.RDFLanguages;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.system.ParserProfile;

/**
 * RDF/POST reader factory.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class RDFPostReaderFactory implements ReaderRIOTFactory
{

    @Override
    public ReaderRIOT create(Lang lang, ParserProfile profile)
    {
        if ( !RDFLanguages.RDFPOST.equals(lang) )
            throw new InternalErrorException("Attempt to parse " + lang + " as RDF/POST") ;
        return new RDFPostReader(lang, profile, profile.getErrorHandler());
    }
    
}
