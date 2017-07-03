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

package org.swblocks.decisiontree.domain;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/**
 * Test cases for {@link WeightedDriver}.
 */
public class WeightedDriverTest {
    @Test
    public void testConstruction() {
        final WeightedDriver test = new WeightedDriver("TestName", 1);
        assertEquals("TestName", test.getName());
        assertEquals(1, test.getWeight());
        assertEquals("WeightedDriver{name='TestName', weight=1}", test.toString());
    }

    @Test
    public void testOrdering() {
        final WeightedDriver test1 = new WeightedDriver("TestName1", 1);
        final WeightedDriver test2 = new WeightedDriver("TestName2", 2);
        final WeightedDriver test3 = new WeightedDriver("TestName3", 3);

        // Insert out of order
        final SortedSet<WeightedDriver> weights = new TreeSet<>();
        weights.add(test2);
        weights.add(test1);
        weights.add(test3);

        // Confirm iterator is in order
        final Iterator<WeightedDriver> iterator = weights.iterator();
        assertEquals("TestName3", iterator.next().getName());
        assertEquals("TestName2", iterator.next().getName());
        assertEquals("TestName1", iterator.next().getName());
    }

    @Test
    public void testEqualsAndHashCode() {
        final WeightedDriver driver = new WeightedDriver("test1", 0);
        // Equals test on name
        assertEquals(driver, driver);
        assertEquals(driver, new WeightedDriver("test1", 2));
        assertNotEquals(driver, new WeightedDriver("test11", 0));
        assertFalse(driver.equals("test11"));
        assertFalse(driver.equals(null));
        // HashCode on name
        assertEquals("test1".hashCode(), new WeightedDriver("test1", 1).hashCode());
    }
}