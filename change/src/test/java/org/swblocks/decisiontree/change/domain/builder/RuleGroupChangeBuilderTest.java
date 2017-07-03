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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.util.DateRange;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.swblocks.decisiontree.change.domain.builder.RuleChangeBuilderTest.assertRuleChange;

/**
 * Test class for {@link RuleGroupChangeBuilder}.
 */
public class RuleGroupChangeBuilderTest {
    private static final UUID GROUP_ID = new UUID(0, 10);
    private static final UUID RULE_CODE = new UUID(0, 2);
    private static final Instant NOW = Instant.now();
    private static final ValueGroup DEFAULT_VALUE_GROUP = new ValueGroup(GROUP_ID, "CMEGroup",
            Arrays.asList("CME", "CBOT"), ValueGroup.DEFAULT_DATE_RANGE);

    private Builder<RuleGroupChangeBuilder, List<RuleChange>> builder;
    private DecisionTreeRuleSet ruleSet;

    @Before
    public void setup() {
        createRuleMapWithSingleRuleAndValueGroup(singleton(DEFAULT_VALUE_GROUP));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWithNoValueGroupChanges() {
        this.builder = RuleGroupChangeBuilder.creator(emptyList(), this.ruleSet);
        this.builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWithNoRuleset() {
        final List<ValueGroupChange> changes = getValueGroupChanges(new DateRange(NOW, NOW.plus(Period.ofWeeks(1))),
                Arrays.asList("CME", "CBOT", "CBOT"), true);
        assertEquals(4, changes.size());

        this.builder = RuleGroupChangeBuilder.creator(changes, null);
        this.builder.build();
    }

    @Test
    public void changeBeforeRuleSegmentsStart() {
        final List<ValueGroupChange> changes = getValueGroupChanges(new DateRange(NOW, NOW.plus(Period.ofWeeks(1))),
                Arrays.asList("CME", "CBOT", "CBOT"), false);
        assertEquals(4, changes.size());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertEquals(6, ruleChanges.size());

        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(3, originals.size());

        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(3, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + getUuid(changes, 3), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + getUuid(changes, 3), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + getUuid(changes, 3), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));
    }

    private void assertOriginalSegmentsRemoved(final List<RuleChange> originals) {
        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void changeAfterRuleSegmentsFinish() {
        final List<ValueGroupChange> changes = getValueGroupChanges(new DateRange(NOW.plus(Period.ofWeeks(10)),
                NOW.plus(Period.ofWeeks(12))), Arrays.asList("CME", "CBOT", "CBOT"), false);
        assertEquals(4, changes.size());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertEquals(6, ruleChanges.size());

        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(3, originals.size());

        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(3, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + getUuid(changes, 1), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + getUuid(changes, 1), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + getUuid(changes, 1), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void valueGroupAmendedForFirstSegment() {
        final List<ValueGroupChange> changes =
                getValueGroupChanges(new DateRange(NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4))),
                        Arrays.asList("CME", "CBOT", "CBOT"), false);
        assertEquals(4, changes.size());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertEquals(6, ruleChanges.size());

        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(3, originals.size());

        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(3, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + getUuid(changes, 2), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "), NOW.plus(Period.ofWeeks(2)),
                NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + getUuid(changes, 3), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"), NOW.plus(Period.ofWeeks(4)),
                NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + getUuid(changes, 3), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"), NOW.plus(Period.ofWeeks(6)),
                NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void valueGroupAmendedFromBeforeSecondStartToAfterSecondSegmentFinish() {
        final List<ValueGroupChange> changes =
                getValueGroupChanges(new DateRange(NOW.plus(Period.ofWeeks(3)), NOW.plus(Period.ofWeeks(7))),
                        Arrays.asList("CME", "CBOT", "CBOT"), false);
        assertEquals(4, changes.size());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertEquals(8, ruleChanges.size());

        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(3, originals.size());
        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(5, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + getUuid(changes, 1), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "), NOW.plus(Period.ofWeeks(2)),
                NOW.plus(Period.ofWeeks(3)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + getUuid(changes, 2), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "), NOW.plus(Period.ofWeeks(3)),
                NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + getUuid(changes, 2), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"), NOW.plus(Period.ofWeeks(4)),
                NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(3), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + getUuid(changes, 2), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"), NOW.plus(Period.ofWeeks(6)),
                NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(4), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + getUuid(changes, 3), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"), NOW.plus(Period.ofWeeks(7)),
                NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void noChangesWhenRulesNotFoundForRuleCode() {
        createRuleMapWithSingleRuleAndNoValueGroupsInRule(null);

        final List<ValueGroupChange> changes = ValueGroupChangeBuilder.creator("CMEGroup")
                .with(ValueGroupChangeBuilder::ruleSet, this.ruleSet)
                .with(ValueGroupChangeBuilder::changeRange, ValueGroup.DEFAULT_DATE_RANGE)
                .with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT", "NYMEX")).build();

        assertEquals(1, changes.size());
        assertEquals(Type.NEW, changes.get(0).getType());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        // ChangeBuilder will set these from ValueGroupChangeBuilder
        this.builder.with(RuleGroupChangeBuilder::ruleCodes, singleton(UUID.randomUUID()));
        this.builder.with(RuleGroupChangeBuilder::driver, "EXCHANGE");

        final List<RuleChange> ruleChanges = this.builder.build();
        assertTrue(ruleChanges.isEmpty());
    }

    @Test
    public void noChangesWhenDriverNameNotFound() {
        createRuleMapWithSingleRuleAndNoValueGroupsInRule(null);

        final List<ValueGroupChange> changes = ValueGroupChangeBuilder.creator("CMEGroup")
                .with(ValueGroupChangeBuilder::ruleSet, this.ruleSet)
                .with(ValueGroupChangeBuilder::changeRange, ValueGroup.DEFAULT_DATE_RANGE)
                .with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT", "NYMEX")).build();

        assertEquals(1, changes.size());
        assertEquals(Type.NEW, changes.get(0).getType());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        // ChangeBuilder will set these from ValueGroupChangeBuilder
        this.builder.with(RuleGroupChangeBuilder::ruleCodes, singleton(RULE_CODE));
        this.builder.with(RuleGroupChangeBuilder::driver, "DRIVER_NOT_FOUND");
        final List<RuleChange> ruleChanges = this.builder.build();
        assertTrue(ruleChanges.isEmpty());
    }

    @Test
    public void addsNewValueGroupToAllMatchingRules() {
        createRuleMapWithSingleRuleAndNoValueGroupsInRule(null);

        final List<ValueGroupChange> changes = ValueGroupChangeBuilder.creator("CMEGroup")
                .with(ValueGroupChangeBuilder::ruleSet, this.ruleSet)
                .with(ValueGroupChangeBuilder::changeRange, ValueGroup.DEFAULT_DATE_RANGE)
                .with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT", "NYMEX"))
                .with(ValueGroupChangeBuilder::ruleCodes, singletonList(RULE_CODE))
                .with(ValueGroupChangeBuilder::driver, "EXCHANGE").build();

        assertEquals(1, changes.size());
        assertEquals(Type.NEW, changes.get(0).getType());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        // ChangeBuilder will set these from ValueGroupChangeBuilder
        this.builder.with(RuleGroupChangeBuilder::ruleCodes, singleton(RULE_CODE));
        this.builder.with(RuleGroupChangeBuilder::driver, "EXCHANGE");

        final List<RuleChange> ruleChanges = this.builder.build();
        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(3, originals.size());

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(3, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + getUuid(changes, 0), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "), NOW.plus(Period.ofWeeks(2)),
                NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + getUuid(changes, 0), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"), NOW.plus(Period.ofWeeks(4)),
                NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + getUuid(changes, 0), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"), NOW.plus(Period.ofWeeks(6)),
                NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void addsNewValueGroupToAllMatchingRulesMidSecondSegment() {
        createRuleMapWithSingleRuleAndNoValueGroupsInRule(null);

        final List<ValueGroupChange> changes = ValueGroupChangeBuilder.creator("CMEGroup")
                .with(ValueGroupChangeBuilder::ruleSet, this.ruleSet)
                .with(ValueGroupChangeBuilder::changeRange,
                        new DateRange(NOW.plus(Period.ofWeeks(5)), DecisionTreeRule.MAX))
                .with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT", "NYMEX"))
                .with(ValueGroupChangeBuilder::ruleCodes, singletonList(RULE_CODE))
                .with(ValueGroupChangeBuilder::driver, "EXCHANGE").build();

        assertEquals(1, changes.size());
        assertEquals(Type.NEW, changes.get(0).getType());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        this.builder.with(RuleGroupChangeBuilder::ruleCodes, singleton(RULE_CODE));
        this.builder.with(RuleGroupChangeBuilder::driver, "EXCHANGE");

        final List<RuleChange> ruleChanges = this.builder.build();
        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(2, originals.size());

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(3, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + getUuid(changes, 0), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"), NOW.plus(Period.ofWeeks(5)),
                NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + getUuid(changes, 0), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"), NOW.plus(Period.ofWeeks(6)),
                NOW.plus(Period.ofWeeks(8)));
    }

    /**
     * This test starts with three rules which are identical apart from the exchange.  A value group is created
     * which links all the exchanges together.  The {@link RuleGroupChangeBuilder} identify the duplicate rules
     * and merges them into one rule with the new value group.
     */
    @Test
    public void addsNewValueGroupToAllMatchingRulesAndMergesRules() {
        createRuleMapWithSingleRuleAndNoValueGroupsForMerge();

        final List<ValueGroupChange> changes = ValueGroupChangeBuilder.creator("CMEGroup")
                .with(ValueGroupChangeBuilder::ruleSet, this.ruleSet)
                .with(ValueGroupChangeBuilder::changeRange, ValueGroup.DEFAULT_DATE_RANGE)
                .with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT", "NYMEX"))
                .with(ValueGroupChangeBuilder::ruleCodes, singletonList(RULE_CODE))
                .with(ValueGroupChangeBuilder::driver, "EXCHANGE").build();

        assertEquals(1, changes.size());
        assertEquals(Type.NEW, changes.get(0).getType());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        // performed in ChangeBuilder
        updateBuilderFromFromValueGroup(changes.get(0).getValueGroup());

        final List<RuleChange> ruleChanges = this.builder.build();
        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(3, originals.size());

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"VOICE", "CBOT", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"VOICE", "CBOT", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(1, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + getUuid(changes, 0), "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "), NOW.plus(Period.ofWeeks(2)),
                NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void confirmsRemovalOfValueGroup() {
        final Set<ValueGroup> groups = new HashSet<>();
        groups.add(DEFAULT_VALUE_GROUP);
        groups.add(new ValueGroup(new UUID(10, 10), "CMEGroup", Arrays.asList("CME", "CBOT", "NYMEX"),
                ValueGroup.DEFAULT_DATE_RANGE));
        createRuleMapWithSingleRuleAndValueGroup(groups);

        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator("CMEGroup");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::id, new UUID(10, 10));
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeRange, new DateRange(null, null));

        final List<ValueGroupChange> changes = valueGroupChangeBuilder.build();
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0).getType(), is(Type.ORIGINAL));
        assertEquals(new UUID(10, 10), changes.get(0).getValueGroup().getId());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        final List<RuleChange> ruleChanges = this.builder.build();

        assertTrue(ruleChanges.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void cannotRemoveActiveValueGroup() {
        final Set<ValueGroup> groups = new HashSet<>();
        groups.add(DEFAULT_VALUE_GROUP);
        groups.add(new ValueGroup(new UUID(10, 10), "CME", Arrays.asList("CME", "CBOT", "NYMEX"),
                ValueGroup.DEFAULT_DATE_RANGE));
        createRuleMapWithSingleRuleAndValueGroup(groups);

        final List<ValueGroupChange> changes = singletonList(new ValueGroupChange(Type.ORIGINAL,
                new ValueGroup(GROUP_ID, "CME", Arrays.asList("CME", "CBOT"), ValueGroup.DEFAULT_DATE_RANGE)));

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        this.builder.build();
    }

    /**
     * Test to apply a new value group to every segment of a list of rules, identified by rule code.
     * In this test, only one rule is impacted.
     */
    @Test
    public void addsExistingValueGroupToAllMatchingRules() {
        createRuleMapWithSingleRuleAndNoValueGroupsInRule(Collections.singleton(DEFAULT_VALUE_GROUP));

        final List<ValueGroupChange> changes = ValueGroupChangeBuilder.creator("CMEGroup")
                .with(ValueGroupChangeBuilder::ruleSet, this.ruleSet)
                .with(ValueGroupChangeBuilder::changeType, Type.NONE)
                .with(ValueGroupChangeBuilder::driver, "EXCHANGE")
                .with(ValueGroupChangeBuilder::ruleCodes, Arrays.asList(RULE_CODE)).build();
        assertThat(changes.size(), is(1));
        assertEquals(Type.NONE, changes.get(0).getType());

        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        /**
         * Next 2 lines are performed internally in ChangeBuilder, duplicating these here in the test
         * to ensure the low level construction works.
         */
        this.builder.with(RuleGroupChangeBuilder::ruleCodes, singleton(RULE_CODE));
        this.builder.with(RuleGroupChangeBuilder::driver, "EXCHANGE");

        final List<RuleChange> ruleChanges = this.builder.build();
        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(3, originals.size());

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate1", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.NEW);
        assertEquals(3, newChanges.size());

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.1 "),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.2"), NOW.plus(Period.ofWeeks(4)),
                NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate1", "1.3"), NOW.plus(Period.ofWeeks(6)),
                NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void willNotAddExistingValueGroupToAllMatchingRulesWithOutRuleInformation() {
        createRuleMapWithSingleRuleAndValueGroup(singleton(DEFAULT_VALUE_GROUP));

        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator("CMEGroup");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeType, Type.NONE);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::driver, "EXCHANGE");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleCodes, Arrays.asList(RULE_CODE));

        final List<ValueGroupChange> changes = valueGroupChangeBuilder.build();
        assertEquals(1, changes.size());
        assertEquals(Type.NONE, changes.get(0).getType());

        // do not set rule codes or driver name
        this.builder = RuleGroupChangeBuilder.creator(changes, this.ruleSet);
        this.builder.with(RuleGroupChangeBuilder::ruleCodes, Collections.emptySet());

        final List<RuleChange> ruleChanges = this.builder.build();
        final List<RuleChange> originals = RuleChangeBuilderTest.getChangesByType(ruleChanges, Type.ORIGINAL);
        assertEquals(0, originals.size());
    }

    private List<ValueGroupChange> getValueGroupChanges(final DateRange range,
                                                        final List<String> drivers,
                                                        final boolean addNewRuleDetails) {
        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator("CMEGroup");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeRange, range);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::drivers, drivers);

        if (addNewRuleDetails) {
            valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleCodes, singletonList(RULE_CODE));
            valueGroupChangeBuilder.with(ValueGroupChangeBuilder::driver, "EXCHANGE");
        }

        return valueGroupChangeBuilder.build();
    }

    private void updateBuilderFromFromValueGroup(final ValueGroup group) {
        this.builder.with(RuleGroupChangeBuilder::driver, group.getDriverName());
        this.builder.with(RuleGroupChangeBuilder::ruleCodes, new HashSet<>(group.getRuleCodes()));
    }

    private void createRuleMapWithSingleRuleAndValueGroup(final Set<ValueGroup> groups) {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator("commissions",
                Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        ruleSetBuilder.with(RuleSetBuilder::groups, groups);

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("VOICE", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.1 "))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("EMAIL", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.2"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(6))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input,
                        Arrays.asList("ELECTRONIC", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 4))
                .with(RuleBuilder::setCode, new UUID(2, 2))
                .with(RuleBuilder::input, Arrays.asList("EMAIL", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        this.ruleSet = ruleSetBuilder.build();
    }

    private void createRuleMapWithSingleRuleAndNoValueGroupsInRule(final Set<ValueGroup> groups) {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator("commissions",
                Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        if (CollectionUtils.isNotEmpty(groups)) {
            ruleSetBuilder.with(RuleSetBuilder::groups, groups);
        }

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.1 "))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("EMAIL", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.2"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(6))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("ELECTRONIC", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        this.ruleSet = ruleSetBuilder.build();
    }

    private void createRuleMapWithSingleRuleAndNoValueGroupsForMerge() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator("commissions",
                Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.1 "))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CBOT", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.1 "))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(6))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, RULE_CODE)
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CBOT", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate1", "1.1 "))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        this.ruleSet = ruleSetBuilder.build();
    }

    private UUID getUuid(final List<ValueGroupChange> changes, final int index) {
        return changes.get(index).getValueGroup().getId();
    }
}
