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
package org.graphity.server.update;

import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import java.util.Map;
import org.apache.jena.riot.WebContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class UpdateProcessRemote extends com.hp.hpl.jena.sparql.modify.UpdateProcessRemote
{
    private static final Logger log = LoggerFactory.getLogger(UpdateProcessRemote.class);
    
    private final UpdateRequest request ;
    private final String endpointURI ;
    private String user = null, password = null ;

    public UpdateProcessRemote(UpdateRequest request, String serviceURI, Context context)
    {
	super(request, serviceURI, context);
        this.request = request ;
        this.endpointURI = serviceURI ;
        //this.context = new Context(ARQ.getContext()) ;
	
	Map<String, Context> serviceContextMap = (Map<String,Context>)context.get(Service.serviceContext);
	if (serviceContextMap != null && serviceContextMap.containsKey(serviceURI))
	{
	    Context serviceContext = serviceContextMap.get(serviceURI);
	    if (log.isDebugEnabled()) log.debug("Endpoint URI {} has SERVICE Context: {} ", serviceURI, serviceContext);

	    String usr = serviceContext.getAsString(Service.queryAuthUser);
	    String pwd = serviceContext.getAsString(Service.queryAuthPwd);
	    
	    if (usr != null || pwd != null)
	    {
		usr = usr==null?"":usr;
		pwd = pwd==null?"":pwd;
		if (log.isDebugEnabled()) log.debug("Setting basic HTTP authentication for endpoint URI {} with username: {} ", serviceURI, usr);
		setBasicAuthentication(usr, pwd);
	    }
	}
    }

    @Override
    public GraphStore getGraphStore()
    {
        return null ;
    }

    @Override
    public void execute()
    {
	Client client = Client.create();
	WebResource wr = client.resource(endpointURI);
	client.addFilter(new LoggingFilter(System.out));
	
	if (user != null && password != null)
	{
	    if (log.isDebugEnabled()) log.debug("Setting HTTP Basic auth for endpoint {} with username {}", endpointURI, user);
	    client.addFilter(new HTTPBasicAuthFilter(user, password));
	}
	
	String reqStr = request.toString();

	if (log.isDebugEnabled()) log.debug("Sending SPARQL request {} to endpoint {}", reqStr, endpointURI);
	ClientResponse response =
	    wr.type(WebContent.contentTypeSPARQLUpdate).
	    accept(WebContent.contentTypeResultsXML).
	    post(ClientResponse.class, reqStr);
	
	if (log.isDebugEnabled()) log.debug("SPARQL endpoint response: {}", response);
    }

    public final void setBasicAuthentication(String user, String password)
    {
        this.user = user ;
        this.password = password ;
    }
}