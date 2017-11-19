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

import org.swblocks.decisiontree.tree.DateRangeDriver;
import org.swblocks.decisiontree.tree.IntegerRangeDriver;
import org.swblocks.decisiontree.tree.RegexDriver;
import org.swblocks.jbl.util.Range;

/**
 * InputBuilder is a utility class to provide simple helper methods to generate the required input format for the
 * special types of inputs to the DecisionTree.
 */
public final class InputBuilder {
    /**
     * Private constructor to enforce static use.
     */
    private InputBuilder() {
    }

    /**
     * regExInput generates a Regular Expression Input String from a basic regular expression.
     *
     * @param regex plain regular expression to be used as input.
     * @return input String prefixed by the {@code RegexDriver.REGEX_PREFIX}
     */
    public static String regExInput(final String regex) {
        return RegexDriver.REGEX_PREFIX + ":" + regex;
    }

    /**
     * dateRangeInput generates a Date Range Input String from a {@link Range} of {@link Instant}.
     *
     * @param range {@link Range} of dates
     * @return input string for Date Ranges, prefixed by the {@code DateRangeDriver.DR_PREFIX}
     */
    public static String dateRangeInput(final Range<Instant> range) {
        return DateRangeDriver.DR_PREFIX + ":" + range.getStart() + "|" + range.getFinish();
    }

    /**
     * integerRangeInput generates an Integer Range Input Strin from a {@link Range} of {@link Integer}
     *
     * @param range {@link Range} of integers
     * @return input string for Integer Ranges, prefixed by the {@code IntegerRangeDriver.IR_PREFIX}
     */
    public static String integerRangeInput(final Range<Integer> range) {
        return IntegerRangeDriver.IR_PREFIX + ":" + range.getStart() + "|" + range.getFinish();
    }
}
