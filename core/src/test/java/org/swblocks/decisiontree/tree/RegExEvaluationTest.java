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
 * Test class for {@link RegExEvaluation}.
 */
public class RegExEvaluationTest {
    @Test
    public void evaluation() {
        final RegExEvaluation driver = new RegExEvaluation("Te.?t1");
        assertTrue(driver.test("Test1"));
        assertFalse(driver.test("Fest1"));
    }

    @Test
    public void hashCodeValue() {
        final RegExEvaluation driver = new RegExEvaluation("Te.?t1");
        assertEquals(new RegExEvaluation("Te.?t1").hashCode(), driver.hashCode());
    }

    @Test
    public void driverEquals() {
        final RegExEvaluation driver = new RegExEvaluation("Te.?t1");
        assertEquals(new RegExEvaluation("Te.?t1"), driver);
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals("Te.?t1"));
    }
}