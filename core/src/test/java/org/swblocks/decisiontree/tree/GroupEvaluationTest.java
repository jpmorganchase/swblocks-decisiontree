/*
 * This file is part of the swblocks-decisiontree library.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.swblocks.decisiontree.tree;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {link @GroupEvaluation}.
 */
public class GroupEvaluationTest {
    @Test
    public void testConstructionAndEquals() {
        final GroupEvaluation groupEvaluation = new GroupEvaluation("TestGroup",
                GroupDriverTest.createSubInputDrivers("test"));
        assertNotNull(groupEvaluation);

        // Equals on name, not list
        assertEquals(new GroupEvaluation("TestGroup", Collections.emptyList()), groupEvaluation);
        assertEquals(new GroupEvaluation("TestGroup", Collections.emptyList()).hashCode(), groupEvaluation.hashCode());

        assertFalse(groupEvaluation.equals(null));
        assertFalse(groupEvaluation.equals("TestGroup"));
        assertTrue(groupEvaluation.equals(groupEvaluation));
        assertNotEquals(new GroupEvaluation("NotTestGroup", Collections.emptyList()), groupEvaluation);
    }

    @Test
    public void testEvaluation() {
        final GroupEvaluation groupEvaluation = new GroupEvaluation("TestGroup",
                GroupDriverTest.createSubInputDrivers("test"));

        assertTrue(groupEvaluation.test("test1"));
        assertTrue(groupEvaluation.test("test2"));
        assertTrue(groupEvaluation.test("test3"));
        assertTrue(groupEvaluation.test("test4"));
        assertFalse(groupEvaluation.test("test5"));
        assertFalse(groupEvaluation.test("TestGroup"));
    }

    @Test
    public void testMutablility() {
        final GroupEvaluation groupEvaluation = new GroupEvaluation("TestGroup",
                GroupDriverTest.createSubInputDrivers("test"));

        groupEvaluation.setSubValues(GroupDriverTest.createSubInputDrivers("foo"));
        assertFalse(groupEvaluation.test("test1"));
        assertFalse(groupEvaluation.test("test2"));
        assertFalse(groupEvaluation.test("test3"));
        assertFalse(groupEvaluation.test("test4"));

        assertTrue(groupEvaluation.test("foo1"));
        assertTrue(groupEvaluation.test("foo2"));
        assertTrue(groupEvaluation.test("foo3"));
        assertTrue(groupEvaluation.test("foo4"));
    }

    @Test
    public void hashCodeValue() {
        final GroupEvaluation driver = new GroupEvaluation("Test1", Arrays.asList(new StringDriver("test")));
        final GroupEvaluation other = new GroupEvaluation("Test1", Arrays.asList(new StringDriver("test")));

        assertTrue(driver.hashCode() == other.hashCode());
    }

    @Test
    public void equalsCorrect() {
        final GroupEvaluation driver = new GroupEvaluation("Test1", Arrays.asList(new StringDriver("test")));
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals(Boolean.TRUE));

        GroupEvaluation other = new GroupEvaluation("Test1", Arrays.asList(new StringDriver("test")));
        assertTrue(driver.equals(other));

        other = new GroupEvaluation("Test10", Arrays.asList(new StringDriver("test")));
        assertFalse(driver.equals(other));
    }
}