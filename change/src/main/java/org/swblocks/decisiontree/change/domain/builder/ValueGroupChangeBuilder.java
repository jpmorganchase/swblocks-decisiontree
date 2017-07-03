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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;

/**
 * ValueGroupChangeBuilder builds a list of changes for the for the domain class {@link ValueGroupChange}.
 *
 * <p>Common logic shared with {@link RuleChangeBuilder} is handled by {@link DomainBuilder}.
 *
 * <p>The {@link Builder} is used to create a list of changes that includes the original change and the new changes.
 *
 * <p>If the {@link ValueGroupChangeBuilder#valueGroups} is empty, a new value group will be created.
 *
 * <p>Example usage to remove a value group segment completely:
 * <blockquote><pre>
 *  public List&lt;ValueGroupChange&gt; deleteValueGroup(final String name,
 *                                                  final UUID id,
 *                                                  final List&lt;ValueGroup&gt; groups) {
 *      Builder&lt;ValueGroupChangeBuilder&gt; builder =
 *          ValueGroupChangeBuilder.creator(name);
 *
 *      builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(null, null));
 *      builder.with(ValueGroupChangeBuilder::valueGroups, this.valueGroups);
 *      builder.with(ValueGroupChangeBuilder::id, id);
 *
 *      return builder.builds();
 *  }
 * </pre></blockquote>
 *
 * <p>Example usage to de-activate a value group segment from a given moment:
 * <blockquote><pre>
 *  public List&lt;ValueGroupChange&gt; amendEndDate(final String name,
 *                                             final UUID id,
 *                                             final List&lt;ValueGroup&gt; groups,
 *                                             final Instant endTime) {
 *      Builder&lt;ValueGroupChangeBuilder&gt; builder =
 *          ValueGroupChangeBuilder.creator(name);
 *
 *      builder.with(ValueGroupChangeBuilder::changeRange, new DateRange(null, endTime));
 *      builder.with(ValueGroupChangeBuilder::valueGroups, this.valueGroups);
 *      builder.with(ValueGroupChangeBuilder::id, id);
 *
 *      return builder.builds();
 *  }
 * </pre></blockquote>
 *
 * <p>Amendment of a value group segment by id with the following date ranges has the following behaviour:
 * <blockquote><pre>
 *      // amends the segment to start at myInstant and finish at the original value group segment finish
 *      DateRange range = new DateRange(myInstant, null);
 *
 *      // amend the value group segment so that it has new start and end times
 *      DateRange modified = new DateRange(myStart, myFinish);
 * </pre></blockquote>
 *
 * <p>Alternatively a change may be applied over the value group segments that the new drivers are effective over the
 * change period specified. If the value group does not exist, then a new value group will be created.
 * <blockquote><pre>
 * public List&lt;ValueGroupChange&gt; createChange(final String name,
 *                                            final List&lt;ValueGroup&gt; groups,
 *                                            final DateRange changeRange
 *                                            final List&lt;ValueGroup&gt; drivers) {
 *      Builder&lt;ValueGroupChangeBuilder&gt; builder =
 *          ValueGroupChangeBuilder.creator(name);
 *
 *      builder.with(ValueGroupChangeBuilder::changeRange, changeRange);
 *      builder.with(ValueGroupChangeBuilder::valueGroups, this.valueGroups);
 *      builder.with(ValueGroupChangeBuilder::drivers, drivers);
 *
 *      return builder.builds();
 * }
 * </pre></blockquote>
 *
 * <p>If a new value group segment is being created, then the drivers must be provided. If the value group segment id
 * and drivers are provided as input to the builder, the drivers for the specific segment will be replacecd.
 */
public final class ValueGroupChangeBuilder extends DomainBuilder<ValueGroupChange, ValueGroup> {
    private final String valueGroupName;
    private final List<ValueGroup> valueGroups = new ArrayList<>(1);
    private final List<UUID> ruleCodes = new ArrayList<>(1);
    private final List<String> drivers = new ArrayList<>(1);
    private String driverName;
    private Type type;

