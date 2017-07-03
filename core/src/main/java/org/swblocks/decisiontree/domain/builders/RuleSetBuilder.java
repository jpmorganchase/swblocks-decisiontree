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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.Result;

/**
 * RuleSetBuilder is the domain builder for the {@link DecisionTreeRuleSet} class.
 *
 * <p>It is designed to be used with the general {@link Builder} class, but can be executed on its own.
 *
 * <p>Example usage creating a DecisionTreeRuleSet
 * <blockquote><pre>
 *  final Builder&lt;RuleSetBuilder, DecisionTreeRuleSet&gt; builder =
 *      RuleSetBuilder.creator(Arrays.asList("driver1", "driver2", "driver3"));
 *  ruleSetBuilder.with(RuleSetBuilder::setName, "simpleRuleSet");
 *
 *  // first rule
 *  ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
 *      .with(RuleBuilder::setId, new UUID(0, 3))
 *      .with(RuleBuilder::setCode, new UUID(0, 3))
 *      .with(RuleBuilder::input, Arrays.asList("input1", "input2", "input3"))
 *      .with(RuleBuilder::output, Collections.singletonList("outputDriver:result1")));
 *
 *  // second rule
 *  ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
 *      .with(RuleBuilder::setId, new UUID(0, 5))
 *      .with(RuleBuilder::setCode, new UUID(0, 5))
 *      .with(RuleBuilder::input, Arrays.asList("input4", "input5", "input6"))
 *      .with(RuleBuilder::output, Collections.singletonList("outputDriver:result2")));
 *
 *  final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
 * </pre></blockquote>
 */
public final class RuleSetBuilder {
    private final List<String> driverNames;
    private final List<Builder<RuleBuilder, DecisionTreeRule>> ruleBuilders = new ArrayList<>(1);
    private final Set<ValueGroup> groups = ConcurrentHashMap.newKeySet();
    private DriverCache cache = new DriverCache();
    private String name;

