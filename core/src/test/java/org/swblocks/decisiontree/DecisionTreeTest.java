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
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.Result;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test Cases for {@link DecisionTree}.  Uses the test CommisionRuleSet from {@link CommisionRuleSetSupplier}
 */
public class DecisionTreeTest {
    @Test
    public void testDecisionTreeEvaluation() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE);
        assertNotNull(decisionTree);

        final Input input = decisionTree.createInputs("VOICE", "CME", "ED", "US", "RATE");

        final Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));
    }

    @Test
    public void testDecisionTreeAdditionalEvaluation() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(
                new Loader<DecisionTreeRuleSet>() {
                    @Override
                    public boolean test(final Result result) {
                        return false;
                    }

                    @Override
                    public Result<DecisionTreeRuleSet> get() {
                        return Result.success(
                                CommisionRuleSetSupplier.getCommissionRuleSetWithExtraEvaluations()
                                        .build());
                    }
                },
                DecisionTreeType.SINGLE);

        final Input input = decisionTree.createInputs("VOICE", "CME", "ED", "US", "RATE");

        final Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));
    }

    @Test
    @Deprecated
    public void testDecisionTreeEvaluationWithDeprecatedMethod() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE);
        assertNotNull(decisionTree);

        final Input input = decisionTree.createInputs("VOICE", "CME", "ED", "US", "RATE");

        final Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));
    }

    @Test
    public void testDecisionTreeEvaluationWithNullDatedEvaluationDate() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE);
        assertNotNull(decisionTree);

        final Instant evaluation = null;
        final Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        final Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));
    }

    @Test
    public void slicedDecisionTreeEvaluation() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(
                new Loader<DecisionTreeRuleSet>() {
                    @Override
                    public boolean test(final Result result) {
                        return false;
                    }

                    @Override
                    public Result<DecisionTreeRuleSet> get() {
                        return Result.success(CommisionRuleSetSupplier.getSlicedRuleSet().build());
                    }
                },
                DecisionTreeType.SLICED);

        assertNotNull(decisionTree);
        testDatedDecisionTree(decisionTree);
    }

    @Test
    public void datedDecisionTreeEvaluation() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(
                new Loader<DecisionTreeRuleSet>() {
                    @Override
                    public boolean test(final Result result) {
                        return false;
                    }

                    @Override
                    public Result<DecisionTreeRuleSet> get() {
                        return Result.success(CommisionRuleSetSupplier.getSlicedRuleSet().build());
                    }
                },
                DecisionTreeType.DATED);

        assertNotNull(decisionTree);
        testDatedDecisionTree(decisionTree);
    }

    private void testDatedDecisionTree(final DecisionTree decisionTree) {
        testDatedDecisionTreeForApril10(decisionTree, Instant.parse("2013-04-10T00:00:00.Z"));
        testDatedDecisionTreeForApril10(decisionTree, ZonedDateTime.parse("2013-04-10T00:00:00+05:30",
                ISO_OFFSET_DATE_TIME).toInstant());

        testDatedDecisionTreeForApril1(decisionTree, ZonedDateTime.parse("2013-03-31T22:00:00-05:30",
                ISO_OFFSET_DATE_TIME).toInstant());
        testDatedDecisionTreeForMarch(decisionTree, ZonedDateTime.parse("2013-03-31T22:00:00+04:00",
                ISO_OFFSET_DATE_TIME).toInstant());

        testDatedDecisionTreeForApril4(decisionTree, ZonedDateTime.parse("2013-04-04T21:00:00-05:30",
                ISO_OFFSET_DATE_TIME).toInstant());
    }

    private void testDatedDecisionTreeForApril10(final DecisionTree decisionTree, final Instant evaluation) {

        Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.6", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
    }

    private void testDatedDecisionTreeForApril1(final DecisionTree decisionTree, final Instant evaluation) {

        Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.2", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "DMA", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.3", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
    }

    private void testDatedDecisionTreeForApril4(final DecisionTree decisionTree, final Instant evaluation) {

        Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.5", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.2", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "DMA", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
    }

    private void testDatedDecisionTreeForMarch(final DecisionTree decisionTree, final Instant evaluation) {

        Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.1", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
    }

    @Test
    public void datedRangeDecisionTreeEvaluation() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(
                new Loader<DecisionTreeRuleSet>() {
                    @Override
                    public boolean test(final Result result) {
                        return false;
                    }

                    @Override
                    public Result<DecisionTreeRuleSet> get() {
                        return Result.success(CommisionRuleSetSupplier.getCommissionRuleSetWithDateRanges().build());
                    }
                },
                DecisionTreeType.SINGLE);

        assertNotNull(decisionTree);

        Input input = decisionTree.createInputs("2013-04-10T00:00:00.Z", "VOICE", "CME", "ED", "US", "RATE");
        Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.6", results.get().results().get("Rate"));

        input = decisionTree.createInputs("2013-04-05T00:00:00.Z", "VOICE", "CME", "ED", "US", "RATE");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.5", results.get().results().get("Rate"));

        input = decisionTree.createInputs("2013-03-31T22:00:00.Z", "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));

        input = decisionTree.createInputs("2013-03-31T22:00:00.Z", "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getSingleEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.1", results.get().results().get("Rate"));
    }

    @Test
    public void evaluationDecisionTreeEvaluation() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(
                new Loader<DecisionTreeRuleSet>() {
                    @Override
                    public boolean test(final Result result) {
                        return false;
                    }

                    @Override
                    public Result<DecisionTreeRuleSet> get() {
                        final Builder<RuleSetBuilder, DecisionTreeRuleSet> commissionRuleSet =
                                CommisionRuleSetSupplier.getCommisionRuleSet();
                        commissionRuleSet.with(RuleSetBuilder::rule, RuleBuilder.creator()
                                .with(RuleBuilder::input, Arrays.asList("VOICE", "LME", "FE", "UK", "METAL"))
                                .with(RuleBuilder::evaluations, Collections.singletonList("IR:50|100"))
                                .with(RuleBuilder::setId, new UUID(0L, 6))
                                .with(RuleBuilder::setCode, new UUID(0L, 6))
                                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.6")));
                        return Result.success(commissionRuleSet.build());
                    }
                },
                DecisionTreeType.SINGLE);

        assertNotNull(decisionTree);

        Input input = decisionTree.createInputs(Collections.singletonMap("FEE", "55"),
                "VOICE", "LME", "FE", "UK", "METAL");
        List<OutputResults> results = decisionTree.getEvaluationsFor(input);
        assertTrue(CollectionUtils.isNotEmpty(results));
        assertEquals("1.6", results.get(0).results().get("Rate"));

        // Fee set to 5, blocks Rule 6, defaults down to wildcard rule 5
        input = decisionTree.createInputs(Collections.singletonMap("FEE", "5"),
                "VOICE", "LME", "FE", "UK", "METAL");
        results = decisionTree.getEvaluationsFor(input);
        assertTrue(CollectionUtils.isNotEmpty(results));
        assertEquals("1.1", results.get(0).results().get("Rate"));
    }

    @Test
    public void integerRangeDecisionTreeEvaluation() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(
                new Loader<DecisionTreeRuleSet>() {
                    @Override
                    public boolean test(final Result result) {
                        return false;
                    }

                    @Override
                    public Result<DecisionTreeRuleSet> get() {
                        return Result.success(CommisionRuleSetSupplier.getCommisionRuleSetWithNotionalRanges().build());
                    }
                },
                DecisionTreeType.SINGLE);

        assertNotNull(decisionTree);

        Input input = decisionTree.createInputs("ELECTRONIC", "CME", "S&P", "US", "INDEX", "1000");
        List<OutputResults> results = decisionTree.getEvaluationsFor(input);
        assertFalse(results.isEmpty());
        assertThat(results.size(), is(1));
        assertEquals("1.11", results.get(0).results().get("Rate"));

        input = decisionTree.createInputs("ELECTRONIC", "CME", "S&P", "US", "INDEX", "9000");
        results = decisionTree.getEvaluationsFor(input);
        assertFalse(results.isEmpty());
        assertThat(results.size(), is(1));
        assertEquals("1.12", results.get(0).results().get("Rate"));

        input = decisionTree.createInputs("ELECTRONIC", "LSE", "L", "UK", "INDEX", "2000");
        results = decisionTree.getEvaluationsFor(input);
        assertFalse(results.isEmpty());
        assertThat(results.size(), is(2));
        assertThat(results.stream().map(r -> r.results().get("Rate")).collect(Collectors.toList()),
                IsCollectionContaining.hasItems("1.17", "1.18"));
    }

    @Test
    public void createsDefaultInput() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE);
        assertNotNull(decisionTree);

        final Input input = decisionTree.createInputs();
        assertNotNull(input);
    }

    @Test
    public void findsNoMatch() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE);

        assertNotNull(decisionTree);

        final Input input = decisionTree.createInputs("VOICE", "CME", "ED", "EU", "PERCENT");

        final Optional<OutputResults> results = decisionTree.getSingleEvaluationFor(input);
        assertFalse(results.isPresent());
        assertEquals(Optional.empty(), results);
    }
}