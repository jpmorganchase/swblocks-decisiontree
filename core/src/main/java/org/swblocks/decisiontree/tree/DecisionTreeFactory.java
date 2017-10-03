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

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;

import org.swblocks.decisiontree.Evaluator;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.jbl.util.Range;

/**
 * Factory class used to create the required decision trees.
 *
 * <p>To create the root node for a decision tree with the DecisionTreeFactory, use the following code:
 * <blockquote><pre>
 *     // "Single" Decision Tree
 *     final TreeNode node = DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTree.SINGLE, null);
 *
 *     // "Dated" Decision Tree
 *     final TreeNode datedNode = DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTree.DATED, null);
 *
 *     // "Time Sliced" Decision Tree - the rules used in the tree are matched against the time
 *     final Instant time = Instant.now();
 *     final TreeNode slicedNode = DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTree.SLICED, time);
 * </pre></blockquote>
 *
 * <p>The class will iterate over the rules and construct the correct tree that can be use by the {@link Evaluator}.
 */
public final class DecisionTreeFactory {
    /**
     * Private constructor to enforce static use.
     */
    private DecisionTreeFactory() {
    }

    /**
     * Construct the decision tree for the appropriate {@link DecisionTreeType} type.
     *
     * <p>Once the date range for a time sliced tree is identified, it is built in the same way as a single decision
     * tree.
     *
     * @param ruleSet the supplied rule set
     * @param type    the decision tree type
     * @return the constructed root node containing the full decision tree
     */
    public static TreeNode constructDecisionTree(final DecisionTreeRuleSet ruleSet, final DecisionTreeType type) {
        if (DecisionTreeType.SLICED == type) {
            return NodeSupplier.createTimeSlicedRootNode(ruleSet).get();
        }

        final TreeNode rootNode =
                createNode(new StringDriver("ROOT"),
                        NodeSupplier.ROOT_NODE_LEVEL, type, NodeSupplier.Type.NODE, null);

        for (final DecisionTreeRule rule : ruleSet.getRules().values()) {
            constructTreeFromRule(rootNode, NodeSupplier.ROOT_NODE_LEVEL,
                    new ArrayDeque<>(Arrays.asList(rule.getDrivers())), rule, type);
        }

        if (type == DecisionTreeType.SINGLE) {
            ((BaseTreeNode) rootNode).calculateFailureNode(Optional.empty());
        }

        rootNode.optimise();
        return rootNode;
    }

    private static void constructTreeFromRule(final TreeNode currentNode,
                                              final int currentLevel,
                                              final Queue<InputDriver> inputs,
                                              final DecisionTreeRule rule,
                                              final DecisionTreeType type) {
        final InputDriver value = inputs.poll();

        if (value == null) {
            return;
        }

        TreeNode newNode;
        final int nextLevel = currentLevel + 1;

        if (inputs.peek() == null) {
            newNode = createNode(value, nextLevel, type, NodeSupplier.Type.RESULT, rule);
        } else {
            newNode = createNode(value, nextLevel, type, NodeSupplier.Type.NODE, rule);
        }

        if (currentNode != null) {
            newNode = currentNode.addNode(newNode);
        }

        constructTreeFromRule(newNode, nextLevel, inputs, rule, type);
    }

    private static TreeNode createNode(final InputDriver driverValue,
                                       final int level,
                                       final DecisionTreeType type,
                                       final NodeSupplier.Type treeType,
                                       final DecisionTreeRule rule) {
        if (type == DecisionTreeType.SINGLE) {
            return getSingleTreeNode(driverValue, level, treeType, rule);
        }

        return getDatedTreeNode(driverValue, level, treeType, rule);
    }

    private static TreeNode getDatedTreeNode(final InputDriver driverValue,
                                             final int level,
                                             final NodeSupplier.Type treeType,
                                             final DecisionTreeRule rule) {
        if (treeType == NodeSupplier.Type.NODE) {
            return NodeSupplier.createDatedTreeNode(driverValue, level, getDateRange(rule)).get();
        }

        return NodeSupplier.createDatedResultTreeNode(driverValue, level, rule).get();
    }

    private static TreeNode getSingleTreeNode(final InputDriver driverValue,
                                              final int level,
                                              final NodeSupplier.Type treeType,
                                              final DecisionTreeRule rule) {
        if (treeType == NodeSupplier.Type.NODE) {
            return NodeSupplier.createTreeNode(driverValue, level).get();
        }

        return NodeSupplier.createResultTreeNode(driverValue, level, rule).get();
    }

    private static Range<Instant> getDateRange(final DecisionTreeRule rule) {
        if (rule == null) {
            return new Range<>(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX);
        }

        return new Range<>(rule.getStart(), rule.getEnd());
    }
}
