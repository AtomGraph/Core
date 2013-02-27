/*
 * Copyright (C) 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.platform.model;

import com.hp.hpl.jena.query.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Generic SPARQL endpoint interface
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://www.w3.org/TR/rdf-sparql-protocol/">SPARQL Protocol for RDF</a>
 */
@Path("/sparql")
@Produces({org.graphity.platform.MediaType.APPLICATION_RDF_XML + "; charset=UTF-8", org.graphity.platform.MediaType.TEXT_TURTLE + "; charset=UTF-8", org.graphity.platform.MediaType.APPLICATION_SPARQL_RESULTS_XML + "; charset=UTF-8", org.graphity.platform.MediaType.APPLICATION_SPARQL_RESULTS_JSON + "; charset=UTF-8"})
public interface SPARQLEndpoint
{
    /**
     * Handles SPARQL Protocol for RDF request and returns query result as response
     * 
     * @param query the submitted SPARQL query or null
     * @return result response (in one of the representation variants)
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/query/Query.html">ARQ Query</a>
     */
    @GET Response query(@QueryParam("query") Query query);
}
