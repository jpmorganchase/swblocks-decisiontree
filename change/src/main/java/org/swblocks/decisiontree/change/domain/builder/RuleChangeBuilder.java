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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.DomainSerialiser;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;

import static java.util.Optional.ofNullable;

/**
 * RuleChangeBuilder builds a list of changes for the domain class {@link RuleChange}.
 *
 * <p>Common logic shared with {@link ValueGroupChangeBuilder} is handled by {@link DomainBuilder}.
 *
 * <p>The builder creates a list of changes that include the original rule as part of the change. It uses the {@link
 * RuleBuilder} to build the amended rules that replace the original rule.
 *
 * <p>If the DecisionTreeRuleSet provided does not contain the ruleCode - a new rule (segment) is created.
 *
 * <p>This should be used as part of the {@link ChangeBuilder} which automatically adds the ruleSet on the
 * {@link RuleChangeBuilder} during the build process.  When used standalone for testing then the ruleSet should be
 * set directly as per the examples below.
 *
 * <p>Example usage to remove a rule segment completely:
 * <blockquote><pre>
 *  public List&lt;RuleChange&gt; deleteRule(final DecisionTreeRuleSet ruleSet,
 *                                     final UUID idToDelete) {
 *      DecisionTreeRule rule = ruleSet.getRules().get(idToDelete);
 *      DateRange range = new DateRange(null, null);
 *
 *      // creates the builder for the given rule code
 *      Builder&lt;RuleChangeBuilder, List&lt;RuleChange&gt;&gt; builder =
 *              RuleChangeBuilder.creator(rule.getRuleCode);
 *
 *      builder.with(RuleChangeBuilder::ruleSet, ruleSet);
 *      builder.with(RuleChangeBuilder::ruleId, rule.getRuleIdentifier());
 *      builder.with(RuleChangeBuilder::changeRange, range);
 *
 *      return builder.builds();
 *  }
 * </pre></blockquote>
 *
 * <p>Example usage to de-activate a rule from a given moment in time.
 * <blockquote><pre>
 *  public List&lt;RuleChange&gt; amendRuleEndDate(final DecisionTreeRuleSet ruleSet,
 *                                           final UUID id,
 *                                           final Instant endTime) {
 *      DecisionTreeRule rule = ruleSet.getRules().get(id);
 *      DateRange range = new DateRange(null, endTime);
 *
 *      // creates the builder for the given rule code
 *      Builder&lt;RuleChangeBuilder, List&lt;RuleChange&gt;&gt; builder =
 *              RuleChangeBuilder.creator(rule.getRuleCode);
 *
 *      builder.with(RuleChangeBuilder::ruleId, rule.getRuleIdentifier());
 *      builder.with(RuleChangeBuilder::changeRange, range);
 *
 *      return builder.builds();
 * }
 * </pre></blockquote>
 *
 * <p>Amendment of a rule segment with the following date ranges does the following:
 * <blockquote><pre>
 *      // amends rule segment to start at myInstant and finish at the original rule segment finish
 *      DateRange range = new DateRange(myInstant, null);
 *
 *      // amend the rule segment so that it has new start and end times
 *      DateRange modified = new DateRange(myStart, myFinish);
 * </pre></blockquote>
 *
 * <p>Alternatively a change may be applied over the rule so that new drivers and outputs are effective over the change
 * period specified. Note that if the rule code does not exist, then a new rule will be created. *
 * <blockquote><pre>
 *  public List&lt;RuleChange&gt; createChange(final DecisionTreeRuleSet ruleSet,
 *                                       final UUID ruleCode,
 *                                       final DateRange range,
 *                                       final List&lt;String&gt; drivers,
 *                                       final List&lt;String&gt; outputs) {
 *      // creates the builder for the given rule code
 *      Builder&lt;RuleChangeBuilder, List&lt;RuleChange&gt;&gt; builder =
 *              RuleChangeBuilder.creator(ruleCode);
 *
 *      builder.with(RuleChangeBuilder::ruleSet, ruleSet);
 *      builder.with(RuleChangeBuilder::changeRange, range);
 *      builder.with(RuleChangeBuilder::input, drivers);
 *      builder.with(RuleChangeBuilder::output, outputs);
 *
 *      return builder.builds();
 *  }
 * </pre></blockquote>
 *
 * <p>Note that outputs and drivers will be required where a new change is created. A new change will not start in any
 * of the existing rule segments.
 *
 * <p>To modify only an existing rule segment with new inputs and outputs, the following code would be used:
 * <blockquote><pre>
 *  public List&lt;RuleChange&gt; createChangesForId(final DecisionTreeRuleSet ruleSet,
 *                                             final UUID ruleId,
 *                                             final List&lt;String&gt; drivers,
 *                                             final List&lt;String&gt; outputs) {
 *      DecisionTreeRule rule = ruleSet.getRules().get(id);
 *      DateRange range = new DateRange(rule.getStart(), rule.getEnd());
 *
 *      // creates the builder for the given rule code
 *      Builder&lt;RuleChangeBuilder, List&lt;RuleChange&gt;&gt; builder =
 *              RuleChangeBuilder.creator(rule.getRuleCode);
 *
 *      builder.with(RuleChangeBuilder::ruleSet, ruleSet);
 *      builder.with(RuleChangeBuilder::ruleId, rule.getRuleIdentifier());
 *      builder.with(RuleChangeBuilder::changeRange, range);
 *      builder.with(RuleChangeBuilder::input, drivers);
 *      builder.with(RuleChangeBuilder::output, outputs);
 *
 *      return builder.builds();
 *  }
 * </pre></blockquote>
 */
