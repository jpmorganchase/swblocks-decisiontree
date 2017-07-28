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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.swblocks.jbl.util.DateRange;

import static java.util.Collections.emptyList;

/**
 * Single tree tree node that can be created within the tree and sub-classed for specialised behaviour.
 *
 * <p>Subclasses are created using the {@link NodeSupplier}
 *
 * <p>The isDeterministic flag is set to true on the assumption that the child nodes will be {@link StringDriver} types.
 * If any other {@link InputDriver} is encountered, the isDeterministic flag is set to false. Deterministic trees are
 * quicker to search as we can evaluate more quickly on the string by retrieving the string value of the key from the
 * nextNodes map.
 */
class BaseTreeNode implements TreeNode {
    private final InputDriver driver;
    private final int driverLevel;
    protected Map<Object, TreeNode> nextNodes = new HashMap<>(4);
    private boolean isDeterministic = true;
    private Optional<TreeNode> failureNode;

    public BaseTreeNode(final InputDriver driver, final int driverLevel) {
        this.driver = driver;
        this.driverLevel = driverLevel;
        this.failureNode = Optional.empty();
    }

    @Override
    public String getValue() {
        return this.driver.getValue();
    }

    @Override
    public Stream<TreeNode> stream() {
        return this.nextNodes.values().stream();
    }

    @Override
    public void optimise() {
        if (this.nextNodes.size() == 1) {
            final Map<Object, TreeNode> nodes = new HashMap<>(1, 1L);
            nodes.putAll(this.nextNodes);
            this.nextNodes = nodes;
        }

        for (final TreeNode node : this.nextNodes.values()) {
            node.optimise();
        }
    }

    @Override
    public boolean evaluate(final String input) {
        return this.driver.evaluate(input);
    }

    @Override
    public InputValueType getDriverType() {
        return getDriver().getType();
    }

    @Override
    public List<TreeNode> getEvaluatedNodes(final List<String> inputs, final Instant time) {
        final String value = inputs.get(this.driverLevel);
        final List<TreeNode> nodes = getEvaluatedNodes(value);
        if (!nodes.isEmpty()) {
            return nodes;
        }

        if (this.failureNode.isPresent()) {
            return Collections.singletonList(getFailureNode().get());
        }

        return emptyList();
    }

    private List<TreeNode> getEvaluatedNodes(final String input) {
        final Optional<TreeNode> matches = Optional.ofNullable(this.nextNodes.get(input));

        if (matches.isPresent() && this.isDeterministic()) {
            return Collections.singletonList(matches.get());
        }

        final List<TreeNode> results = new ArrayList<>(1);

        if (matches.isPresent()) {
            final TreeNode match = matches.get();
            if (match.getDriverType() == InputValueType.STRING) {
                results.add(matches.get());
            }
        }

        for (final TreeNode node : this.nextNodes.values()) {
            if (node.getDriverType() != InputValueType.STRING && node.getDriver().evaluate(input)) {
                results.add(node);
            }
        }

        return results;
    }

    /**
     * Calculation of the failure nodes iterates down the left most side of the tree identifying the wildcode names and
     * then sets these wildcard nodes as the failure nodes as it recursively moves back along the tree.
     *
     * @param currentFailureNode current reference to the node to add as the failure noe.
     */
    void calculateFailureNode(final Optional<TreeNode> currentFailureNode) {
        final Optional<TreeNode> wildCardNode =
                Optional.ofNullable(this.nextNodes.get(InputValueType.WILDCARD));
        if (wildCardNode.isPresent()) {
            ((BaseTreeNode) wildCardNode.get()).calculateFailureNode(currentFailureNode);
            this.failureNode = wildCardNode;
        } else {
            this.failureNode = currentFailureNode;
        }

        for (final TreeNode node : this.nextNodes.values()) {
            if (!InputValueType.WILDCARD.equals(node.getValue())) {
                ((BaseTreeNode) node).calculateFailureNode(this.failureNode);
            }
        }
    }

    Optional<TreeNode> getFailureNode() {
        return this.failureNode;
    }

    @Override
    public DateRange getDateRange() {
        return null;
    }

    void setDateRange(final DateRange range) {
        // do nothing
    }

    @Override
    public InputDriver getDriver() {
        return this.driver;
    }

    @Override
    public Optional<TreeNode> getExactNode(final TreeNode inputValue) {
        return Optional.ofNullable(this.nextNodes.get(inputValue.getValue()));
    }

    int getDriverLevel() {
        return this.driverLevel;
    }

    public boolean isDeterministic() {
        return this.isDeterministic;
    }

    public void setDeterministic(final boolean deterministic) {
        this.isDeterministic = deterministic;
    }

    @Override
    public TreeNode addNode(final TreeNode newNode) {
        if (newNode.getDriverType() != InputValueType.STRING) {
            this.setDeterministic(false);
        }

        final Optional<TreeNode> node = getExactNode(newNode);
        if (node.isPresent()) {
            return node.get();
        } else {
            this.nextNodes.put(newNode.getValue(), newNode);
        }

        return newNode;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        return this.driver.equals(((BaseTreeNode) other).getDriver());
    }

    @Override
    public int hashCode() {
        return this.driver.getValue().hashCode();
    }
}