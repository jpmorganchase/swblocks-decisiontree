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
 * Test class for {@link StringDriver}.
 */
public class StringDriverTest {
    @Test
    public void correctValue() {
        final InputDriver driver = createStringDriver("Test1");
        assertEquals("Test1", driver.getValue());
    }

    @Test
    public void correctType() {
        final InputDriver driver = createStringDriver("Test1");
        assertEquals(InputValueType.STRING, driver.getType());
    }

    @Test
    public void evaluation() {
        final InputDriver driver = createStringDriver("Test1");
        assertTrue(driver.evaluate("Test1"));
        assertFalse(driver.evaluate("Test2"));
    }

    @Test
    public void hashCodeValue() {
        final InputDriver driver = createStringDriver("Test1");
        assertEquals(80698815, driver.hashCode());
    }

    @Test
    public void driverEquals() {
        final InputDriver driver = createStringDriver("Test1");
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals(new Integer(1)));

        InputDriver other = createStringDriver("Test1");
        assertTrue(driver.equals(other));

        other = createStringDriver("Test3");
        assertFalse(driver.equals(other));

        other = new RegexDriver("Test1");
        assertFalse(driver.equals(other));
    }

    private InputDriver createStringDriver(final String driverName) {
        return new StringDriver(driverName);
    }
}