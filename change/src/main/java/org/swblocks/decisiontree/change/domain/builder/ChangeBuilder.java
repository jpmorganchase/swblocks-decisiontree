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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.change.domain.Audit;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;

/**
 * ChangeBuilder builds the list of {@link RuleChange} objects and list of {@link ValueGroup} that comprise a single
 * change. The changes are encapsulated in a {@link Change} object.
 *
 * <p>The {@link ChangeBuilder#activationTime} is the time at which the change is activated. The {@link
 * ChangeBuilder#range} is used the define the period over which the change is applied.
 *
 * <p>Example usage creating a {@link RuleChange} list for a {@link DecisionTreeRule}:
 * <blockquote><pre>
 *  final Builder&lt;ChangeBuilder, Change&gt; builder = ChangeBuilder.creator(MyRuleSet);
 *
 *  Instant now = Instant.now();
 *  // Create audit object with initiator only...
 *  builder.with(ChangeBuilder::audit, new Audit("USER1", now, null, null));
 *  builder.with(ChangeBuilder::changeRange, new DateRange(now, now.plus(Period.ofWeeks(4))));
 *
 *  builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(myRuleSet, new UUID(0, 2))
 *      .with(RuleChangeBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "RATE"))
 *      .with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.0")));
 *
 *  final Change change = builder.build();
 * </pre></blockquote>
 *
 * <p>Example usage creating a {@link ValueGroupChange} list for a {@link ValueGroup}.
 * <blockquote><pre>
 *  final Builder&lt;ChangeBuilder, Change&gt; builder = ChangeBuilder.creator(MyRuleSet);
 *
 *  Instant now = Instant.now();
 *  // Create audit object with initiator only...
 *  builder.with(ChangeBuilder::audit, new Audit("USER1", now, null, null));
 *  builder.with(ChangeBuilder::changeRange, new DateRange(now, now.plus(Period.ofWeeks(4))));
 *
 *  builder.with(ChangeBuilder::valueGroupChange, ValueGroupChangeBuilder.creator("VG:ValueGroup")
 *      .with(ValueGroupChangeBuilder::valueGroups, myValueGroups)
 *      .with(ValueGroupChangeBuilder::drivers, "driver1:driver2:driver3"));
 *
 *  final Change change = builder.build();
 * </pre></blockquote>
 */
public final class ChangeBuilder {
    private final List<Builder<RuleChangeBuilder, List<RuleChange>>> ruleChangeBuilders = new ArrayList<>(1);
    private final List<Builder<ValueGroupChangeBuilder, List<ValueGroupChange>>> valueGroupChangeBuilders =
            new ArrayList<>(1);
    private final DecisionTreeRuleSet ruleSet;
    private UUID id = UUID.randomUUID();
    private Instant activationTime;
    private Audit audit;
    private DateRange range = new DateRange(Instant.now(), DecisionTreeRule.MAX);

