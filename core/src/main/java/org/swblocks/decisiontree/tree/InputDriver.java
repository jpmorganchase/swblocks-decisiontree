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

package org.swblocks.decisiontree.tree;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Input driver value that stores the value of ths input driver and the associated datatype {@link InputValueType}.
 *
 * <p>Sub-classes are {@link StringDriver}, {@link RegexDriver} and {@link GroupDriver}.
 */
public abstract class InputDriver {
    private final String value;
    private final InputValueType type;
    private Predicate<String> evaluation;

    /**
     * Constructor to build the {@link InputDriver}
     *
     * @param value      Name of the driver
     * @param type       {@link InputValueType} type of driver
     * @param evaluation {@link Predicate} evaluation class to evaluate in the DecisionTree.
     */
    public InputDriver(final String value, final InputValueType type, final Predicate<String> evaluation) {
        this.value = value;
        this.type = type;
        this.evaluation = evaluation;
    }

    public String getValue() {
        return this.value;
    }

    public InputValueType getType() {
        return this.type;
    }

    protected void setEvaluation(final Predicate<String> evaluation) {
        this.evaluation = evaluation;
    }

    /**
     * Evaluate the input parameter against the internal information in the {@link InputDriver}.
     *
     * @param input Input to test against
     * @return true if input matches against the {@link InputDriver}
     */
    public boolean evaluate(final String input) {
        return this.evaluation.test(input);
    }

    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final InputDriver otherDriver = (InputDriver) other;
        return Objects.equals(this.getValue(), otherDriver.getValue()) &&
                Objects.equals(this.getType(), otherDriver.getType());

    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }
}