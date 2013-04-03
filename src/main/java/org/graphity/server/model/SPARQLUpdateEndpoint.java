/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.model;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.graphity.server.MediaType;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public interface SPARQLUpdateEndpoint
{    
    @POST @Consumes(MediaType.APPLICATION_FORM_URLENCODED) Response update(@FormParam("update") String updateString, @FormParam("using-graph-uri") String defaultGraphUri, @FormParam("using-named-graph-uri") String graphUri);
    
    @POST @Consumes(MediaType.APPLICATION_SPARQL_UPDATE) Response update(@QueryParam("using-graph-uri") String defaultGraphUri, @QueryParam("using-named-graph-uri") String graphUri);
}
