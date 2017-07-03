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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.StringDriver;
import org.swblocks.jbl.test.utils.JblTestClassUtils;
import org.swblocks.jbl.util.DateRange;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Test class for {@link ChangeSet}.
 */
public class ChangeSetTest {
    private static final Instant NOW = Instant.now();

    private static final UUID INITIAL_CHANGE_ID = new UUID(0, 2);
    private static final UUID ADDED_CHANGE_ID = new UUID(0, 4);
    private static final DateRange CHANGE_RANGE = new DateRange(NOW.plus(Period.ofWeeks(5)),
            NOW.plus(Period.ofWeeks(4)));

    private final Map<String, Object> injectedValues = new HashMap<>();
    private ChangeSet bean;

    @Before
    public void setup() {
        this.injectedValues.clear();
        setBean(null);
    }

    @Test
    public void testConstruction() {
        final Set<RuleChange> ruleChanges = new HashSet<>(1);

        final Instant start = NOW.minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final InputDriver[] drivers = getInputDriverArray("Test1", "Test2", "Test3");
        final Map<String, String> outputs = Collections.singletonMap("outputDriver", "result");
        final DecisionTreeRule rule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), drivers, outputs, start, end);
        final RuleChange ruleChange = new RuleChange(Type.NEW, rule);

        ruleChanges.add(ruleChange);

        final Set<ValueGroupChange> valueGroupChanges = new HashSet<>(1);
        final List<String> valueGroupDrivers = getValueGroupsDrivers("Test1", "Test2", "Test3");
        final ValueGroup valueGroup = new ValueGroup(new UUID(0, 1), "TestValueGroup", valueGroupDrivers,
                range);
        final ValueGroupChange valueGroupChange =
                new ValueGroupChange(Type.NEW, valueGroup);

        valueGroupChanges.add(valueGroupChange);

        final Instant initiatorTime = NOW;
        final Instant authoriserTime = initiatorTime.plus(Period.ofWeeks(1));

        final Audit audit = new Audit("User1", initiatorTime, "User2", authoriserTime);
        final Change change = new Change(INITIAL_CHANGE_ID, "TEST-RULESET", NOW, CHANGE_RANGE, audit, ruleChanges,
                valueGroupChanges);

        final Set<Change> changes = new HashSet<>(1);
        changes.add(change);

        final UUID uuid = new UUID(0, 3);
        final ChangeSet changeSet = new ChangeSet(uuid, "changeset-name", changes);
        setBean(changeSet);

        this.injectedValues.put("id", uuid);
        this.injectedValues.put("name", "changeset-name");
        this.injectedValues.put("changes", changes);

        JblTestClassUtils.assertGetterCorrectForConstructorInjection(this.injectedValues, getBean());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotCreateChangeWithNullData() {
        new ChangeSet(null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotCreateChangeWithNullId() {
        final Audit audit = new Audit("User1", NOW, "User2", NOW);
        new ChangeSet(null, "changeset-name", Collections.singleton(new Change(INITIAL_CHANGE_ID,
                "TEST-RULESET", NOW, CHANGE_RANGE, audit, Collections.emptySet(), Collections.emptySet())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotCreateChangeWithNullName() {
        final Audit audit = new Audit("User1", NOW, "User2", NOW);
        new ChangeSet(UUID.randomUUID(), null, Collections.singleton(new Change(INITIAL_CHANGE_ID,
                "TEST-RULESET", NOW, CHANGE_RANGE, audit, Collections.emptySet(), Collections.emptySet())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotCreateChangeWithNullChangeSet() {
        new ChangeSet(UUID.randomUUID(), "changeset-name", null);
    }


    @Test
    public void addChange() {
        testConstruction();

        final Set<RuleChange> ruleChanges = new HashSet<>(1);

        final Instant start = NOW.minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final InputDriver[] drivers = getInputDriverArray("Test1", "Test2", "Test3");
        final Map<String, String> outputs = Collections.singletonMap("outputDriver", "result");
        final DecisionTreeRule rule =
                new DecisionTreeRule(new UUID(0, 7), new UUID(0, 7), drivers, outputs, start, end);
        final RuleChange ruleChange = new RuleChange(Type.ORIGINAL, rule);

        ruleChanges.add(ruleChange);

        final Set<ValueGroupChange> valueGroupChanges = new HashSet<>();
        final List<String> valueGroupDrivers = getValueGroupsDrivers("Test1", "Test2", "Test3");
        final ValueGroup valueGroup = new ValueGroup(new UUID(0, 8), "TestValueGroup", valueGroupDrivers,
                range);
        final ValueGroupChange valueGroupChange =
                new ValueGroupChange(Type.NEW, valueGroup);

        valueGroupChanges.add(valueGroupChange);

        final Instant initiatorTime = NOW;
        final Instant authoriserTime = initiatorTime.plus(Period.ofWeeks(1));

        final Audit audit = new Audit("User1", initiatorTime, "User2", authoriserTime);
        final Change change = new Change(ADDED_CHANGE_ID, "TEST-RULESET", NOW, CHANGE_RANGE, audit, ruleChanges,
                valueGroupChanges);

        getBean().addChange(change);

        assertThat(getBean().getChanges(), hasSize(2));
        assertThat(getBean().getChanges(), hasItem(change));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddNullChange() {
        testConstruction();
        getBean().addChange(null);
    }

    @Test
    public void removeChange() {
        addChange();

        final Audit audit = new Audit("User1", null, "User2", null);
        Change change = new Change(new UUID(0, 6), "TEST-RULESET", NOW, CHANGE_RANGE, audit, Collections.emptySet(),
                Collections.emptySet());
        getBean().removeChange(change);
        assertThat(getBean().getChanges(), hasSize(2));

        change = new Change(ADDED_CHANGE_ID, "TEST-RULESET", NOW, CHANGE_RANGE, audit, Collections.emptySet(),
                Collections.emptySet());
        getBean().removeChange(change);
        assertThat(getBean().getChanges(), hasSize(1));
    }

    private InputDriver[] getInputDriverArray(final String... inputs) {
        final InputDriver[] drivers = new InputDriver[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            drivers[i] = new StringDriver(inputs[i]);
        }

        return drivers;
    }

    private List<String> getValueGroupsDrivers(final String... inputs) {
        return Arrays.asList(inputs);
    }

    private ChangeSet getBean() {
        return this.bean;
    }

    private void setBean(final ChangeSet bean) {
        this.bean = bean;
    }
}
