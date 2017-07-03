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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link StringEvaluation}.
 */
public class StringEvaluationTest {
    @Test
    public void testEvaluation() {
        final StringEvaluation stringEvaluation = new StringEvaluation("Test1");
        assertTrue(stringEvaluation.test("Test1"));
        assertFalse(stringEvaluation.test("Test"));
        assertEquals(new StringEvaluation("Test1"), stringEvaluation);
        assertTrue(stringEvaluation.equals(stringEvaluation));
        assertFalse(stringEvaluation.equals("Test1"));
        assertFalse(stringEvaluation.equals(null));
    }

    @Test
    public void hashCodeValue() {
        final StringEvaluation driver = new StringEvaluation("Test1");
        assertEquals(new StringEvaluation("Test1").hashCode(), driver.hashCode());
    }

    @Test
    public void equalsCorrect() {
        final StringEvaluation driver = new StringEvaluation("Test1");
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals(new Integer(1)));

        StringEvaluation other = new StringEvaluation("Test1");
        assertTrue(driver.equals(other));

        other = new StringEvaluation("Test3");
        assertFalse(driver.equals(other));

        final RegExEvaluation regExEvaluation = new RegExEvaluation("Test1");
        assertFalse(driver.equals(regExEvaluation));
    }
}