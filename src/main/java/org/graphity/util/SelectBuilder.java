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

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.model.Query;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.model.Variable;
import org.topbraid.spin.vocabulary.SP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class SelectBuilder extends QueryBuilder implements Select
{
    private static final Logger log = LoggerFactory.getLogger(SelectBuilder.class);

    private Select select = null;

    protected SelectBuilder(Select select)
    {
	super(select);
	this.select = select;
    }

    public static SelectBuilder fromSelect(Select select)
    {
	return new SelectBuilder(select);
    }

    public static SelectBuilder fromResource(Resource resource)
    {
	if (resource == null) throw new IllegalArgumentException("Select Resource cannot be null");
	
	Query query = SPINFactory.asQuery(resource);
	if (query == null || !(query instanceof Select))
	    throw new IllegalArgumentException("SelectBuilder Resource must be a SPIN SELECT Query");

	return fromSelect((Select)query);
    }
    
    @Override
    protected Select getQuery()
    {
	return select;
    }


    public SelectBuilder limit(Long limit)
    {
	if (limit == null) throw new IllegalArgumentException("LIMIT cannot be null");

	if (log.isTraceEnabled()) log.trace("Setting LIMIT param: {}", limit);
	
	removeAll(SP.limit).
	    addLiteral(SP.limit, limit);
	
	return this;
    }

    public SelectBuilder offset(Long offset)
    {
	if (offset == null) throw new IllegalArgumentException("OFFSET cannot be null");
	
	if (log.isTraceEnabled()) log.trace("Setting OFFSET param: {}", offset);
	
	removeAll(SP.offset)
	    .addLiteral(SP.offset, offset);
	
	return this;
    }

    public SelectBuilder orderBy(String varName)
    {	
	return orderBy(varName, false);
    }

    public SelectBuilder orderBy(String varName, Boolean desc)
    {
	if (varName != null)
	    return orderBy(SPINFactory.createVariable(getModel(), varName), desc);
	else    
	    return orderBy((Variable)null, desc);
    }

    public SelectBuilder orderBy(Resource var)
    {
	if (var == null) throw new IllegalArgumentException("ORDER BY resource cannot be null");

	return orderBy(SPINFactory.asVariable(var), false);
    }

    public SelectBuilder orderBy(Resource var, Boolean desc)
    {
	if (var == null) throw new IllegalArgumentException("ORDER BY resource cannot be null");

	return orderBy(SPINFactory.asVariable(var), desc);
    }

    public SelectBuilder orderBy(Variable var)
    {
	return orderBy(var, false);
    }
    
    public SelectBuilder orderBy(Variable var, Boolean desc)
    {
	if (var == null) throw new IllegalArgumentException("ORDER BY variable cannot be null");
	if (desc == null) throw new IllegalArgumentException("DESC cannot be null");
	
	if (log.isTraceEnabled()) log.trace("Setting ORDER BY variable: {}", var);
	removeAll(SP.orderBy);

	Resource bnode = getModel().createResource().addProperty(SP.expression, var);
	addProperty(SP.orderBy, getModel().createList(new RDFNode[]{bnode}));

	if (desc)
	    bnode.addProperty(RDF.type, SP.Desc);
	else
	    bnode.addProperty(RDF.type, SP.Asc);
	
	return this;
    }

    @Override
    public List<Resource> getResultVariables()
    {
	return getQuery().getResultVariables();
    }

    @Override
    public boolean isDistinct()
    {
	return getQuery().isDistinct();
    }

    @Override
    public boolean isReduced()
    {
	return getQuery().isReduced();
    }

    @Override
    public Long getLimit()
    {
	return getQuery().getLimit();
    }

    @Override
    public Long getOffset()
    {
	return getQuery().getOffset();
    }

}