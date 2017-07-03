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

import org.swblocks.jbl.util.DateRange;

/**
 * Node key for time dated decision trees.
 *
 * <p>Requires a string value and the date range
 */
final class DatedNodeKey {
    private final DateRange range;
    private final String value;

    DatedNodeKey(final String value, final DateRange range) {
        this.value = value;
        this.range = range;
    }

    public DateRange getRange() {
        return this.range;
    }

    public String getValue() {
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

        final DatedNodeKey otherKey = (DatedNodeKey) other;
        return Objects.equals(getValue(), otherKey.getValue()) && Objects.equals(getRange(), otherKey.getRange());
    }

    @Override
    public int hashCode() {
        return getValue().hashCode() + getRange().hashCode();
    }
}
