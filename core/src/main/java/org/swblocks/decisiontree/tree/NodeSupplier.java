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
import java.util.function.Supplier;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.jbl.util.Range;

/**
 * Class that creates the various types of tree node that are used in the construction of the decision tree.
 *
 * <p>For example, a string tree node is created in the following way using the {@link StringDriver}:
 * <blockquote><pre>
 *     TreeNode treeNode = NodeSupplier.createTreeNode(new StringDriver("value"), 0,).get();
 * </pre></blockquote>
 *
 * <p>The tree node is created with the {@link StringDriver} which provides the correct functionality in conjunction
 * with the {@link BaseTreeNode} class.
 *
 * <p>The result tree node is the final leaf in the tree and the behaviour of the underlying tree node must still be
 * known. A result string tree node can be created as follows using the {@link StringDriver}:
 *
 * <blockquote><pre>
 *      TreeNode treeNode = NodeSupplier.createResultTreeNode(new StringDriver("value"), 0, rule).get();
 * </pre></blockquote>
 *
 * <p>The overridden behaviour used by the string tree node is provided by calling the methods on the delegate that is
 * created in the result tree node. The result tree node additionally stores the matching rule and its weight that
 * match
 * the input criteria.
 */
final class NodeSupplier {
    static final int ROOT_NODE_LEVEL = 0;

    static Supplier<TreeNode> createTimeSlicedRootNode(final DecisionTreeRuleSet ruleSet) {
        return () -> new TimeSlicedRootNode(ruleSet);
    }

    static Supplier<TreeNode> createTreeNode(final InputDriver driver, final int level) {
        return () -> new BaseTreeNode(driver, level);
    }

    static Supplier<TreeNode> createDatedTreeNode(final InputDriver driver, final int level,
                                                  final Range<Instant> dateRange) {
        return () -> new DatedTreeNode(driver, level, dateRange);
    }

    private static Supplier<TreeNode> createResultTreeNode(final DecisionTreeRule rule, final TreeNode delegate) {
        return () ->
                new ResultNode(delegate.getDriver(), ((BaseTreeNode) delegate).getDriverLevel(),
                        rule, delegate);
    }

    static Supplier<TreeNode> createResultTreeNode(final InputDriver driver,
                                                   final int level,
                                                   final DecisionTreeRule rule) {
        return createResultTreeNode(rule, createTreeNode(driver, level).get());
    }

    static Supplier<TreeNode> createDatedResultTreeNode(final InputDriver driver,
                                                        final int level,
                                                        final DecisionTreeRule rule) {
        return createResultTreeNode(rule, createDatedTreeNode(driver, level, new Range<>(rule.getStart(),
                rule.getEnd())).get());
    }

    enum Type {
        NODE, RESULT
    }
}