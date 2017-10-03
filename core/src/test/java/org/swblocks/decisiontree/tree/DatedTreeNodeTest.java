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
import java.time.Period;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.swblocks.jbl.util.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link DatedTreeNode} created using the {@link NodeSupplier}.
 */
public class DatedTreeNodeTest {
    private static final Instant NOW = Instant.now();

    @Test
    public void equalsCorrect() {
        final Instant start = NOW.minus(Period.ofWeeks(5));
        final Instant end = NOW.plus(Period.ofWeeks(5));

        final TreeNode node = createDatedTreeNode("Test1", start, end);

        TreeNode other = createDatedTreeNode("Test1", start, end);
        assertTrue(node.equals(other));

        other = createDatedTreeNode("Test1", start, Instant.MAX);
        assertTrue(node.equals(other));

        other = createDatedTreeNode("Test1", NOW, Instant.MAX);
        assertTrue(node.equals(other));

        other = createDatedTreeNode("Test1", end, Instant.MAX);
        assertTrue(node.equals(other));

        other = createDatedTreeNode("Test1", Instant.MIN, start);
        assertTrue(node.equals(other));

        other = createDatedTreeNode("Test1", Instant.MIN, NOW);
        assertTrue(node.equals(other));

        other = createDatedTreeNode("Test1", Instant.MIN, end);
        assertTrue(node.equals(other));

        other = createDatedTreeNode("Test1", Instant.MIN, Instant.MAX);
        assertFalse(node.equals(other));
    }

    @Test
    public void hashCodeCorrect() {
        TreeNode node = createDatedTreeNode("Test1", Instant.MIN, Instant.MAX);
        assertEquals(node.hashCode(), createDatedTreeNode("Test1", Instant.MIN, Instant.MAX).hashCode());

        node = createDatedTreeNode("Test1", null, Instant.MAX);
        assertEquals(node.hashCode(), createDatedTreeNode("Test1", null, Instant.MAX).hashCode());

        node = createDatedTreeNode("Test1", Instant.MIN, null);
        assertEquals(node.hashCode(), createDatedTreeNode("Test1", Instant.MIN, null).hashCode());
    }

    @Test
    public void dateRange() {
        final Instant start = NOW.minus(Period.ofWeeks(5));
        final Instant end = NOW.plus(Period.ofWeeks(5));

        final TreeNode node = createDatedTreeNode("Test1", start, end);

        Range<Instant> range = new Range<>(start, end);
        assertEquals(range, node.getDateRange());

        range = new Range<Instant>(Instant.MIN, Instant.MAX);
        node.setDateRange(range);
        assertEquals(range, node.getDateRange());
    }

    @Test
    public void exactMatch() {
        final TreeNode node = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);
        final TreeNode exact = createDatedTreeNode("Test1", Instant.MIN, Instant.MAX);

