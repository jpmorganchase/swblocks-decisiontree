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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.swblocks.decisiontree.TreeChange;
import org.swblocks.decisiontree.TreeRule;
import org.swblocks.decisiontree.TreeValueGroup;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.tree.RegexDriver;
import org.swblocks.decisiontree.tree.StringDriver;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link DecisionTreeRuleSet}.
 */
public class DecisionTreeRuleSetTest {
    @Test
    public void testConstruction() {
        final Instant startInstance = Instant.now();
        final Instant endInstance = startInstance.plus(1, ChronoUnit.DAYS);

        final UUID uuid = new UUID(0, 1);
        final DecisionTreeRule testRule = new DecisionTreeRule(uuid, UUID.randomUUID(), getInputDriverArray(),
                Collections.singletonMap("outputDriver", "result"), startInstance, endInstance);

        final List<String> driverNames = Arrays.asList("driver1", "driver2");
        final List<WeightedDriver> weightedDriverResults = Arrays.asList(new WeightedDriver("driver1", 4),
                new WeightedDriver("driver2", 2));

        final DecisionTreeRuleSet ruleSet = new DecisionTreeRuleSet("TestName",
                Collections.singletonMap(uuid, testRule), driverNames);
        assertEquals("TestName", ruleSet.getName());
        assertEquals(1, ruleSet.getRules().size());
        assertEquals(Collections.emptyList(), ruleSet.getEvaluationNames());
        assertEquals(uuid, ruleSet.getRules().get(uuid).getRuleIdentifier());
        assertEquals(driverNames, ruleSet.getDriverNames());
        assertEquals(weightedDriverResults, ruleSet.getWeightedDrivers());
        assertEquals("DecisionTreeRuleSet{name='TestName'}", ruleSet.toString());
    }

    @Test
    public void testConstructionWithEvaluation() {
        final Instant startInstance = Instant.now();
        final Instant endInstance = startInstance.plus(1, ChronoUnit.DAYS);

        final UUID uuid = new UUID(0, 1);
        final DecisionTreeRule testRule = new DecisionTreeRule(uuid, UUID.randomUUID(), getInputDriverArray(),
                Collections.singletonMap("outputDriver", "result"), startInstance, endInstance);

        final List<String> driverNames = Arrays.asList("driver1", "driver2");
        final List<WeightedDriver> weightedDriverResults = Arrays.asList(new WeightedDriver("driver1", 4),
                new WeightedDriver("driver2", 2));
        final List<String> evaluationNames = Arrays.asList("Eval1", "eval2");

        final DecisionTreeRuleSet ruleSet = new DecisionTreeRuleSet("TestName",
                Collections.singletonMap(uuid, testRule), driverNames, evaluationNames);
        assertEquals("TestName", ruleSet.getName());
        assertEquals(1, ruleSet.getRules().size());
        assertEquals(evaluationNames, ruleSet.getEvaluationNames());
        assertEquals(uuid, ruleSet.getRules().get(uuid).getRuleIdentifier());
        assertEquals(driverNames, ruleSet.getDriverNames());
        assertEquals(evaluationNames, ruleSet.getEvaluationNames());
        assertEquals(weightedDriverResults, ruleSet.getWeightedDrivers());
        assertEquals("DecisionTreeRuleSet{name='TestName'}", ruleSet.toString());
    }

    @Test
    public void testEquality() {
        final List<String> driverNames = Arrays.asList("driver1", "driver2");
        final DecisionTreeRuleSet ruleSet = new DecisionTreeRuleSet("TestName", null, driverNames);
        assertEquals(ruleSet, ruleSet);
        assertEquals(new DecisionTreeRuleSet("TestName", null, driverNames), ruleSet);
        assertNotEquals(new DecisionTreeRuleSet("NotTestName", null, driverNames), ruleSet);
        assertNotEquals(1, ruleSet);
        assertNotEquals(ruleSet, null);
        assertEquals(new DecisionTreeRuleSet("TestName", null, driverNames).hashCode(), ruleSet.hashCode());
        assertNotEquals(new DecisionTreeRuleSet("NotTestName", null, driverNames).hashCode(), ruleSet.hashCode());
    }

