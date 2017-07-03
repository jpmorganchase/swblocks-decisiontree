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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.TreeRule;
import org.swblocks.decisiontree.TreeValueGroup;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.StringDriver;
import org.swblocks.jbl.test.utils.JblTestClassUtils;
import org.swblocks.jbl.util.DateRange;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link Change}.
 */
public class ChangeTest {
    private static final Instant NOW = Instant.now();

    private static final UUID ADDED_ID = new UUID(0, 3);
    private static final UUID UNKNOWN_ID = new UUID(0, 5);
    private static final DateRange CHANGE_RANGE = new DateRange(NOW, NOW.plus(Period.ofWeeks(5)));
    private static final UUID BASE_ID = new UUID(0, 1);
    private final Map<String, Object> injectedValues = new HashMap<>();
    private Change bean;

    @Before
    public void setup() {
        this.injectedValues.clear();
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
                new DecisionTreeRule(BASE_ID, new UUID(0, 2), drivers, outputs, start, end);
        final RuleChange ruleChange = new RuleChange(Type.ORIGINAL, rule);

        ruleChanges.add(ruleChange);

        final Set<ValueGroupChange> valueGroupChanges = new HashSet<>();

        final List<String> valueGroupDrivers = Arrays.asList("Test1", "Test2", "Test3");
        final ValueGroup valueGroup = new ValueGroup(BASE_ID, "TestValueGroup", valueGroupDrivers, range);
        final ValueGroupChange valueGroupChange = new ValueGroupChange(Type.ORIGINAL, valueGroup);

        valueGroupChanges.add(valueGroupChange);

        final UUID id = new UUID(0, 2);
        final Instant initiatorTime = NOW;
        final Instant authoriserTime = initiatorTime.plus(Period.ofWeeks(1));

        final Audit audit = new Audit("USER1", initiatorTime, "USER2", authoriserTime);
        final Change change = new Change(id, "TEST-RULESET", NOW, CHANGE_RANGE, audit, ruleChanges, valueGroupChanges);
        setBean(change);

        this.injectedValues.put("id", id);
        this.injectedValues.put("ruleSetName", "TEST-RULESET");
        this.injectedValues.put("ruleChanges", ruleChanges);
        this.injectedValues.put("valueGroupChanges", valueGroupChanges);
        this.injectedValues.put("activationTime", NOW);
        this.injectedValues.put("changeRange", CHANGE_RANGE);

        JblTestClassUtils.assertGetterCorrectForConstructorInjection(this.injectedValues, getBean());
    }

    @Test
    public void changeAuditInfo() {
        testConstruction();

        Instant initiatorTime = NOW;

        final Audit audit = getBean().getAudit();
        assertEquals("USER1", audit.getInitiator());
        assertEquals(initiatorTime, audit.getInitiatorTime());
        assertEquals("USER2", audit.getAuthoriser());

        Instant authoriserTime = initiatorTime.plus(Period.ofWeeks(1));
        assertEquals(authoriserTime, audit.getAuthoriserTime());

        initiatorTime = NOW.plus(Period.ofWeeks(5));
        authoriserTime = initiatorTime.plus(Period.ofWeeks(1));
        final Audit newAudit = new Audit("USER3", initiatorTime, "USER4", authoriserTime);
        getBean().setAudit(newAudit);

        this.injectedValues.clear();

        this.injectedValues.put("initiator", "USER3");
        this.injectedValues.put("initiatorTime", initiatorTime);
        this.injectedValues.put("authoriser", "USER4");
        this.injectedValues.put("authoriserTime", authoriserTime);

        JblTestClassUtils.assertGetterCorrectForConstructorInjection(this.injectedValues, getBean().getAudit());
    }

    @Test
    public void addRuleChange() {
        testConstruction();

        final Instant start = NOW.minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final InputDriver[] drivers = getInputDriverArray("Test5", "Test6", "Test7");
        final Map<String, String> outputs = Collections.singletonMap("outputDriver", "result");

        final DecisionTreeRule rule =
                new DecisionTreeRule(ADDED_ID, new UUID(0, 4), drivers, outputs, start, end);
        final RuleChange ruleChange = new RuleChange(Type.NEW, rule);
        getBean().addRuleChange(ruleChange);
        assertThat(getBean().getRuleChanges(), hasSize(2));
        assertThat(getBean().getRuleChanges(), hasItem(ruleChange));
        assertThat(getBean().getValueGroupChanges(), hasSize(1));
    }

