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
import java.util.UUID;

import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.jbl.util.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ResultNode} created using the {@link NodeSupplier}.
 */
public class ResultNodeTest {
    @Test
    public void testStringResultNode() {
        final TreeNode treeNode =
                NodeSupplier.createResultTreeNode(createStringDriver("TestInput"),
                        NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        final ResultNode resultNode = (ResultNode) treeNode;
        final BaseTreeNode baseTreeNode = (BaseTreeNode) treeNode;

        assertEquals(new UUID(0, 1), resultNode.getRuleIdentifier());
        assertEquals(14L, resultNode.getWeight());

        assertTrue(resultNode.evaluate("TestInput"));
        assertFalse(resultNode.evaluate("TestInputs"));

        assertTrue(resultNode.getDriverType() == InputValueType.STRING);
        // a result node never equals another
        assertFalse(resultNode.equals(resultNode));

        final InputDriver driverValue = baseTreeNode.getDriver();
        assertTrue(driverValue.getValue().equals("TestInput"));
    }

    @Test
    public void testStringDatedResultNode() {
        final TreeNode treeNode =
                NodeSupplier.createDatedResultTreeNode(createStringDriver("TestInput"),
                        NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        final ResultNode resultNode = (ResultNode) treeNode;
        final BaseTreeNode baseTreeNode = (BaseTreeNode) treeNode;

        assertEquals(new UUID(0, 1L), resultNode.getRuleIdentifier());
        assertEquals(14L, resultNode.getWeight());

        assertTrue(resultNode.evaluate("TestInput"));
        assertFalse(resultNode.evaluate("TestInputs"));

        assertTrue(resultNode.getDriverType() == InputValueType.STRING);
        // a result node never equals another
        assertFalse(resultNode.equals(resultNode));

        final InputDriver driverValue = baseTreeNode.getDriver();
        assertTrue(driverValue.getValue().equals("TestInput"));

        final Range<Instant> range = new Range<>(Instant.EPOCH, DecisionTreeRule.MAX);
        assertEquals(range, treeNode.getDateRange());
    }

    @Test
    public void testRegexResultNode() {
        final TreeNode treeNode =
                NodeSupplier.createResultTreeNode(
                        createRegexDriver("Test.?nput"), NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        final ResultNode resultNode = (ResultNode) treeNode;
        final BaseTreeNode baseTreeNode = (BaseTreeNode) treeNode;

        assertEquals(new UUID(0, 1), resultNode.getRuleIdentifier());
        assertEquals(14L, resultNode.getWeight());

        assertTrue(resultNode.evaluate("TestInput"));
        assertFalse(resultNode.evaluate("TestInputs"));

        assertTrue(resultNode.getDriverType() == InputValueType.REGEX);
        // a result node never equals another
        assertFalse(resultNode.equals(resultNode));

        final InputDriver driverValue = baseTreeNode.getDriver();
        assertTrue(driverValue.getValue().equals("Test.?nput"));
    }

    @Test
    public void testGroupResultNode() {
        final TreeNode treeNode =
                NodeSupplier.createResultTreeNode(
                        createGroupDriver("CMEGroup"), NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        final ResultNode resultNode = (ResultNode) treeNode;
        final BaseTreeNode baseTreeNode = (BaseTreeNode) treeNode;

        assertEquals(new UUID(0, 1), resultNode.getRuleIdentifier());
        assertEquals(14L, resultNode.getWeight());

        assertTrue(resultNode.evaluate("test1"));
        assertTrue(resultNode.evaluate("test4"));

        assertTrue(resultNode.getDriverType() == InputValueType.VALUE_GROUP);
        // a result node never equals another
        assertFalse(resultNode.equals(resultNode));

        final InputDriver driverValue = baseTreeNode.getDriver();
        assertTrue(driverValue.getValue().equals("CMEGroup"));
    }

    @Test
    public void equalsCorrect() {
        final TreeNode treeNode =
                NodeSupplier.createResultTreeNode(
                        createGroupDriver("CMEGroup"), NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        assertFalse(treeNode.equals(treeNode));
    }

    @Test
    public void hashCodeCorrect() {
        final TreeNode treeNode =
                NodeSupplier.createDatedResultTreeNode(
                        createGroupDriver("CMEGroup"), NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        final TreeNode otherNode =
                NodeSupplier.createDatedResultTreeNode(
                        createGroupDriver("CMEGroup"), NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        assertEquals(treeNode.hashCode(), otherNode.hashCode());
    }

    @Test
    public void hashCodeDatedTreeNodeCorrect() {
        final TreeNode treeNode =
                NodeSupplier.createResultTreeNode(
                        createGroupDriver("CMEGroup"), NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        final TreeNode otherNode =
                NodeSupplier.createResultTreeNode(
                        createGroupDriver("CMEGroup"), NodeSupplier.ROOT_NODE_LEVEL, getRule()).get();
        assertEquals(treeNode.hashCode(), otherNode.hashCode());
    }

    private InputDriver createRegexDriver(final String driverName) {
        return new RegexDriver(driverName);
    }

    private InputDriver createStringDriver(final String driverName) {
        return new StringDriver(driverName);
    }

    private InputDriver createGroupDriver(final String driverName) {
        final List<InputDriver> driverValueSet = new ArrayList<>(1);
        driverValueSet.add(new StringDriver("test1"));
        driverValueSet.add(new StringDriver("test2"));
        driverValueSet.add(new StringDriver("test3"));
        driverValueSet.add(new RegexDriver("tes.?4"));

        final List<InputDriver> drivers = new ArrayList<>(1);
        drivers.add(new GroupDriver(driverName, driverValueSet));

        return new GroupDriver(driverName, drivers);
    }

    private DecisionTreeRule getRule() {
        return new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("*", "CME", "S&P", "US", "*"),
                Collections.emptyMap(), Instant.EPOCH, DecisionTreeRule.MAX);
    }

    private InputDriver[] getInputDriverArray(final String... inputs) {
        final InputDriver[] drivers = new InputDriver[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            drivers[i] = new StringDriver(inputs[i]);
        }

        return drivers;
    }
}
