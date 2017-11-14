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

import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.DecisionTreeFactory;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.tree.TreeNode;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test Cases for {@link Evaluator} using the {@link DecisionTreeFactory} creating a dated decision tree.
 */
public class DatedEvaluatorTest {
    private List<String> inputA;
    private List<String> inputB;
    private List<String> inputC;
    private List<String> inputD;
    private List<String> inputE;
    private List<String> inputF;

    @Before
    public void setUp() {
        this.inputA = Arrays.asList("VOICE", "CME", "ED", "US", "RATE");
        this.inputB = Arrays.asList("VOICE", "CME", "EB", "US", "RATE");
        this.inputC = Arrays.asList("VOICE", "CME", "S&P", "US", "INDEX");
        this.inputD = Arrays.asList("ELECTRONIC", "LIFFE", "L", "UK", "INDEX");
        this.inputE = Arrays.asList("ELECTRONIC", "CME", "EB", "US", "RATE");
        this.inputF = Arrays.asList("VOICE", "LIFFE", "L", "UK", "INDEX");
    }

    @Test
    public void singleRule() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();
        final Period period = Period.ofWeeks(4);

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", now.minus(period),
                now.plus(period), 1L, "1.1");
        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        checkMatch(this.inputC, now, node, 1L);

        checkNoMatch(this.inputC, now.plus(Period.ofWeeks(8)), node);
        checkNoMatch(this.inputC, now.minus(Period.ofWeeks(8)), node);

        checkMatch(this.inputC, now.plus(Period.ofDays(2)), node, 1L);

        checkNoMatch(this.inputA, now, node);
        checkNoMatch(this.inputB, now, node);
        checkNoMatch(this.inputD, now, node);
        checkNoMatch(this.inputE, now, node);
        checkNoMatch(this.inputF, now, node);
    }

    @Test
    public void twoRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", now.minus(Period.ofWeeks(12)),
                now.plus(Period.ofWeeks(12)), 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", now.minus(Period.ofWeeks(4)),
                now.plus(Period.ofWeeks(4)), 2L, "1.2");
        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        // Rule 1 and Rule 2 match - Rule 2 has higher weight
        checkMatch(this.inputC, now, node, 2L);
        // check that 2 come back
        final List<EvaluationResult> result = Evaluator.evaluateAllResults(this.inputC, now, node);
        assertEquals(2, result.size());

        final EvaluationResult ruleOneResult;
        final EvaluationResult ruleTwoResult;

        if (result.get(0).getRuleIdentifier().equals(new UUID(0, 1))) {
            ruleOneResult = result.get(0);
            ruleTwoResult = result.get(1);
        } else {
            ruleOneResult = result.get(1);
            ruleTwoResult = result.get(0);
        }

        assertEquals(new UUID(0, 1), ruleOneResult.getRuleIdentifier());
        assertEquals(9, ruleOneResult.getWeight());
        assertEquals(new UUID(0, 2), ruleTwoResult.getRuleIdentifier());
        assertEquals(13, ruleTwoResult.getWeight());

        checkMatch(this.inputC, now.minus(Period.ofWeeks(8)), node, 1L);
        checkMatch(this.inputC, now.plus(Period.ofWeeks(8)), node, 1L);
        checkMatch(this.inputC, now.plus(Period.ofDays(2)), node, 2L);

        checkNoMatch(this.inputA, now, node);
        checkNoMatch(this.inputB, now, node);
        checkNoMatch(this.inputD, now, node);
        checkNoMatch(this.inputE, now, node);
        checkNoMatch(this.inputF, now, node);
    }

    @Test
    public void twoIdenticalRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", now.minus(Period.ofWeeks(12)),
                now.plus(Period.ofWeeks(12)), 1L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", now.minus(Period.ofWeeks(4)),
                now.plus(Period.ofWeeks(4)), 2L, "1.2");
        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        // check that 2 come back and that they have the same weight as identical
        final List<EvaluationResult> result = Evaluator.evaluateAllResults(this.inputC, now, node);
        assertEquals(2, result.size());
        assertEquals(new UUID(0, 1), result.get(0).getRuleIdentifier());
        assertEquals(13, result.get(0).getWeight());
        assertEquals(new UUID(0, 2), result.get(1).getRuleIdentifier());
        assertEquals(13, result.get(1).getWeight());
    }

    @Test
    public void twoRulesWithOneInFuture() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", now.minus(Period.ofWeeks(4)),
                now.plus(Period.ofWeeks(4)), 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", now.plus(Period.ofWeeks(4)),
                now.plus(Period.ofWeeks(12)), 2L, "1.2");
        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        checkMatch(this.inputC, now, node, 1L);
        checkMatch(this.inputC, now.plus(Period.ofWeeks(8)), node, 2L);

        checkNoMatch(this.inputA, now, node);
        checkNoMatch(this.inputB, now, node);
        checkNoMatch(this.inputD, now, node);
        checkNoMatch(this.inputE, now, node);
        checkNoMatch(this.inputF, now, node);
    }

    @Test
    public void threeRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();
        final Instant start = now.minus(Period.ofWeeks(1));
        final Instant finish = now.plus(Period.ofWeeks(4));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", start, finish, 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", start, finish, 2L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE", start, finish, 3L, "1.4");
        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        checkMatch(this.inputA, now, node, 3L);
        checkMatch(this.inputC, now, node, 2L);

        checkNoMatch(this.inputB, now, node);
        checkNoMatch(this.inputD, now, node);
        checkNoMatch(this.inputE, now, node);
        checkNoMatch(this.inputF, now, node);
    }

    @Test
    public void testFourRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();
        final Instant start = now.minus(Period.ofWeeks(1));
        final Instant finish = now.plus(Period.ofWeeks(4));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", start, finish, 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", start, finish, 2L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE", start, finish, 3L, "1.4");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "*", "*", "US", "*", start, finish, 4L, "1.5");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        checkMatch(this.inputA, now, node, 3L);
        checkMatch(this.inputB, now, node, 4L);
        checkMatch(this.inputC, now, node, 4L);

        checkNoMatch(this.inputD, now, node);
        checkNoMatch(this.inputE, now, node);
        checkNoMatch(this.inputF, now, node);
    }

    @Test
    public void testFiveRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();
        final Instant start = now.minus(Period.ofWeeks(1));
        final Instant finish = now.plus(Period.ofWeeks(4));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", start, finish, 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", start, finish, 2L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE", start, finish, 3L, "1.4");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "*", "*", "US", "*", start, finish, 4L, "1.5");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "*", "*", "US", "*", start, finish, 5L, "1.2");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        checkMatch(this.inputA, now, node, 3L);
        checkMatch(this.inputB, now, node, 4L);
        checkMatch(this.inputC, now, node, 4L);
        checkMatch(this.inputE, now, node, 5L);

        checkNoMatch(this.inputD, now, node);
        checkNoMatch(this.inputF, now, node);
    }

    @Test
    public void testSixRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = ruleSetBuilder();
        final Instant now = Instant.now();
        final Instant start = now.minus(Period.ofWeeks(1));
        final Instant finish = now.plus(Period.ofWeeks(4));
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", start, finish, 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", start, finish, 2L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE", start, finish, 3L, "1.4");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "*", "*", "US", "*", start, finish, 4L, "1.5");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "*", "*", "US", "*", start, finish, 5L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "*", "*", "UK", "*", start, finish, 6L, "1.1");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        checkMatch(this.inputA, now, node, 3L);
        checkMatch(this.inputB, now, node, 4L);
        checkMatch(this.inputC, now, node, 4L);
        checkMatch(this.inputD, now, node, 6L);
        checkMatch(this.inputE, now, node, 5L);
        checkMatch(this.inputF, now, node, 6L);
    }

    @Test
    public void testAlternativePathsFailurePathsAndValueGroups() {
        final Instant now = Instant.now();
        final Instant start = now.minus(Period.ofWeeks(1));
        final Instant finish = now.plus(Period.ofWeeks(4));

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator(Arrays.asList(
                "MARKUP_CODE", "CLIENT_ACCOUNT", "CLIENT_SPN", "ADVISOR_SPN", "EXECUTION_VENUE", "MEMBERSHIP",
                "SECURITY_TYPE", "CONTRACT", "FEE_GROUP", "PRODUCT_GROUP", "EXCHANGE", "REGION"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:VG1589CLM:0151488:4679563:7888805", "*", "*", "*", "*", "381", "Equity indices",
                        "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::start, start)
                .with(RuleBuilder::end, finish)
                .with(RuleBuilder::setId, new UUID(0, 1L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:VG1589CLM:0151488:4679563:7888805", "*", "*", "*", "*", "*", "*", "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::start, start)
                .with(RuleBuilder::end, finish)
                .with(RuleBuilder::setId, new UUID(0, 2L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:AdVG1589CLM:0151488:3679563:6888805", "*", "*", "*", "*", "*", "*", "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::start, start)
                .with(RuleBuilder::end, finish)
                .with(RuleBuilder::setId, new UUID(0, 3L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:VG1589CLM:0151488:4679563:7888805", "ELEC", "NMEM", "*", "*", "123", "Equity indices",
                        "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::start, start)
                .with(RuleBuilder::end, finish)
                .with(RuleBuilder::setId, new UUID(0, 4L)));

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        final List<String> inputs = Arrays.asList("CLM", "A102059551", "0152035", "0151488",
                "ELEC", "NMEM", "FUTURE", "Y2", "381", "Equity indices", "IFLL", "ER");
        final Optional<UUID> result = Evaluator.singleEvaluate(inputs, now, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 1L), result.get());
    }

    private Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder() {
        return RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
    }

    private void checkNoMatch(final List<String> input, final Instant time, final TreeNode node) {
        final Optional<UUID> result = Evaluator.singleEvaluate(input, time, node);
        assertFalse(result.isPresent());
    }

    private void checkMatch(final List<String> input, final Instant time, final TreeNode node, final long id) {
        final Optional<UUID> result = Evaluator.singleEvaluate(input, time, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, id), result.get());
    }

    private TreeNode constructTree(final DecisionTreeRuleSet ruleSet) {
        return DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTreeType.DATED);
    }
}