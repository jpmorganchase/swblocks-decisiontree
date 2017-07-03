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

import java.util.Map;
import java.util.UUID;

import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.jbl.util.DateRange;

/**
 * Defines the operations for a {@link TreeRule}.
 */
public interface TreeRule {
    /**
     * Returns the unique identifier for the rule.
     *
     * @return UUID
     */
    UUID getRuleIdentifier();

    /**
     * Returns an array of the input drivers for the rule.  The array is in weighted order.
     *
     * @return Array of input drivers
     */
    InputDriver[] getDrivers();

    /**
     * Map of name/value pairs of the output definition of the rule.
     *
     * @return Map of name value pairs
     */
    Map<String, String> getOutputs();

    /**
     * Gets the {@link DateRange} the {@link TreeRule} is valid over.
     *
     * @return The {@link DateRange}
     */
    DateRange getRange();

    /**
     * Gets the unique {@link UUID} of the {@link TreeRule}.
     *
     * @return unique {@link UUID}
     */
    UUID getRuleCode();
}
