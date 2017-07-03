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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link BaseTreeNode} when a value group tree node is created using {@link NodeSupplier}.
 *
 * <p>The overridden methods are tested as behaviour is otherwise similar to the string tree node.
 */
public class GroupTreeNodeTest {
    @Test
    public void testGetsCorrectInputValueType() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createInputDriver("testVG"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(InputValueType.VALUE_GROUP == testNode.getDriverType());
    }

    @Test
    public void testEvaluate() {
        final TreeNode testNode = NodeSupplier.createTreeNode(createInputDriver("testVG"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(testNode.evaluate("test1"));
        assertTrue(testNode.evaluate("test2"));
        assertTrue(testNode.evaluate("test3"));
        assertTrue(testNode.evaluate("test4"));
        assertFalse(testNode.evaluate("test5"));

        assertFalse(testNode.evaluate("testVG"));
    }

    @Test
    public void equalsCorrect() {
        final TreeNode node = NodeSupplier.createTreeNode(createInputDriver("testVG"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode otherNode = NodeSupplier.createTreeNode(createInputDriver("testVG"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        assertTrue(node.equals(node));
        assertTrue(node.equals(otherNode));
        assertFalse(node.equals(null));
        assertFalse(node.equals(NodeSupplier.createTreeNode(createInputDriver("testVG1"),
                NodeSupplier.ROOT_NODE_LEVEL)));
        assertFalse(node.equals(Mockito.mock(TreeNode.class)));
        assertFalse(node.equals(NodeSupplier.createTreeNode(new StringDriver("Test1"),
                NodeSupplier.ROOT_NODE_LEVEL).get()));
    }

    @Test
    public void hashCodeCorrect() {
        final TreeNode node = NodeSupplier.createTreeNode(createInputDriver("testVG"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final TreeNode otherNode = NodeSupplier.createTreeNode(createInputDriver("testVG"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        assertEquals(node.hashCode(), otherNode.hashCode());
    }

    private InputDriver createInputDriver(final String driverName) {
        final List<InputDriver> drivers = new ArrayList<>(1);
        drivers.add(new StringDriver("test1"));
        drivers.add(new StringDriver("test2"));
        drivers.add(new StringDriver("test3"));
        drivers.add(new RegexDriver("tes.?4"));

        return new GroupDriver(driverName, drivers);
    }
}
