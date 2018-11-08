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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.swblocks.decisiontree.TreeRule;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.jbl.util.DateRange;

import static java.util.Optional.ofNullable;

/**
 * DecisionTreeRule is the definition of a rule used to build the decision tree.
 *
 * <p>The Rule defines the input drivers, data/time range the rule is valid between and the output.
 */
public final class DecisionTreeRule implements TreeRule {
    public static final int INITIAL_WEIGHTED_VALUE = 1;
    /**
     * These are defined as default time for rules where start and end dates are not specified.
     * The default Instant.MAX time is too large for a Long, therefore it is redefined here to
     * ensure it fits in a Long when persisted.
     */
    public static final Instant EPOCH = Instant.EPOCH;
    public static final Instant MAX = Instant.ofEpochMilli(Long.MAX_VALUE);

    private final UUID ruleIdentifier;
    private final UUID ruleCode;
    private final InputDriver[] drivers;
    private final InputDriver[] evaluations;
    private final Map<String, String> outputs;
    private final Instant start;
    private final Instant end;

    /**
     * Constructor to create a fully formed DecisionTreeRule object.
     *
     * @param ruleIdentifier Unique identifier of the rule
     * @param ruleCode       Unique identifier of the same rule with different version
     * @param drivers        Array of {@link InputDriver} defining the input drivers in weighted order
     * @param evaluations    Array of {@link InputDriver} defining the input drivers evaluated
     * @param outputs        Map of name/value pairs of strings defining the output of the rule
     * @param start          Start time of the rule, defaults to {@code DecisionTreeRule.EPOCH} if null
     * @param end            End time of the rule, defaults to {@code DecisionTreeRule.MAX} if null
     */
    public DecisionTreeRule(final UUID ruleIdentifier, final UUID ruleCode, final InputDriver[] drivers,
                            final InputDriver[] evaluations, final Map<String, String> outputs,
                            final Instant start, final Instant end) {
        this.ruleIdentifier = ruleIdentifier;
        this.ruleCode = ruleCode;
        this.drivers = drivers;
        this.evaluations = evaluations;
        this.outputs = outputs;
        this.start = ofNullable(start).orElse(DecisionTreeRule.EPOCH);
        this.end = ofNullable(end).orElse(DecisionTreeRule.MAX);
    }

    /**
     * Constructor to create a fully formed DecisionTreeRule object.
     *
     * @param ruleIdentifier Unique identifier of the rule
     * @param ruleCode       Unique identifier of the same rule with different version
     * @param drivers        Array of {@link InputDriver} defining the input drivers in weighted order
     * @param outputs        Map of name/value pairs of strings defining the output of the rule
     * @param start          Start time of the rule, defaults to {@code DecisionTreeRule.EPOCH} if null
     * @param end            End time of the rule, defaults to {@code DecisionTreeRule.MAX} if null
     */
    public DecisionTreeRule(final UUID ruleIdentifier, final UUID ruleCode, final InputDriver[] drivers,
                            final Map<String, String> outputs, final Instant start, final Instant end) {
        this(ruleIdentifier, ruleCode, drivers, null, outputs, start, end);
    }

    /**
     * Returns true if the rule is active at the point in time passed in.
     *
     * @param time to check is rule is valid.
     * @return true is rule is active at time.
     */
    public boolean isActiveAt(final Instant time) {
        return time.isAfter(start) && time.isBefore(end);
    }

    /**
     * Returns the unique identifier for the rule.
     *
     * @return UUID
     * @see TreeRule#getRuleIdentifier()
     */
    @Override
    public UUID getRuleIdentifier() {
        return ruleIdentifier;
    }

    /**
     * Returns an array of the input drivers for the rule.  The array is in weighted order.
     *
     * @return Array of input drivers
     * @see TreeRule#getDrivers()
     */
    @Override
    public InputDriver[] getDrivers() {
        return drivers;
    }

    /**
     * Returns an array of the drivers for the rule to be evaluated on the outputs.
     *
     * @return Optional of the Array of input drivers
     * @see TreeRule#getEvaluations()
     */
    @Override
    public Optional<InputDriver[]> getEvaluations() {
        if (evaluations == null || evaluations.length == 0) {
            return Optional.empty();
        }
        return Optional.of(evaluations);
    }

    /**
     * Map of name/value pairs of the output definition of the rule.
     *
     * @return Map of name value pairs
     * @see TreeRule#getOutputs()
     */
    @Override
    public Map<String, String> getOutputs() {
        return Collections.unmodifiableMap(outputs);
    }

    @Override
    public DateRange getRange() {
        return new DateRange(start, end);
    }

    /**
     * Returns the start date time instant the rule is active from.
     *
     * @return Instant
     */
    public Instant getStart() {
        return start;
    }

    /**
     * Returns the end date time instant the rule is active until.
     *
     * @return Instant
     */
    public Instant getEnd() {
        return end;
    }

