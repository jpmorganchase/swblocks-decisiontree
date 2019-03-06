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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.TreeNode;

/**
 * Evaluates the decision tree given the fully populated list of inputs and the root node.
 */
public final class Evaluator {
    /**
     * Private constructor to enforce static use.
     */
    private Evaluator() {
    }

    /**
     * Evaluates the dated decision tree using the supplied time.  Only one result is returned, if multiple nodes
     * with the same weight, then only the first node is returned.
     *
     * <p>When the decision tree is for a single decision tree, then the date is ignored. For dated decision trees the
     * time is used in the construction of the decision tree.
     *
     * @param searchInputs List of String inputs to search the decision tree for.  The size of the list must match the
     *                     number of drivers in the ruleset the decision tree is based on.
     * @param time         The point in time at which the evaluation is taking place
     * @param rootNode     The starting node of the search
     * @return the Output unique Id of the rule found or {@code Optional.empty} if no match
     */
    public static Optional<UUID> singleEvaluate(final List<String> searchInputs, final Instant time,
                                                final TreeNode rootNode) {
        final List<UUID> results = evaluate(searchInputs, time, Collections.emptyList(), rootNode);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }

    /**
     * Evaluates the dated decision tree using the supplied time.  All matching nodes with the highest weighting are
     * returned.
     *
     * <p>When the decision tree is for a single decision tree, then the date is ignored. For dated decision trees the
     * time is used in the construction of the decision tree.
     *
     * @param searchInputs List of String inputs to search the decision tree for.  The size of the list must match the
     *                     number of drivers in the ruleset the decision tree is based on.
     * @param time         The point in time at which the evaluation is taking place
     * @param rootNode     The starting node of the search
     * @return List of highest weighted matching unique id's or {@code Collections.emptyList} if no match
     */
    public static List<UUID> evaluate(final List<String> searchInputs, final Instant time,
                                      final List<String> evaluationInputs,
                                      final TreeNode rootNode) {
        final List<EvaluationResult> results = evaluateAllResults(searchInputs, time, rootNode);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        final List<EvaluationResult> evaluatedResults = new ArrayList<>(results.size());
        for (final EvaluationResult id : results) {
            final Optional<InputDriver[]> evaluations = id.getEvaluations();
            if (evaluations.isPresent()) {
                for (int i = 0; i < evaluations.get().length; i++) {
                    final InputDriver driver = evaluations.get()[i];
                    final boolean evaluate = driver.evaluate(evaluationInputs.get(i));
                    if (evaluate) {
                        evaluatedResults.add(id);
                    }
                }
            } else {
                evaluatedResults.add(id);
            }
        }

        if (evaluatedResults.isEmpty()) {
            return Collections.emptyList();
        } else if (evaluatedResults.size() == 1) {
            return Collections.singletonList(evaluatedResults.get(0).getRuleIdentifier());
        }
        final List<UUID> bestResults = new ArrayList<>(evaluatedResults.size());
        EvaluationResult bestNode = evaluatedResults.get(0);
        for (final EvaluationResult result : evaluatedResults) {
            if (result.getWeight() > bestNode.getWeight()) {
                bestResults.clear();
                bestNode = result;
                bestResults.add(result.getRuleIdentifier());
            } else if (result.getWeight() == bestNode.getWeight()) {
                bestResults.add(result.getRuleIdentifier());
            }
        }
        return bestResults;
    }

    /**
     * Evaluates the decision tree using the supplied time and returns multiple matching {@link EvaluationResult} nodes
     * where each node is a successful evaluation path.  The weight in each result gives the best fit indication.
     *
     * <p>Multiple dated nodes are returned where there are two or more possible matches for a node, not including the
     * wildcard.
     *
     * @param searchInputs List of String inputs to search the decision tree for.  The size of the list must match the
     *                     number of drivers in the ruleset the decision tree is based on.
     * @param time         The point in time at which the evaluation is taking place
     * @param rootNode     The starting node of the search
     * @return A list of {@link EvaluationResult} that can be used to determine the best fit
     */
    public static List<EvaluationResult> evaluateAllResults(final List<String> searchInputs, final Instant time,
                                                            final TreeNode rootNode) {
        final List<TreeNode> nextNodes = rootNode.getEvaluatedNodes(searchInputs, time);

        final List<EvaluationResult> results = new ArrayList<>(1);
        for (final TreeNode nextNode : nextNodes) {
            if (nextNode instanceof EvaluationResult) {
                results.add((EvaluationResult) nextNode);
            } else {
                results.addAll(evaluateAllResults(searchInputs, time, nextNode));
            }
        }
        return results;
    }
}