    @Test
    public void addRuleChanges() {
        testConstruction();
        final Instant start = NOW.minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final RuleChange ruleChange1 = new RuleChange(Type.NEW,
                new DecisionTreeRule(ADDED_ID, new UUID(0, 4),
                        getInputDriverArray("Test5", "Test6", "Test7"),
                        Collections.singletonMap("outputDriver", "result"), start, end));
        final DecisionTreeRule originalVersionOfNewRule = new DecisionTreeRule(ADDED_ID, new UUID(0, 4),
                getInputDriverArray("Test1", "Test2", "Test3"),
                Collections.singletonMap("outputDriver", "result"), start, end);
        final RuleChange ruleChange2 = new RuleChange(Type.ORIGINAL, originalVersionOfNewRule);
        getBean().addRuleChange(new HashSet<>(Arrays.asList(ruleChange1, ruleChange2)));
        assertThat(getBean().getRuleChanges(), hasSize(3));
        assertThat(getBean().getRuleChanges(), hasItems(ruleChange1, ruleChange2));
        assertThat(getBean().getValueGroupChanges(), hasSize(1));

        // Extends the test for the addRuleChange to ensure that the rules are exported correctly.
        final Map<UUID, Optional<TreeRule>> changedRules = getBean().getRules();
        assertNotNull(changedRules);
        assertThat(changedRules.size(), is(2));
        assertThat(changedRules, allOf(
                Matchers.not(hasEntry(ADDED_ID, Optional.<TreeRule>empty())),
                hasEntry(BASE_ID, Optional.<TreeRule>empty()),
                hasEntry(ADDED_ID, Optional.<TreeRule>of(originalVersionOfNewRule))));
    }


    @Test(expected = IllegalArgumentException.class)
    public void cannotAddNullRuleChange() {
        testConstruction();
        getBean().addRuleChange((RuleChange) null);
    }

    @Test
    public void removeRuleChange() {
        addRuleChange();
        assertThat(getBean().getRuleChanges(), hasSize(2));

        final Instant start = NOW.minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final InputDriver[] drivers = getInputDriverArray("Test5", "Test6", "Test7");
        final Map<String, String> outputs = Collections.singletonMap("outputDriver", "result");

        DecisionTreeRule rule = new DecisionTreeRule(UNKNOWN_ID, new UUID(0, 4), drivers, outputs, start, end);
        RuleChange ruleChange = new RuleChange(Type.ORIGINAL, rule);
        getBean().removeRuleChange(ruleChange);
        assertThat(getBean().getRuleChanges(), hasSize(2));
        assertThat(getBean().getValueGroupChanges(), hasSize(1));

        rule = new DecisionTreeRule(UNKNOWN_ID, new UUID(0, 4), drivers, outputs, start, end);
        ruleChange = new RuleChange(Type.ORIGINAL, rule);
        getBean().removeRuleChange(ruleChange);
        assertThat(getBean().getRuleChanges(), hasSize(2));
        assertThat(getBean().getValueGroupChanges(), hasSize(1));

        rule = new DecisionTreeRule(ADDED_ID, new UUID(0, 4), drivers, outputs, start, end);
        ruleChange = new RuleChange(Type.ORIGINAL, rule);
        getBean().removeRuleChange(ruleChange);
        assertThat(getBean().getRuleChanges(), hasSize(2));
        assertThat(getBean().getValueGroupChanges(), hasSize(1));

        rule = new DecisionTreeRule(ADDED_ID, new UUID(0, 4), drivers, outputs, start, end);
        ruleChange = new RuleChange(Type.NEW, rule);
        getBean().removeRuleChange(ruleChange);
        assertThat(getBean().getRuleChanges(), hasSize(1));
        assertThat(getBean().getValueGroupChanges(), hasSize(1));
    }

