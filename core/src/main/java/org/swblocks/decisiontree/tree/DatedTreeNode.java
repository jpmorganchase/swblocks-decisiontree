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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.swblocks.jbl.util.Range;

/**
 * Specialised {@link TreeNode} that includes a {@link Range<Instant>} for when the node is active.
 *
 * <p>Nodes are stored in {@link DecisionTreeType#DATED} decision trees.
 */
final class DatedTreeNode extends BaseTreeNode {
    private Range<Instant> range;

    DatedTreeNode(final InputDriver driver, final int level, final Range<Instant> dateRange) {
        super(driver, level);
        this.range = dateRange;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(super.equals(obj))) {
            return false;
        }

        final Range<Instant> other = ((BaseTreeNode) obj).getDateRange();
        return Objects.equals(this.range, other) || inRange(other.getStart(), this.range) ||
                inRange(other.getFinish(), this.range);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.range.hashCode();
    }

    @Override
    public Range<Instant> getDateRange() {
        return this.range;
    }

    @Override
    public void setDateRange(final Range<Instant> range) {
        this.range = range;
    }

    @Override
    public Optional<TreeNode> getExactNode(final TreeNode inputValue) {
        final Optional<TreeNode> match = Optional.ofNullable(this.nextNodes.get(
                new DatedNodeKey(inputValue.getValue(), inputValue.getDateRange())));

        if (match.isPresent()) {
            return match;
        }

        for (final TreeNode node : this.nextNodes.values()) {
            if (node.equals(inputValue)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    @Override
    public TreeNode addNode(final TreeNode newNode) {
        final Optional<TreeNode> node = getExactNode(newNode);
        if (node.isPresent()) {
            final TreeNode exactNode = node.get();
            updateDateRange(exactNode, newNode.getDateRange());
            return exactNode;
        } else {
            this.nextNodes.put(new DatedNodeKey(newNode.getValue(), newNode.getDateRange()), newNode);
        }
        return newNode;
    }

    @Override
    public List<TreeNode> getEvaluatedNodes(final List<String> inputs, final Instant time) {
        final String inputValue = inputs.get(this.getDriverLevel());
        final List<TreeNode> nodes = getEvaluatedNodes(inputValue, time);

        if (!nodes.isEmpty()) {
            return nodes;
        }

        return Collections.emptyList();
    }

    private List<TreeNode> getEvaluatedNodes(final String input, final Instant time) {
        final List<TreeNode> results = new ArrayList<>(1);
        for (final TreeNode node : this.nextNodes.values()) {
            if ((node.evaluate(input) ||
                    node.getValue().equals(InputValueType.WILDCARD)) &&
                    inRange(time, node.getDateRange())) {
                results.add(node);
            }
        }
        return results;
    }

    private boolean inRange(final Instant time, final Range<Instant> range) {
        final boolean isAfterStart = time.compareTo(range.getStart()) >= 0;
        final boolean isBeforeEnd = time.compareTo(range.getFinish()) <= 0;

        return isAfterStart && isBeforeEnd;
    }

    private void updateDateRange(final TreeNode exactNode, final Range<Instant> range) {
        final Range<Instant> original = exactNode.getDateRange();
        Instant start = original.getStart();
        Instant finish = original.getFinish();

        if (start.isAfter(range.getStart())) {
            start = range.getStart();
        }
        if (finish.isBefore(range.getFinish())) {
            finish = range.getFinish();
        }

        if (start.compareTo(original.getStart()) != 0 ||
                finish.compareTo(original.getFinish()) != 0) {
            this.nextNodes.remove(new DatedNodeKey(exactNode.getValue(), original));

            exactNode.setDateRange(new Range<Instant>(start, finish));
            this.nextNodes.put(
                    new DatedNodeKey(exactNode.getValue(), exactNode.getDateRange()), exactNode);
        }
    }
}
