/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graphity.ldp.model.impl;

import com.hp.hpl.jena.query.Query;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import org.graphity.ldp.model.EndpointResultSetResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class EndpointResultSetResourceImpl extends org.graphity.model.impl.EndpointResultSetResourceImpl implements EndpointResultSetResource
{
    private static final Logger log = LoggerFactory.getLogger(EndpointResultSetResourceImpl.class);

    private Request req = null;
    private UriInfo uriInfo = null;
    private MediaType acceptType = org.graphity.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE;

    public EndpointResultSetResourceImpl(String endpointUri,
	@Context UriInfo uriInfo, @Context Request req,
	@QueryParam("accept") MediaType acceptType,
	@QueryParam("limit") @DefaultValue("20") long limit,
	@QueryParam("offset") @DefaultValue("0") long offset,
	@QueryParam("order-by") String orderBy,
	@QueryParam("desc") @DefaultValue("true") boolean desc,
	@QueryParam("query") Query query)
    {
	super(endpointUri, query);
	this.req = req;
	this.uriInfo = uriInfo;
	if (acceptType != null) this.acceptType = acceptType;
    }

    @Override
    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    @Override
    public Request getRequest()
    {
	return req;
    }
    
    @Override
    public Response getResponse()
    {
	if (log.isDebugEnabled()) log.debug("Accept param: {}, writing SPARQL results (XML or JSON)", getAcceptType());

	// uses ResultSetWriter
	return Response.ok(getResultSet(), getAcceptType()).build();
    }

    public MediaType getAcceptType()
    {
	return acceptType;
    }

    @Override
    public EntityTag getEntityTag()
    {
	//return super.getEntityTag();
	return null;
    }

}
