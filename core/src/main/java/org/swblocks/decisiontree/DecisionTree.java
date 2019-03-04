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
import java.util.*;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.tree.DecisionTreeFactory;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.TimeSlicedRootNode;
import org.swblocks.decisiontree.tree.TreeNode;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;
import org.swblocks.jbl.util.retry.ActionRetrier;
import org.swblocks.jbl.util.retry.Retrier;

/**
 * Main accessor class for operating on a decision tree.
 */
public class DecisionTree {
    protected final DecisionTreeRuleSet ruleSet;
    private final Loader<DecisionTreeRuleSet> loader;
    private final DecisionTreeType type;
    private TreeNode node;

    protected DecisionTree(final Loader<DecisionTreeRuleSet> loader,
                           final DecisionTreeType type,
                           final DecisionTreeRuleSet decisionTreeRuleSet) {
        this.loader = loader;
        this.type = type;
        ruleSet = decisionTreeRuleSet;
        initialiseRootNode();
    }

    /**
     * Creates an instance of the {@link DecisionTree} with the specified {@link Loader}
     *
     * @param loader {@link Loader} to use to instantiate the underlying {@link DecisionTreeRuleSet}
     * @param type   {@link DecisionTreeType} Type of evaluation tree to create.
     * @return instance of {@link DecisionTree}
     */
    public static DecisionTree instanceOf(final Loader<DecisionTreeRuleSet> loader,
                                          final DecisionTreeType type) {
        final ActionRetrier<DecisionTreeRuleSet> retrier = Retrier.createNonRetrier();
        final Result<DecisionTreeRuleSet> result = retrier.run(loader, booleanResult -> false);
        EhSupport.checkResult(result);

        return new DecisionTree(loader, type, result.getData());
    }

    /**
     * Creates the holder to populate the search values for the Decision Tree evaluation.
     *
     * @return {@link Input}
     */
    public Input createInputs() {
        return Input.create(ruleSet.getName(), ruleSet.getWeightedDrivers());
    }

    /**
     * Creates the holder and populates the search values for the Decision Tree evaluation.
     *
     * @param searchValues vararg search values in weighted order.
     * @return {@link Input}
     */
    public Input createInputs(final String... searchValues) {
        return Input.create(ruleSet.getName(), ruleSet.getWeightedDrivers(), searchValues);
    }

    /**
     * Creates the holder and populates the search values for the Decision Tree evaluation.
     * Includes a Map of evaluation criteria to be applied to the nodes
     *
     * @param evaluationsMap map of evaluation names to actual values.
     * @param searchValues vararg search values in weighted order.
     * @return {@link Input}
     */
    public Input createInputs(final Map<String, String> evaluationsMap, final String... searchValues) {
        return Input.create(ruleSet.getName(), ruleSet.getWeightedDrivers(), Instant.now(), evaluationsMap,
                searchValues);
    }

    /**
     * Creates the holder and populates the search values for the Decision Tree evaluation.
     *
     * @param evaluationDate date of evaluation for rules.
     * @param searchValues   vararg search values in weighted order.
     * @return {@link Input}
     */
    public Input createInputs(final Instant evaluationDate, final String... searchValues) {
        if (evaluationDate == null) {
            return createInputs(searchValues);
        }

        return Input.create(ruleSet.getName(), ruleSet.getWeightedDrivers(), evaluationDate, searchValues);
    }

    /**
     * Evaluates the {@link Input} data against the Decision Tree.
     *
     * <p>Decision tree root nodes are cached in this class when evaluating single node trees or dated time trees. The
     * time sliced root node caches the individual time sliced trees separately and so the caching is delegated to the
     * {@link TimeSlicedRootNode} class.
     *
     * @param input Search values to be used.
     * @return {@link Optional} Output holder for the results.
     */
    public Optional<OutputResults> getSingleEvaluationFor(final Input input) {
        final TreeNode rootNodeToTree = node;
        final Optional<UUID> result = Evaluator.singleEvaluate(input.getEvaluationInputs(), input.getEvaluationDate(),
                rootNodeToTree);
        if (result.isPresent()) {
            return Optional.of(new OutputResults(ruleSet.getRules().get(result.get())));
        }
        return Optional.empty();
    }

    /**
     * @param input Search values to be used.
     * @return {@link Optional} Output holder for the results.
     * @deprecated use {@link #getSingleEvaluationFor(Input)}. Evaluates the {@link Input} data against the Decision
     * Tree.
     *
     * <p>Decision tree root nodes are cached in this class when evaluating single node trees or dated time trees. The
     * time sliced root node caches the individual time sliced trees separately and so the caching is delegated to the
     * {@link TimeSlicedRootNode} class.
     */
    @Deprecated
    public Optional<OutputResults> getEvaluationFor(final Input input) {
        return getSingleEvaluationFor(input);
    }

    /**
     * Evaluates the {@link Input} data against the Decision Tree returning all highest weighted results.
     *
     * <p>Decision tree root nodes are cached in this class when evaluating single node trees or dated time trees. The
     * time sliced root node caches the individual time sliced trees separately and so the caching is delegated to the
     * {@link TimeSlicedRootNode} class.
     *
     * @param input Search values to be used.
     * @return {@link List} of highest weighted results.
     */
    public List<OutputResults> getEvaluationsFor(final Input input) {
        final List<UUID> result = Evaluator.evaluate(input.getEvaluationInputs(), input.getEvaluationDate(),
                input.getEvaluationMap(), node);
        return result.stream().map(uuid ->
                new OutputResults(ruleSet.getRules().get(uuid))).collect(Collectors.toList());
    }

    protected void initialiseRootNode() {
        node = DecisionTreeFactory.constructDecisionTree(ruleSet, type);
    }
}