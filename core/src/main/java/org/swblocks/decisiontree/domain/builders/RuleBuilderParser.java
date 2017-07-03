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

import java.io.InputStream;

import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;

/**
 * Interface to define the parsing interface for loading a {@link DecisionTreeRuleSet}.
 */
@FunctionalInterface
public interface RuleBuilderParser {
    /**
     * Constructs a {@link DecisionTreeRuleSet} from an {@link InputStream}.
     *
     * @param inputStream of the json to process
     * @return Constructed {@link DecisionTreeRuleSet}
     */
    DecisionTreeRuleSet parseRuleSet(final InputStream inputStream);
}
