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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.jbl.test.utils.JblTestClassUtils;
import org.swblocks.jbl.util.DateRange;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ValueGroup}.
 */
public class ValueGroupTest {
    private static final Instant NOW = Instant.now();

    private final Map<String, Object> injectedValues = new HashMap<>();
    private ValueGroup bean;

    @Before
    public void setup() {
        this.injectedValues.clear();
    }

    @Test
    public void testConstructionFullConstructor() {
        final DateRange range = new DateRange(Instant.now(), Instant.MAX);

        final UUID id = new UUID(0, 50);
        this.bean = new ValueGroup(id, "test-group", Arrays.asList("input1", "input2"), range);

        this.injectedValues.put("id", id);
        this.injectedValues.put("name", "test-group");
        this.injectedValues.put("values", Arrays.asList("input1", "input2"));
        this.injectedValues.put("range", range);

        JblTestClassUtils.assertGetterCorrectForConstructorInjection(this.injectedValues, this.bean);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullId() {
        this.bean = new ValueGroup(null, "test-group", Arrays.asList("input1"), ValueGroup.DEFAULT_DATE_RANGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullName() {
        this.bean = new ValueGroup(UUID.randomUUID(), null, Arrays.asList("input1"), ValueGroup.DEFAULT_DATE_RANGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullValues() {
        this.bean = new ValueGroup(UUID.randomUUID(), "test-group", null, ValueGroup.DEFAULT_DATE_RANGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullRange() {
        this.bean = new ValueGroup(UUID.randomUUID(), "test-group", Arrays.asList("input1"), null);
    }

    @Test
    public void testShortConstructor() {
        this.bean = new ValueGroup("test-group", Arrays.asList("input1"));
        assertNotNull(this.bean.getId());
        assertEquals("test-group", this.bean.getName());
        assertEquals(Arrays.asList("input1"), this.bean.getValues());
        assertEquals(ValueGroup.DEFAULT_DATE_RANGE, this.bean.getRange());
    }

    @Test
    public void updatesValues() {
        final DateRange range = new DateRange(Instant.now(), Instant.MAX);
        final ValueGroup group =
                new ValueGroup(UUID.randomUUID(), "test-group", Arrays.asList("input1", "input2"), range);
        assertThat(group.getValues(), contains("input1", "input2"));

        final List<String> values = Collections.singletonList("input3");
        group.updateValues(values);
        assertThat(group.getValues(), contains("input3"));

        group.updateValues(new ArrayList<>(1));
        assertThat(group.getValues(), contains("input3"));

    }

    @Test
    public void equalsCorrect() {
        final UUID uuid = new UUID(0, 1);
        final DateRange range = new DateRange(NOW, Instant.MAX);

        final ValueGroup group = new ValueGroup(uuid, "test-group", Arrays.asList("input1", "input2"), range);
        assertTrue(group.equals(group));

        assertFalse(group.equals(null));
        assertFalse(group.equals(Integer.parseInt("1")));

        ValueGroup other = new ValueGroup(new UUID(0, 1), "test-group",
                Arrays.asList("input1", "input2"), new DateRange(NOW, Instant.MAX));
        assertTrue(group.equals(other));

        other = new ValueGroup(new UUID(0, 2), "test-group",
                Arrays.asList("input1", "input2"), new DateRange(NOW, Instant.MAX));
        assertFalse(group.equals(other));

        other = new ValueGroup(new UUID(0, 1), "test-group1",
                Arrays.asList("input1", "input2"), new DateRange(NOW, Instant.MAX));
        assertFalse(group.equals(other));

        other = new ValueGroup(new UUID(0, 1), "test-group",
                Arrays.asList("input1"), new DateRange(NOW, Instant.MAX));
        assertFalse(group.equals(other));

        other = new ValueGroup(new UUID(0, 1), "test-group",
                Arrays.asList("input1", "input2"), ValueGroup.DEFAULT_DATE_RANGE);
        assertFalse(group.equals(other));
    }

    @Test
    public void hashCodeCorrect() {
        final UUID uuid = new UUID(0, 1);
        final DateRange range = new DateRange(NOW, Instant.MAX);

        final ValueGroup group = new ValueGroup(uuid, "test-group", Arrays.asList("input1", "input2"), range);

        final ValueGroup other = new ValueGroup(new UUID(0, 1), "test-group",
                Arrays.asList("input1", "input2"), new DateRange(NOW, Instant.MAX));

        assertTrue(group.hashCode() == other.hashCode());
    }

    @Test
    public void toStringCorrect() {
        final UUID uuid = new UUID(0, 1);
        final DateRange range = new DateRange(NOW, Instant.MAX);
        final ValueGroup group = new ValueGroup(uuid, "test-group", Arrays.asList("input1", "input2"), range);

        Assert.assertEquals(GroupDriver.VG_PREFIX + new UUID(0, 1), group.toString());
    }
}