    @Test
    public void testConversionOfDriversToWeightedDrivers() {
        final List<String> driverNames = Arrays.asList("driver1", "driver2", "driver3");
        final List<WeightedDriver> weightedDriverResults = Arrays.asList(new WeightedDriver("driver1", 8),
                new WeightedDriver("driver2", 4), new WeightedDriver("driver3", 2));
        final List<WeightedDriver> results = DecisionTreeRuleSet.convertNamesToWeightedDrivers(driverNames);
        assertEquals(weightedDriverResults, results);
        for (int i = 0; i < results.size(); i++) {
            assertEquals(weightedDriverResults.get(i).getName(), results.get(i).getName());
            assertEquals(weightedDriverResults.get(i).getWeight(), results.get(i).getWeight());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyDriversToWeightedDrivers() {
        final List<String> driverNames = IntStream.range(1, 33).mapToObj(String::valueOf).collect(Collectors.toList());
        DecisionTreeRuleSet.convertNamesToWeightedDrivers(driverNames);
    }

    private InputDriver[] getInputDriverArray() {
        return new InputDriver[]{new StringDriver("input1"), new StringDriver("input2")};
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testingImmutableValueGroups() {
        final DecisionTreeRuleSet commisssionRuleSet = CommisionRuleSetSupplier.getCommissionRuleSetWithRegex().build();
        commisssionRuleSet.getValueGroups().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testingImmutableDrivers() {
        final DecisionTreeRuleSet commisssionRuleSet = CommisionRuleSetSupplier.getCommissionRuleSetWithRegex().build();
        commisssionRuleSet.getDriverNames().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testingImmutableEvaluationNames() {
        final DecisionTreeRuleSet commisssionRuleSet = CommisionRuleSetSupplier.getCommissionRuleSetWithRegex().build();
        commisssionRuleSet.getEvaluationNames().clear();
    }

    @Test
    public void testFindingInputDrivers() {
        final DecisionTreeRuleSet commisssionRuleSet = CommisionRuleSetSupplier.getCommissionRuleSetWithRegex().build();
        List<InputDriver> driversByType = commisssionRuleSet.getDriversByType(InputValueType.STRING);
        assertNotNull(driversByType);
        assertEquals(11, driversByType.size());
        assertThat(driversByType,
                IsCollectionContaining.hasItems(new StringDriver("VOICE"), new StringDriver("RATE"),
                        new StringDriver("UK"), new StringDriver("*"), new StringDriver("CME"),
                        new StringDriver("INDEX"), new StringDriver("S&P"),
                        new StringDriver("US"), new StringDriver("ED"), new StringDriver("NDK")));

        driversByType = commisssionRuleSet.getDriversByType(InputValueType.REGEX);
        assertNotNull(driversByType);
        assertEquals(3, driversByType.size());
        assertThat(driversByType, IsCollectionContaining.hasItems(new RegexDriver("AP.?C"),
                new RegexDriver("C.?E"), new RegexDriver("^[A-Z]{1,2}[A-Z][0-9]{1,2}$")));
    }

    @Test
    public void testMergingRules() {
        final DecisionTreeRuleSet ruleset1 = CommisionRuleSetSupplier.getSimpleTestRuleSet(5000, 0, 10,
                new int[]{500, 500, 500, 500, 500, 500, 500, 500, 500, 500}, 0).build();
        final DecisionTreeRuleSet ruleset2 = CommisionRuleSetSupplier.getSimpleTestRuleSet(5, 75, 10,
                new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 0).build();
        final TreeChange updateChange = new TreeChange() {
            @Override
            public Map<UUID, Optional<TreeRule>> getRules() {
                final HashMap<UUID, Optional<TreeRule>> rules = new HashMap<>();
                for (final Map.Entry<UUID, DecisionTreeRule> entry : ruleset2.getRules().entrySet()) {
                    rules.put(entry.getKey(), Optional.of(entry.getValue()));
                }
                return rules;
            }
        };

        final TreeChange deleteChange = new TreeChange() {
            @Override
            public Map<UUID, Optional<TreeRule>> getRules() {
                final HashMap<UUID, Optional<TreeRule>> rules = new HashMap<>();
                for (final Map.Entry<UUID, DecisionTreeRule> entry : ruleset2.getRules().entrySet()) {
                    rules.put(entry.getKey(), Optional.empty());
                }
                return rules;
            }
        };

        ruleset1.updateRules(updateChange);

        // Check that all the ruleset2 rules are now in ruleset1
        for (final DecisionTreeRule rule : ruleset2.getRules().values()) {
            final DecisionTreeRule otherRule = ruleset1.getRules().get(rule.getRuleIdentifier());
            assertTrue("rule " + rule.getRuleIdentifier() + " data " +
                            Arrays.asList(otherRule.getDrivers()) + " not changed to " +
                            Arrays.asList(rule.getDrivers()),
                    rule.isDuplicateInputData(otherRule));
        }

        // Now remove them
        final Map<UUID, DecisionTreeRule> deleteSet = new HashMap<>();
        ruleset2.getRules().entrySet().forEach(entity -> deleteSet.put(entity.getKey(), null));

        ruleset1.updateRules(deleteChange);

        // Check that all the deleteset rules are deleted
        ruleset2.getRules().keySet().forEach(ruleId -> assertFalse(ruleset1.getRules().containsKey(ruleId)));
    }

    @Test
    public void testUpdatingValueGroups() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        final Set<ValueGroup> groups = ruleSet.getValueGroups();
        final ValueGroup group = groups.iterator().next();
        InputDriver inputDriver = ruleSet.getDriverCache().get(group.getId().toString(),
                InputValueType.VALUE_GROUP);

        assertThat(group.getValues(), Matchers.containsInAnyOrder("CME", "CBOT"));
        assertThat(Arrays.stream(((GroupDriver) inputDriver).getSubDrivers(false))
                        .map(InputDriver::getValue).collect(Collectors.toList()),
                Matchers.containsInAnyOrder("CME", "CBOT"));

        final TreeChange updateChange = new TreeChange() {
            @Override
            public Map<UUID, Optional<TreeValueGroup>> getGroups() {
                group.updateValues(Arrays.asList("CME", "CBOT", "NYMEX"));
                return Collections.singletonMap(group.getId(), Optional.of(group));
            }
        };

        ruleSet.updateRules(updateChange);

        inputDriver = ruleSet.getDriverCache().get(group.getId().toString(),
                InputValueType.VALUE_GROUP);

        assertThat(group.getValues(), Matchers.containsInAnyOrder("CME", "CBOT", "NYMEX"));
        assertThat(Arrays.stream(((GroupDriver) inputDriver).getSubDrivers(false))
                        .map(InputDriver::getValue).collect(Collectors.toList()),
                Matchers.containsInAnyOrder("CME", "CBOT", "NYMEX"));
    }
}