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

package org.swblocks.decisiontree;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.domain.WeightedDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.jbl.eh.EhSupport;

import static java.util.stream.Collectors.collectingAndThen;

/**
 * Definitions of the input data to the Decision Tree evaluation.
 */
public class Input {
    public static final String EMPTY = "";
    private final SortedMap<WeightedDriver, String> driverMap;
    private final List<WeightedDriver> driverList;
    private final String ruleSetName;
    private final Instant evaluationDate;

    private Input(final String ruleSetName, final List<WeightedDriver> drivers, final List<String> searchValues,
                  final Instant evaluationDate) {
        this.ruleSetName = ruleSetName;
        this.evaluationDate = evaluationDate;
        this.driverList = drivers;
        this.driverMap = new TreeMap<>();
        int counter = 0;
        for (final WeightedDriver weightedDriver : drivers) {
            this.driverMap.put(weightedDriver, searchValues.get(counter));
            ++counter;
        }
    }

    /**
     * Creates an Input for a RuleSet.
     *
     * <p>Should be called from the {@link DecisionTree} access class.
     */
    static Input create(final String ruleSetName, final List<WeightedDriver> driverNames) {
        return create(ruleSetName, driverNames, Instant.now());
    }

    /**
     * Creates an Input for a RuleSet.
     *
     * <p>Should be called from the {@link DecisionTree} access class.
     */
    static Input create(final String ruleSetName, final List<WeightedDriver> driverNames,
                        final String... searchValues) {
        EhSupport.ensureArg(driverNames.size() == searchValues.length,
                "The number of Search Values does not match the number of Drivers.");
        return new Input(ruleSetName, driverNames, Arrays.asList(searchValues), Instant.now());
    }

    /**
     * Creates an Input for a RuleSet.
     *
     * <p>Should be called from the {@link DecisionTree} access class.
     */
    static Input create(final String ruleSetName, final List<WeightedDriver> driverNames, final Instant evaluationDate,
                        final String... searchValues) {
        EhSupport.ensureArg(driverNames.size() == searchValues.length,
                "The number of Search Values does not match the number of Drivers.");
        EhSupport.ensureArg(evaluationDate != null, "The evaluation date cannot be null");
        return new Input(ruleSetName, driverNames, Arrays.asList(searchValues), evaluationDate);
    }

    /**
     * Creates an Input for a RuleSet to be evaluated at a particular time
     *
     * <p>Should be called from the {@link DecisionTree} access class.
     */
    static Input create(final String ruleSetName, final List<WeightedDriver> driverNames,
                        final Instant evaluationDate) {
        final List<String> wildcards = new ArrayList<>(driverNames.size());
        driverNames.forEach(weightedDriver -> wildcards.add(InputValueType.WILDCARD));
        return new Input(ruleSetName, driverNames, wildcards, evaluationDate);
    }

    /**
     * Gets the RuleSet Name associated with this Input.
     *
     * @return name of the ruleset
     */
    public String getRuleSetName() {
        return this.ruleSetName;
    }

    /**
     * Sets the search value for a input driver.
     *
     * @param driverName driver name for the search value
     * @param value      search value for the driver
     * @return true if the driverName is valid, false otherwise.
     */
    public boolean putValueForDriverName(final String driverName, final String value) {
        // Check for valid key before accepting
        final WeightedDriver key = getWeightedDriverForDriverName(driverName);
        if (key != null && this.driverMap.containsKey(key)) {
            this.driverMap.put(key, value);
            return true;
        }

        return false;
    }

    private WeightedDriver getWeightedDriverForDriverName(final String driverName) {
        // Check for valid key before accepting
        final int driverPosition = this.driverList.indexOf(new WeightedDriver(driverName, 0));
        if (driverPosition >= 0) {
            return this.driverList.get(driverPosition);
        }
        return null;
    }

    public String getValueForDriverName(final String driverName) {
        return this.driverMap.get(getWeightedDriverForDriverName(driverName));
    }

    /**
     * Sets the search value for a driver to empty.  This will perform a match for the empty string and not a wildcard.
     *
     * @param driverName driver name for the search value
     * @return true if the driverName is valid, false otherwise.
     */
    public boolean putBlankValueForDriverName(final String driverName) {
        return putValueForDriverName(driverName, EMPTY);
    }

    /**
     * Gets the {@link Instant} the Input will be evaluated at.
     *
     * @return Instant
     */
    public Instant getEvaluationDate() {
        return this.evaluationDate;
    }

    /**
     * Gets the list of inputs to evaluate in weighted order.
     *
     * @return List of Strings to be evaluated.
     */
    public List<String> getEvaluationInputs() {
        return this.driverMap.values().stream().collect(collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    @Override
    public String toString() {
        return "Input{driverMap=" + this.driverMap +
                ", ruleSetName='" + this.ruleSetName + '\'' +
                ", evaluationDate=" + this.evaluationDate +
                '}';
    }
}