    private RuleSetBuilder(final String name, final List<String> driverNames) {
        this.name = name;
        this.driverNames = new ArrayList<>(driverNames);
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the {@link
     * DecisionTreeRuleSet} as the domain result.
     *
     * @param driverNames list of input driver names
     * @return Instance of the Builder to create and build.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> creator(final List<String> driverNames) {
        return creator("", driverNames);
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the {@link
     * DecisionTreeRuleSet} as the domain result.
     *
     * @param name        name of the ruleset
     * @param driverNames list of input driver names
     * @return Instance of the Builder to create and build.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> creator(final String name,
                                                                       final List<String> driverNames) {
        return Builder.instanceOf(RuleSetBuilder.create(name, driverNames), RuleSetBuilder::builds);
    }

    /**
     * Static build method to generate a {@link DecisionTreeRuleSet} from this domain builder class.
     *
     * @param builder domain builder object to use to build the result
     * @return The generated {@link DecisionTreeRuleSet}
     */
    private static DecisionTreeRuleSet builds(final RuleSetBuilder builder) {
        RuleSetBuilder.addValueGroupsToDriverCache(builder.cache, builder.groups);

        Map<UUID, DecisionTreeRule> rules =
                builder.getRuleBuilders().stream().map(Builder::build).collect(
                        Collectors.toConcurrentMap(DecisionTreeRule::getRuleIdentifier, r -> r));

        if (builder.groups.isEmpty() &&
                CollectionUtils.isNotEmpty(builder.cache.findByInputDriverType(InputValueType.VALUE_GROUP))) {
            rules = correctMissingValueGroups(builder, rules);
        }
        return new DecisionTreeRuleSet(builder.name, rules, builder.driverNames, builder.cache, builder.groups);
    }

    /**
     * Static method to identify any GroupDrivers which were found in the rules, but were not passed in as part of the
     * pre-populated {@link #groups}.
     *
     * @param builder domain builder object to use to build the result.
     * @param rules   initial construction of the rules.
     * @return New map of rules to be used to generate the {@link DecisionTreeRuleSet}
     */
    private static Map<UUID, DecisionTreeRule> correctMissingValueGroups(final RuleSetBuilder builder,
                                                                         final Map<UUID, DecisionTreeRule> rules) {
        final Map<String, String> oldToNewValueGroupNames = new HashMap<>();

        for (final InputDriver inputDriver : builder.cache.findByInputDriverType(InputValueType.VALUE_GROUP)) {
            final ValueGroup valueGroup = new ValueGroup(getIdOrNewUuid(inputDriver), inputDriver.getValue(),
                    ((GroupDriver) inputDriver).convertDrivers(), ValueGroup.DEFAULT_DATE_RANGE);
            oldToNewValueGroupNames.put(inputDriver.getValue(), GroupDriver.VG_PREFIX + valueGroup.getId().toString());
            builder.groups.add(valueGroup);
        }
        RuleSetBuilder.addValueGroupsToDriverCache(builder.cache, builder.groups);

        builder.getRuleBuilders().clear();
        for (final DecisionTreeRule rule : rules.values()) {
            final List<String> newInputs = new ArrayList<>(rule.getDrivers().length);

            for (final InputDriver inputDriver : rule.getDrivers()) {
                final String newMapping = oldToNewValueGroupNames.get(inputDriver.getValue());
                newInputs.add(newMapping != null && InputValueType.VALUE_GROUP.equals(inputDriver.getType()) ?
                        newMapping : inputDriver.getValue());
            }
            builder.rule(RuleBuilder.creator()
                    .with(RuleBuilder::cache, builder.cache)
                    .with(RuleBuilder::input, newInputs)
                    .with(RuleBuilder::output, rule.getOutputs())
                    .with(RuleBuilder::setId, rule.getRuleIdentifier())
                    .with(RuleBuilder::setCode, rule.getRuleCode())
                    .with(RuleBuilder::start, rule.getStart())
                    .with(RuleBuilder::end, rule.getEnd()));
        }
        return builder.getRuleBuilders().stream().map(Builder::build).collect(
                Collectors.toConcurrentMap(DecisionTreeRule::getRuleIdentifier, r -> r));
    }

    private static UUID getIdOrNewUuid(final InputDriver inputDriver) {
        final Result<UUID> result = parseUuid(inputDriver.getValue());
        return result.isSuccess() ? result.getData() : UUID.randomUUID();
    }

    private static Result<UUID> parseUuid(final String uuid) {
        try {
            return Result.success(UUID.fromString(uuid));
        } catch (final Exception exception) {
            return Result.failure(() -> exception);
        }
    }

    private static Supplier<RuleSetBuilder> create(final String name, final List<String> driverNames) {
        return () -> new RuleSetBuilder(name, driverNames);
    }

    /**
     * Static build method to populate the {@link DriverCache} with the data from the valueGroups map.
     * The valueGroups map is iterated twice, the first time to create all the entries in the cache and
     * the second to ensure the {@link GroupDriver} entries are fully populated with sub groups which may not have
     * been created in the cache the first time round.
     *
     * @param cache       {@link DriverCache}
     * @param valueGroups {@link Collection} Collection of ValueGroupName with {@link List} of {@link String}
     */
    public static void addValueGroupsToDriverCache(final DriverCache cache,
                                                   final Collection<ValueGroup> valueGroups) {
        valueGroups.forEach(valueGroup -> {
            GroupDriver existingDriver = getGroupDriver(cache, valueGroup.getId().toString());
            if (existingDriver == null) {
                existingDriver = new GroupDriver(valueGroup.getId().toString(), Collections.emptyList());
            }

            final List<InputDriver> inputDriversFromStrings = getInputDriversFromStrings(cache, valueGroup);
            existingDriver.setSubValues(inputDriversFromStrings);
            cache.put(existingDriver);
        });

        valueGroups.forEach(valueGroup -> {
            final List<InputDriver> inputDriversFromStrings = getInputDriversFromStrings(cache, valueGroup);
            getGroupDriver(cache, valueGroup.getId().toString())
                    .setSubValues(inputDriversFromStrings);
        });
    }

    private static GroupDriver getGroupDriver(final DriverCache cache, final String key) {
        return (GroupDriver) cache.get(key, InputValueType.VALUE_GROUP);
    }

    private static List<InputDriver> getInputDriversFromStrings(final DriverCache cache,
                                                                final ValueGroup valueGroup) {

        return valueGroup.getValues().stream().map(driver ->
                DomainSerialiser.createInputDriver(driver, cache).get()).collect(Collectors.toList());
    }

    /**
     * Build method to add a rule using the {@link RuleBuilder}.
     *
     * <p>This sets the internal {@link DriverCache} for the rule to the cache for the {@link RuleSetBuilder},
     * overriding any set of the cache on a {@link RuleBuilder}.  This ensures that a single cache is used for all
     * rules.
     *
     * @param ruleBuilderBuilder ruleBuilder for this rule
     */
    public void rule(final Builder<RuleBuilder, DecisionTreeRule> ruleBuilderBuilder) {
        ruleBuilderBuilder.with(RuleBuilder::setAlternativeId, (long) this.ruleBuilders.size());
        ruleBuilderBuilder.with(RuleBuilder::setDriverCount, (long) this.driverNames.size());
        ruleBuilderBuilder.with(RuleBuilder::cache, this.cache);
        this.ruleBuilders.add(ruleBuilderBuilder);
    }

    /**
     * Set the rule set name.
     *
     * @param name the name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Set the driver cache.
     *
     * @param cache the {@code DriverCache}
     */
    public void setCache(final DriverCache cache) {
        this.cache = cache;
    }

    /**
     * Set the collection of value groups used in the builder.
     *
     * <p>This is required where the rule set contains value groups - these are added to the cache initially before the
     * rules are created.
     *
     * @param groups the {@code ValueGroup} collection
     */
    public void groups(final Set<ValueGroup> groups) {
        if (CollectionUtils.isNotEmpty(groups)) {
            this.groups.addAll(groups);
        }
    }

    public List<Builder<RuleBuilder, DecisionTreeRule>> getRuleBuilders() {
        return this.ruleBuilders;
    }
}
