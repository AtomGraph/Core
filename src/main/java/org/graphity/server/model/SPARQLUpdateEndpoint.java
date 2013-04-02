/*
 * Copyright (C) 2013 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
