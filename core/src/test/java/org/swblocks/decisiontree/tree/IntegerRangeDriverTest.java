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
 * Test cases for {@link IntegerRangeDriver}.
 */
public class IntegerRangeDriverTest {
    private final Range<Integer> testRange = new Range<>(100, 500);
    private final InputDriver driver = new IntegerRangeDriver("TestRange", testRange);

    @Test
    public void correctValue() {
        assertEquals("TestRange", driver.getValue());
    }

    @Test
    public void correctType() {
        assertEquals(InputValueType.INTEGER_RANGE, driver.getType());
    }

    @Test
    public void evaluation() {
        assertTrue(driver.evaluate("150"));
        assertFalse(driver.evaluate("1000"));
    }

    @Test
    public void hashCodeValue() {
        final InputDriver otherDriver = new IntegerRangeDriver("TestRange", null);
        assertEquals(otherDriver.hashCode(), driver.hashCode());
    }

    @Test
    public void driverEquals() {
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals(new StringDriver("TestRange")));

        final InputDriver otherDriver = new IntegerRangeDriver("TestRange", null);
        assertTrue(driver.equals(otherDriver));
    }
}