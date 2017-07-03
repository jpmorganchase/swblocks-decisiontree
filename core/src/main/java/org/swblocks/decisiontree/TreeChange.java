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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;

/**
 * A TreeChange defines the information required to make a change to a {@link DecisionTreeRuleSet}.
 */
public interface TreeChange {
    /**
     * Gets the new, modified and deleted {@link TreeRule} in a map.
     *
     * <p>Deleted rules are indicated by a valid UUID as the key, but a NULL value.
     *
     * @return Map of changed rules.
     */
    default Map<UUID, Optional<TreeRule>> getRules() {
        return Collections.emptyMap();
    }

    /**
     * Gets the new, modified and deleted {@link TreeValueGroup} in a map.
     *
     * <p>Deleted rules are indicated by a valid UUID as the key, but a NULL value.
     *
     * @return Map of changed groups.
     */
    default Map<UUID, Optional<TreeValueGroup>> getGroups() {
        return Collections.emptyMap();
    }
}
