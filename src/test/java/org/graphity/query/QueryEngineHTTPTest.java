/*
 * Copyright (C) 2014 gliuk
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
package org.graphity.query;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gliuk
 */
public class QueryEngineHTTPTest {

    public QueryEngineHTTPTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test 
    public void testQueryEngineHTTPWithTwoStrings() {
        // TODO review the generated test code and remove the default call to fail.
        QueryEngineHTTP tester = new QueryEngineHTTP("uri", "querystring");
        try {
            new QueryEngineHTTP("uri", "querystring");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
