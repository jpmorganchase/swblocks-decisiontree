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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link BaseTreeNode} when a string tree node is created using {@link NodeSupplier}.
 */
public class StringTreeNodeTest {
    @Test
    public void testGettingStream() {
        final TreeNode testNode1 = NodeSupplier.createTreeNode(createStringDriver("TestOne"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode testNode11 = NodeSupplier.createTreeNode(createStringDriver("TestTwo"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        testNode1.addNode(testNode11);
        testNode1.optimise();

        assertEquals(1L, testNode1.stream().count());
        assertEquals(Optional.of(testNode11), testNode1.stream().findFirst());
    }

    @Test
    public void testGetsCorrectInputValueType() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createStringDriver("TestOne"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(InputValueType.STRING == testNode.getDriverType());
    }

    @Test
    public void correctDriver() {
        final InputDriver driver = createStringDriver("TestOne");
        final TreeNode testNode = NodeSupplier.createTreeNode(driver,
                NodeSupplier.ROOT_NODE_LEVEL).get();
        assertEquals(driver, testNode.getDriver());
    }

    @Test
    public void testEvaluate() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createStringDriver("TestOne"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(testNode.evaluate("TestOne"));
        assertFalse(testNode.evaluate("TestTwo"));
    }

    @Test
    public void equalsCorrect() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createStringDriver("TestOne"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode testOtherNode = NodeSupplier.createTreeNode(createStringDriver("TestOne"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode regexNode = NodeSupplier.createTreeNode(
                new RegexDriver("TestOne"), NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(testNode.equals(testNode));
        assertTrue(testNode.equals(testOtherNode));
        assertFalse(testNode.equals(null));
        assertFalse(testNode.equals(regexNode));
        assertFalse(testNode.equals(NodeSupplier.createTreeNode(createStringDriver("TestTwo"),
                NodeSupplier.ROOT_NODE_LEVEL)));
        assertFalse(testNode.equals(Mockito.mock(TreeNode.class)));
    }

    @Test
    public void hashCorrect() {
        assertEquals("TestRoot".hashCode(),
                NodeSupplier.createTreeNode(createStringDriver("TestRoot"),
                        NodeSupplier.ROOT_NODE_LEVEL).get().hashCode());
    }

    @Test
    public void testGetExactNodeMatch() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createStringDriver("TestRoot"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        setup(testNode, 10001);

        final TreeNode resultNode =
                testNode.getExactNode(NodeSupplier.createTreeNode(createStringDriver("Test5000"),
                        NodeSupplier.ROOT_NODE_LEVEL).get()).get();
        assertEquals("Test5000", resultNode.getValue());

        final Optional<TreeNode> optional =
                testNode.getExactNode(NodeSupplier.createTreeNode(createStringDriver("TestOne"),
                        NodeSupplier.ROOT_NODE_LEVEL).get());
        assertFalse(optional.isPresent());
    }

    @Test
    public void testGetEvaluation() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createStringDriver("TestRoot"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        setup(testNode, 10001);
        final List<TreeNode> resultNodes = testNode.getEvaluatedNodes(Collections.singletonList("Test5000"), null);
        assertNotNull(resultNodes);
        assertEquals(1L, resultNodes.size());

        assertEquals("Test5000", resultNodes.get(0).getValue());

        final List<TreeNode> optional = testNode.getEvaluatedNodes(Collections.singletonList("TestOne"), null);
        assertTrue(optional.isEmpty());
    }

    @Test
    public void testEvaluationOnLargeList() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createStringDriver("TestRoot"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        setup(testNode, 10001);
        final List<TreeNode> resultNodes = testNode.getEvaluatedNodes(Collections.singletonList("Test5000"), null);
        assertNotNull(resultNodes);
        assertEquals(1L, resultNodes.size());
        assertEquals("Test5000", resultNodes.get(0).getValue());
    }

    @Test
    public void testAddNodesOnlyAddsUniqueNodes() {
        final TreeNode testNode1 = NodeSupplier.createTreeNode(createStringDriver("TestOne"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode testNode11 = NodeSupplier.createTreeNode(createStringDriver("TestTwo"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode testNode11a = NodeSupplier.createTreeNode(createStringDriver("TestTwo"),
                NodeSupplier.ROOT_NODE_LEVEL).get();

        testNode1.addNode(testNode11);
        assertEquals(1L, testNode1.stream().count());

        testNode1.addNode(testNode11a);
        assertEquals(1L, testNode1.stream().count());
    }

    @Test
    public void testNullDateRange() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createStringDriver("TestRoot"),
                NodeSupplier.ROOT_NODE_LEVEL).get();

        assertNull(testNode.getDateRange());
    }

    private void setup(final TreeNode node, final int itemsToAdd) {
        for (int i = 0; i < itemsToAdd; i++) {
            node.addNode(NodeSupplier.createTreeNode(createStringDriver("Test" + i),
                    NodeSupplier.ROOT_NODE_LEVEL).get());
        }
    }

    private InputDriver createStringDriver(final String driverName) {
        return new StringDriver(driverName);
    }
}