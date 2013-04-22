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

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.update.UpdateRequest;
import com.sun.jersey.api.core.ResourceConfig;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.*;
import org.graphity.server.util.DataManager;
import org.graphity.server.vocabulary.GS;
import org.graphity.server.vocabulary.VoID;
import org.graphity.util.ModelUtils;
import org.graphity.util.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL endpoint resource, implementing SPARQL HTTP protocol
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Path("/sparql")
public class SPARQLEndpointBase implements SPARQLEndpoint
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);

    public static final List<Variant> MODEL_VARIANTS = Variant.VariantListBuilder.newInstance().
		mediaTypes(org.graphity.server.MediaType.APPLICATION_RDF_XML_TYPE,
			org.graphity.server.MediaType.TEXT_TURTLE_TYPE).
		add().build();

    public static final List<Variant> RESULT_SET_VARIANTS = Variant.VariantListBuilder.newInstance().
			mediaTypes(org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_XML_TYPE,
			    org.graphity.server.MediaType.APPLICATION_SPARQL_RESULTS_JSON_TYPE).
			add().build();

    public static final List<Variant> VARIANTS;
    static
    {
	List<Variant> variants = new ArrayList<Variant>();
	variants.addAll(MODEL_VARIANTS);
	variants.addAll(RESULT_SET_VARIANTS);
	VARIANTS = variants;
    }
    
    private final Resource resource;
    private final Request request;
    private final ResourceConfig resourceConfig;

    public SPARQLEndpointBase(@Context Request request, @Context ResourceConfig resourceConfig)
    {
	this((resourceConfig.getProperty(VoID.sparqlEndpoint.getURI()) == null) ?
		null :
		ResourceFactory.createResource(resourceConfig.getProperty(VoID.sparqlEndpoint.getURI()).toString()),
	    request, resourceConfig);
    }
    
    protected SPARQLEndpointBase(Resource endpoint, Request request, ResourceConfig resourceConfig)
    {
	if (endpoint == null) throw new IllegalArgumentException("Endpoint cannot be null");
	if (!endpoint.isURIResource()) throw new IllegalArgumentException("Endpoint must be URI Resource (not a blank node)");
	if (request == null) throw new IllegalArgumentException("Request cannot be null");
	if (resourceConfig == null) throw new IllegalArgumentException("ResourceConfig cannot be null");

	this.resource = endpoint;
	this.request = request;
	this.resourceConfig = resourceConfig;
	if (log.isDebugEnabled()) log.debug("Constructing SPARQLEndpointBase with endpoint: {}", endpoint);
    }

    // SPARQL Query

    @Override
    @GET
    public Response query(@QueryParam("query") Query query,
	@QueryParam("default-graph-uri") URI defaultGraphUri, @QueryParam("named-graph-uri") URI graphUri)
    {
	return getResponseBuilder(query).build();
    }

    @Override
    public Response queryEncoded(@FormParam("query") Query query,
	@FormParam("default-graph-uri") URI defaultGraphUri, @FormParam("named-graph-uri") URI graphUri)
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    @POST
    public Response queryDirectly(Query query, @QueryParam("default-graph-uri") URI defaultGraphUri,
	@QueryParam("named-graph-uri") URI graphUri)
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    // SPARQL Update

    @Override
    @POST
    @Consumes(org.graphity.server.MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@FormParam("update") UpdateRequest update,
	@FormParam("using-graph-uri") URI defaultGraphUri,
	@FormParam("using-named-graph-uri") URI graphUri)
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    @POST
    @Consumes(org.graphity.server.MediaType.APPLICATION_SPARQL_UPDATE)
    public Response update(@QueryParam("using-graph-uri") URI defaultGraphUri,
	@QueryParam("using-named-graph-uri") URI graphUri)
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResponseBuilder getResponseBuilder(Query query)
    {
	if (query == null) throw new WebApplicationException(Response.Status.BAD_REQUEST);

	if (query.isSelectType())
	{
	    if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing SELECT query: {}", query);
	    if (getResourceConfig().getProperty(GS.resultLimit.getURI()) != null)
		query.setLimit(Long.parseLong(getResourceConfig().getProperty(GS.resultLimit.getURI()).toString()));

	    return getResponseBuilder(loadResultSetRewindable(getResource(), query));
	}

	if (query.isConstructType() || query.isDescribeType())
	{
	    if (log.isDebugEnabled()) log.debug("SPARQL endpoint executing CONSTRUCT/DESCRIBE query: {}", query);
	    return getResponseBuilder(loadModel(getResource(), query));
	}

	if (log.isWarnEnabled()) log.warn("SPARQL endpoint received unknown type of query: {}", query);
	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    @Override
    public ResponseBuilder getResponseBuilder(Model model)
    {
	return getResponseBuilder(model, MODEL_VARIANTS);
    }
    
    @Override
    public ResponseBuilder getResponseBuilder(Model model, List<Variant> variants)
    {
	return getResponseBuilder(new EntityTag(Long.toHexString(ModelUtils.hashModel(model))),
		model, variants);
    }
    
    @Override
    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet)
    {
	return getResponseBuilder(resultSet, RESULT_SET_VARIANTS);
    }
    
    @Override
    public ResponseBuilder getResponseBuilder(ResultSetRewindable resultSet, List<Variant> variants)
    {
	EntityTag entityTag = new EntityTag(Long.toHexString(ResultSetUtils.hashResultSet(resultSet)));
	resultSet.reset(); // ResultSet needs to be rewinded back to the beginning
	return getResponseBuilder(entityTag,
		resultSet, variants);
    }
    
    @Override
    public ResponseBuilder getResponseBuilder(EntityTag entityTag, Object entity, List<Variant> variants)
    {	
	Response.ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
	if (rb != null)
	{
	    if (log.isTraceEnabled()) log.trace("Resource not modified, skipping Response generation");
	    return rb;
	}
	else
	{
	    Variant variant = getRequest().selectVariant(variants);
	    if (variant == null)
	    {
		if (log.isTraceEnabled()) log.trace("Requested Variant {} is not on the list of acceptable Response Variants: {}", variant, getVariants());
		return Response.notAcceptable(variants);
	    }	
	    else
	    {
		if (log.isTraceEnabled()) log.trace("Generating RDF Response with Variant: {} and EntityTag: {}", variant, entityTag);
		return Response.ok(entity, variant).
			tag(entityTag);
	    }
	}	
    }
    
    public ResultSetRewindable loadResultSetRewindable(Resource endpoint, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading ResultSet from SPARQL endpoint: {} using Query: {}", endpoint.getURI(), query);
	return DataManager.get().loadResultSet(endpoint.getURI(), query); // .getResultSetRewindable()
    }

    @Override
    public ResultSetRewindable loadResultSetRewindable(Query query)
    {
	return loadResultSetRewindable(getResource(), query);
    }
    
    public Model loadModel(Resource endpoint, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Loading Model from SPARQL endpoint: {} using Query: {}", endpoint, query);
	return DataManager.get().loadModel(endpoint.getURI(), query);
    }

    @Override
    public Model loadModel(Query query)
    {
	return loadModel(getResource(), query);
    }

    private Resource getResource()
    {
	return resource;
    }

   @Override
    public String getURI()
    {
	return getResource().getURI();
    }

    @Override
    public Model getModel()
    {
	return getResource().getModel();
    }

    public List<Variant> getVariants()
    {
	return VARIANTS;
    }
    
    public Request getRequest()
    {
	return request;
    }

    public ResourceConfig getResourceConfig()
    {
	return resourceConfig;
    }

    @Override
    public AnonId getId()
    {
	return getResource().getId();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource inModel(Model model)
    {
	return getResource().inModel(model);
    }

    @Override
    public boolean hasURI(String string)
    {
	return getResource().hasURI(string);
    }

    @Override
    public String getNameSpace()
    {
	return getResource().getNameSpace();
    }

    @Override
    public String getLocalName()
    {
	return getResource().getLocalName();
    }

    @Override
    public Statement getRequiredProperty(Property prprt)
    {
	return getResource().getRequiredProperty(prprt);
    }

    @Override
    public Statement getProperty(Property prprt)
    {
	return getResource().getProperty(prprt);
    }

    @Override
    public StmtIterator listProperties(Property prprt)
    {
	return getResource().listProperties(prprt);
    }

    @Override
    public StmtIterator listProperties()
    {
	return getResource().listProperties();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, boolean bln)
    {
	return getResource().addLiteral(prprt, bln);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, long l)
    {
	return getResource().addLiteral(prprt, l);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, char c)
    {
	return getResource().addLiteral(prprt, c);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, double d)
    {
	return getResource().addLiteral(prprt, d);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, float f)
    {
	return getResource().addLiteral(prprt, f);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, Object o)
    {
	return getResource().addLiteral(prprt, o);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addLiteral(Property prprt, Literal ltrl)
    {
	return getResource().addLiteral(prprt, ltrl);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, String string)
    {
	return getResource().addLiteral(prprt, string);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, String string, String string1)
    {
	return getResource().addProperty(prprt, string, string1);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, String string, RDFDatatype rdfd)
    {
	return getResource().addProperty(prprt, prprt);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource addProperty(Property prprt, RDFNode rdfn)
    {
	return getResource().addProperty(prprt, rdfn);
    }

    @Override
    public boolean hasProperty(Property prprt)
    {
	return getResource().hasProperty(prprt);
    }

    @Override
    public boolean hasLiteral(Property prprt, boolean bln)
    {
	return getResource().hasLiteral(prprt, bln);
    }

    @Override
    public boolean hasLiteral(Property prprt, long l)
    {
	return getResource().hasLiteral(prprt, l);
    }

    @Override
    public boolean hasLiteral(Property prprt, char c)
    {
	return getResource().hasLiteral(prprt, c);
    }

    @Override
    public boolean hasLiteral(Property prprt, double d)
    {
	return getResource().hasLiteral(prprt, d);
    }

    @Override
    public boolean hasLiteral(Property prprt, float f)
    {
	return getResource().hasLiteral(prprt, f);
    }

    @Override
    public boolean hasLiteral(Property prprt, Object o)
    {
	return getResource().hasLiteral(prprt, o);
    }

    @Override
    public boolean hasProperty(Property prprt, String string)
    {
	return getResource().hasProperty(prprt, string);
    }

    @Override
    public boolean hasProperty(Property prprt, String string, String string1)
    {
	return getResource().hasProperty(prprt, string, string1);
    }

    @Override
    public boolean hasProperty(Property prprt, RDFNode rdfn)
    {
	return getResource().hasProperty(prprt, rdfn);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource removeProperties()
    {
	return getResource().removeProperties();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource removeAll(Property prprt)
    {
	return getResource().removeAll(prprt);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource begin()
    {
	return getResource().begin();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource abort()
    {
	return getResource().abort();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource commit()
    {
	return getResource().commit();
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource getPropertyResourceValue(Property prprt)
    {
	return getResource().getPropertyResourceValue(prprt);
    }

    @Override
    public boolean isAnon()
    {
	return getResource().isAnon();
    }

    @Override
    public boolean isLiteral()
    {
	return getResource().isLiteral();
    }

    @Override
    public boolean isURIResource()
    {
	return getResource().isURIResource();
    }

    @Override
    public boolean isResource()
    {
	return getResource().isResource();
    }

    @Override
    public <T extends RDFNode> T as(Class<T> type)
    {
	return getResource().as(type);
    }

    @Override
    public <T extends RDFNode> boolean canAs(Class<T> type)
    {
	return getResource().canAs(type);
    }

    @Override
    public Object visitWith(RDFVisitor rdfv)
    {
	return getResource().visitWith(rdfv);
    }

    @Override
    public com.hp.hpl.jena.rdf.model.Resource asResource()
    {
	return getResource().asResource();
    }

    @Override
    public Literal asLiteral()
    {
	return getResource().asLiteral();
    }

    @Override
    public Node asNode()
    {
	return getResource().asNode();
    }

    @Override
    public String toString()
    {
	return getResource().toString();
    }

}