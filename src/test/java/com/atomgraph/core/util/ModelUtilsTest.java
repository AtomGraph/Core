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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 * What the model hash is relied on for, and the property it must not have.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ModelUtilsTest
{

    private static Model modelOf(String... objects)
    {
        Model model = ModelFactory.createDefaultModel();
        Resource subject = model.createResource("http://example.com/subject");
        for (String object : objects) subject.addProperty(model.createProperty("http://example.com/predicate"), object);
        return model;
    }

    @Test
    public void sameStatementsHashAlike() // conditional GET and conditional writes both need this
    {
        assertEquals(ModelUtils.hashModel(modelOf("one")), ModelUtils.hashModel(modelOf("one")));
    }

    @Test
    public void statementOrderDoesNotMatter()
    {
        assertEquals(ModelUtils.hashModel(modelOf("one", "two")), ModelUtils.hashModel(modelOf("two", "one")));
    }

    @Test
    public void differentStatementsHashDifferently()
    {
        assertNotEquals(ModelUtils.hashModel(modelOf("one")), ModelUtils.hashModel(modelOf("two")));
    }

    @Test
    public void addingAStatementDoesNotMoveTheHashPredictably()
    {
        // The hash becomes an entity tag, and an entity tag reaches agents who may write without
        // being able to read - so it must not be LINEAR in the set of statements. A fold by XOR
        // satisfies
        //
        //     hash(G+a) XOR hash(G+b) == hash(G) XOR hash(G+a+b)
        //
        // since both sides reduce to hash(a) XOR hash(b). That identity is what let such an agent add
        // a statement, look at how far the tag moved, and learn whether it was already there - a set
        // gains nothing when you add a member it already has. A digest satisfies it only by accident.
        long base = ModelUtils.hashModel(modelOf("one"));
        long withA = ModelUtils.hashModel(modelOf("one", "a"));
        long withB = ModelUtils.hashModel(modelOf("one", "b"));
        long withBoth = ModelUtils.hashModel(modelOf("one", "a", "b"));

        assertNotEquals(withA ^ withB, base ^ withBoth,
            "the hash is linear in the statement set, which makes an entity tag over it an oracle");
    }

    @Test
    public void anEmptyModelHashesConsistently()
    {
        assertEquals(ModelUtils.hashModel(ModelFactory.createDefaultModel()), ModelUtils.hashModel(ModelFactory.createDefaultModel()));
    }

}
