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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link RollbackChangeBuilder}.
 */
public class RollbackChangeBuilderTest {
    private static final Instant NOW = Instant.now();

    private Change toRevert;

    @Before
    public void setup() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();

        final Builder<ChangeBuilder, Change> builder = ChangeBuilderTest.getChangeBuilderWithDefaultChangeRange();
        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(new UUID(0, 2))
                .with(RuleChangeBuilder::input, Arrays.asList("VOICE", "NYMEX", "ED", "US", "RATE"))
                .with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.1")));

        this.toRevert = builder.build();
        assertEquals(4, this.toRevert.getRuleChanges().size());
        assertEquals(2, this.toRevert.getValueGroupChanges().size());
    }

    @Test
    public void noChangeInformation() {
        final Builder<RollbackChangeBuilder, Change> builder = RollbackChangeBuilder.creator();
        assertNull(builder.build());
    }

    @Test
    public void createsChangeWithDate() {
        assertRollbackChange(NOW);
    }

    @Test
    public void createsChangeWithDefaultDate() {
        assertRollbackChange(null);
    }

    private void assertRollbackChange(final Instant activationTime) {
        final Builder<RollbackChangeBuilder, Change> builder = RollbackChangeBuilder.creator();
        builder.with(RollbackChangeBuilder::change, this.toRevert);
        if (activationTime != null) {
            builder.with(RollbackChangeBuilder::date, NOW);
        }
        builder.with(RollbackChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));

        final Change change = builder.build();
        assertNotNull(change.getId());
        assertEquals("commissions", change.getRuleSetName());

        assertTrue(activationTime != null ? NOW.equals(change.getActivationTime()) :
                null == change.getActivationTime());
        assertTrue((new Audit("USER1", NOW, "USER2", NOW)).equals(change.getAudit()));

        final Set<RuleChange> ruleChanges = change.getRuleChanges();
        List<RuleChange> reverted = ruleChanges.stream().filter(ruleChange ->
                ruleChange.getType() == Type.NEW).collect(Collectors.toList());
        assertThat(reverted, hasSize(2));

        // rule with rule code 2
        DecisionTreeRule rule = reverted.stream().filter(ruleChange ->
                ruleChange.getRule().getRuleCode().equals(new UUID(0, 2)))
                .collect(Collectors.toList()).get(0).getRule();
        assertEquals(Collections.singletonMap("Rate", "1.4"), rule.getOutputs());
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
        assertEquals(new UUID(0, 2), rule.getRuleCode());
        assertNotNull(rule.getRuleIdentifier());

        final List<String> ruleDrivers = new ArrayList<>(1);
        Arrays.stream(rule.getDrivers()).forEach(inputDriver -> ruleDrivers.add(inputDriver.toString()));
        assertThat(ruleDrivers, contains("VOICE", "CME", "ED", "*", "RATE"));

        // rule with rule code 0
        rule = reverted.stream().filter(ruleChange -> ruleChange.getRule().getRuleCode().equals(new UUID(0, 0)))
                .collect(Collectors.toList()).get(0).getRule();
        assertEquals(Collections.singletonMap("Rate", "1.1"), rule.getOutputs());
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
        assertEquals(new UUID(0, 0), rule.getRuleCode());
        assertNotNull(rule.getRuleIdentifier());

        ruleDrivers.clear();
        Arrays.stream(rule.getDrivers()).forEach(inputDriver -> ruleDrivers.add(inputDriver.toString()));
        assertThat(ruleDrivers, contains("*", "VG:" + new UUID(0, 1), "*", "*", "INDEX"));

        reverted = ruleChanges.stream().filter(ruleChange ->
                ruleChange.getType() == Type.ORIGINAL).collect(Collectors.toList());
        assertThat(reverted, hasSize(2));

        // rule with rule code 0
        rule = reverted.stream().filter(ruleChange -> ruleChange.getRule().getRuleCode().equals(new UUID(0, 0)))
                .collect(Collectors.toList()).get(0).getRule();
        assertEquals(Collections.singletonMap("Rate", "1.1"), rule.getOutputs());
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
        assertEquals(new UUID(0, 0), rule.getRuleCode());
        assertNotNull(rule.getRuleIdentifier());

        ruleDrivers.clear();
        Arrays.stream(rule.getDrivers()).forEach(inputDriver -> ruleDrivers.add(inputDriver.toString()));
        final Optional<InputDriver> groupDriver = Arrays.stream(rule.getDrivers()).filter(inputDriver ->
                inputDriver.getType() == InputValueType.VALUE_GROUP).findFirst();
        assertTrue(groupDriver.isPresent());
        assertThat(ruleDrivers, contains("*", groupDriver.get().toString(), "*", "*", "INDEX"));

        // rule with rule code 2
        rule = reverted.stream().filter(ruleChange -> ruleChange.getRule().getRuleCode().equals(new UUID(0, 2)))
                .collect(Collectors.toList()).get(0).getRule();
        assertEquals(Collections.singletonMap("Rate", "2.1"), rule.getOutputs());
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
        assertEquals(new UUID(0, 2), rule.getRuleCode());
        assertNotNull(rule.getRuleIdentifier());

        final Set<ValueGroupChange> valueGroupChanges = change.getValueGroupChanges();
        final List<ValueGroupChange> originalGroups = valueGroupChanges.stream().filter(valueGroupChange ->
                valueGroupChange.getType() == Type.ORIGINAL).collect(Collectors.toList());
        assertThat(originalGroups, hasSize(1));

        final List<ValueGroupChange> newGroups = valueGroupChanges.stream().filter(valueGroupChange ->
                valueGroupChange.getType() == Type.NEW).collect(Collectors.toList());
        assertThat(newGroups, hasSize(1));

        final ValueGroup originalGroup = originalGroups.get(0).getValueGroup();
        final ValueGroup newGroup = newGroups.get(0).getValueGroup();

        assertNotEquals(originalGroup.getId(), newGroup.getId());
        assertEquals(originalGroup.getName(), newGroup.getName());
        assertEquals(originalGroup.getRange(), newGroup.getRange());
        assertEquals(Arrays.asList("CME", "KCBOT"), originalGroup.getValues());
        assertEquals(Arrays.asList("CME", "CBOT"), newGroup.getValues());
    }
}
