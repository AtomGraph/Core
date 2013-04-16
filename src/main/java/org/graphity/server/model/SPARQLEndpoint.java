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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public interface SPARQLEndpoint extends SPARQLQueryEndpoint, SPARQLUpdateEndpoint
{
    ResponseBuilder getResponseBuilder(Query query);
    
    ResponseBuilder getResponseBuilder(Model model);
    
    ResponseBuilder getResponseBuilder(Model model, List<Variant> variants);
    
    ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet);
    
    ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet, List<Variant> variants);
    
    ResponseBuilder getResponseBuilder(EntityTag entityTag, Object entity, List<Variant> variants);
    
    Model loadModel(Query query);
    
    ResultSetRewindable loadResultSetRewindable(Query query);

}