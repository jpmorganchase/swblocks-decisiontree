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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ValueGroupChangeBuilder}.
 */
public class ValueGroupChangeBuilderTest {
    private static final Instant NOW = Instant.now();

    private Set<ValueGroup> valueGroups;
    private Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> builder;
    private DecisionTreeRuleSet ruleSet;

    @Before
    public void setup() {
        final UUID id = new UUID(0, 1);
        final Instant start = NOW.plus(Period.ofWeeks(1));
        final Instant end = NOW.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final List<String> drivers = Arrays.asList("Test1", "Test2", "Test3");
        this.valueGroups = Collections.singleton(new ValueGroup(id, "TestValueGroup", drivers, range));
        this.ruleSet = new DecisionTreeRuleSet("TestRuleSet", Collections.emptyMap(), Collections.emptyList(),
                new DriverCache(), this.valueGroups);
        this.builder = ValueGroupChangeBuilder.creator("TestValueGroup");
        this.builder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullNameProvided() {
        this.builder = ValueGroupChangeBuilder.creator(null);
        this.builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankNameProvided() {
        this.builder = ValueGroupChangeBuilder.creator(" ");
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void noRuleSet() {
        this.builder = ValueGroupChangeBuilder.creator("TestValueGroup");
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void noRequiredArgumentsProvided() {
        this.builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyDrivers() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Collections.emptyList());
        this.builder.build();
    }

    @Test
    public void createsNewChangeWithNoRuleData() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(1))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(1));

        assertValueGroupChange(changes.get(0), Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(1)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createsNewChangeFailsWithNoRuleCodes() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(1))));
        this.builder.with(ValueGroupChangeBuilder::driver, "TestDriver");

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(1));

        assertValueGroupChange(changes.get(0), Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(1)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test
    public void createsBrandNewValueGroup() {
        // builder that only has value group with name "TestValueGroup"
        this.builder = ValueGroupChangeBuilder.creator("BrandNewTestValueGroup");
        this.builder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(1))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(1));

        assertValueGroupChange(changes.get(0), Type.NEW, null, "BrandNewTestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(1)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test
    public void createsNewChange() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(1))));
        this.builder.with(ValueGroupChangeBuilder::driver, "TestDriver");

        final UUID id = UUID.randomUUID();
        this.builder.with(ValueGroupChangeBuilder::ruleCodes, Collections.singletonList(id));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(1));

        final ValueGroupChange change = changes.get(0);

        assertValueGroupChange(change, Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(1)), Arrays.asList("Test1", "Test2", "Test4"));
        assertEquals("TestDriver", change.getValueGroup().getDriverName());
        assertThat(change.getValueGroup().getRuleCodes(), contains(id));
    }

    @Test
    public void createsNewChangeWithOutOfScopeValueGroups() {
        final UUID id = UUID.randomUUID();
        final ValueGroup trueValueGroup = new ValueGroup(id, "TestValueGroup", Arrays.asList("Test1", "Test2", "Test3"),
                ValueGroup.DEFAULT_DATE_RANGE);
        final ValueGroup rogueValueGroup = new ValueGroup("AGROUP", Arrays.asList("Test4"));

        this.ruleSet = new DecisionTreeRuleSet("TestRuleSet", Collections.emptyMap(), Collections.emptyList(),
                new DriverCache(), new HashSet<>(Arrays.asList(trueValueGroup, rogueValueGroup)));

        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator("TestValueGroup")
                        .with(ValueGroupChangeBuilder::ruleSet, this.ruleSet)
                        .with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"))
                        .with(ValueGroupChangeBuilder::changeRange, ValueGroup.DEFAULT_DATE_RANGE)
                        .with(ValueGroupChangeBuilder::driver, "TestDriver")
                        .with(ValueGroupChangeBuilder::ruleCodes, Collections.singletonList(id));

        List<ValueGroupChange> changes = valueGroupChangeBuilder.build();
        assertThat(changes, hasSize(2));

        changes = getChangesByType(changes, Type.NEW);
        assertThat(changes, hasSize(1));

        final ValueGroupChange change = changes.get(0);
        assertValueGroupChange(change, Type.NEW, null, "TestValueGroup",
                DecisionTreeRule.EPOCH, DecisionTreeRule.MAX, Arrays.asList("Test1", "Test2", "Test4"));

        assertEquals("TestDriver", change.getValueGroup().getDriverName());
        assertThat(change.getValueGroup().getRuleCodes(), contains(id));
    }

    @Test
    public void createsNewChangeFinishingBeforeFirstSegmentStarts() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(1))));
        this.builder.with(ValueGroupChangeBuilder::driver, "TestDriver");
        this.builder.with(ValueGroupChangeBuilder::ruleCodes, Collections.singletonList(UUID.randomUUID()));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(1));

        assertValueGroupChange(changes.get(0), Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(1)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test
    public void createsNewChangeStartingBeforeAndFinishingInFirstSegment() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(2))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(3));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(2)), Arrays.asList("Test1", "Test2", "Test4"));

        assertValueGroupChange(newChanges.get(1), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));
    }

    @Test
    public void createsNewChangeStartingBeforeAndFinishingAtFirstSegmentFinish() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(5))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test
    public void createsNewChangeStartingBeforeFinishingAfterFirstSegmentFinishes() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(NOW, NOW.plus(Period.ofWeeks(9))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW, NOW.plus(Period.ofWeeks(9)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test
    public void createsNewChangeReplacingExistingSegment() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test
    public void createsNewChangeStartingAtExistingSegmentStartAndFinishingAfterSegmentFinish() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));

        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(9))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(9)), Arrays.asList("Test1", "Test2", "Test4"));
    }

    @Test
    public void createsNewChangeStartingAtExistingSegmentStartAndFinishingFinishingBeforeSegmentFinish() {
        this.builder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("Test1", "Test2", "Test4"));
        this.builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(4))));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(3));

        final List<ValueGroupChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertValueGroupChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));

        final List<ValueGroupChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertValueGroupChange(newChanges.get(0), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(4)), Arrays.asList("Test1", "Test2", "Test4"));

        assertValueGroupChange(newChanges.get(1), Type.NEW, null, "TestValueGroup",
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)), Arrays.asList("Test1", "Test2", "Test3"));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreatesValueGroupChangesForExistingGroupsWithNoDriverName() {
        this.builder.with(ValueGroupChangeBuilder::changeType, Type.NONE);
        this.builder.with(ValueGroupChangeBuilder::ruleCodes, Arrays.asList(UUID.randomUUID()));

        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreatesValueGroupChangesForExistingGroupsWithNoRuleCodes() {
        this.builder.with(ValueGroupChangeBuilder::changeType, Type.NONE);
        this.builder.with(ValueGroupChangeBuilder::driver, "TestDriver");

        this.builder.build();
    }

    @Test
    public void createsValueGroupChangesForExistingValueGroups() {
        final UUID code = UUID.randomUUID();
        this.builder.with(ValueGroupChangeBuilder::changeType, Type.NONE);
        this.builder.with(ValueGroupChangeBuilder::driver, "TestDriver");
        this.builder.with(ValueGroupChangeBuilder::ruleCodes, Arrays.asList(code));

        final List<ValueGroupChange> changes = this.builder.build();
        assertThat(changes, hasSize(1));

        final ValueGroupChange groupChange = changes.get(0);
        assertEquals(Type.NONE, groupChange.getType());

        final ValueGroup created = groupChange.getValueGroup();
        final ValueGroup original = this.valueGroups.stream().findFirst().get();

        assertEquals(original.getId(), created.getId());
        assertEquals(original.getName(), created.getName());
        assertEquals(original.getRange(), created.getRange());
        assertThat(created.getValues(), hasItems("Test1", "Test2", "Test3"));
        assertNull(original.getDriverName());
        assertEquals("TestDriver", created.getDriverName());
        assertTrue(original.getRuleCodes().isEmpty());
        assertThat(created.getRuleCodes(), hasItem(code));
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