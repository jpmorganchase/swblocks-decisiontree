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

import org.junit.Test;
import org.swblocks.jbl.util.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link IntegerRangeEvaluation}.
 */
public class IntegerRangeEvaluationTest {
    @Test
    public void evaluation() {
        final Range<Integer> testRange = new Range<>(100, 500);
        final IntegerRangeEvaluation driver = new IntegerRangeEvaluation("TestRange", testRange);

        assertTrue(driver.test("100"));
        assertFalse(driver.test("501"));

        // Start value is inclusive and end value is exclusive.
        assertTrue(driver.test("100"));
        assertFalse(driver.test("500"));

        // Allow any input wildcard matching.
        assertTrue(driver.test("*"));
    }

    @Test(expected = NumberFormatException.class)
    public void failEvaluationNotADate() {
        final Range<Integer> testRange = new Range<>(100, 500);
        final IntegerRangeEvaluation driver = new IntegerRangeEvaluation("TestRange", testRange);
        // Fail any none integer input
        driver.test("not an int");
    }

    @Test
    public void hashCodeValue() {
        final Range<Integer> testRange = new Range<>(100, 500);
        final IntegerRangeEvaluation driver = new IntegerRangeEvaluation("TestRange", testRange);
        assertEquals(new IntegerRangeEvaluation("TestRange", null).hashCode(), driver.hashCode());
    }

    @Test
    public void driverEquals() {
        final IntegerRangeEvaluation driver = new IntegerRangeEvaluation("TestRange", null);
        assertEquals(new IntegerRangeEvaluation("TestRange", null), driver);
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals("TestRange"));
        assertFalse(driver.equals(new StringEvaluation("TestRange")));
    }
}