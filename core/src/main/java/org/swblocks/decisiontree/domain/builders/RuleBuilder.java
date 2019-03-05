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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.jbl.builders.Builder;

/**
 * RuleBuilder is the domain builder for the {@link DecisionTreeRule} class.
 *
 * <p>It is designed to be used with the general {@link Builder} class, but can be executed on its own.
 *
 * <p>Example usage creating a new rule:
 * <blockquote><pre>
 *  public DecisionTreeRule createRule(final UUID id,
 *                                     final UUID ruleCode,
 *                                     final List&lt;String&gt; drivers,
 *                                     final long driverCount,
 *                                     final Map&lt;Sting, String&gt; outputs,
 *                                     final DateRange range) {
 *      final Builder&lt;RuleBuilder, DecisionTreeRule&gt; ruleBuilder = RuleBuilder.creator();
 *
 *      ruleBuilder.with(RuleBuilder::setId, id);               // defaulted to {@link UUID#randomUUID()} if not set
 *      ruleBuilder.with(RuleBuilder::setCode, ruleCode);       // defaulted to {@link UUID#randomUUID()} if not set
 *      ruleBuilder.with(RuleBuilder::input, drivers);
 *      ruleBuilder.with(RuleBuilder::setDriverCount, driverCount);
 *      ruleBuilder.with(RuleBuilder::output, outputs);
 *
 *      // if 'start' not set, it is defaulted to {@link DecisionTreeRule#EPOCH}
 *      ruleBuilder.with(RuleBuilder::start, range.getStart());
 *      // if 'end' not set, it is defaulted to {@link DecisionTreeRule#MAX}
 *      ruleBuilder.with(RuleBuilder::end, range.getEnd());
 *
 *      final DecisionTreeRule rule = ruleBuilder.build();
 *  }
 * </pre></blockquote>
 *
 * <p>A cache can be supplied to the builder - this is used to share {@link InputDriver} objects where these are shared
 * amongst a number of rules. {@code DecisionTreeRule} objects are created by the {@link RuleSetBuilder} which supplies
 * the cache ({@link RuleSetBuilder#cache}).
 *
 * <p>The builder can also generate the id using an alternative id if required. The variable {@link
 * RuleBuilder#alternativeId} must be set and a {@code UUID} is created based on the alternative id.
 */
public final class RuleBuilder {
    private final List<String> drivers = new ArrayList<>(1);
    private final List<String> evaluations = new ArrayList<>(1);
    private DriverCache cache = null;
    private Map<String, String> outputs = new HashMap<>();
    private UUID id;
    private Long alternativeId;
    private UUID code;
    private Instant startTime;
    private Instant endTime;

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the {@link
     * DecisionTreeRule} as the domain result.
     *
     * @return Instance of the Builder to create and build.
     */
    public static Builder<RuleBuilder, DecisionTreeRule> creator() {
        return Builder.instanceOf(RuleBuilder.create(), RuleBuilder.validate(), RuleBuilder::builds);
    }

    /**
     * Static build method to generate a {@link DecisionTreeRule} from this domain builder class.
     *
     * @param builder domain builder object to use to build the result
     * @return The generated {@link DecisionTreeRule}
     */
    public static DecisionTreeRule builds(final RuleBuilder builder) {
        if (builder.cache == null) {
            builder.cache = new DriverCache();
        }

        final List<InputDriver> inputDrivers = new ArrayList<>(builder.drivers.size());
        builder.drivers.forEach(driver ->
                inputDrivers.add(DomainSerialiser.createInputDriver(driver, builder.cache).get()));

        final List<InputDriver> evaluations = new ArrayList<>(builder.evaluations.size());
        builder.evaluations.forEach(evaluation ->
                evaluations.add(DomainSerialiser.createInputDriver(evaluation, builder.cache).get()));

        return new DecisionTreeRule(builder.id != null ? builder.id :
                builder.alternativeId != null ? new UUID(0, builder.alternativeId) : UUID.randomUUID(),
                builder.code != null ? builder.code : UUID.randomUUID(),
                inputDrivers.toArray(new InputDriver[0]),
                evaluations.toArray(new InputDriver[0]),
                builder.outputs,
                builder.startTime,
                builder.endTime);
    }

    private static Supplier<RuleBuilder> create() {
        return RuleBuilder::new;
    }

    private static Predicate<RuleBuilder> validate() {
        return ruleBuilder -> !ruleBuilder.drivers.isEmpty();
    }

    /**
     * Sets a fixed ID for the rule.
     *
     * <p>If not set, then it defaults to the alternative id when building the rule.
     *
     * @param id Unique Identifier to set
     */
    public void setId(final UUID id) {
        this.id = id;
    }

    /**
     * Sets an alternative Id for the rule.
     *
     * <p>This allows a default id to be set if the primary id is not specified. The alternative id will be converted
     * to a {@link UUID} on rule creation.
     *
     * @param alternativeId Alternative identifier to set
     */
    public void setAlternativeId(final Long alternativeId) {
        this.alternativeId = alternativeId;
    }

    /**
     * Sets a UUID which links different versions of the same rule.
     *
     * @param uuid Unique Id of the rule
     */
    public void setCode(final UUID uuid) {
        this.code = uuid;
    }

    /**
     * Number of drivers for this ruleset.
     *
     * @param driverCount number of input drivers required.
     */
    public void setDriverCount(final Long driverCount) {
        if (drivers.size() != driverCount) {
            throw new IllegalArgumentException("Number of drivers values expected " + driverCount +
                    ". Failure was in rule " + id + " id");
        }
    }

    /**
     * Sets the inputs for this rule.
     *
     * <p>To share the drivers across all rules, specify a common cache using {@link #cache(DriverCache) cache} method.
     *
     * @param drivers List of string inputs.
     * @return this for method chaining.
     */
    public RuleBuilder input(final List<String> drivers) {
        this.drivers.addAll(drivers);
        return this;
    }

    /**
     * Sets the evaluations for this rule.
     *
     * @param evaluations List of string inputs.
     * @return this for method chaining.
     */
    public RuleBuilder evaluations(final List<String> evaluations) {
        this.evaluations.addAll(evaluations);
        return this;
    }

    /**
     * Sets the holder that can be used to cache the {@link InputDriver} instances across rules.
     *
     * @param cache cache that can be used to store drivers to use across a ruleset.
     * @return this for method chaining.
     */
    public RuleBuilder cache(final DriverCache cache) {
        this.cache = cache;
        return this;
    }

    /**
     * Sets the outputs for a rule, each element in the map is a name/value of the output of the rule.
     *
     * @param outputs Map to be assigned as the rule output.
     * @return reference to this for method chaining
     */
    public RuleBuilder output(final Map<String, String> outputs) {
        this.outputs = outputs;
        return this;
    }

    /**
     * Sets the outputs for a rule.
     *
     * <p>Each element in the list is a name/value of the output of the rule separated by a :
     *
     * @param outputs Map to be assigned as the rule output.
     * @return reference to this for method chaining
     */
    public RuleBuilder output(final List<String> outputs) {
        this.outputs = DomainSerialiser.convertOutputs(outputs);
        return this;
    }

    /**
     * Sets the start time of the rule, default to {@code DecisionTreeRule.EPOCH} if not set.
     *
     * @param startTime Start time of the rule.
     */
    public void start(final Instant startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the end time of the rule, default to {@code DecisionTreeRule.MAX} if not set.
     *
     * @param endTime End time of the rule.
     */
    public void end(final Instant endTime) {
        this.endTime = endTime;
    }
}
