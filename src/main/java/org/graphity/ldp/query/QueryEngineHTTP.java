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
package org.graphity.ldp.query;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.sparql.util.Context;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryEngineHTTP extends com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP
{
    private static final Logger log = LoggerFactory.getLogger(QueryEngineHTTP.class);

    public QueryEngineHTTP(String serviceURI, String queryString)
    {
	super(serviceURI, queryString);
	
	Map<String, Context> serviceContextMap = (Map<String,Context>)getContext().get(Service.serviceContext);
	if (serviceContextMap != null && serviceContextMap.containsKey(serviceURI))
	{
	    Context serviceContext = serviceContextMap.get(serviceURI);
	    if (log.isDebugEnabled()) log.debug("Endpoint URI {} has SERVICE Context: {} ", serviceURI, serviceContext);

	    String user = serviceContext.getAsString(Service.queryAuthUser);
	    String pwd = serviceContext.getAsString(Service.queryAuthPwd);
	    
	    if (user != null || pwd != null)
	    {
		user = user==null?"":user;
		pwd = pwd==null?"":pwd;
		if (log.isDebugEnabled()) log.debug("Setting basic HTTP authentication for endpoint URI {} with username: {} ", serviceURI, user);
		setBasicAuthentication(user, pwd.toCharArray());
	    }
	}
    }

    public QueryEngineHTTP(String serviceURI, Query query)
    {
	this(serviceURI, query.toString());	
    }
    
}