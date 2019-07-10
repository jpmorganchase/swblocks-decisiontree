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

package org.swblocks.decisiontree.domain.builders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.tree.StringDriver;
import org.swblocks.jbl.builders.Builder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link RuleSetBuilder}.
 */
public class RuleSetBuilderTest {
    @Test
    public void testRuleSetBuilderFactory() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> instance =
                RuleSetBuilder.creator(Arrays.asList("input1", "input2", "input3"));
        assertNotNull(instance);
        final DecisionTreeRuleSet ruleSet = instance.build();
        assertNotNull(ruleSet);
        assertEquals("", ruleSet.getName());
    }

    @Test
    public void testRuleSetBuilderFactoryWithName() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> instance =
                RuleSetBuilder.creator("TestRuleSetName", Arrays.asList("input1", "input2", "input3"));
        assertNotNull(instance);
        final DecisionTreeRuleSet ruleSet = instance.build();
        assertNotNull(ruleSet);
        assertEquals("TestRuleSetName", ruleSet.getName());
    }

    @Test
    public void testConstructingSimpleRuleSet() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("input1", "input2", "input3"));
        ruleSetBuilder.with(RuleSetBuilder::setName, "testSimpleRuleSet");
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 3))
                .with(RuleBuilder::input, Arrays.asList("input1", "input2", "input4"))
                .with(RuleBuilder::output, Collections.singletonList("outputDriver:result")));

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        assertNotNull(ruleSet);
        assertEquals(1L, ruleSet.getRules().size());
        assertEquals("testSimpleRuleSet", ruleSet.getName());

        final DecisionTreeRule rule = ruleSet.getRules().get(new UUID(0, 3));
        assertNotNull(rule);
        assertEquals(new UUID(0, 3), rule.getRuleIdentifier());
        assertEquals(3, rule.getDrivers().length);
        assertEquals(Arrays.asList("input1", "input2", "input3"), ruleSet.getDriverNames());

        assertEquals("result", rule.getOutputs().get("outputDriver"));
    }


    @Test
    public void testConstructingRuleSetWithEvaluations() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("input1", "input2", "input3"));
        ruleSetBuilder.with(RuleSetBuilder::setName, "testSimpleRuleSet");
        ruleSetBuilder.with(RuleSetBuilder::setEvaluationNames, Collections.singletonList("Eval1"));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 3))
                .with(RuleBuilder::input, Arrays.asList("input1", "input2", "input4"))
                .with(RuleBuilder::evaluations, Collections.singletonList("Eval1"))
                .with(RuleBuilder::output, Collections.singletonList("outputDriver:result")));

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        assertNotNull(ruleSet);
        assertThat(ruleSet.getRules(), is(notNullValue()));
        assertThat(ruleSet.getRules().size(), is(1));
        assertThat(ruleSet.getName(), is("testSimpleRuleSet"));
        assertThat(ruleSet.getEvaluationNames(), is(notNullValue()));
        assertThat(ruleSet.getEvaluationNames(), hasSize(1));
        assertThat(ruleSet.getEvaluationNames(), hasItem("Eval1"));

        final DecisionTreeRule rule = ruleSet.getRules().get(new UUID(0, 3));
        assertThat(rule, is(notNullValue()));
        assertThat(rule.getRuleIdentifier(), is(new UUID(0, 3)));
        assertThat(Arrays.asList(rule.getDrivers()), hasSize(3));
        assertThat(ruleSet.getDriverNames(), contains("input1", "input2", "input3"));
        assertThat(rule.getEvaluations().isPresent(), is(true));
        assertThat(Arrays.asList(rule.getEvaluations().get()), hasItem(new StringDriver("Eval1")));
        assertThat(rule.getOutputs().get("outputDriver"), is("result"));
    }

    @Test
    public void testConstructingSimpleRegexRuleSet() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("input1", "input2", "input3"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("input1", "inp.?t2", "input4.*"))
                .with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result")));

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        assertNotNull(ruleSet);
        assertEquals(1L, ruleSet.getRules().size());
    }

    @Test
    public void testConstructingSimpleValueGroupRuleSetWithoutValueGroups() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("input1", "input2", "input3"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("VG:VG1:input1:input2", "VG:VG2:input3:input4",
                        "VG:VG3:input5:input6:VG:VG4:input7:input8"))
                .with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result")));

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        assertNotNull(ruleSet);
        assertEquals(1L, ruleSet.getRules().size());

        final Set<ValueGroup> groups = ruleSet.getValueGroups();
        // build has populated the groups to allow for saving in future
        assertEquals(4, groups.size());

        final DriverCache cache = ruleSet.getDriverCache();
        final List<InputDriver> drivers = cache.findByInputDriverType(InputValueType.VALUE_GROUP);
        // Double entries in the cache, as it has the named versions and the actual UUID'ed versions
        assertEquals(8, drivers.size());

        InputDriver driver = cache.get("VG1", InputValueType.VALUE_GROUP);
        assertNotNull(driver);
        assertTrue(driver.evaluate("input1"));
        assertTrue(driver.evaluate("input2"));

        driver = cache.get("VG2", InputValueType.VALUE_GROUP);
        assertTrue(driver.evaluate("input3"));
        assertTrue(driver.evaluate("input4"));

        driver = cache.get("VG3", InputValueType.VALUE_GROUP);
        assertTrue(driver.evaluate("input5"));
        assertTrue(driver.evaluate("input6"));
        assertTrue(driver.evaluate("input7"));
        assertTrue(driver.evaluate("input8"));

        driver = cache.get("VG4", InputValueType.VALUE_GROUP);
        assertTrue(driver.evaluate("input7"));
        assertTrue(driver.evaluate("input8"));
    }

    @Test
    public void constructsValueGroupRuleSetWithSpecifiedGroups() {
        final Set<ValueGroup> groups = new HashSet<>(4);
        final UUID vg1 = new UUID(0, 1);
        final UUID vg2 = new UUID(0, 2);
        final UUID vg3 = new UUID(0, 3);
        final UUID vg4 = new UUID(0, 4);

        ValueGroup group =
                new ValueGroup(vg1, "VG1", Arrays.asList("input1", "input2"), ValueGroup.DEFAULT_DATE_RANGE);
        groups.add(group);
        group = new ValueGroup(vg2, "VG2", Arrays.asList("input3", "input4"), ValueGroup.DEFAULT_DATE_RANGE);
        groups.add(group);
        group = new ValueGroup(vg3, "VG3", Arrays.asList(GroupDriver.VG_PREFIX + vg4, "input5", "input6"),
                ValueGroup.DEFAULT_DATE_RANGE);
        groups.add(group);
        group = new ValueGroup(vg4, "VG4", Arrays.asList("input7", "input8"), ValueGroup.DEFAULT_DATE_RANGE);
        groups.add(group);

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("input1", "input2", "input3"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList(GroupDriver.VG_PREFIX + vg1, GroupDriver.VG_PREFIX + vg2,
                        GroupDriver.VG_PREFIX + vg3))
                .with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result")));
        ruleSetBuilder.with(RuleSetBuilder::groups, groups);

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        assertNotNull(ruleSet);
        assertEquals(1L, ruleSet.getRules().size());

        final DriverCache cache = ruleSet.getDriverCache();
        final List<InputDriver> groupDrivers = cache.findByInputDriverType(InputValueType.VALUE_GROUP);
        assertThat(groupDrivers, hasSize(4));

        GroupDriver added = (GroupDriver) groupDrivers.stream().filter(inputDriver ->
                vg1.toString().equals(inputDriver.getValue())).findFirst().get();
        assertNotNull(added);
        assertThat(Arrays.stream(added.getSubDrivers(false)).map(InputDriver::getValue).collect(Collectors.toList()),
                containsInAnyOrder("input1", "input2"));
        assertTrue(added.evaluate("input1"));
        assertTrue(added.evaluate("input2"));

        added = (GroupDriver) groupDrivers.stream().filter(inputDriver ->
                vg2.toString().equals(inputDriver.getValue())).findFirst().get();
        assertNotNull(added);
        assertThat(Arrays.stream(added.getSubDrivers(false)).map(InputDriver::getValue).collect(Collectors.toList()),
                containsInAnyOrder("input3", "input4"));
        assertTrue(added.evaluate("input3"));
        assertTrue(added.evaluate("input4"));

        added = (GroupDriver) groupDrivers.stream().filter(inputDriver ->
                vg3.toString().equals(inputDriver.getValue())).findFirst().get();
        assertNotNull(added);
        assertThat(Arrays.stream(added.getSubDrivers(false)).map(InputDriver::getValue).collect(Collectors.toList()),
                containsInAnyOrder("input5", "input6", vg4.toString()));
        assertTrue(added.evaluate("input5"));
        assertTrue(added.evaluate("input6"));
        assertTrue(added.evaluate("input7"));
        assertTrue(added.evaluate("input8"));

        added = (GroupDriver) groupDrivers.stream().filter(inputDriver ->
                vg4.toString().equals(inputDriver.getValue())).findFirst().get();
        assertNotNull(added);
        assertThat(Arrays.stream(added.getSubDrivers(false)).map(InputDriver::getValue).collect(Collectors.toList()),
                containsInAnyOrder("input7", "input8"));
        assertTrue(added.evaluate("input7"));
        assertTrue(added.evaluate("input8"));
    }

    @Test
    public void testCreatingBasicValueGroupDriversInCache() {
        final DriverCache cache = new DriverCache();
        final Set<ValueGroup> valueGroups = new HashSet<>(1);

        final ValueGroup group = new ValueGroup("vg1", Arrays.asList("input1", "input2", "input3"));
        valueGroups.add(group);
        RuleSetBuilder.addValueGroupsToDriverCache(cache, valueGroups);

        assertTrue(cache.contains(GroupDriver.VG_PREFIX + group.getId()));
        assertTrue(cache.contains("input1"));
        assertTrue(cache.contains("input2"));
        assertTrue(cache.contains("input3"));
        final InputDriver valueGroup = cache.get(group.getId().toString(), InputValueType.VALUE_GROUP);
        assertTrue(valueGroup.evaluate("input1"));
        assertTrue(valueGroup.evaluate("input2"));
        assertTrue(valueGroup.evaluate("input3"));
        assertFalse(valueGroup.evaluate("input4"));
    }

    @Test
    public void testCreatingValueGroupsOfValueGroupsInCache() {
        final DriverCache cache = new DriverCache();
        final Set<ValueGroup> valueGroups = new HashSet<>(2);

        final ValueGroup vg11 = new ValueGroup("vg1.1", Arrays.asList("input1.1", "input1.2"));
        valueGroups.add(vg11);
        final ValueGroup vg1 = new ValueGroup("vg1",
                Arrays.asList("input1", "input2", GroupDriver.VG_PREFIX + vg11.getId()));
        valueGroups.add(vg1);

        RuleSetBuilder.addValueGroupsToDriverCache(cache, valueGroups);

        assertTrue(cache.contains(GroupDriver.VG_PREFIX + vg1.getId()));
        assertTrue(cache.contains(GroupDriver.VG_PREFIX + vg11.getId()));

        // Check evaluations work
        final InputDriver driverVg1 = cache.get(vg1.getId().toString(), InputValueType.VALUE_GROUP);
        assertTrue(driverVg1.evaluate("input1.1"));
        assertTrue(driverVg1.evaluate("input1.2"));
        assertTrue(driverVg1.evaluate("input1"));
        assertFalse(driverVg1.evaluate("item1.3"));
    }

    @Test
    public void testRecursiveDefinitionsOfSelf() {
        final DriverCache cache = new DriverCache();
        final Set<ValueGroup> valueGroups = new HashSet<>(2);

        final ValueGroup vg1 = new ValueGroup("vg1", Arrays.asList("input1", "input2", "VG:vg1.1"));
        valueGroups.add(vg1);
        final ValueGroup vg11 = new ValueGroup("vg1.1", Arrays.asList("input1.1", "input1.2"));
        valueGroups.add(vg11);
        RuleSetBuilder.addValueGroupsToDriverCache(cache, valueGroups);

        assertTrue(cache.contains(GroupDriver.VG_PREFIX + vg1.getId()));
        assertTrue(cache.contains(GroupDriver.VG_PREFIX + vg11.getId()));
    }

    @Test
    public void testRecursiveDefinitionsOfParent() {
        final DriverCache cache = new DriverCache();
        final Set<ValueGroup> valueGroups = new HashSet<>(2);

        final ValueGroup vg1 = new ValueGroup("vg1", Arrays.asList("input1", "input2", "VG:vg1.1"));
        valueGroups.add(vg1);
        final ValueGroup vg11 = new ValueGroup("vg1.1", Arrays.asList("input1.1", "input1.2", "VG:vg1"));
        valueGroups.add(vg11);
        RuleSetBuilder.addValueGroupsToDriverCache(cache, valueGroups);

        assertTrue(cache.contains(GroupDriver.VG_PREFIX + vg1.getId()));
        assertTrue(cache.contains(GroupDriver.VG_PREFIX + vg11.getId()));
    }

    @Test
    public void testSameGroupNameAsStringValue() {
        final DriverCache cache = new DriverCache();
        final Set<ValueGroup> valueGroups = new HashSet<>(2);

        final ValueGroup group = new ValueGroup("test", Arrays.asList("test", "input1", "input2"));
        valueGroups.add(group);

        RuleSetBuilder.addValueGroupsToDriverCache(cache, valueGroups);
        assertTrue(cache.contains(GroupDriver.VG_PREFIX + group.getId()));
        assertTrue(cache.contains("test"));

        final InputDriver testGroup = cache.get(group.getId().toString(), InputValueType.VALUE_GROUP);
        assertTrue(testGroup.evaluate("test"));
    }
}