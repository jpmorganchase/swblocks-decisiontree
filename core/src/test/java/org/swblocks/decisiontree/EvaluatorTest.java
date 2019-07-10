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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.DecisionTreeFactory;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.tree.TreeNode;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.test.utils.JblTestClassUtils;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test Cases for {@link Evaluator} using the {@link DecisionTreeFactory} for a single decision tree.
 */
public class EvaluatorTest {
    @Test
    public void testPrivateConstructor() {
        assertTrue(JblTestClassUtils.assertConstructorIsPrivate(Evaluator.class));
    }

    @Test
    public void testSimpleEvaluatorWithAllInputs() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        final TreeNode node = constructTree(ruleSet);

        Optional<UUID> result = Evaluator.singleEvaluate(Arrays.asList("VOICE", "CME", "ED", "US", "RATE"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 2), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("VOICE", "CME", "EB", "US", "RATE"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 3), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("VOICE", "CME", "S&P", "US", "INDEX"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 3), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("ELECTRONIC", "CME", "S&P", "US", "INDEX"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 1), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("ELECTRONIC", "LIFFE", "L", "UK", "INDEX"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 5), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("ELECTRONIC", "CME", "EB", "US", "RATE"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 4), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("VOICE", "LIFFE", "L", "UK", "INDEX"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 5), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("VOICE", "CME", "ED", "UK", "INDEX"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 0), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("VOICE", "CME", "ED", "UK", "BOND"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 5), result.get());

        result = Evaluator.singleEvaluate(Arrays.asList("ELECTRONIC", "EURONEXT", "DEX", "EU", "BOND"), null, node);
        assertFalse(result.isPresent());

        result = Evaluator.singleEvaluate(Arrays.asList("VOICE", "CME", "ED", "EU", "BOND"), null, node);
        assertFalse(result.isPresent());

    }

    @Test
    public void testEvaluatorWithRegexInput() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommissionRuleSetWithRegex().build();
        final TreeNode node = constructTree(ruleSet);

        final Optional<UUID> result =
                Evaluator.singleEvaluate(Arrays.asList("VOICE", "CME", "NDK", "APAC", "INDEX"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 6), result.get());
    }

    @Test
    public void testEvaluatorWithRegexMultipleRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                CommisionRuleSetSupplier.getCommissionRuleSetWithRegex();

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);

        final List<EvaluationResult> results = Evaluator.evaluateAllResults(Arrays.asList("ELECTRONIC", "CME", "S&P",
                "US", "INDEX"), null, node);

        assertNotNull(results);
        assertEquals(3, results.size());
        final List<UUID> idResults = results.stream().map(EvaluationResult::getRuleIdentifier)
                .collect(Collectors.toList());
        assertThat(idResults, IsCollectionContaining.hasItems(new UUID(0, 0), new UUID(0, 1), new UUID(0, 7)));

        final Optional<UUID> result = Evaluator.singleEvaluate(Arrays.asList("ELECTRONIC", "CME", "S&P",
                "US", "INDEX"), null, node);
        assertTrue(result.isPresent());
        assertEquals(new UUID(0, 7), result.get());
    }

    @Test
    public void testEvaluatorWithRegexMultipleRulesAndWildcards() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                CommisionRuleSetSupplier.getCommissionRuleSetWithRegex();

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);

        final List<EvaluationResult> results = Evaluator.evaluateAllResultsWithWildcards(
                Arrays.asList("ELECTRONIC", "CME", "S&P", "US", "INDEX"), null, node);

        assertNotNull(results);
        assertEquals(4, results.size());
        assertThat(results.stream().map(EvaluationResult::getRuleIdentifier).collect(Collectors.toList()),
                IsCollectionContaining.hasItems(new UUID(0, 0),
                        new UUID(0, 1), new UUID(0, 4),
                        new UUID(0, 7)));
    }

    @Test
    public void testEvaluatorWithAlternativePathsAndValueGroups() {
        // Example taken from an issue using data which generated the error.
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("MARKUP_CODE", "CLIENT_ACCOUNT", "CLIENT_SPN",
                        "ADVISOR_SPN", "EXECUTION_VENUE", "MEMBERSHIP",
                        "SECURITY_TYPE", "CONTRACT", "FEE_GROUP", "PRODUCT_GROUP", "EXCHANGE", "REGION"));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:VG1589CLM:0151488:4679563:7888805", "*", "*", "*", "*", "381", "Equity indices",
                        "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::setId, new UUID(1, 0)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:VG1589CLM:0151488:4679563:7888805",
                        "*", "*", "*", "*", "*", "*", "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::setId, new UUID(2, 0)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:AdVG1589CLM:0151488:3679563:6888805",
                        "*", "*", "*", "*", "*", "*", "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::setId, new UUID(3, 0)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("CLM", "*", "VG:AdVG1589CLM:0152035:03227805:0521659",
                        "VG:VG1589CLM:0151488:4679563:7888805",
                        "ELEC", "NMEM", "*", "*", "123", "Equity indices", "VG:IFEU:IFLL", "ER"))
                .with(RuleBuilder::setId, new UUID(4, 0)));

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);

        final Optional<UUID> result = Evaluator.singleEvaluate(Arrays.asList("CLM", "A102059551", "0152035", "0151488",
                "ELEC", "NMEM", "FUTURE", "Y2", "381", "Equity indices", "IFLL", "ER"), null, node);
        assertTrue(result.isPresent());

        assertEquals(new UUID(1, 0), result.get());
    }

    private TreeNode constructTree(final DecisionTreeRuleSet ruleSet) {
        return DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTreeType.SINGLE);
    }
}