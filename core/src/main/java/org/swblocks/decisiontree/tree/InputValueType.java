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

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.jbl.util.DateRange;

/**
 * Enum for the type of tree node used in the decision tree.
 *
 * <p>STRING represents a string tree node where the input value is matched against a string.
 *
 * <p>REGEX represents a regex tree node where the input value is matched against the regex.
 *
 * <p>VALUE_GROUP represents a value group tree node where an input value is matched within the value group.
 *
 * <p>DATE_RANGE represents a {@link DateRange} tree node where the input value is validated it is a date and within the
 * range defined in the {@link DateRange} within the {@link DecisionTreeRule}.
 *
 */
public enum InputValueType {
    STRING, REGEX, VALUE_GROUP, DATE_RANGE, INTEGER_RANGE;

    public static final String WILDCARD = "*";
}
