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
import java.util.Optional;

import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.eh.Result;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

        final Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
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

        Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.6", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
    }

    private void testDatedDecisionTreeForApril1(final DecisionTree decisionTree, final Instant evaluation) {

        Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.2", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "DMA", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.3", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
    }

    private void testDatedDecisionTreeForApril4(final DecisionTree decisionTree, final Instant evaluation) {

        Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.5", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.2", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "DMA", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
    }

    private void testDatedDecisionTreeForMarch(final DecisionTree decisionTree, final Instant evaluation) {

        Input input = decisionTree.createInputs(evaluation, "VOICE", "CME", "ED", "US", "RATE");

        Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "CME", "S&P", "US", "INDEX");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.1", results.get().results().get("Rate"));

        input = decisionTree.createInputs(evaluation, "VOICE", "NYMEX", "AMZ", "US", "SSO");
        results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.9", results.get().results().get("Rate"));
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

        final Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
        assertFalse(results.isPresent());
        assertEquals(Optional.empty(), results);
    }
}