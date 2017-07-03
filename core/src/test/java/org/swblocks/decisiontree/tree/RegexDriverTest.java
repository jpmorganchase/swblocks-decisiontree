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
 * Test class for {@link RegexDriver}.
 */
public class RegexDriverTest {
    @Test
    public void correctValue() {
        final InputDriver driver = createRegexDriver("Te.?t1");
        assertEquals("Te.?t1", driver.getValue());
    }

    @Test
    public void correctType() {
        final InputDriver driver = createRegexDriver("Te.?t1");
        assertEquals(InputValueType.REGEX, driver.getType());
    }

    @Test
    public void evaluation() {
        final InputDriver driver = createRegexDriver("Te.?t1");
        assertTrue(driver.evaluate("Test1"));
        assertFalse(driver.evaluate("Fest1"));
    }

    @Test
    public void hashCodeValue() {
        final InputDriver driver = createRegexDriver("Te.?t1");
        final InputDriver other = createRegexDriver("Te.?t1");
        assertEquals(driver.hashCode(), other.hashCode());
    }

    @Test
    public void driverEquals() {
        final InputDriver driver = createRegexDriver("Te.?t1");
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals(new Integer(1)));

        InputDriver other = createRegexDriver("Te.?t1");
        assertTrue(driver.equals(other));

        other = createRegexDriver("Fe.?t1");
        assertFalse(driver.equals(other));

        other = new StringDriver("Te.?t1");
        assertFalse(driver.equals(other));
    }

    private InputDriver createRegexDriver(final String driverName) {
        return new RegexDriver(driverName);
    }
}