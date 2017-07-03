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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.ChangeSet;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ChangeSetBuilder}.
 */
public class ChangeSetBuilderTest {
    private DecisionTreeRuleSet commissionRuleSet;

    @Before
    public void setup() {
        this.commissionRuleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBuildWithoutName() {
        final Builder<ChangeSetBuilder, ChangeSet> builder = ChangeSetBuilder.creator(null);
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBuildWithBlankName() {
        final Builder<ChangeSetBuilder, ChangeSet> builder = ChangeSetBuilder.creator(" ");
        builder.build();
    }

    @Test
    public void cannotBuildWithoutChanges() {
        final Builder<ChangeSetBuilder, ChangeSet> builder = ChangeSetBuilder.creator("TestChangeSet");
        assertNull(builder.build());
    }

    @Test
    public void noChangesWhenNullChangeAdded() {
        final Builder<ChangeSetBuilder, ChangeSet> builder = ChangeSetBuilder.creator("TestChangeSet");
        builder.with(ChangeSetBuilder::change, null);
        assertNull(builder.build());
    }

    @Test
    public void noChangesWhenCollectionIsNull() {
        final Builder<ChangeSetBuilder, ChangeSet> builder = ChangeSetBuilder.creator("TestChangeSet");
        builder.with(ChangeSetBuilder::changes, null);
        assertNull(builder.build());
    }

    @Test
    public void createsChangeSetContainingSingleChange() {
        final Builder<ChangeSetBuilder, ChangeSet> builder = ChangeSetBuilder.creator("TestChangeSet");
        final Change change = new Change(UUID.randomUUID(), "TestRuleSet", null, null, null, null, null);

        builder.with(ChangeSetBuilder::change, change);
        final ChangeSet changeSet = builder.build();
        assertNotNull(changeSet.getId());
        assertThat(changeSet.getName(), is("TestChangeSet"));
        assertThat(changeSet.getChanges().size(), is(1));
        assertTrue(changeSet.getChanges().contains(change));
    }

    @Test
    public void createsChangeSetWithChangeCollection() {
        final Builder<ChangeSetBuilder, ChangeSet> builder = ChangeSetBuilder.creator("TestChangeSet");

        final Change first = new Change(UUID.randomUUID(), "FirstTestRuleSet", null, null, null, null, null);
        final Change second = new Change(UUID.randomUUID(), "SecondTestRuleSet", null, null, null, null, null);
        final Set<Change> changes = new HashSet<>();
        changes.add(first);
        changes.add(second);

        builder.with(ChangeSetBuilder::changes, changes);
        final ChangeSet changeSet = builder.build();
        assertNotNull(changeSet.getId());
        assertThat(changeSet.getName(), is("TestChangeSet"));
        assertThat(changeSet.getChanges().size(), is(2));
        assertTrue(changeSet.getChanges().contains(first));
        assertTrue(changeSet.getChanges().contains(second));
    }

    @Test
    public void createOutputChangeToCommissionsUkRuleFromToday() {
        final Builder<ChangeBuilder, Change> changeBuilder = ChangeBuilder.creator(this.commissionRuleSet)
                .with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(new UUID(0L, 5))
                        .with(RuleChangeBuilder::output, Arrays.asList("Rate:1.1", "Unit:£")));

        final ChangeSet changeSet = ChangeSetBuilder.creator("TestChangeSet")
                .with(ChangeSetBuilder::changes, Collections.singletonList(changeBuilder.build())).build();
        assertNotNull(changeSet);
        assertThat(changeSet.getChanges().size(), is(1));
        final Change generatedChange = changeSet.getChanges().iterator().next();
        assertNotNull(generatedChange);
        assertThat(generatedChange.getRuleChanges().size(), is(3));
        final List<RuleChange> originalRuleChanges = generatedChange.getRuleChanges().stream().filter(ruleChange ->
                Type.ORIGINAL.equals(ruleChange.getType())).collect(Collectors.toList());
        assertThat(originalRuleChanges.size(), is(1));
        final List<RuleChange> newRuleChanges = generatedChange.getRuleChanges().stream().filter(ruleChange ->
                Type.NEW.equals(ruleChange.getType())).collect(Collectors.toList());
        assertThat(newRuleChanges.size(), is(2));
    }

    @Test
    public void createValueGroupChangeToCommissionsCmeGroupFromToday() {
        final Builder<ChangeBuilder, Change> changeBuilder = ChangeBuilder.creator(this.commissionRuleSet)
                .with(ChangeBuilder::valueGroupChange,
                        ValueGroupChangeBuilder.creator("CMEGroup")
                                .with(ValueGroupChangeBuilder::ruleSet, this.commissionRuleSet)
                                .with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME, CBOT, NYMEX")));

        final ChangeSet changeSet = ChangeSetBuilder.creator("TestChangeSet")
                .with(ChangeSetBuilder::change, changeBuilder.build()).build();

        assertNotNull(changeSet);
        assertThat(changeSet.getChanges().size(), is(1));
        final Change generatedChange = changeSet.getChanges().iterator().next();
        assertNotNull(generatedChange);
    }
}