public final class RuleChangeBuilder extends DomainBuilder<RuleChange, DecisionTreeRule> {
    private final UUID ruleCode;
    private final List<String> drivers = new ArrayList<>(1);
    private final Map<String, String> outputs = new HashMap<>();
    private List<DecisionTreeRule> rules;
    private long driverCount;
    private DriverCache cache;
    private Set<ValueGroup> groups;
    private DecisionTreeRuleSet ruleSet;

    private RuleChangeBuilder(final UUID ruleCode) {
        EhSupport.ensureArg(ruleCode != null, "No rule code supplied");
        this.ruleCode = ofNullable(ruleCode).orElse(UUID.randomUUID());
    }

    private static Predicate<RuleChangeBuilder> validate() {
        return builder -> builder.ruleSet != null;
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the list of {@link
     * RuleChange} returned as the result.
     *
     * @param ruleCode the rule code that the change is for
     * @return the builder instance
     */
    public static Builder<RuleChangeBuilder, List<RuleChange>> creator(final UUID ruleCode) {
        return Builder.instanceOf(RuleChangeBuilder.create(ruleCode), RuleChangeBuilder.validate(),
                RuleChangeBuilder::builds);
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the list of {@link
     * RuleChange} returned as the result.
     *
     * @param ruleSet  the Ruleset to apply the change into.
     * @param ruleCode the rule code that the change is for
     * @return the builder instance
     */
    public static Builder<RuleChangeBuilder, List<RuleChange>> creator(final DecisionTreeRuleSet ruleSet,
                                                                       final UUID ruleCode) {
        return Builder.instanceOf(RuleChangeBuilder.create(ruleCode), RuleChangeBuilder.validate(),
                RuleChangeBuilder::builds).with(RuleChangeBuilder::ruleSet, ruleSet);
    }

    /**
     * Static build method to return a list of {@link RuleChange} objects as the change result.
     *
     * @param builder the {@link RuleChangeBuilder} domain builder.
     * @return the list of {@link RuleChange}
     */
    private static List<RuleChange> builds(final RuleChangeBuilder builder) {
        builder.driverCount = builder.ruleSet.getDriverNames().size();
        builder.rules = builder.ruleSet.getRules().values().stream().filter(rule ->
                rule.getRuleCode().equals(builder.ruleCode)).collect(Collectors.toList());
        builder.cache = builder.ruleSet.getDriverCache();
        builder.groups = builder.ruleSet.getValueGroups();

        return builds(builder, builder.rules);
    }

    private static Supplier<RuleChangeBuilder> create(final UUID ruleCode) {
        return () -> new RuleChangeBuilder(ruleCode);
    }

    private static void checkGroupDriversValid(final BaseBuilder<RuleChange, DecisionTreeRule> baseBuilder,
                                               final List<String> drivers,
                                               final Instant start,
                                               final Instant end) {
        final RuleChangeBuilder builder = (RuleChangeBuilder) baseBuilder;
        final Set<String> values = drivers.stream().filter(inputDriver -> inputDriver.startsWith(GroupDriver.VG_PREFIX))
                .collect(Collectors.toSet());

        values.forEach(value -> {
            final Optional<ValueGroup> matching = builder.groups.stream().filter(valueGroup ->
                    valueGroup.toString().equals(value)).findFirst();

            EhSupport.ensure(matching.isPresent(), "Found a group driver but no matching value group from rule set");
            EhSupport.ensure(SLICE_IN_SEGMENT.test(
                    new DateRange(start, end), matching.get().getRange()),
                    "Group driver %s is no longer valid for start %s and finish %s",
                    matching.get().getId(), start, end);
        });
    }

    static boolean checkSegmentsMatch(final DecisionTreeRule latestSegment, final DecisionTreeRule createdSegment) {
        return latestSegment.getEnd().equals(createdSegment.getStart()) &&
                latestSegment.getOutputs().equals(createdSegment.getOutputs()) &&
                Arrays.equals(latestSegment.getDrivers(), createdSegment.getDrivers());
    }

    static DecisionTreeRule getMergedRule(final DecisionTreeRule latestSegment,
                                          final DecisionTreeRule createdSegment) {
        return new DecisionTreeRule(latestSegment.getRuleIdentifier(), latestSegment.getRuleCode(),
                latestSegment.getDrivers(), latestSegment.getOutputs(), latestSegment.getStart(),
                createdSegment.getEnd());
    }

    /**
     * Sets the inputs for the rule change.
     *
     * @param drivers List of string inputs.
     * @return this for method chaining.
     */
    public RuleChangeBuilder input(final List<String> drivers) {
        if (CollectionUtils.isNotEmpty(drivers)) {
            this.drivers.addAll(drivers);
        }
        return this;
    }

    /**
     * Sets the outputs for a rule change, each element in the map is a name/value of the output of the rule.
     *
     * @param outputs Map to be assigned as the rule output.
     * @return reference to this for method chaining
     */
    public RuleChangeBuilder output(final Map<String, String> outputs) {
        if (CollectionUtils.isNotEmpty(outputs)) {
            this.outputs.putAll(outputs);
        }
        return this;
    }

    /**
     * Sets the outputs for a rule change.
     *
     * <p>Each element in the list is a name/value of the output of the rule separated by a :
     *
     * @param outputs Map to be assigned as the rule output.
     * @return reference to this for method chaining
     */
    public RuleChangeBuilder output(final List<String> outputs) {
        this.outputs.putAll(DomainSerialiser.convertOutputs(outputs));
        return this;
    }

    /**
     * Sets the period over which a change is effective.
     *
     * <p>If the change range is not specified, it is defaulted to {@link RuleChangeBuilder#range} and will start from
     * now and end in the (distant) future.
     *
     * @param range Effective changeRange for the change.
     * @return reference to this for method chaining
     */
    public RuleChangeBuilder changeRange(final DateRange range) {
        if (range != null) {
            this.range = range;
        }
        return this;
    }

    /**
     * Sets the ruleset to be operated on for the rules sepecified in the {@link RuleChangeBuilder}.
     *
     * <p>This should only be called when using {@link RuleChangeBuilder} outside of a {@link ChangeBuilder}.
     *
     * @param ruleSet Ruleset
     * @return reference to this for method chaining
     */
    public RuleChangeBuilder ruleSet(final DecisionTreeRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        return this;
    }

    /**
     * Sets the rule identifier that will be de-activated or date range changed.
     *
     * @param id the rule identifier
     * @return reference to this for method chaining
     */
    public RuleChangeBuilder ruleId(final UUID id) {
        this.id = id;
        return this;
    }

    @Override
    Optional<DecisionTreeRule> getSegment(final List<DecisionTreeRule> segments, final UUID id) {
        return segments.stream().filter(rule -> rule.getRuleIdentifier().equals(id)).findFirst();
    }

    @Override
    DecisionTreeRule getMergedSegment(final BaseBuilder<RuleChange, DecisionTreeRule> baseBuilder,
                                      final DecisionTreeRule latestSegment,
                                      final DecisionTreeRule createdSegment) {
        checkGroupDriversValid(baseBuilder, DomainSerialiser.convertDrivers(latestSegment.getDrivers()),
                latestSegment.getStart(), createdSegment.getEnd());
        return getMergedRule(latestSegment, createdSegment);
    }

    @Override
    DecisionTreeRule getNewSegmentBasedOnChange(final DomainBuilder<RuleChange, DecisionTreeRule> domainBuilder,
                                                final Instant start,
                                                final Instant end,
                                                final DecisionTreeRule segment) {
        final RuleChangeBuilder builder = (RuleChangeBuilder) domainBuilder;

        final List<String> changeDrivers = new ArrayList<>(1);
        if (builder.drivers.isEmpty()) {
            changeDrivers.addAll(DomainSerialiser.convertDrivers(segment.getDrivers()));
        } else {
            changeDrivers.addAll(builder.drivers);
        }

        final Map<String, String> changeOutputs = new HashMap<>();
        if (builder.outputs.isEmpty()) {
            changeOutputs.putAll(segment.getOutputs());
        } else {
            changeOutputs.putAll(builder.outputs);
        }

        return createRule(builder, changeDrivers, changeOutputs, start, end);
    }

    @Override
    DecisionTreeRule getNewSegmentBasedOnExisting(final DomainBuilder<RuleChange, DecisionTreeRule> domainBuilder,
                                                  final Instant start,
                                                  final Instant end,
                                                  final DecisionTreeRule segment) {

        checkGroupDriversValid(domainBuilder, DomainSerialiser.convertDrivers(segment.getDrivers()), start, end);
        return new DecisionTreeRule(UUID.randomUUID(), segment.getRuleCode(), segment.getDrivers(),
                segment.getOutputs(), start, end);
    }

    @Override
    DecisionTreeRule getNewSegmentFromChangeInput(final DomainBuilder<RuleChange, DecisionTreeRule> domainBuilder,
                                                  final Instant start,
                                                  final Instant end) {
        final RuleChangeBuilder builder = (RuleChangeBuilder) domainBuilder;
        EhSupport.ensure(!(builder.drivers.isEmpty() || builder.outputs.isEmpty()),
                "Drivers and outputs must be provided as a new rule is being created");

        return createRule(builder, this.drivers, this.outputs, start, end);
    }

    @Override
    boolean segmentsMatch(final DecisionTreeRule latestSegment, final DecisionTreeRule createdSegment) {
        return checkSegmentsMatch(latestSegment, createdSegment);
    }

    @Override
    Instant getStart(final DecisionTreeRule rule) {
        return rule.getStart();
    }

    @Override
    Instant getFinish(final DecisionTreeRule rule) {
        return rule.getEnd();
    }

    @Override
    RuleChange getNewChange(final DecisionTreeRule segment) {
        return new RuleChange(Type.NEW, segment);
    }

    @Override
    RuleChange getOriginalChange(final DecisionTreeRule segment) {
        return new RuleChange(Type.ORIGINAL, segment);
    }

    @Override
    void amendInputsAndOutputs(final DomainBuilder<RuleChange, DecisionTreeRule> domainBuilder,
                               final DecisionTreeRule segment) {
        final RuleChangeBuilder builder = (RuleChangeBuilder) domainBuilder;

        if (builder.outputs.isEmpty()) {
            builder.output(segment.getOutputs());
        }

        if (builder.drivers.isEmpty()) {
            builder.input(DomainSerialiser.convertDrivers(segment.getDrivers()));
        }
    }

    @Override
    void checkInputsAndOutputs(final DomainBuilder<RuleChange, DecisionTreeRule> domainBuilder) {
        final RuleChangeBuilder ruleChangeBuilder = (RuleChangeBuilder) domainBuilder;
        EhSupport.ensure(!(ruleChangeBuilder.drivers.isEmpty() && ruleChangeBuilder.outputs.isEmpty()),
                "Inputs and outputs are empty");
    }

    private DecisionTreeRule createRule(final RuleChangeBuilder builder,
                                        final List<String> drivers,
                                        final Map<String, String> outputs,
                                        final Instant start,
                                        final Instant end) {
        checkGroupDriversValid(builder, drivers, start, end);

        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        ruleBuilder.with(RuleBuilder::setId, UUID.randomUUID());
        ruleBuilder.with(RuleBuilder::setCode, builder.ruleCode);
        ruleBuilder.with(RuleBuilder::cache, builder.cache);
        ruleBuilder.with(RuleBuilder::input, drivers);
        ruleBuilder.with(RuleBuilder::setDriverCount, builder.driverCount);
        ruleBuilder.with(RuleBuilder::output, outputs);
        ruleBuilder.with(RuleBuilder::start, start);
        ruleBuilder.with(RuleBuilder::end, end);
        return ruleBuilder.build();
    }
}