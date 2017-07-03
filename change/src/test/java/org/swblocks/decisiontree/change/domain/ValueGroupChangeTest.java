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

package org.swblocks.decisiontree.change.domain;

import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.jbl.test.utils.JblTestClassUtils;
import org.swblocks.jbl.util.DateRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ValueGroupChange}.
 */
public class ValueGroupChangeTest {
    private final Map<String, Object> injectedValues = new HashMap<>();
    private ValueGroupChange bean;

    @Before
    public void setup() {
        this.injectedValues.clear();
        setBean(null);
    }

    @Test
    public void testConstruction() {
        final UUID id = new UUID(0, 1);
        final Instant start = Instant.now().minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final List<String> drivers = Arrays.asList("Test1", "Test2", "Test3");
        final ValueGroup valueGroup = new ValueGroup(id, "TestValueGroup", drivers, range);

        final ValueGroupChange change = new ValueGroupChange(Type.NEW, valueGroup);

        setBean(change);

        this.injectedValues.put("type", Type.NEW);
        this.injectedValues.put("valueGroup", valueGroup);

        JblTestClassUtils.assertGetterCorrectForConstructorInjection(this.injectedValues, getBean());
    }

    @Test
    public void testEquals() {
        final UUID id = new UUID(0, 1);
        final Instant start = Instant.now().minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final List<String> drivers = Arrays.asList("Test1", "Test2", "Test3");
        final ValueGroup valueGroup = new ValueGroup(id, "TestValueGroup", drivers, range);

        final ValueGroupChange change = new ValueGroupChange(Type.NEW, valueGroup);

        assertTrue(change.equals(change));
        assertFalse(change.equals(null));
        assertFalse(change.equals(Boolean.TRUE));

        ValueGroupChange other = new ValueGroupChange(Type.NEW, valueGroup);
        assertTrue(change.equals(other));

        final ValueGroup otherValueGroup = new ValueGroup(id, "VG:TestOtherValueGroup", drivers, range);
        other = new ValueGroupChange(Type.NEW, otherValueGroup);
        assertFalse(change.equals(other));

        other = new ValueGroupChange(Type.ORIGINAL, otherValueGroup);
        assertFalse(change.equals(other));
    }

    @Test
    public void hashCodeCorrect() {
        final UUID id = new UUID(0, 1);
        final Instant start = Instant.now().minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final List<String> drivers = Arrays.asList("Test1", "Test2", "Test3");
        final ValueGroup valueGroup = new ValueGroup(id, "TestValueGroup", drivers, range);

        final ValueGroupChange change = new ValueGroupChange(Type.NEW, valueGroup);
        final ValueGroupChange other = new ValueGroupChange(Type.NEW, valueGroup);

        assertEquals(change.hashCode(), other.hashCode());
    }

    private ValueGroupChange getBean() {
        return this.bean;
    }

    private void setBean(final ValueGroupChange bean) {
        this.bean = bean;
    }
}
