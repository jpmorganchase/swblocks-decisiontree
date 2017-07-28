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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import org.swblocks.jbl.util.DateRange;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link DateRangeDriver}.
 */
public class DateRangeDriverTest {
    private final DateRange testRange = new DateRange(Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS));
    private final InputDriver driver = new DateRangeDriver("TestRange", testRange);

    @Test
    public void correctValue() {
        assertEquals("TestRange", driver.getValue());
    }

    @Test
    public void correctType() {
        assertEquals(InputValueType.DATE_RANGE, driver.getType());
    }

    @Test
    public void evaluation() {
        assertTrue(driver.evaluate(Instant.now().plus(1, ChronoUnit.DAYS).toString()));
        assertFalse(driver.evaluate(Instant.now().minus(1, ChronoUnit.DAYS).toString()));
    }

    @Test
    public void hashCodeValue() {
        final InputDriver otherDriver = new DateRangeDriver("TestRange", null);
        assertEquals(otherDriver.hashCode(), driver.hashCode());
    }

    @Test
    public void driverEquals() {
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals(new StringDriver("TestRange")));

        final InputDriver otherDriver = new DateRangeDriver("TestRange", null);
        assertTrue(driver.equals(otherDriver));
    }
}