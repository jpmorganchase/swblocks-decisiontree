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

import java.util.List;
import java.util.UUID;

import org.swblocks.jbl.util.DateRange;

/**
 * Interface defining the access operations of a list of Values which are grouped together.
 */
public interface TreeValueGroup {
    /**
     * Gets the unique identifier for the {@link TreeValueGroup}.
     *
     * @return Unique Identifier of the form of UUID
     */
    UUID getId();

    /**
     * Gets the name of the {@link TreeValueGroup}.
     *
     * @return Name
     */
    String getName();

    /**
     * Gets the values within the group.
     *
     * @return {@link List} of values
     */
    List<String> getValues();

    /**
     * Gets the {@link DateRange} the {@link TreeValueGroup} is active across.
     *
     * @return {@link DateRange}
     */
    DateRange getRange();
}