    private ChangeBuilder(final DecisionTreeRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    private static Supplier<ChangeBuilder> create(final DecisionTreeRuleSet ruleSet) {
        return () -> new ChangeBuilder(ruleSet);
    }

    private static Predicate<ChangeBuilder> validate() {
        return builder -> builder.ruleSet != null;
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the {@link Change} class
     * returned as the result.
     *
     * @param ruleSet the ruleSet being changed
     * @return the {@link Builder} used to build the {@link Change}
     */
    public static Builder<ChangeBuilder, Change> creator(final DecisionTreeRuleSet ruleSet) {
        return Builder.instanceOf(ChangeBuilder.create(ruleSet), ChangeBuilder.validate(),
                ChangeBuilder::builds);
    }

    /**
     * Static method that builds the change object.
     *
     * @param builder the builder
     * @return the {@link Change}
     */
    private static Change builds(final ChangeBuilder builder) {
        final Set<RuleChange> ruleChanges = new HashSet<>(1);
        builder.ruleChangeBuilders.forEach(ruleChangeBuilder -> {
            final List<RuleChange> changes = ruleChangeBuilder.build();
            checkIfChangeExistsForRule(ruleChanges, changes);
            ruleChanges.addAll(changes);
        });

        final Set<ValueGroupChange> valueGroupChanges = new HashSet<>(1);

        builder.valueGroupChangeBuilders.forEach(groupChangeBuilder -> {
            final List<ValueGroupChange> groupChanges = groupChangeBuilder.build();
            checkIfChangeExistsForValueGroup(valueGroupChanges, groupChanges);
            final Builder<RuleGroupChangeBuilder, List<RuleChange>> ruleGroupBuilder =
                    RuleGroupChangeBuilder.creator(groupChanges, builder.ruleSet);

            final Optional<ValueGroupChange> groupChange =
                    groupChanges.stream().filter(valueGroupChange ->
                            valueGroupChange.getValueGroup().getDriverName() != null &&
                                    !valueGroupChange.getValueGroup().getRuleCodes().isEmpty()).findFirst();

            final boolean noValueGroupChanges = groupChanges.stream().anyMatch(valueGroupChange ->
                    valueGroupChange.getType() == Type.NONE);

            if (noValueGroupChanges) {
                EhSupport.ensure(groupChange.isPresent(), "Cannot apply an existing value group without rule data");
            } else {
                valueGroupChanges.addAll(groupChanges);
            }

            if (groupChange.isPresent()) {
                final ValueGroup group = groupChange.get().getValueGroup();
                ruleGroupBuilder.with(RuleGroupChangeBuilder::driver, group.getDriverName());
                ruleGroupBuilder.with(RuleGroupChangeBuilder::ruleCodes, new HashSet<>(group.getRuleCodes()));
            }

            final List<RuleChange> changes = ruleGroupBuilder.build();
            checkIfChangeExistsForRule(ruleChanges, changes);
            ruleChanges.addAll(changes);
        });

        return new Change(builder.id, builder.ruleSet.getName(), builder.activationTime, builder.range,
                builder.audit, ruleChanges, valueGroupChanges);
    }

    private static void checkIfChangeExistsForRule(final Collection<RuleChange> current,
                                                   final Collection<RuleChange> created) {
        EhSupport.ensure(Collections.disjoint(getRuleCodes(current), getRuleCodes(created)),
                "Multiple rule changes found");
    }

    private static Set<UUID> getRuleCodes(final Collection<RuleChange> changes) {
        return changes.stream().map(ruleChange -> ruleChange.getRule().getRuleCode()).collect(Collectors.toSet());
    }

    private static void checkIfChangeExistsForValueGroup(final Collection<ValueGroupChange> current,
                                                         final Collection<ValueGroupChange> created) {
        EhSupport.ensure(Collections.disjoint(getGroupNames(current), getGroupNames(created)),
                "Multiple group changes found");
    }

    private static Set<String> getGroupNames(final Collection<ValueGroupChange> changes) {
        return changes.stream().map(groupChange -> groupChange.getValueGroup().getName()).collect(Collectors.toSet());
    }

    /**
     * Method to add a {@link RuleChangeBuilder} to the list of rule change builders.
     *
     * @param ruleChangeBuilderBuilder the rule change builder
     * @return this builder
     */
    public ChangeBuilder ruleChange(final Builder<RuleChangeBuilder, List<RuleChange>> ruleChangeBuilderBuilder) {
        ruleChangeBuilderBuilder.with(RuleChangeBuilder::changeRange, this.range);
        ruleChangeBuilderBuilder.with(RuleChangeBuilder::ruleSet, this.ruleSet);
        this.ruleChangeBuilders.add(ruleChangeBuilderBuilder);
        return this;
    }

    /**
     * Method to add a {@link ValueGroupChangeBuilder} to the list of value group change builders.
     *
     * @param valueGroupChangeBuilderBuilder the builder to add
     * @return this builder
     */
    public ChangeBuilder valueGroupChange(final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>>
                                                  valueGroupChangeBuilderBuilder) {
        valueGroupChangeBuilderBuilder.with(ValueGroupChangeBuilder::changeRange, this.range);
        valueGroupChangeBuilderBuilder.with(ValueGroupChangeBuilder::ruleSet, this.ruleSet);
        this.valueGroupChangeBuilders.add(valueGroupChangeBuilderBuilder);
        return this;
    }

    /**
     * Sets a fixed ID for the change.  If not set, defaults to {@code UUID.randomUUID()}
     *
     * @param id Unique Identifier to set
     */
    public void setId(final UUID id) {
        this.id = id;
    }

    /**
     * Method to set the audit information in the change.
     *
     * @param audit the {@link Audit} object
     * @return this builder
     */
    public ChangeBuilder audit(final Audit audit) {
        this.audit = audit;
        return this;
    }

    /**
     * Method to set the activation time - it has been applied to the decision tree if it is not null.
     *
     * @param activationTime the time that the change is activated
     * @return this builder
     */
    public ChangeBuilder activation(final Instant activationTime) {
        this.activationTime = activationTime;
        return this;
    }

    /**
     * Sets the period over which a change is effective.
     *
     * <p>If the change range is not specified, it is defaulted to {@link ChangeBuilder#range} and will start from now
     * and end in the (distant) future.
     *
     * @param range Effective changeRange for the change.
     */
    public void changeRange(final DateRange range) {
        EhSupport.ensureArg(range != null, "Applied change range cannot be null");
        this.range = range;
    }
}