/**
 *  Copyright 2020 Martynas Jusevičius <martynas@atomgraph.com>
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWrapper;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class StreamRDFOutput implements StreamingOutput
{

    private final TypedInputStream stream;
    private final Function<StreamRDF, StreamRDF> wrapper;
    
    public <T extends StreamRDFWrapper> StreamRDFOutput(TypedInputStream stream, Function<StreamRDF, StreamRDF> wrapper)
    {
        this.stream = stream;
        this.wrapper = wrapper;
    }
    
    @Override
    public void write(OutputStream os) throws IOException, WebApplicationException
    {
        StreamRDF streamRDF = StreamRDFLib.writer(os);
        if (getWrapper() != null) streamRDF = getWrapper().apply(streamRDF);
        
        streamRDF.start();
        RDFDataMgr.parse(streamRDF, getTypedInputStream(), RDFLanguages.contentTypeToLang(getTypedInputStream().getContentType()));
        streamRDF.finish();
    }
    
    public TypedInputStream getTypedInputStream()
    {
        return stream;
    }
    
    public Function<StreamRDF, StreamRDF> getWrapper()
    {
        return wrapper;
    }
    
}