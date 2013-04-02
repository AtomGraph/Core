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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.core.ResourceContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.graphity.server.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-write Linked Data resources
 * 
 * @see LDPResource
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class LDPResourceBase extends QueriedResourceBase implements LDPResource
{    
    private static final Logger log = LoggerFactory.getLogger(LDPResourceBase.class);

    public LDPResourceBase(@Context UriInfo uriInfo, @Context ResourceConfig resourceConfig, @Context ResourceContext resourceContext)
    {
	super(uriInfo, resourceConfig, resourceContext);
    }

    protected LDPResourceBase(Resource resource, SPARQLEndpointBase endpoint, CacheControl cacheControl)
    {
	super(resource, endpoint, cacheControl);
    }
    
    @Override
    /**
     * @link <a href="http://lists.w3.org/Archives/Public/public-ldp-wg/2012Oct/0181.html">What is the document base URI of a POSTed document?</a>
     * 
     */
    public Response post(Model model)
    {
	throw new WebApplicationException(405);
	
	//getOntResource().getOntModel().add(model);
	
	//return Response.created(null).build();
    }

    @Override
    //@GET
    public Response put(Model model)
    {
	//getUriInfo().
	DataManager.get().putModel(getEndpoint().getURI(), model);
	
	return Response.ok().build();
    }

    @Override
    public Response delete()
    {
	throw new WebApplicationException(405);

	// if (getService() != null) DataManager.get().deleteModel(endpointUri, getUriInfo().getAbsolutePath()
	
	//getOntResource().remove();
	
	//return Response.noContent().build(); // 204 No Content
	// 410 Gone if provenance shows previous versions: http://www.w3.org/TR/chips/#cp4.2
    }

}