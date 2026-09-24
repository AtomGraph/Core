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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Result set hash calculator.
 *
 * A digest over the solutions in order, for the reasons given on {@link ModelUtils}: a fold by XOR is
 * linear, which turns an entity tag into an oracle for whoever may write but not read. Two further
 * things that fold lost, both of which a result set is required to keep:
 *
 * <ul>
 * <li><strong>row order.</strong> XOR is commutative, so a result set and its reordering hashed the
 * same - and ORDER BY makes that a different result set, served from cache under one validator;</li>
 * <li><strong>which variable held which value.</strong> The per-solution fold XORed node hashes
 * across variables, so ?a=1 ?b=2 and ?a=2 ?b=1 hashed alike.</li>
 * </ul>
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ResultSetUtils
{

    /**
     * Returns a hash of the result set.
     *
     * Consumes the iterator; callers holding a {@link org.apache.jena.query.ResultSetRewindable}
     * reset it afterwards.
     *
     * @param result the result set
     * @return hash value
     */
    public static long hashResultSet(ResultSet result)
    {
        StringBuilder canonical = new StringBuilder();

        while (result.hasNext()) canonical.append(canonicalize(result.next())).append('\n'); // in order: ORDER BY makes it a different result set

        return ModelUtils.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static long hashQuerySolution(QuerySolution solution)
    {
        return ModelUtils.digest(canonicalize(solution).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Renders a solution as a string that distinguishes which variable held which value.
     * Variables are sorted, so the order they are iterated in does not matter; their values are not,
     * because a binding is a variable AND its value.
     *
     * @param solution the solution
     * @return canonical string
     */
    private static String canonicalize(QuerySolution solution)
    {
        List<String> varNames = new ArrayList<>();
        Iterator<String> it = solution.varNames();
        while (it.hasNext()) varNames.add(it.next());
        Collections.sort(varNames);

        StringBuilder canonical = new StringBuilder();
        for (String varName : varNames)
        {
            RDFNode node = solution.get(varName);
            if (node != null) canonical.append(varName).append('=').append(node).append('\t');
        }

        return canonical.toString();
    }

}
