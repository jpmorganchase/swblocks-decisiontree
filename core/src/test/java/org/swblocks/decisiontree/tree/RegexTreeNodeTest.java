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

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link BaseTreeNode} when a regex tree node is created using {@link NodeSupplier}.
 *
 * <p>The overridden methods are tested as behaviour is otherwise similar to the string tree node.
 */
public class RegexTreeNodeTest {
    @Test
    public void testGetsCorrectInputValueType() {
        final TreeNode testNode =
                NodeSupplier.createTreeNode(createRegexDriver("Te.?t1"), NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(InputValueType.REGEX == testNode.getDriverType());
    }

    @Test
    public void testEvaluateSingleRegex() {
        final TreeNode testNode =
                NodeSupplier.createTreeNode(createRegexDriver("Te.?t1"), NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(testNode.evaluate("Test1"));
        assertFalse(testNode.evaluate("NotTest1"));
    }

    @Test
    public void testEvaluateMultiRegex() {
        final TreeNode testNode =
                NodeSupplier.createTreeNode(createRegexDriver(".*Test1"), NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(testNode.evaluate("Test1"));
        assertTrue(testNode.evaluate("IsTest1"));
        assertFalse(testNode.evaluate("NotTst1"));
    }

    @Test
    public void equalsCorrect() {
        final TreeNode node =
                NodeSupplier.createTreeNode(createRegexDriver("Te.?t1"), NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode otherNode =
                NodeSupplier.createTreeNode(createRegexDriver("Te.?t1"), NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(node.equals(node));
        assertTrue(node.equals(otherNode));
        assertFalse(node.equals(null));
        assertFalse(node.equals(
                NodeSupplier.createTreeNode(createRegexDriver("Not.?est1"), NodeSupplier.ROOT_NODE_LEVEL)));
        assertFalse(node.equals(Mockito.mock(TreeNode.class)));
        assertFalse(node.equals(NodeSupplier.createTreeNode(
                new StringDriver("Test1"), NodeSupplier.ROOT_NODE_LEVEL).get()));
    }

    @Test
    public void hashCorrect() {
        final TreeNode node =
                NodeSupplier.createTreeNode(createRegexDriver("Te.?t1"), NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode otherNode =
                NodeSupplier.createTreeNode(createRegexDriver("Te.?t1"), NodeSupplier.ROOT_NODE_LEVEL).get();
        assertEquals(node.hashCode(), otherNode.hashCode());
    }

    private InputDriver createRegexDriver(final String driverName) {
        return new RegexDriver(driverName);
    }
}
