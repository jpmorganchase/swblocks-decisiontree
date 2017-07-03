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

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.swblocks.decisiontree.Evaluator;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;

/**
 * Failure Path test cases for {@link DecisionTreeFactory} creating a single decision tree.
 */
public class SingleDecisionTreeFailurePathTest {
    @Test
    public void testSimpleFailurePath() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("DRIVER1", "DRIVER2", "DRIVER3"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("ONE", "ONE", "ONE")));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("ONE", "*", "*")));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("TWO", "TWO", "TWO")));

        final TreeNode node = constructTree(ruleSetBuilder);
        // This will fail as there is no wildcards on the TWO path.
        Optional<UUID> result = Evaluator.evaluate(Arrays.asList("TWO", "NOT TWO", "TWO"), null, node);
        Assert.assertFalse(result.isPresent());

        // This will succeed by following the failure path from the first rile to the second rule.
        result = Evaluator.evaluate(Arrays.asList("ONE", "ONE", "NOT ONE"), null, node);
        Assert.assertTrue(result.isPresent());
    }

    @Test
    public void testComplexFailurePath() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("DRIVER1", "DRIVER2", "DRIVER3", "DRIVER4"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setCode, new UUID(0, 0))
                .with(RuleBuilder::input, Arrays.asList("ONE", "ONE", "ONE", "ONE")));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setCode, new UUID(0, 1))
                .with(RuleBuilder::input, Arrays.asList("TWO", "TWO", "TWO", "TWO")));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("*", "*", "*", "FOUR")));

        final TreeNode node = constructTree(ruleSetBuilder);

        final Optional<UUID> result = Evaluator.evaluate(Arrays.asList("ONE", "TWO", "THREE", "FOUR"), null, node);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(new UUID(0, 2), result.get());
    }

    private TreeNode constructTree(final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder) {
        return DecisionTreeFactory.constructDecisionTree(ruleSetBuilder.build(), DecisionTreeType.SINGLE);
    }
}
