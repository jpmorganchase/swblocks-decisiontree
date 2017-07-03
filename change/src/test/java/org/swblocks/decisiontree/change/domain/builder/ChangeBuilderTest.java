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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.change.domain.Audit;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ChangeBuilder}.
 */
public class ChangeBuilderTest {
    private static final Instant NOW = Instant.now();

    private Builder<RuleChangeBuilder, List<RuleChange>> ruleBuilder;
    private DecisionTreeRuleSet ruleSet;

    static DecisionTreeRuleSet getRuleSet(final Instant now) {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator("commissions",
                Arrays.asList("VOICE", "CME", "ED", "US", "Rate"));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.1"))
                .with(RuleBuilder::start, now.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, now.plus(Period.ofWeeks(4)).minusMillis(1L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("EMAIL", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.2"))
                .with(RuleBuilder::start, now.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, now.plus(Period.ofWeeks(6)).minusMillis(1L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("ELECTRONIC", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, now.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, now.plus(Period.ofWeeks(8))));

        return ruleSetBuilder.build();
    }

    static Builder<ChangeBuilder, Change> getChangeBuilderWithDefaultChangeRange() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        final Set<ValueGroup> current = ruleSet.getValueGroups();
        assertEquals(1, current.size());

        final ValueGroup group = current.stream().findFirst().get();
        final DateRange changeRange = new DateRange(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX);
        final List<String> drivers = Arrays.asList("CME", "KCBOT");

        // Change the value groups
        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator(group.getName());
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::drivers, drivers);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeRange, changeRange);

        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(ruleSet);
        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        builder.with(ChangeBuilder::changeRange, changeRange);
        builder.with(ChangeBuilder::valueGroupChange, valueGroupChangeBuilder);
        return builder;
    }

    @Before
    public void setup() {
        createRuleBuilder();
        assertNotNull(this.ruleSet);
        assertNotNull(this.ruleBuilder);
    }

    @Test
    public void noRuleSet() {
        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(null);
        assertNull(builder.build());
    }

    @Test
    public void createsChangeWithId() {
        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(this.ruleSet);
        final UUID changeId = UUID.randomUUID();
        builder.with(ChangeBuilder::setId, changeId);

        final Change change = builder.build();
        assertNotNull(change);
        assertEquals(changeId, change.getId());
    }

    @Test
    public void createsChangeWithRules() {
        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(this.ruleSet);

        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        builder.with(ChangeBuilder::changeRange,
                new DateRange(NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4))));
        builder.with(ChangeBuilder::activation, NOW);

        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2))
                .with(RuleChangeBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "Rate"))
                .with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.0")));

        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 5))
                .with(RuleChangeBuilder::input, Arrays.asList("VOICE", "CME", "ED", "UK", "Rate"))
                .with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.1")));

        final Change change = builder.build();
        assertNotNull(change);
        assertNotNull(change.getId());
        assertEquals("commissions", change.getRuleSetName());
        assertEquals(NOW, change.getActivationTime());
        assertEquals(new DateRange(NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4))),
                change.getChangeRange());
        assertEquals(NOW, change.getActivationTime());
        assertTrue((new Audit("USER1", NOW, "USER2", NOW)).equals(change.getAudit()));

        final Set<RuleChange> ruleChanges = change.getRuleChanges();
        assertThat(ruleChanges, hasSize(3));

        final Comparator<RuleChange> typeComparator = (first, second) -> {
            if (first.getType() == Type.ORIGINAL && second.getType() == Type.NEW) {
                return -1;
            } else if (first.getType() == second.getType()) {
                return 0;
            }
            return 1;
        };

        final Comparator<RuleChange> startDateComparator =
                Comparator.comparing(ruleChange -> ruleChange.getRule().getStart());

        final Comparator<RuleChange> codeComparator =
                Comparator.comparing(ruleChange -> ruleChange.getRule().getRuleCode());

        // sort by ruleCode, type and then changeRange - group the rule changes in order
        final List<RuleChange> changes = ruleChanges.stream().sorted(codeComparator.thenComparing(typeComparator)
                .thenComparing(startDateComparator)).collect(Collectors.toList());

        RuleChange tmp = changes.get(0);
        assertEquals(Type.ORIGINAL, tmp.getType());
        Assert.assertEquals(new UUID(0, 2), tmp.getRule().getRuleCode());

        tmp = changes.get(1);
        assertEquals(Type.NEW, tmp.getType());
        Assert.assertEquals(new UUID(0, 2), tmp.getRule().getRuleCode());

        tmp = changes.get(2);
        assertEquals(Type.NEW, tmp.getType());
        Assert.assertEquals(new UUID(0, 5), tmp.getRule().getRuleCode());
    }

    @Test
    public void createsChangeWithValueGroup() {
        final Builder<ChangeBuilder, Change> builder = getChangeBuilderWithDefaultChangeRange();

        final Change change = builder.build();
        assertNotNull(change);
        assertNotNull(change.getId());
        assertEquals("commissions", change.getRuleSetName());
        assertNull(change.getActivationTime());
        assertTrue((new Audit("USER1", NOW, "USER2", NOW)).equals(change.getAudit()));

        final Set<ValueGroupChange> changes = change.getValueGroupChanges();
        assertThat(changes, hasSize(2));

        final List<ValueGroupChange> newChanges =
                changes.stream().filter(valueGroupChange ->
                        valueGroupChange.getType() == Type.NEW &&
                                valueGroupChange.getValueGroup().getName().equals("CMEGroup"))
                        .collect(Collectors.toList());
        assertThat(newChanges, hasSize(1));

        assertThat(change.getRuleChanges(), hasSize(2));
    }

    @Test
    public void createsChangeWithValueGroupAndDefaultChangeRange() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        final Set<ValueGroup> current = ruleSet.getValueGroups();
        assertEquals(1, current.size());

        final ValueGroup group = current.stream().findFirst().get();
        final List<String> drivers = Arrays.asList("CME", "KCBOT");

        // Change the value groups
        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator(group.getName());
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::drivers, drivers);

        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(ruleSet);
        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        builder.with(ChangeBuilder::valueGroupChange, valueGroupChangeBuilder);

        final Change change = builder.build();
        assertNotNull(change);
        assertNotNull(change.getId());
        assertEquals("commissions", change.getRuleSetName());
        assertNull(change.getActivationTime());
        assertTrue((new Audit("USER1", NOW, "USER2", NOW)).equals(change.getAudit()));

        final Set<ValueGroupChange> changes = change.getValueGroupChanges();
        assertThat(changes, hasSize(3));

        final List<ValueGroupChange> newChanges =
                changes.stream().filter(valueGroupChange ->
                        valueGroupChange.getType() == Type.NEW &&
                                valueGroupChange.getValueGroup().getName().equals("CMEGroup"))
                        .collect(Collectors.toList());
        assertThat(newChanges, hasSize(2));

        assertThat(change.getRuleChanges(), hasSize(3));
        final List<RuleChange> ruleCodeChanges = change.getRuleChanges().stream().filter(ruleChange ->
                ruleChange.getRule().getRuleCode().equals(new UUID(0, 0)))
                .collect(Collectors.toList());
        assertThat(ruleCodeChanges, hasSize(3));

        assertThat(ruleCodeChanges.stream().filter(ruleChange ->
                ruleChange.getType() == Type.ORIGINAL).collect(Collectors.toList()), hasSize(1));
        assertThat(ruleCodeChanges.stream().filter(ruleChange ->
                ruleChange.getType() == Type.NEW).collect(Collectors.toList()), hasSize(2));
    }

    @Test
    public void createsChangeForExistingGroupsInRuleSet() {
        this.ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        final Set<ValueGroup> current = this.ruleSet.getValueGroups();
        assertEquals(1, current.size());

        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator("CMEGroup");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeType, Type.NONE);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::driver, "EXCHANGE");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleCodes, Arrays.asList(new UUID(0, 2)));

        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(this.ruleSet);
        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        builder.with(ChangeBuilder::valueGroupChange, valueGroupChangeBuilder);

        final Change change = builder.build();
        assertNotNull(change);
        assertNotNull(change.getId());
        assertEquals("commissions", change.getRuleSetName());
        assertNull(change.getActivationTime());
        assertTrue((new Audit("USER1", NOW, "USER2", NOW)).equals(change.getAudit()));

        assertThat(change.getValueGroupChanges(), hasSize(0));

        final Set<RuleChange> changes = change.getRuleChanges();

        final List<RuleChange> originals = changes.stream().filter(ruleChange ->
                Type.ORIGINAL == ruleChange.getType()).collect(Collectors.toList());
        assertThat(originals, hasSize(1));

        final DecisionTreeRule original = originals.get(0).getRule();
        assertEquals(new UUID(0, 2), original.getRuleIdentifier());

        final List<RuleChange> newRules = changes.stream().filter(ruleChange ->
                Type.NEW == ruleChange.getType()).collect(Collectors.toList());
        assertThat(originals, hasSize(1));

        final DecisionTreeRule newRule = newRules.get(0).getRule();

        final List<String> drivers = Arrays.stream(newRule.getDrivers()).map(InputDriver::toString)
                .collect(Collectors.toList());
        assertThat(drivers, hasItems("VOICE", GroupDriver.VG_PREFIX + new UUID(0, 1), "ED", "*", "RATE"));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreateValueGroupChangeAndRuleChangeForSameRule() {
        this.ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        final Set<ValueGroup> current = this.ruleSet.getValueGroups();

        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(this.ruleSet);
        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));

        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator("CMEGroup");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeType, Type.NONE);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::driver, "EXCHANGE");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleCodes, Arrays.asList(new UUID(0, 2)));

        builder.with(ChangeBuilder::valueGroupChange, valueGroupChangeBuilder);

        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2))
                .with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.0")));

        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreateMultipleRulesChangeForSameRule() {
        this.ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();

        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(this.ruleSet);
        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));

        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2))
                .with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "1.2")));
        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2))
                .with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.1")));

        builder.build();
    }

    private void createRuleBuilder() {
        this.ruleSet = getRuleSet(NOW);
        this.ruleBuilder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2));
    }
}