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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import org.swblocks.jbl.util.DateRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link DateRangeEvaluation}.
 */
public class DateRangeEvaluationTest {
    @Test
    public void evaluation() {
        final DateRange testRange = new DateRange(Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS));
        final DateRangeEvaluation driver = new DateRangeEvaluation("TestRange", testRange);

        assertTrue(driver.test(Instant.now().plus(1, ChronoUnit.DAYS).toString()));
        assertFalse(driver.test(Instant.now().minus(1, ChronoUnit.DAYS).toString()));

        // Start date in inclusive and end date is exclusive.
        assertTrue(driver.test(testRange.getStart().toString()));
        assertFalse(driver.test(testRange.getFinish().toString()));

        // Allow any input wildcard matching.
        assertTrue(driver.test("*"));
    }

    @Test (expected = DateTimeParseException.class)
    public void failEvaluationNotADate() {
        final DateRange testRange = new DateRange(Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS));
        final DateRangeEvaluation driver = new DateRangeEvaluation("TestRange", testRange);
        // Fail any none date input
        driver.test("not a date");
    }

    @Test
    public void hashCodeValue() {
        final DateRange testRange = new DateRange(Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS));
        final DateRangeEvaluation driver = new DateRangeEvaluation("TestRange", testRange);
        assertEquals(new DateRangeEvaluation("TestRange", null).hashCode(), driver.hashCode());
    }

    @Test
    public void driverEquals() {
        final DateRangeEvaluation driver = new DateRangeEvaluation("TestRange", null);
        assertEquals(new DateRangeEvaluation("TestRange", null), driver);
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals("TestRange"));
        assertFalse(driver.equals(new StringEvaluation("TestRange")));
    }
}