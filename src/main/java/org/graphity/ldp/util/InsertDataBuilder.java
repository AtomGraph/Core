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
package org.graphity.ldp.util;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.update.InsertData;
import org.topbraid.spin.model.update.Update;
import org.topbraid.spin.vocabulary.SP;

/**
 * SPARQL INSERT DATA builder based on SPIN RDF syntax
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see InsertDataBuilder
 * @see <a href="http://spinrdf.org/sp.html">SPIN - SPARQL Syntax</a>
 * @see <a href="http://topbraid.org/spin/api/1.2.0/spin/apidocs/org/topbraid/spin/model/Query.html">SPIN Query</a>
 */
public class InsertDataBuilder extends UpdateBuilder
{
    private InsertData insertData = null;
    
    protected InsertDataBuilder(InsertData insertData)
    {
	super(insertData);
	this.insertData = insertData;
    }
    
    public static InsertDataBuilder fromInsertData(InsertData insertData)
    {
	return new InsertDataBuilder(insertData);
    }

    public static InsertDataBuilder fromResource(Resource resource)
    {
	if (resource == null) throw new IllegalArgumentException("InsertData Resource cannot be null");
	
	Update update = SPINFactory.asUpdate(resource);
	if (update == null || !(update instanceof InsertData))
	    throw new IllegalArgumentException("InsertDataBuilder Resource must be a SPIN INSERT DATA Query");

	return fromInsertData((InsertData)update);
    }

    public static InsertDataBuilder newInstance()
    {
	return fromResource(ModelFactory.createDefaultModel().createResource().
	    addProperty(RDF.type, SP.InsertData));
    }

    public static InsertDataBuilder fromData(Model model)
    {
	return newInstance().data(model);
    }
    
    public InsertDataBuilder data(Model model)
    {
	addProperty(SP.data, createDataList(model));
	
	return this;
    }

    private Resource createTripleTemplate(Statement stmt)
    {
	return getModel().createResource().
	    addProperty(SP.subject, stmt.getSubject()).
	    addProperty(SP.predicate, stmt.getPredicate()).
	    addProperty(SP.object, stmt.getObject());
    }

    private RDFList createDataList(Model model)
    {
	RDFList data = getModel().createList();
	
	StmtIterator it = model.listStatements();
	while (it.hasNext())
	    data = data.with(createTripleTemplate(it.next()));
	
	return data;
    }

    @Override
    protected InsertData getUpdate()
    {
	return insertData;
    }

}