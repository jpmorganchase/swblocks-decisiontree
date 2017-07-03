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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;

/**
 * RuleGroupChangeBuilder builds a list of changes for the domain class {@link RuleChange}.
 *
 * <p>The changes are made as a result of changes to the collection of {@link ValueGroup} used in the rule set. The
 * value group changes used are created from the {@link ValueGroupChangeBuilder}.
 *
 * <p>Example code to amend a rule set as a result of a value group change:
 * <blockquote><pre>
 *      public List&lt;RuleChange&gt; amendRuleSet(final DecisionTreeRuleSet ruleSet,
 *                                       final List&lt;ValueGroupChange&gt; changes) {
 *          Builder&lt;RuleGroupChangeBuilder, List&lt;RuleChange&gt;&gt; builder =
 *              = RuleGroupChangeBuilder.creator(changes, ruleSet);
 *
 *          return builder.build();
 *      }
 * </pre></blockquote>
 *
 * <p>The above code is also used when a value group is removed from the rule set. The builder checks that there are no
 * rules using the value group.
 *
 * <p>To add a value group to a rule - the following code would apply:
 * <blockquote><pre>
 *      public List&lt;RuleChange&gt; amendRuleSet(final DecisionTreeRuleSet ruleSet,
 *                                       final List&lt;ValueGroupChange&gt; changes,
 *                                       final List&lt;UUID&gt; ruleCodes,
 *                                       final String driverName) {
 *          Builder&lt;RuleGroupChangeBuilder, List&lt;RuleChange&gt;&gt; builder =
 *              = RuleGroupChangeBuilder.creator(changes, ruleSet);
 *          builder.with(RuleGroupChangeBuilder::ruleCodes, ruleCodes);
 *          builder.with(RuleGroupChangeBuilder::driver, driverName);
 *
 *          return builder.build();
 *      }
 * </pre></blockquote>
 *
 * <p>Changes to ValueGroups that are not used by any rules do not change any rule changes. When building changes, the
 * logic to determine the changes is done by the {@link ChangeBuilder}.
 */
public final class RuleGroupChangeBuilder extends BaseBuilder<RuleChange, DecisionTreeRule> {
    private final Map<UUID, List<DecisionTreeRule>> ruleMap = new HashMap<>();
    private final DriverCache cache = new DriverCache();
    private final List<ValueGroupChange> groupChanges = new ArrayList<>(1);
    private final DecisionTreeRuleSet ruleSet;
    private final Map<UUID, ValueGroup> currentGroups = new HashMap<>();
    private final Set<UUID> ruleCodes = new HashSet<>();

    private Set<ValueGroup> changeGroup;
    private String driverName;

    private RuleGroupChangeBuilder(final List<ValueGroupChange> groupChanges, final DecisionTreeRuleSet ruleSet) {
        EhSupport.ensureArg(CollectionUtils.isNotEmpty(groupChanges), "No value group changes specified");
        EhSupport.ensureArg(ruleSet != null, "No rule set supplied");
        this.groupChanges.addAll(groupChanges);
        this.ruleSet = ruleSet;
    }

    /**
     * Static method creating the {@link Builder} with this class as the domain builder returning a list of
     * {@link RuleChange} as the result.
     *
     * @param groupChanges the collection of value group changes
     * @param ruleSet      the rule set
     * @return the builder instance
     */
    public static Builder<RuleGroupChangeBuilder, List<RuleChange>> creator(final List<ValueGroupChange> groupChanges,
                                                                            final DecisionTreeRuleSet ruleSet) {
        return Builder.instanceOf(RuleGroupChangeBuilder.create(groupChanges, ruleSet), RuleGroupChangeBuilder::builds);
    }

