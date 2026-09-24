/**
 *  Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>
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

import java.util.Arrays;
import java.util.List;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.graph.NodeFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 * A result set's hash has to tell apart everything that makes it a different result set: which
 * variable held which value, and what order the rows came in.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ResultSetUtilsTest
{

    private static final Var A = Var.alloc("a"), B = Var.alloc("b");

    private static Binding binding(String a, String b)
    {
        return BindingFactory.binding(A, NodeFactory.createLiteralString(a), B, NodeFactory.createLiteralString(b));
    }

    private static ResultSet resultSet(Binding... bindings)
    {
        return ResultSetStream.create(List.of(A, B), Arrays.asList(bindings).iterator());
    }

    @Test
    public void sameRowsHashAlike()
    {
        assertEquals(ResultSetUtils.hashResultSet(resultSet(binding("1", "2"))),
                     ResultSetUtils.hashResultSet(resultSet(binding("1", "2"))));
    }

    @Test
    public void rowOrderMatters()
    {
        // ORDER BY makes a reordering a different result set. An XOR fold is commutative, so the two
        // hashed alike and one could be served from cache under the other's validator.
        assertNotEquals(ResultSetUtils.hashResultSet(resultSet(binding("1", "2"), binding("3", "4"))),
                        ResultSetUtils.hashResultSet(resultSet(binding("3", "4"), binding("1", "2"))));
    }

    @Test
    public void whichVariableHeldWhichValueMatters()
    {
        // the per-solution fold XORed node hashes across variables, so these two hashed alike
        assertNotEquals(ResultSetUtils.hashResultSet(resultSet(binding("1", "2"))),
                        ResultSetUtils.hashResultSet(resultSet(binding("2", "1"))));
    }

    @Test
    public void differentValuesHashDifferently()
    {
        assertNotEquals(ResultSetUtils.hashResultSet(resultSet(binding("1", "2"))),
                        ResultSetUtils.hashResultSet(resultSet(binding("1", "3"))));
    }

}
