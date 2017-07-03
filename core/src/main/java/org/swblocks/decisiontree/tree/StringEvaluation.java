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

import java.util.function.Predicate;

/**
 * Evaluation class to compare the {@link String} input with the {@link String} driver within the DecisionTree.
 */
final class StringEvaluation implements Predicate<String> {
    private final String value;

    StringEvaluation(final String value) {
        this.value = value;
    }

    @Override
    public boolean test(final String input) {
        return this.value.equals(input);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return this.value.equals(((StringEvaluation) other).value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
}
