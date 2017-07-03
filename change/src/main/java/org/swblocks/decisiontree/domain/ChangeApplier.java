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

package org.swblocks.decisiontree.domain;

import org.swblocks.decisiontree.change.domain.Change;

/**
 * Simple class to apply a {@link Change} to the {@link DecisionTreeRuleSet}.
 */
public final class ChangeApplier {
    private ChangeApplier() {
    }

    /**
     * Applies the {@link Change} to the {@link DecisionTreeRuleSet}.
     *
     * @param ruleSet {@link DecisionTreeRuleSet} to apply the change to
     * @param change  {@link Change} to apply to the ruleset
     */
    public static void apply(final DecisionTreeRuleSet ruleSet, final Change change) {
        ruleSet.updateRules(change);
    }
}
