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

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import org.swblocks.decisiontree.EvaluationResult;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.jbl.util.DateRange;

/**
 * Final leaf node that matches a rule.
 *
 * <p>The class is created using the {@link NodeSupplier}. The result node could be for a string tree node or any of
 * the
 * other enum types defined in {@link InputValueType}
 */
final class ResultNode extends BaseTreeNode implements EvaluationResult, TreeNode {
    private final UUID ruleIdentifier;
    private final long weight;
    private final DateRange range;

    ResultNode(final InputDriver driver,
               final int driverLevel,
               final DecisionTreeRule rule,
               final TreeNode delegate) {
        super(driver, driverLevel);
        this.ruleIdentifier = rule.getRuleIdentifier();
        this.weight = rule.getRuleWeight();
        this.range = delegate.getDateRange();
        this.nextNodes = Collections.emptyMap();
    }

    @Override
    public boolean equals(final Object other) {
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hashCode(this.range);
    }

    @Override
    public DateRange getDateRange() {
        return this.range;
    }

    @Override
    public UUID getRuleIdentifier() {
        return this.ruleIdentifier;
    }

    @Override
    public long getWeight() {
        return this.weight;
    }
}