        node.addNode(exact);
        assertEquals(exact, node.getExactNode(exact).get());
    }

    @Test
    public void noMatch() {
        final TreeNode node = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);
        final TreeNode exact = createDatedTreeNode("Test1", Instant.MIN, Instant.MAX);
        assertFalse(node.getExactNode(exact).isPresent());

        final TreeNode other = createDatedTreeNode("Test2", Instant.MIN, Instant.MAX);
        node.addNode(other);
        assertFalse(node.getExactNode(exact).isPresent());
    }

    @Test
    public void addMultipleNodes() {
        final TreeNode node = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);
        final TreeNode exact = createDatedTreeNode("Test1", Instant.MIN, Instant.MAX);

        node.addNode(exact);
        assertEquals(exact, node.getExactNode(exact).get());

        assertEquals(exact, node.addNode(exact));
        assertEquals(1, node.stream().count());

        final TreeNode other = createDatedTreeNode("Test2", Instant.MIN, Instant.MAX);
        node.addNode(other);
        assertEquals(other, node.addNode(other));
        assertEquals(2, node.stream().count());
    }

    @Test
    public void addNodeUpdatesDateRangeStart() {
        final TreeNode root = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);

        final Instant start = NOW.minus(Period.ofWeeks(5));
        final Instant end = NOW.plus(Period.ofWeeks(5));

        final TreeNode node = createDatedTreeNode("Test1", start, end);
        root.addNode(node);

        final TreeNode other = createDatedTreeNode("Test1", start.minus(Period.ofWeeks(1)), end);
        root.addNode(other);

        final Range<Instant> range = new Range<>(start.minus(Period.ofWeeks(1)), end);
        assertEquals(range, node.getDateRange());
    }

    @Test
    public void addNodeUpdatesDateRangeEnd() {
        final TreeNode root = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);

        final Instant start = NOW.minus(Period.ofWeeks(5));
        final Instant end = NOW.plus(Period.ofWeeks(5));

        final TreeNode node = createDatedTreeNode("Test1", start, end);
        root.addNode(node);

        final TreeNode other = createDatedTreeNode("Test1", start, Instant.MAX);
        root.addNode(other);
        assertEquals(node, root.getExactNode(node).get());

        final Range<Instant> range = new Range<>(start, Instant.MAX);
        assertEquals(range, node.getDateRange());
    }

    @Test
    public void evaluatesCorrectly() {
        final TreeNode root = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);

        final Instant start = NOW.minus(Period.ofWeeks(5));
        final Instant end = NOW.plus(Period.ofWeeks(5));

        final TreeNode nodeOne = createDatedTreeNode("Test1", start, end);
        root.addNode(nodeOne);
        final TreeNode nodeTwo = createDatedTreeNode("Test2", start, end);
        root.addNode(nodeTwo);
        final TreeNode nodeThree = createDatedTreeNode("Test3", start, end);
        root.addNode(nodeThree);

        assertEquals(3, root.stream().count());

        final List<TreeNode> evaluated = root.getEvaluatedNodes(Collections.singletonList("Test2"), NOW);
        assertEquals(1, evaluated.size());
        assertEquals(nodeTwo, evaluated.get(0));
    }

    @Test
    public void evaluatesCorrectlyWithWildcard() {
        final TreeNode root = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);

        final Instant start = NOW.minus(Period.ofWeeks(5));
        final Instant end = NOW.plus(Period.ofWeeks(5));

        final TreeNode nodeOne = createDatedTreeNode("Test1", start, end);
        root.addNode(nodeOne);
        final TreeNode nodeTwo = createDatedTreeNode("Test2", start, end);
        root.addNode(nodeTwo);
        final TreeNode nodeThree = createDatedTreeNode("*", start, end);
        root.addNode(nodeThree);

        assertEquals(3, root.stream().count());

        final List<TreeNode> evaluated = root.getEvaluatedNodes(Collections.singletonList("Wildcard"), NOW);
        assertEquals(1, evaluated.size());
        assertEquals(nodeThree, evaluated.get(0));
    }

    @Test
    public void emptyEvaluation() {
        final TreeNode root = createDatedTreeNode("Root", Instant.MIN, Instant.MAX);

        final Instant start = NOW.minus(Period.ofWeeks(5));
        final Instant end = NOW.plus(Period.ofWeeks(5));

        final TreeNode nodeOne = createDatedTreeNode("Test1", start, end);
        root.addNode(nodeOne);
        final TreeNode nodeTwo = createDatedTreeNode("Test2", start, end);
        root.addNode(nodeTwo);
        final TreeNode nodeThree = createDatedTreeNode("Test3", start, end);
        root.addNode(nodeThree);

        assertEquals(3, root.stream().count());

        final List<TreeNode> evaluated = root.getEvaluatedNodes(Collections.singletonList("Fourth"), NOW);
        assertEquals(0, evaluated.size());
    }

    private TreeNode createDatedTreeNode(final String value, final Instant start, final Instant end) {
        return (NodeSupplier.createDatedTreeNode(new StringDriver(value),
                NodeSupplier.ROOT_NODE_LEVEL, new Range<Instant>(start, end))).get();
    }
}
