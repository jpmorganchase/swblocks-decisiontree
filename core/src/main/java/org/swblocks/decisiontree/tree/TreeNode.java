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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.swblocks.jbl.util.DateRange;

/**
 * TreeNode is the base interface for all decision tree nodes.
 */

public interface TreeNode {
    /**
     * Returns a stream of TreeNodes for operating on.
     *
     * @return stream of {@code TreeNodes}
     */
    Stream<TreeNode> stream();

    /**
     * Optimise tree nodes.
     */
    default void optimise() {
    }

    /**
     * Evaluates the input value against the node and returns if the node matches the input. The evaluation depends on
     * the type of implementation of the {@code TreeNode}
     *
     * @param input input value to evaluate against.
     * @return if the node matches the input.
     */
    boolean evaluate(final String input);

    /**
     * Returns a list of {@code TreeNode} under this node which evaluates true given the input list of data and specific
     * point in time. The node knows which driver position is represents in the list of input data to evaluate against.
     *
     * <p>Used for evaluation of the tree.  The tree evaluation can jump between nodes during evaluation
     *
     * @param inputs input list to evaluate.
     * @param time   the point in time at which evaluation takes place.
     * @return {@link List} TreeNode, populated if found.
     */
    List<TreeNode> getEvaluatedNodes(final List<String> inputs, final Instant time);

    /**
     * Returns the {@code TreeNode} under this node which equals the {code TreeNode} passed in.
     *
     * <p>Used for construction of the tree.
     *
     * @param inputValue node to check for equality .
     * @return {@link Optional} TreeNode, populated if found, {@code Optional.empty} if not.
     */
    Optional<TreeNode> getExactNode(final TreeNode inputValue);

    /**
     * Adds a new node to leaf elements of this node if there is not an equal one already on the node.
     *
     * <p>The {@code TreeNode} returned is the method parameter if the node does not exist in the list of tree nodes.
     * If there is a node that matches the newNode, the node matching equality is returned.
     *
     * @param newNode new {@code TreeNode} to add.
     * @return the {@code TreeNode} in the list.
     */
    TreeNode addNode(final TreeNode newNode);

    /**
     * Returns the enum that is associated with this tree node.
     *
     * @return the {@link InputValueType}
     */
    InputValueType getDriverType();

    /**
     * Method to return the value of the node.
     *
     * @return the node value
     */
    String getValue();

    /**
     * Method to return the {@link InputDriver} associated with this node.
     *
     * @return the driver
     */
    InputDriver getDriver();

    /**
     * Method to get the effective date range of the node.
     *
     * @return the node date range
     */
    DateRange getDateRange();

    default void setDateRange(DateRange dateRange){}
}
