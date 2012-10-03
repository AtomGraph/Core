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

package org.graphity.util;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.Iterator;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.*;
import org.topbraid.spin.model.print.PrintContext;
import org.topbraid.spin.model.print.StringPrintContext;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class QueryBuilder
{
    private static final Logger log = LoggerFactory.getLogger(QueryBuilder.class);
    private org.topbraid.spin.model.Query spinQuery = null;
    private ARQ2SPIN arq2spin = null; //new ARQ2SPIN(model);
	
    protected static QueryBuilder newInstance()
    {
        // Initialize system functions and templates
        SPINModuleRegistry.get().init();
	return new QueryBuilder();
    }
    
    public static QueryBuilder fromQuery(Query query, String uri)
    {
	return newInstance().query(query, uri);
    }

    public static QueryBuilder fromQuery(Query query)
    {
	return newInstance().query(query);
    }

    public static QueryBuilder fromConstructTemplate(Resource subject, Resource predicate, RDFNode object)
    {
	return newInstance().construct(subject, predicate, object);
    }

    public static QueryBuilder fromQueryString(String queryString)
    {
	return newInstance().query(QueryFactory.create(queryString), null);
    }

    public static QueryBuilder fromResource(Resource resource)
    {
	return newInstance().query(resource);
    }

    public static QueryBuilder fromDescribe(String uri)
    {
	return newInstance().describe(uri);
    }

    public static QueryBuilder fromDescribe(Resource resultNode)
    {
	return newInstance().describe(resultNode);
    }

    public static QueryBuilder fromDescribe(RDFList resultNodes)
    {
	return newInstance().describe(resultNodes);
    }

    public static QueryBuilder fromDescribe()
    {
	return newInstance().describe();
    }

    protected QueryBuilder query(Resource resource)
    {
	if (resource == null) throw new IllegalArgumentException("Query resource cannot be null");

	spinQuery = SPINFactory.asQuery(resource);
	if (spinQuery == null) throw new IllegalArgumentException("Resource is not a SPIN query");
	
	arq2spin = new ARQ2SPIN(spinQuery.getModel());
	
	return this;
    }

    protected QueryBuilder query(Query query)
    {
	return query(query, null);
    }
    
    protected QueryBuilder query(Query query, String uri)
    {
	if (query == null) throw new IllegalArgumentException("Query cannot be null");
	
	arq2spin = new ARQ2SPIN(ModelFactory.createDefaultModel());
	spinQuery = arq2spin.createQuery(query, uri);

	return this;
    }

    public QueryBuilder describe(String uri)
    {
	if (uri == null) throw new IllegalArgumentException("DESCRIBE URI cannot be null");
	
	return describe(ModelFactory.createDefaultModel().createResource(uri));
    }

    public QueryBuilder describe(Resource resultNode)
    {
	if (resultNode == null) throw new IllegalArgumentException("DESCRIBE resource cannot be null");
	
	if (resultNode.canAs(RDFList.class))
	    return describe(resultNode.as(RDFList.class));
	else
	    return describe(resultNode.getModel().createList(new RDFNode[]{resultNode}));
    }

    public QueryBuilder describe(RDFList resultNodes)
    {
	if (resultNodes == null) throw new IllegalArgumentException("DESCRIBE resource list cannot be null");

	return query(resultNodes.getModel().createResource(SP.Describe).
		addProperty(SP.resultNodes, resultNodes));
    }
    
    public QueryBuilder describe()
    {
	return query(ModelFactory.createDefaultModel().createResource(SP.Describe));
    }
    
    public QueryBuilder construct(TripleTemplate template)
    {
	if (template == null) throw new IllegalArgumentException("CONSTRUCT template cannot be null");

	Resource queryRes = ModelFactory.createDefaultModel().createResource(SP.Construct);
		
	((Construct)queryRes).getTemplates().add(template);

	return query(queryRes);
    }
    
    public QueryBuilder construct(Resource subject, Resource predicate, RDFNode object)
    {
	if (subject == null) throw new IllegalArgumentException("CONSTRUCT subject cannot be null");
	if (predicate == null) throw new IllegalArgumentException("CONSTRUCT predicate cannot be null");
	if (object == null) throw new IllegalArgumentException("CONSTRUCT object cannot be null");

	Resource queryRes = ModelFactory.createDefaultModel().createResource(SP.Construct);
	
	queryRes.addProperty(SP.templates, queryRes.getModel().createList(new RDFNode[]{queryRes.
		getModel().createResource().
		    addProperty(SP.subject, subject).
		    addProperty(SP.predicate, predicate).
		    addProperty(SP.object, object)}));
	
	return query(queryRes);
    }

    public QueryBuilder where(Resource element)
    {
	if (element == null) throw new IllegalArgumentException("WHERE element cannot be null");

	return where(SPINFactory.asElement(element));
    }
    
    public QueryBuilder where(Element element)
    {
	if (element == null) throw new IllegalArgumentException("WHERE element cannot be null");
	//spinQuery.getWhereElements().add(element); // doesn't work?

	if (!spinQuery.hasProperty(SP.where))
	    spinQuery.addProperty(SP.where, spinQuery.getModel().createList(new RDFNode[]{element}));
	else
	    spinQuery.getPropertyResourceValue(SP.where).
		    as(RDFList.class).
		    add(element);
	
	return this;
    }
    
    public QueryBuilder subQuery(QueryBuilder builder)
    {	
	return subQuery(builder.buildSPIN());
    }

    public QueryBuilder subQuery(Select select)
    {
	if (select == null) throw new IllegalArgumentException("SELECT sub-query cannot be null");
	
	SubQuery subQuery = SPINFactory.createSubQuery(spinQuery.getModel(), select);
	if (log.isTraceEnabled()) log.trace("SubQuery: {}", subQuery);
	return where(subQuery);
    }

    public QueryBuilder subQuery(Resource query)
    {
	if (query == null) throw new IllegalArgumentException("Sub-query resource cannot be null");
	
	spinQuery.getModel().add(query.getModel());
	return subQuery((Select)SPINFactory.asQuery(query));  // exception if not SELECT ?
    }

    public QueryBuilder optional(Resource optional)
    {
	return where(optional);
    }

    public QueryBuilder optional(Optional optional)
    {
	return where(optional);
    }

    public QueryBuilder optional(TriplePattern pattern)
    {
	if (pattern == null) throw new IllegalArgumentException("OPTIONAL pattern cannot be null");

	return where(SPINFactory.createOptional(spinQuery.getModel(),
		SPINFactory.createElementList(spinQuery.getModel(), new Element[]{pattern})));
    }

    public QueryBuilder optional(Resource subject, Resource predicate, RDFNode object)
    {
	if (subject == null) throw new IllegalArgumentException("OPTIONAL subject cannot be null");
	if (predicate == null) throw new IllegalArgumentException("OPTIONAL predicate cannot be null");
	if (object == null) throw new IllegalArgumentException("OPTIONAL object cannot be null");

	return optional(SPINFactory.createTriplePattern(spinQuery.getModel(), subject, predicate, object));
    }

    public QueryBuilder filter(Filter filter)
    {
	if (filter == null) throw new IllegalArgumentException("FILTER cannot be null");
	
	return where(filter);
    }
	
    public QueryBuilder filter(Variable var, Locale locale)
    {
	if (var == null) throw new IllegalArgumentException("FILTER variable name cannot be null");
	if (locale == null) throw new IllegalArgumentException("Locale cannot be null");
	
	if (log.isTraceEnabled()) log.trace("Setting FILTER (LANG({}) = \"{}\")", var, locale.toLanguageTag());
	
	Resource langExpr = spinQuery.getModel().createResource().
		addProperty(RDF.type, SP.getArgProperty("lang")).
		addProperty(SP.getArgProperty(1), var);
	
	Resource eqExpr = spinQuery.getModel().createResource().
		addProperty(RDF.type, SP.eq).
		addProperty(SP.getArgProperty(1), langExpr).
		addLiteral(SP.getArgProperty(2), spinQuery.getModel().createLiteral(locale.toLanguageTag()));

	return filter(SPINFactory.createFilter(spinQuery.getModel(), eqExpr));
    }

    public QueryBuilder filter(String varName, Locale locale)
    {
	if (varName == null) throw new IllegalArgumentException("FILTER variable name cannot be null");
	if (locale == null) throw new IllegalArgumentException("Locale cannot be null");
	
	return filter(SPINFactory.createVariable(spinQuery.getModel(), varName), locale);
    }

    public QueryBuilder filter(Variable var, RDFList resources)
    {
	if (var == null) throw new IllegalArgumentException("FILTER variable cannot be null");
	if (resources == null) throw new IllegalArgumentException("FILTER resource list cannot be null");

	if (log.isTraceEnabled()) log.trace("Setting FILTER param: {}", resources.getModel());
	
	return filter(SPINFactory.createFilter(spinQuery.getModel(), getFilterExpression(var, resources)));
    }

    private Resource getFilterExpression(Variable var, RDFList resources)
    {
	Resource eqExpr = spinQuery.getModel().createResource().
		addProperty(RDF.type, SP.eq).
		addProperty(SP.getArgProperty(1), var).
		addProperty(SP.getArgProperty(2), resources.getHead());

	if (resources.getTail().isEmpty()) // no more resources in list
	    return eqExpr;
	else
	    // more resources follow - join recursively with current value using || (or)
	    return spinQuery.getModel().createResource().
		addProperty(RDF.type, SP.getArgProperty("or")).
		addProperty(SP.getArgProperty(1), eqExpr).
		addProperty(SP.getArgProperty(2), getFilterExpression(var, resources.getTail()));
    }
    
    public QueryBuilder filter(String varName, RDFList resources)
    {
	if (varName == null) throw new IllegalArgumentException("FILTER variable name cannot be null");
	
	return filter(SPINFactory.createVariable(spinQuery.getModel(), varName), resources);
    }

    public QueryBuilder replaceFilter(String varName, RDFList resources)
    {
	if (log.isTraceEnabled()) log.trace("Replacing FILTER");

	while (findFilter(spinQuery.getWhere()) != null) // iterator.remove() doesn't work
	{
	    RDFNode filter = findFilter(spinQuery.getWhere());
	    if (log.isTraceEnabled()) log.trace("Removing FILTER: {}", filter);
	    spinQuery.getWhere().remove(filter);
	}
	
	return filter(varName, resources);
    }

    private RDFNode findFilter(ElementList list)
    {
	Iterator<RDFNode> it = list.iterator();

	while (it.hasNext())
	{
	    RDFNode node = it.next();
	    // if (SPINFactory.asExpression(node) instanceof Filter)
	    if (node.isResource() && node.asResource().hasProperty(RDF.type, SP.Filter))
		return node;
	}
	
	return null;
    }
    
    public QueryBuilder limit(Long limit)
    {
	if (limit == null) throw new IllegalArgumentException("LIMIT cannot be null");

	if (log.isTraceEnabled()) log.trace("Setting LIMIT param: {}", limit);
	
	spinQuery.removeAll(SP.limit).
	    addLiteral(SP.limit, limit);
	
	return this;
    }

    public QueryBuilder offset(Long offset)
    {
	if (offset == null) throw new IllegalArgumentException("OFFSET cannot be null");
	
	if (log.isTraceEnabled()) log.trace("Setting OFFSET param: {}", offset);
	
	spinQuery.removeAll(SP.offset)
	    .addLiteral(SP.offset, offset);
	
	return this;
    }

    public QueryBuilder orderBy(String varName)
    {	
	return orderBy(varName, false);
    }

    public QueryBuilder orderBy(String varName, Boolean desc)
    {
	if (varName != null)
	    return orderBy(SPINFactory.createVariable(spinQuery.getModel(), varName), desc);
	else    
	    return orderBy((Variable)null, desc);
    }

    public QueryBuilder orderBy(Resource var)
    {
	if (var == null) throw new IllegalArgumentException("ORDER BY resource cannot be null");

	return orderBy(SPINFactory.asVariable(var), false);
    }

    public QueryBuilder orderBy(Resource var, Boolean desc)
    {
	if (var == null) throw new IllegalArgumentException("ORDER BY resource cannot be null");

	return orderBy(SPINFactory.asVariable(var), desc);
    }

    public QueryBuilder orderBy(Variable var)
    {
	return orderBy(var, false);
    }
    
    public QueryBuilder orderBy(Variable var, Boolean desc)
    {
	if (var == null) throw new IllegalArgumentException("ORDER BY variable cannot be null");
	if (desc == null) throw new IllegalArgumentException("DESC cannot be null");
	
	if (log.isTraceEnabled()) log.trace("Setting ORDER BY variable: {}", var);
	spinQuery.removeAll(SP.orderBy);

	Resource bnode = spinQuery.getModel().createResource().addProperty(SP.expression, var);
	spinQuery.addProperty(SP.orderBy, spinQuery.getModel().createList(new RDFNode[]{bnode}));

	if (desc)
	    bnode.addProperty(RDF.type, SP.Desc);
	else
	    bnode.addProperty(RDF.type, SP.Asc);
	
	return this;
    }

    public QueryBuilder replaceVar(String varName, String uri)
    {
	if (varName == null) throw new IllegalArgumentException("Variable name cannot be null");
	if (uri == null) throw new IllegalArgumentException("URI value cannot be null");
	
	Resource var = getVarByName(varName);
	var.removeAll(SP.varName);
	ResourceUtils.renameResource(var, uri);

	return this;
    }
    
    public QueryBuilder replaceVar(String varName, Resource resource)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	
	return replaceVar(varName, resource.getURI());
    }

    protected Resource getVarByName(String name)
    {
	ResIterator it = spinQuery.getModel().listResourcesWithProperty(SP.varName, name);
	if (it.hasNext()) return it.nextResource();
	else return null;
    }

    public Query build()
    {
	// ARQFactory.get().setUseCaches(false) to avoid caching
	return ARQFactory.get().createQuery(buildSPIN());
    }

    public org.topbraid.spin.model.Query buildSPIN()
    {
	if (log.isTraceEnabled()) log.trace("Generated SPIN Query: {}", spinQuery.toString()); // no PREFIXes

	// generate SPARQL query string
	StringBuilder sb = new StringBuilder();
	PrintContext pc = new StringPrintContext(sb);
	pc.setPrintPrefixes(true);
	spinQuery.print(pc);

	spinQuery.removeAll(SP.text)
	    .addLiteral(SP.text, spinQuery.getModel().createTypedLiteral(sb.toString()));

	return spinQuery;
    }

}
