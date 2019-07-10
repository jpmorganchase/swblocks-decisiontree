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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.swblocks.decisiontree.TreeChange;
import org.swblocks.decisiontree.TreeRule;
import org.swblocks.decisiontree.TreeValueGroup;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.jbl.eh.EhSupport;

import static java.util.stream.Collectors.collectingAndThen;

/**
 * DecisionTreeRuleSet is the grouping of {@link DecisionTreeRule} to build a decision tree.
 */
public final class DecisionTreeRuleSet {
    private final String name;
    private final Map<UUID, DecisionTreeRule> rules;
    private final List<WeightedDriver> driverNames;
    private final List<String> evaluationNames;
    private final DriverCache cache;
    private final Map<UUID, ValueGroup> groups;

    /**
     * Constructor to build the Ruleset taking the rules. The rules are passed as a map with the ID and {@link
     * DecisionTreeRule}.
     *
     * <p>This constructor initialises a {@link DriverCache} and a {@link ValueGroup} set. This constructor should only
     * be used in the case of a simple rule set which does not contain any value groups.
     *
     * @param name        Name of the RuleSet
     * @param rules       Map of rules in the RuleSet
     * @param driverNames List of driver names in weighted order, highest first
     */
    DecisionTreeRuleSet(final String name, final Map<UUID, DecisionTreeRule> rules,
                        final List<String> driverNames) {
        this(name, rules, driverNames, Collections.emptyList(), new DriverCache(), null);
    }

    /**
     * Constructor to build the Ruleset taking the rules. The rules are passed as a map with the ID and {@link
     * DecisionTreeRule}.
     *
     * <p>This constructor initialises a {@link DriverCache} and a {@link ValueGroup} set. This constructor should only
     * be used in the case of a simple rule set which does not contain any value groups.
     *
     * @param name            Name of the RuleSet
     * @param rules           Map of rules in the RuleSet
     * @param driverNames     List of driver names in weighted order, highest first
     * @param evaluationNames List of evaluation names used in the RuleSet
     */
    DecisionTreeRuleSet(final String name, final Map<UUID, DecisionTreeRule> rules,
                        final List<String> driverNames, final List<String> evaluationNames) {
        this(name, rules, driverNames, evaluationNames, new DriverCache(), null);
    }

    /**
     * Constructor to build the Ruleset taking the rules. The rules are passed as a map with the ID and {@link
     * DecisionTreeRule}.
     *
     * @param name        Name of the RuleSet
     * @param rules       Map of rules in the RuleSet
     * @param driverNames List of driver names in weighted order, highest first
     * @param evaluationNames List of evaluation names used in the RuleSet
     * @param cache       Cache of {@link InputDriver} used within the {@link DecisionTreeRuleSet}
     * @param valueGroups the collection of {@link ValueGroup}
     */
    public DecisionTreeRuleSet(final String name,
                               final Map<UUID, DecisionTreeRule> rules,
                               final List<String> driverNames,
                               final List<String> evaluationNames,
                               final DriverCache cache,
                               final Set<ValueGroup> valueGroups) {
        this.name = name;
        this.rules = rules;
        this.driverNames = convertNamesToWeightedDrivers(driverNames);
        this.evaluationNames = evaluationNames;
        this.cache = cache;
        groups = valueGroups == null ? new ConcurrentHashMap() :
                valueGroups.stream().collect(Collectors.toMap(ValueGroup::getId, vg -> vg));
    }

    /**
     * Converts an order {@link List} of driver names to a {@link List} of {@link WeightedDriver}.
     *
     * <p>It generates the WeightedDriver list by adding a weight of 2^(size-pos in array) example, driver1, driver2
     * gives 4, 2 as the weights.
     *
     * @param driverNames List of ordered names
     * @return List of {@link WeightedDriver}
     */
    public static List<WeightedDriver> convertNamesToWeightedDrivers(final List<String> driverNames) {
        EhSupport.ensureArg(driverNames.size() < 32, "Maximum number of drivers is 31");
        return IntStream.range(0, driverNames.size())
                .mapToObj(counter -> new WeightedDriver(driverNames.get(counter), 1 << driverNames.size() - counter))
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public String getName() {
        return name;
    }

    public Map<UUID, DecisionTreeRule> getRules() {
        return Collections.unmodifiableMap(rules);
    }

    /**
     * Update the existing rules with new rules and valuegroups defined within a {@link TreeChange}.
     *
     * @param change the {@link TreeChange} containing the changes to apply.
     */
    void updateRules(final TreeChange change) {
        // Update Cache
        RuleSetBuilder.addValueGroupsToDriverCache(cache,
                change.getGroups().values().stream().filter(Optional::isPresent).map(Optional::get)
                        .map(ValueGroup.class::cast).collect(Collectors.toList()));

        for (final Map.Entry<UUID, Optional<TreeValueGroup>> decisionTreeGroupEntry : change.getGroups().entrySet()) {
            if (!decisionTreeGroupEntry.getValue().isPresent()) {
                groups.remove(decisionTreeGroupEntry.getKey());
            } else {
                groups.put(decisionTreeGroupEntry.getKey(), (ValueGroup) decisionTreeGroupEntry.getValue().get());
            }
        }

        for (final Map.Entry<UUID, Optional<TreeRule>> decisionTreeRuleEntry : change.getRules().entrySet()) {
            if (!decisionTreeRuleEntry.getValue().isPresent()) {
                rules.remove(decisionTreeRuleEntry.getKey());
            } else {
                final DecisionTreeRule newRule = (DecisionTreeRule) decisionTreeRuleEntry.getValue().get();
                newRule.replaceDriversFromCache(cache);
                rules.put(decisionTreeRuleEntry.getKey(), newRule);
            }
        }
    }

    /**
     * Returns an {@code Collections.unmodifiableList} of driver names ordered with the highest weighting first.
     *
     * @return List of {@code String} names of the drivers.
     */
    public List<String> getDriverNames() {
        return driverNames.stream().map(WeightedDriver::getName)
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    /**
     * Returns an {@code Collections.unmodifiableList} of evaluation names.
     *
     * @return List of {@code String} names of the possible evaluations
     */
    public List<String> getEvaluationNames() {
        return Collections.unmodifiableList(evaluationNames);
    }

    /**
     * Returns the ordered {@link WeightedDriver} list of drivers.
     *
     * @return Immutable list of weighted drivers.
     */
    public List<WeightedDriver> getWeightedDrivers() {
        return Collections.unmodifiableList(driverNames);
    }

    /**
     * Gets all the {@link InputDriver} in the {@link DecisionTreeRuleSet} of the {@link InputValueType}.
     *
     * @param type {@link InputValueType} type to search for.
     * @return List of matching {@link InputDriver}
     */
    public List<InputDriver> getDriversByType(final InputValueType type) {
        return cache.findByInputDriverType(type);
    }

    /**
     * Gets the list of {@link ValueGroup} types associated with this rule set.
     *
     * @return the collection of value groups
     */
    public Set<ValueGroup> getValueGroups() {
        return groups.values().stream()
                .collect(collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
    }

    public DriverCache getDriverCache() {
        return cache;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final DecisionTreeRuleSet ruleSet = (DecisionTreeRuleSet) other;
        return Objects.equals(name, ruleSet.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "DecisionTreeRuleSet{name='" + name + "'}";
    }

}
