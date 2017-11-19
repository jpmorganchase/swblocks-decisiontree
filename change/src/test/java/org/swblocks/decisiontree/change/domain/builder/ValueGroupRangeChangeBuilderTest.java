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

package org.swblocks.decisiontree.change.domain.builder;

import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Test class for {@link ValueGroupChangeBuilder} where a value group segment can be de-activated or have the date range
 * modified.
 */
public class ValueGroupRangeChangeBuilderTest {
    private static final Instant NOW = Instant.now();

    private Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> builder;

    @Before
    public void setup() {
        final UUID id = new UUID(0, 1);
        final Instant start = NOW.plus(Period.ofWeeks(1));
        final Instant end = NOW.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final List<String> drivers = Arrays.asList("Test1", "Test2", "Test3");
        final Set<ValueGroup> valueGroups = Collections.singleton(
                new ValueGroup(id, "TestValueGroup", drivers, range));

        final DecisionTreeRuleSet ruleSet = new DecisionTreeRuleSet("TestRuleSet", Collections.emptyMap(), Collections.emptyList(),
                new DriverCache(), valueGroups);

        builder = ValueGroupChangeBuilder.creator("TestValueGroup");
        builder.with(ValueGroupChangeBuilder::ruleSet, ruleSet);
        builder.with(ValueGroupChangeBuilder::id, new UUID(0, 1));
    }

    @Test(expected = IllegalStateException.class)
    public void noGroupsToAmend() {
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void noDateRangeSupplied() {
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void unknownId() {
        builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(9))));
        builder.with(ValueGroupChangeBuilder::id, new UUID(0, 5));
        builder.build();
    }

    @Test
    public void deactivatesValueGroup() {
        builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(null, null));

        final List<ValueGroupChange> changes = builder.build();
        assertThat(changes, hasSize(1));

        assertValueGroupChange(changes.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));
    }

    @Test
    public void amendsStartDateOfValueGroup() {
        builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, null));

        final List<ValueGroupChange> changes = builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));
    }

    @Test
    public void amendsEndDateOfValueGroup() {
        builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(null, NOW.plus(Period.ofWeeks(4))));

        final List<ValueGroupChange> changes = builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(4)), Arrays.asList("Test1", "Test2", "Test3"));
    }

    @Test
    public void amendsStartAndEndDateOfValueGroup() {
        builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW.plus(Period.ofWeeks(2)),
                NOW.plus(Period.ofWeeks(4))));

        final List<ValueGroupChange> changes = builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)), Arrays.asList("Test1", "Test2", "Test3"));
    }

    @Test
    public void amendsStartAndEndDateOfValueGroupAndDrivers() {
        builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW.plus(Period.ofWeeks(2)),
                NOW.plus(Period.ofWeeks(4))));
        builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));

        final List<ValueGroupChange> changes = builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test(expected = IllegalStateException.class)
    public void triesToAmendStartAndEndDateOfValueGroupDatesNotInChronologicalOrder() {
        builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW.plus(Period.ofWeeks(4)),
                NOW.plus(Period.ofWeeks(2))));

        builder.build();
    }

    private List<ValueGroupChange> getChangesByType(final List<ValueGroupChange> changes, final Type type) {
        return changes.stream().filter(change -> change.getType() == type)
                .sorted(Comparator.comparing(change2 -> change2.getValueGroup().getRange().getStart()))
                .collect(Collectors.toList());
    }

    private void assertValueGroupChange(final ValueGroupChange change,
                                        final Type type,
                                        final UUID id,
                                        final String name,
                                        final Instant start,
                                        final Instant end,
                                        final List<String> drivers) {
        assertEquals(type, change.getType());

        final ValueGroup group = change.getValueGroup();

        if (id == null) {
            assertNotNull(group.getId());
        } else {
            assertEquals(id, group.getId());
        }

        assertEquals(name, group.getName());
        assertEquals(start, group.getRange().getStart());
        assertEquals(end, group.getRange().getFinish());
        assertEquals(drivers, group.getValues());
    }
}
