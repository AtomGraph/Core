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
package org.graphity.ldp.model;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.core.*;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public interface Resource
{
    @GET Response getResponse();

    Request getRequest(); // Request can be injected

    //UriInfo getUriInfo(); // Request can be injected

    List<Variant> getVariants();
    
    //EntityTag getEntityTag();
}