    /**
     * Static build method to return a list of {@link RuleChange} objects as the change result.
     *
     * @param builder the {@link RuleGroupChangeBuilder} domain builder.
     * @return the list of {@link RuleChange}
     */
    private static List<RuleChange> builds(final RuleGroupChangeBuilder builder) {
        // get original value groups from rule set before changes
        final Optional<ValueGroupChange> first = builder.groupChanges.stream().findFirst();
        EhSupport.ensure(first.isPresent(), "No value group change found");
        final String name = first.get().getValueGroup().getName();

        builder.ruleSet.getValueGroups().forEach(valueGroup ->
                builder.currentGroups.put(valueGroup.getId(), valueGroup));
        builder.changeGroup = builder.currentGroups.values().stream().filter(valueGroup ->
                name.equals(valueGroup.getName())).collect(Collectors.toSet());

        final List<ValueGroup> originals = new ArrayList<>(1);
        // remove original from effective rule set groups - a change is required
        builder.groupChanges.forEach(valueGroupChange -> {
            final ValueGroup group = valueGroupChange.getValueGroup();
            if (valueGroupChange.getType() == Type.ORIGINAL) {
                builder.changeGroup.remove(group);
                builder.currentGroups.remove(group.getId());
                originals.add(group);
            } else {
                builder.changeGroup.add(group);
            }
        });

        final List<ValueGroupChange> groupChanges = builder.groupChanges;
        // find the rules that are affected by any removed value groups - ORIGINAL
        final Set<DecisionTreeRule> rules = new HashSet<>(1);
        for (final ValueGroup change : originals) {
            rules.addAll(builder.ruleSet.getRules().values().stream().filter(decisionTreeRule ->
                    Arrays.stream(decisionTreeRule.getDrivers()).anyMatch(inputDriver ->
                            inputDriver.getType() == InputValueType.VALUE_GROUP &&
                                    inputDriver.getValue().equals(change.getId().toString())))
                    .collect(Collectors.toSet()));
        }

        // check for a deletion
        if (groupChanges.size() == 1 && groupChanges.get(0).getType() == Type.ORIGINAL) {
            final ValueGroupChange change = groupChanges.get(0);

            EhSupport.ensure(!CollectionUtils.isNotEmpty(rules), "%s group is active - cannot delete value group",
                    change.getValueGroup().getName());
            return Collections.emptyList();
        }

        RuleSetBuilder.addValueGroupsToDriverCache(builder.cache, builder.changeGroup);

        // rule codes -  for specific rule codes or rules using the value groups
        final Set<UUID> ruleCodes = CollectionUtils.isNotEmpty(builder.ruleCodes) ? builder.ruleCodes :
                rules.stream().map(DecisionTreeRule::getRuleCode).collect(Collectors.toSet());
        // get the rules for each rule code
        ruleCodes.forEach(uuid -> {
            final List<DecisionTreeRule> matching = builder.ruleSet.getRules().values().stream().filter(
                    decisionTreeRule -> decisionTreeRule.getRuleCode().equals(uuid)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(matching)) {
                builder.ruleMap.put(uuid, matching);
            }
        });

        return CollectionUtils.isNotEmpty(builder.ruleMap) ? getRuleChanges(builder) : Collections.emptyList();
    }

    private static List<RuleChange> getRuleChanges(final RuleGroupChangeBuilder builder) {
        final Set<DecisionTreeRule> removed = new HashSet<>();
        final List<DecisionTreeRule> created = new ArrayList<>();

        for (final Map.Entry<UUID, List<DecisionTreeRule>> entry : builder.ruleMap.entrySet()) {
            final List<DecisionTreeRule> segments = entry.getValue();
            final List<DateRange> slices = getSegmentDateRanges(segments, builder.changeGroup);

            for (final DateRange slice : slices) {
                final Instant start = slice.getStart();
                final Instant end = slice.getFinish();

                // find matching rule
                final Optional<DecisionTreeRule> matching = segments.stream().filter(rule ->
                        SLICE_IN_CHANGE.test(slice, new DateRange(rule.getStart(), rule.getEnd())) ||
                                SLICE_IN_SEGMENT.test(slice, new DateRange(rule.getStart(),
                                        rule.getEnd()))).findFirst();
                if (!matching.isPresent() || builder.changeGroup.stream().noneMatch(valueGroup ->
                        isChangeRequired(builder, valueGroup.getRange(), slice, matching.get()))) {
                    continue;
                }

                final DecisionTreeRule rule = matching.get();
                // replace named driver or to update an existing value group
                final InputDriver[] drivers = getAmendedDrivers(builder, slice, rule);
                if (Arrays.stream(drivers).noneMatch(Objects::isNull)) {
                    removed.add(rule);
                    created.add(new DecisionTreeRule(UUID.randomUUID(), rule.getRuleCode(), drivers, rule.getOutputs(),
                            start, end));
                }
            }
        }

        final List<RuleChange> changes = new ArrayList<>(1);
        removed.forEach(decisionTreeRule -> changes.add(new RuleChange(Type.ORIGINAL, decisionTreeRule)));
        mergeAndCreateNewChanges(builder, created, changes);
        return changes;
    }