    @Test
    public void addValueGroupChange() {
        testConstruction();
        final Instant start = NOW.minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final List<String> drivers = Arrays.asList("Test5", "Test6", "Test7");
        final ValueGroup valueGroup = new ValueGroup(ADDED_ID, "TestValueGroup", drivers, range);
        final ValueGroupChange valueGroupChange = new ValueGroupChange(Type.NEW, valueGroup);

        final ValueGroup originalValueGroupChanged = new ValueGroup(BASE_ID, "TestValueGroup",
                Arrays.asList("Test2", "Test3"), range);
        final ValueGroupChange originalValueGroupChange = new ValueGroupChange(Type.NEW, originalValueGroupChanged);

        getBean().addValueGroupChange(valueGroupChange);
        getBean().addValueGroupChange(originalValueGroupChange);
        assertThat(getBean().getValueGroupChanges(), hasSize(3));
        assertThat(getBean().getValueGroupChanges(), hasItems(getBean().getValueGroupChanges().iterator().next(),
                valueGroupChange, originalValueGroupChange));
        assertThat(getBean().getRuleChanges(), hasSize(1));

        final Map<UUID, Optional<TreeValueGroup>> groups = getBean().getGroups();
        assertNotNull(groups);
        assertThat(groups.size(), is(2));
        assertThat(groups, allOf(
                hasEntry(BASE_ID, Optional.<TreeValueGroup>of(originalValueGroupChanged)),
                hasEntry(ADDED_ID, Optional.<TreeValueGroup>of(valueGroup))));

    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddNullValueGroupChange() {
        testConstruction();
        getBean().addValueGroupChange(null);
    }

    @Test
    public void removeValueGroup() {
        testConstruction();
        final Instant start = NOW.minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final DateRange range = new DateRange(start, end);
        final List<String> drivers = Arrays.asList("Test5", "Test6", "Test7");
        final ValueGroup valueGroup = new ValueGroup(ADDED_ID, "TestValueGroup", drivers, range);
        final ValueGroupChange valueGroupChange = new ValueGroupChange(Type.NEW, valueGroup);

        getBean().addValueGroupChange(valueGroupChange);

        ValueGroup valueGroupForTest = new ValueGroup(UNKNOWN_ID, "TestValueGroup", drivers, range);
        ValueGroupChange valueGroupChangeForTest = new ValueGroupChange(Type.NEW, valueGroupForTest);
        getBean().removeValueGroupChange(valueGroupChangeForTest);
        assertThat(getBean().getValueGroupChanges(), hasSize(2));
        assertThat(getBean().getRuleChanges(), hasSize(1));

        valueGroupForTest = new ValueGroup(UNKNOWN_ID, "TestValueGroup", drivers, range);
        valueGroupChangeForTest = new ValueGroupChange(Type.ORIGINAL, valueGroupForTest);
        getBean().removeValueGroupChange(valueGroupChangeForTest);
        assertThat(getBean().getValueGroupChanges(), hasSize(2));
        assertThat(getBean().getRuleChanges(), hasSize(1));

        valueGroupForTest = new ValueGroup(ADDED_ID, "TestValueGroup", drivers, range);
        valueGroupChangeForTest = new ValueGroupChange(Type.ORIGINAL, valueGroupForTest);
        getBean().removeValueGroupChange(valueGroupChangeForTest);
        assertThat(getBean().getValueGroupChanges(), hasSize(2));
        assertThat(getBean().getRuleChanges(), hasSize(1));

        valueGroupForTest = new ValueGroup(ADDED_ID, "TestValueGroup", drivers, range);
        valueGroupChangeForTest = new ValueGroupChange(Type.NEW, valueGroupForTest);
        getBean().removeValueGroupChange(valueGroupChangeForTest);
        assertThat(getBean().getValueGroupChanges(), hasSize(1));
        assertThat(getBean().getRuleChanges(), hasSize(1));
    }

    @Test
    public void equalsCorrect() {
        final Set<RuleChange> ruleChanges = null;
        final Set<ValueGroupChange> valueGroupChanges = null;

        final UUID id = new UUID(0, 2);
        final Instant initiatorTime = NOW;
        final Instant authoriserTime = initiatorTime.plus(Period.ofWeeks(1));

        final Audit audit = new Audit("USER1", initiatorTime, "USER2", authoriserTime);
        final Change change = new Change(id, "TEST-RULESET", NOW, CHANGE_RANGE, audit, ruleChanges, valueGroupChanges);

        assertTrue(change.equals(change));
        assertFalse(change.equals(null));
        assertFalse(change.equals(Boolean.TRUE));

        final Audit otherAudit = new Audit("USER1", initiatorTime, "USER2", authoriserTime);
        Change other = new Change(new UUID(0, 2), "TEST-RULESET", NOW, CHANGE_RANGE, otherAudit, ruleChanges,
                valueGroupChanges);
        assertTrue(change.equals(other));

        other = new Change(new UUID(0, 1), "TEST-RULESET", NOW, CHANGE_RANGE, otherAudit, ruleChanges,
                valueGroupChanges);
        assertFalse(change.equals(other));

        other = new Change(id, "TESTING-RULESET", NOW, CHANGE_RANGE, otherAudit, ruleChanges, valueGroupChanges);
        assertFalse(change.equals(other));

        other = new Change(new UUID(0, 1), "TESTING-RULESET", NOW, CHANGE_RANGE, otherAudit, ruleChanges,
                valueGroupChanges);
        assertFalse(change.equals(other));
    }

    @Test
    public void hashCodeCorrect() {
        final Set<RuleChange> ruleChanges = null;
        final Set<ValueGroupChange> valueGroupChanges = null;

        final Instant initiatorTime = NOW;
        final Instant authoriserTime = initiatorTime.plus(Period.ofWeeks(1));

        final Audit audit = new Audit("USER1", initiatorTime, "USER2", authoriserTime);
        final Change change = new Change(new UUID(0, 2), "TEST-RULESET", NOW, CHANGE_RANGE, audit, ruleChanges,
                valueGroupChanges);

        final Audit otherAudit = new Audit("USER1", initiatorTime, "USER2", authoriserTime);
        final Change otherChange = new Change(new UUID(0, 2), "TEST-RULESET", NOW, CHANGE_RANGE, otherAudit,
                ruleChanges, valueGroupChanges);

        assertEquals(change.hashCode(), otherChange.hashCode());
    }

    private InputDriver[] getInputDriverArray(final String... inputs) {
        final InputDriver[] drivers = new InputDriver[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            drivers[i] = new StringDriver(inputs[i]);
        }

        return drivers;
    }

    private Change getBean() {
        return this.bean;
    }

    private void setBean(final Change bean) {
        this.bean = bean;
    }
}