    private ValueGroupChangeBuilder(final String valueGroupName) {
        EhSupport.ensureArg(valueGroupName != null && !valueGroupName.trim().isEmpty(),
                "Value group name must be provided");
        this.valueGroupName = valueGroupName;
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder returning a list of {@link
     * ValueGroupChange} representing the change.
     *
     * <p>Initialises the builder with the value groups that correspond to the name.
     *
     * @param valueGroupName the name of the value group
     * @return the builder instance
     */
    public static Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> creator(final String valueGroupName) {
        return Builder.instanceOf(ValueGroupChangeBuilder.create(valueGroupName),
                ValueGroupChangeBuilder::builds);
    }

    private static Supplier<ValueGroupChangeBuilder> create(final String valueGroupName) {
        return () -> new ValueGroupChangeBuilder(valueGroupName);
    }

    /**
     * Static method that builds the change returning the list of {@link ValueGroupChange}.
     *
     * <p>When the type is {@link Type#NONE}, the builder returns a list of the current value groups with a change type
     * of NONE. This allows existing value groups to be applied to a rule set in conjunction with the
     * {@link RuleGroupChangeBuilder}.
     *
     * @param builder the builder used in creating the change
     * @return the list of {@link ValueGroupChange} representing the change
     */
    private static List<ValueGroupChange> builds(final ValueGroupChangeBuilder builder) {
        if (Type.NONE == builder.type) {
            EhSupport.ensure(CollectionUtils.isNotEmpty(builder.valueGroups), "No value groups specified");
            EhSupport.ensure(builder.driverName != null && !builder.ruleCodes.isEmpty(),
                    "Driver name and rule set codes have not been specified");

            final List<ValueGroupChange> changes = new ArrayList<>(builder.valueGroups.size());
            builder.valueGroups.forEach(valueGroup -> changes.add(new ValueGroupChange(Type.NONE,
                    builder.getValueGroup(builder, valueGroup.getId(), valueGroup.getName(), valueGroup.getValues(),
                            valueGroup.getRange()))));
            return changes;
        }

        return builds(builder, builder.valueGroups);
    }

    /**
     * Sets the ruleset groups for the change.
     *
     * @param ruleSet Current active value groups that will be changed
     * @return this builder
     */
    public ValueGroupChangeBuilder ruleSet(final DecisionTreeRuleSet ruleSet) {
        EhSupport.ensureArg(ruleSet != null, "RuleSet must not be null");
        this.valueGroups.addAll(ruleSet.getValueGroups().stream().filter(
                valueGroup -> valueGroup.getName().equals(this.valueGroupName)).collect(Collectors.toList()));
        return this;
    }

    /**
     * Sets the new drivers that will be effective from the change time.
     *
     * @param drivers the new value group drivers
     * @return this builder
     */
    public ValueGroupChangeBuilder drivers(final List<String> drivers) {
        EhSupport.ensureArg(CollectionUtils.isNotEmpty(drivers), "Drivers must not be null or empty");
        this.drivers.addAll(drivers);
        return this;
    }

    /**
     * Sets the period over which a change is effective.
     *
     * <p>If the change range is not specified, it is defaulted to {@link ValueGroupChangeBuilder#range} and will start
     * from now and end in the (distant) future.
     *
     * @param range Effective changeRange for the change.
     */
    public void changeRange(final DateRange range) {
        if (range != null) {
            this.range = range;
        }
    }

    /**
     * Sets the value group id that will be de-activated or have the date range changed.
     *
     * @param id the value group id
     * @return reference to this for method chaining
     */
    public ValueGroupChangeBuilder id(final UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Set the name of the driver that will be updated if value group created is a new value group.
     *
     * @param driverName the driver name that will be updated in the {@link DecisionTreeRule}
     * @return the name of the driver
     */
    public ValueGroupChangeBuilder driver(final String driverName) {
        this.driverName = driverName;
        return this;
    }

    /**
     * Set the list of {@link DecisionTreeRule#ruleCode} that value groups are applied to.
     *
     * @param ruleCodes list of rule codes that the change applies to for a new value group
     * @return this builder
     */
    public ValueGroupChangeBuilder ruleCodes(final List<UUID> ruleCodes) {
        if (CollectionUtils.isNotEmpty(ruleCodes)) {
            this.ruleCodes.addAll(ruleCodes);
        }
        return this;
    }

    /**
     * Set the type of change required - if set to {@link Type#NONE} then returns the existing groups.
     *
     * @param type the type of change
     * @return this builder
     */
    public ValueGroupChangeBuilder changeType(final Type type) {
        if (type != null) {
            this.type = type;
        }
        return this;
    }

    @Override
    Instant getStart(final ValueGroup valueGroup) {
        return valueGroup.getRange().getStart();
    }

    @Override
    Instant getFinish(final ValueGroup valueGroup) {
        return valueGroup.getRange().getFinish();
    }

    @Override
    Optional<ValueGroup> getSegment(final List<ValueGroup> segments, final UUID id) {
        return segments.stream().filter(group -> group.getId().equals(id)).findFirst();
    }

    @Override
    ValueGroup getMergedSegment(final BaseBuilder<ValueGroupChange, ValueGroup> baseBuilder,
                                final ValueGroup latestSegment,
                                final ValueGroup createdSegment) {
        final ValueGroupChangeBuilder builder = (ValueGroupChangeBuilder) baseBuilder;
        return getValueGroup(builder, latestSegment.getId(), latestSegment.getName(), latestSegment.getValues(),
                new DateRange(latestSegment.getRange().getStart(), createdSegment.getRange().getFinish()));
    }

    @Override
    ValueGroup getNewSegmentBasedOnChange(final DomainBuilder<ValueGroupChange, ValueGroup> domainBuilder,
                                          final Instant start,
                                          final Instant end,
                                          final ValueGroup segment) {
        final ValueGroupChangeBuilder builder = (ValueGroupChangeBuilder) domainBuilder;
        return getValueGroup(builder, UUID.randomUUID(), segment.getName(),
                builder.drivers, new DateRange(start, end));
    }

    @Override
    ValueGroup getNewSegmentBasedOnExisting(final DomainBuilder<ValueGroupChange, ValueGroup> domainBuilder,
                                            final Instant start,
                                            final Instant end,
                                            final ValueGroup segment) {
        final ValueGroupChangeBuilder builder = (ValueGroupChangeBuilder) domainBuilder;
        return getValueGroup(builder, UUID.randomUUID(), segment.getName(), segment.getValues(),
                new DateRange(start, end));
    }

    @Override
    ValueGroup getNewSegmentFromChangeInput(final DomainBuilder<ValueGroupChange, ValueGroup> domainBuilder,
                                            final Instant start,
                                            final Instant end) {
        final ValueGroupChangeBuilder builder = (ValueGroupChangeBuilder) domainBuilder;
        return getValueGroup(builder, UUID.randomUUID(), builder.valueGroupName,
                builder.drivers, new DateRange(start, end));
    }

    @Override
    boolean segmentsMatch(final ValueGroup latestSegment, final ValueGroup createdSegment) {
        return latestSegment.getRange().getFinish().equals(createdSegment.getRange().getStart()) &&
                latestSegment.getValues().equals(createdSegment.getValues());
    }

    @Override
    ValueGroupChange getNewChange(final ValueGroup segment) {
        return new ValueGroupChange(Type.NEW, segment);
    }

    @Override
    ValueGroupChange getOriginalChange(final ValueGroup valueGroup) {
        return new ValueGroupChange(Type.ORIGINAL, valueGroup);
    }

    @Override
    void amendInputsAndOutputs(final DomainBuilder<ValueGroupChange, ValueGroup> domainBuilder,
                               final ValueGroup segment) {
        final ValueGroupChangeBuilder builder = (ValueGroupChangeBuilder) domainBuilder;
        if (!CollectionUtils.isNotEmpty(builder.drivers)) {
            builder.drivers(segment.getValues());
        }
    }

    @Override
    void checkInputsAndOutputs(final DomainBuilder<ValueGroupChange, ValueGroup> domainBuilder) {
        final ValueGroupChangeBuilder builder = (ValueGroupChangeBuilder) domainBuilder;
        EhSupport.ensure(CollectionUtils.isNotEmpty(builder.drivers),
                "No value group drivers supplied");
    }

    private ValueGroup getValueGroup(final ValueGroupChangeBuilder builder,
                                     final UUID id,
                                     final String name,
                                     final List<String> values,
                                     final DateRange range) {
        final ValueGroup group = new ValueGroup(id, name, values, range);

        if (builder.driverName != null) {
            group.setNewRuleData(builder.driverName, builder.ruleCodes);
        }
        return group;
    }
}
