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

import java.time.Instant;
import java.util.function.Predicate;
import java.time.format.DateTimeFormatter;
import org.swblocks.jbl.util.Range;

/**
 * Evaluation class for evaluating String inputs of {@link Instant} formatted to {@link DateTimeFormatter#ISO_INSTANT}.
 */
public final class DateRangeEvaluation implements Predicate<String> {
    private final Range<Instant> range;
    private final String name;

    DateRangeEvaluation(final String name, final Range<Instant> range) {
        this.range = range;
        this.name = name;
    }

    @Override
    public boolean test(final String dateString) {
        if (InputValueType.WILDCARD.equals(dateString)) {
            return true;
        }
        final Instant evalInstant = Instant.parse(dateString);
        return Range.RANGE_CHECK.test(this.range, evalInstant);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return this.name.equals(((DateRangeEvaluation) other).name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
