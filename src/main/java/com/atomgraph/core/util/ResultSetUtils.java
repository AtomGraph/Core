/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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

package com.atomgraph.core.util;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import java.util.Iterator;

/**
 * Result set hash calculator.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ResultSetUtils
{
    public static long hashResultSet(ResultSet result)
    {
    	long hash = 0;
	
	while (result.hasNext()) hash ^= hashQuerySolution(result.next());
	
	return hash;
    }
    
    public static long hashQuerySolution(QuerySolution solution)
    {
	long hash = 0;
	
	Iterator<String> it = solution.varNames();
	while (it.hasNext())
	{
	    RDFNode node = solution.get(it.next());
	    if (node != null) hash ^= (long) node.hashCode();
	}
	
	return hash;
    }
    
}