    /**
     * Providates a duplicate check based on the data and not the ruleIdentifier which is used for equality.
     *
     * @param other {@code DecisionTreeRule} to compare against.
     * @return true if the inputs, outputs and date ranges from the other {@code DecisionTreeRule} are identical.
     */
    public boolean isDuplicateRule(final DecisionTreeRule other) {
        if (other == null) {
            return false;
        }
        if (evaluations != null || other.evaluations != null) {
            return isDuplicateInputData(other) && isDuplicateOutputData(other) &&
                    isDuplicateDateRange(other) && isDuplicateEvaluations(other);
        }
        return isDuplicateInputData(other) && isDuplicateOutputData(other) &&
                isDuplicateDateRange(other);
    }

    /**
     * Provides a duplicate check based on the full set of input data. The true equals check is on the ruleIdentifier,
     * but when creating rules there is a requirement to check for duplicated input data.
     *
     * @param other {@code DecisionTreeRule} to compare against.
     * @return true if the inputs from the other {@code DecisionTreeRule} are identical.
     */
    public boolean isDuplicateInputData(final DecisionTreeRule other) {
        if (other == null || drivers.length != other.drivers.length) {
            return false;
        }
        for (int i = 0; i < drivers.length; i++) {
            final InputDriver thisValue = drivers[i];
            final InputDriver otherValue = other.drivers[i];
            if (!thisValue.equals(otherValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Provides a duplicate check based on the optional set of evaluations. As the evaluations are optional, then either
     * being empty is not duplicate
     *
     * @param other {@code DecisionTreeRule} to compare against.
     * @return true if the evaluations from the other {@code DecisionTreeRule} are both populated and identical.
     */
    public boolean isDuplicateEvaluations(final DecisionTreeRule other) {
        if (other == null || evaluations == null || other.evaluations == null ||
                evaluations.length != other.evaluations.length) {
            return false;
        }
        for (int i = 0; i < evaluations.length; i++) {
            final InputDriver thisValue = evaluations[i];
            final InputDriver otherValue = other.evaluations[i];
            if (!thisValue.equals(otherValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Provides a duplicate check based on the full set of output data. The true equals check is on the ruleIdentifier.
     *
     * @param other {@code DecisionTreeRule} to compare against.
     * @return true if the outputs from the other {@code DecisionTreeRule} are identical.
     */
    public boolean isDuplicateOutputData(final DecisionTreeRule other) {
        if (other == null || outputs.size() != other.outputs.size()) {
            return false;
        }

        for (final Map.Entry<String, String> entry : outputs.entrySet()) {
            final String value = other.outputs.get(entry.getKey());
            if (!entry.getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }

    public boolean isDuplicateDateRange(final DecisionTreeRule other) {
        return other != null && start.equals(other.start) && end.equals(other.end);
    }

    /**
     * Calculates the weight of the inputs.
     *
     * <p>The weighting is based on the binary pattern where a wildcard is a 0 and anything else is 1. For example:
     * Inputs-"1", "2", "*", "*" gives 1100 which is 12. It is expected that this method would be called only once to
     * set it on the result node of the evaluation tree. If it is called a lot, then storing the weight as part of the
     * constructor may improve performance, but increase the storage required for a DecisionTreeRule.
     *
     * @return the calculated weight of the inputs.
     */
    public long getRuleWeight() {
        long weight = 0;
        long weightedLevel = INITIAL_WEIGHTED_VALUE;

        // The inputDriver list is in descending weighted order, need to iterate in reverse
        for (int i = drivers.length - 1; i >= 0; i--) {
            if (!InputValueType.WILDCARD.equals(drivers[i].getValue())) {
                weight += weightedLevel;
            }
            weightedLevel = weightedLevel * 2;
        }
        return weight;
    }

    /**
     * Gets the {@link UUID} for the rule.
     *
     * @return Universal Unique Identifier
     */
    @Override
    public UUID getRuleCode() {
        return ruleCode;
    }

    /**
     * Replaces the {@link InputDriver} with objects from the {@link DriverCache}.  Any missing {@link InputDriver}
     * are added to the cache.
     *
     * @param cache {@link DriverCache} of {@link InputDriver}
     */
    void replaceDriversFromCache(final DriverCache cache) {
        for (int i = 0; i < drivers.length; i++) {
            final InputDriver driver = drivers[i];
            InputDriver cachedDriver = cache.get(driver.getValue(), driver.getType());
            if (cachedDriver == null) {
                cache.put(driver);
                cachedDriver = cache.get(driver.getValue(), driver.getType());
            }
            drivers[i] = cachedDriver;
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final DecisionTreeRule otherRule = (DecisionTreeRule) other;

        return ruleIdentifier.equals(otherRule.ruleIdentifier);
    }

    @Override
    public int hashCode() {
        return ruleIdentifier.hashCode();
    }

    @Override
    public String toString() {
        return "DecisionTreeRule{" +
                "ruleIdentifier=" + ruleIdentifier +
                ", ruleCode=" + ruleCode +
                ", drivers=" + Arrays.toString(drivers) +
                (evaluations != null ? ", evaluations=" + Arrays.toString(evaluations) : "") +
                ", outputs=" + outputs +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
