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

package org.graphity.query;

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