    private static InputDriver[] getAmendedDrivers(final RuleGroupChangeBuilder builder,
                                                   final DateRange slice,
                                                   final DecisionTreeRule rule) {
        final InputDriver[] drivers = new InputDriver[rule.getDrivers().length];

        if (CollectionUtils.isNotEmpty(builder.ruleCodes)) {
            final String driverName = builder.driverName;
            final int index = builder.ruleSet.getDriverNames().indexOf(driverName);
            if (index == -1) {
                return drivers;
            }

            for (int i = 0; i < drivers.length; ++i) {
                final InputDriver driver = rule.getDrivers()[i];
                if (index == i) {
                    assignDriverFromValueGroup(builder, slice, drivers, i, driver);
                } else {
                    drivers[i] = driver;
                }
            }
        } else {
            final List<InputDriver> groupDrivers = Arrays.stream(rule.getDrivers()).filter(inputDriver ->
                    inputDriver.getType() == InputValueType.VALUE_GROUP &&
                            !builder.currentGroups.containsKey(UUID.fromString(inputDriver.getValue())))
                    .collect(Collectors.toList());
            // new value group has been found - so get the drivers
            if (!groupDrivers.isEmpty()) {
                assignDriversForNewValueGroup(builder, slice, rule, drivers);
            }
        }

        return drivers;
    }

    private static void assignDriversForNewValueGroup(final RuleGroupChangeBuilder builder,
                                                      final DateRange slice,
                                                      final DecisionTreeRule rule,
                                                      final InputDriver[] drivers) {
        for (int i = 0; i < drivers.length; ++i) {
            final InputDriver driver = rule.getDrivers()[i];
            if (driver.getType() == InputValueType.VALUE_GROUP &&
                    !builder.currentGroups.containsKey(UUID.fromString(driver.getValue()))) {
                // value group has been changed - find matching value group....
                assignDriverFromValueGroup(builder, slice, drivers, i, driver);
            } else {
                drivers[i] = driver;
            }
        }
    }

    private static void assignDriverFromValueGroup(final RuleGroupChangeBuilder builder,
                                                   final DateRange slice,
                                                   final InputDriver[] drivers,
                                                   final int driverIndex,
                                                   final InputDriver driver) {
        for (final ValueGroup valueGroup : builder.changeGroup) {
            if (SLICE_IN_SEGMENT.test(slice, valueGroup.getRange()) ||
                    SLICE_IN_CHANGE.test(slice, valueGroup.getRange())) {
                drivers[driverIndex] = builder.cache.get(valueGroup.getId().toString(), InputValueType.VALUE_GROUP);
                return;
            }
        }

        drivers[driverIndex] = driver;
    }

    private static Supplier<RuleGroupChangeBuilder> create(final List<ValueGroupChange> groupChanges,
                                                           final DecisionTreeRuleSet ruleSet) {
        return () -> new RuleGroupChangeBuilder(groupChanges, ruleSet);
    }

    private static List<DateRange> getSegmentDateRanges(final List<DecisionTreeRule> rules,
                                                        final Set<ValueGroup> changeGroup) {
        final Set<Instant> times = new TreeSet<>();
        rules.forEach(rule -> {
            times.add(rule.getStart());
            times.add(rule.getEnd());
        });
        changeGroup.forEach(group -> {
            times.add(group.getRange().getStart());
            times.add(group.getRange().getFinish());
        });

        final List<Instant> ordered = new ArrayList<>(times);
        final List<DateRange> slices = new ArrayList<>(1);
        for (int i = 0; i < times.size() - 1; ++i) {
            final DateRange dateTimeSlice = new DateRange(ordered.get(i), ordered.get(i + 1));
            slices.add(dateTimeSlice);
        }
        return slices;
    }

    /**
     * Set the rule set driver name that will have the value group changed.
     *
     * @param driver the rule set driver
     * @return reference to this builder for method chaining
     */
    public RuleGroupChangeBuilder driver(final String driver) {
        this.driverName = driver;
        return this;
    }

    /**
     * Set the rule codes for the rules to be changed.
     *
     * @param ruleCodes the rule code collection
     * @return reference to this builder for method chaining
     */
    public RuleGroupChangeBuilder ruleCodes(final Set<UUID> ruleCodes) {
        if (CollectionUtils.isNotEmpty(ruleCodes)) {
            this.ruleCodes.addAll(ruleCodes);
        }

        return this;
    }

    @Override
    RuleChange getNewChange(final DecisionTreeRule segment) {
        return new RuleChange(Type.NEW, segment);
    }

    @Override
    boolean segmentsMatch(final DecisionTreeRule latestSegment, final DecisionTreeRule createdSegment) {
        return RuleChangeBuilder.checkSegmentsMatch(latestSegment, createdSegment);
    }

    @Override
    DecisionTreeRule getMergedSegment(final BaseBuilder<RuleChange, DecisionTreeRule> builder,
                                      final DecisionTreeRule latestSegment,
                                      final DecisionTreeRule createdSegment) {
        return RuleChangeBuilder.getMergedRule(latestSegment, createdSegment);
    }

    @Override
    Instant getStart(final DecisionTreeRule segment) {
        return segment.getStart();
    }

    @Override
    Instant getFinish(final DecisionTreeRule segment) {
        return segment.getEnd();
    }
}
