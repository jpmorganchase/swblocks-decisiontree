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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.test.utils.JblTestClassUtils;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link DecisionTreeFactory} creating a single decision tree.
 */
public class SingleDecisionTreeFactoryTest {
    @Test
    public void hasPrivateConstructor() {
        JblTestClassUtils.assertConstructorIsPrivate(DecisionTreeFactory.class);
    }

    @Test
    public void testBasicConstruction() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();

        final TreeNode node = DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTreeType.SINGLE);
        Assert.assertNotNull(node);
        checkTreeNode(node, ruleSet);
    }

    /**
     * Test that can be called externally to check a tree node structure madae from the same rules.
     *
     * @param node the tree node to be checked.
     */
    public void checkTreeNode(final TreeNode node, final DecisionTreeRuleSet ruleSet) {
        final List<TreeNode> rootNodes = assertAndGetChildNodes(node, 2);

        final TreeNode wildcard = rootNodes.get(0).getValue().equals("*") ? rootNodes.get(0) : rootNodes.get(1);
        final TreeNode other = !rootNodes.get(0).getValue().equals("*") ? rootNodes.get(0) : rootNodes.get(1);

        // drill down first root node
        checkWildCardRootNode(wildcard, ruleSet);

        // drill down second root node
        checkOtherRootNode(other);
    }

    private void checkWildCardRootNode(final TreeNode node, final DecisionTreeRuleSet ruleSet) {
        final BaseTreeNode exmethodNode1 = (BaseTreeNode) node;
        assertEquals("*", exmethodNode1.getValue());
        assertEquals(1, exmethodNode1.getDriverLevel());

        final List<TreeNode> exchangeNodes = assertAndGetChildNodes(exmethodNode1, 3);

        // CME string driver value
        BaseTreeNode tmpNode = (BaseTreeNode) exchangeNodes.stream().filter(treeNode ->
                treeNode.getValue().equals("CME")).findFirst().get();
        assertEquals("CME", tmpNode.getValue());
        assertEquals(2, tmpNode.getDriverLevel());

        List<TreeNode> nodes = assertAndGetChildNodes(tmpNode, 1);

        // drill down * tree
        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("S&P", tmpNode.getValue());
        assertEquals(3, tmpNode.getDriverLevel());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("*", tmpNode.getValue());
        assertEquals(4, tmpNode.getDriverLevel());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        ResultNode resultNode = (ResultNode) nodes.get(0);
        assertEquals("INDEX", resultNode.getValue());
        assertEquals(new UUID(0, 1), resultNode.getRuleIdentifier());
        assertEquals(13L, resultNode.getWeight());
        assertEquals(5, resultNode.getDriverLevel());

        // wildcard node value
        tmpNode = (BaseTreeNode) exchangeNodes.stream().filter(treeNode ->
                treeNode.getValue().equals("*")).findFirst().get();
        assertEquals("*", tmpNode.getValue());
        assertEquals(2, tmpNode.getDriverLevel());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("*", tmpNode.getValue());
        assertEquals(3, tmpNode.getDriverLevel());

        nodes = assertAndGetChildNodes(tmpNode, 2);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals(4, tmpNode.getDriverLevel());
        if ("US".equals(tmpNode.getValue())) {
            assertEquals("US", tmpNode.getValue());

            nodes = assertAndGetChildNodes(tmpNode, 1);

            resultNode = (ResultNode) nodes.get(0);
            assertEquals("*", resultNode.getValue());
            assertEquals(new UUID(0, 4), resultNode.getRuleIdentifier());
            assertEquals(2L, resultNode.getWeight());
            assertEquals(5, resultNode.getDriverLevel());
        } else {
            assertEquals("UK", tmpNode.getValue());

            nodes = assertAndGetChildNodes(tmpNode, 1);

            resultNode = (ResultNode) nodes.get(0);
            assertEquals("*", resultNode.getValue());
            assertEquals(new UUID(0, 5), resultNode.getRuleIdentifier());
            assertEquals(2L, resultNode.getWeight());
            assertEquals(5, resultNode.getDriverLevel());
        }

        // Value Group
        final ValueGroup group = ruleSet.getValueGroups().stream().filter(valueGroup ->
                valueGroup.getName().equals("CMEGroup")).findFirst().get();
        tmpNode = (BaseTreeNode) exchangeNodes.stream().filter(treeNode ->
                treeNode.getValue().equals(group.getId().toString())).findFirst().get();
        assertEquals(group.getId().toString(), tmpNode.getValue());
        assertEquals(2, tmpNode.getDriverLevel());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("*", tmpNode.getValue());
        assertEquals(3, tmpNode.getDriverLevel());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("*", tmpNode.getValue());
        assertEquals(4, tmpNode.getDriverLevel());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("INDEX", tmpNode.getValue());
        assertEquals(5, tmpNode.getDriverLevel());
    }

    private void checkOtherRootNode(final TreeNode node) {
        final BaseTreeNode exmethodNode2 = (BaseTreeNode) node;
        assertEquals("VOICE", exmethodNode2.getValue());

        List<TreeNode> nodes = assertAndGetChildNodes(exmethodNode2, 2);

        BaseTreeNode tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("CME", tmpNode.getValue());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("ED", tmpNode.getValue());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        tmpNode = (BaseTreeNode) nodes.get(0);
        assertEquals("*", tmpNode.getValue());

        nodes = assertAndGetChildNodes(tmpNode, 1);

        final ResultNode resultNode = (ResultNode) nodes.get(0);
        assertEquals("RATE", resultNode.getValue());
        assertEquals(new UUID(0, 2), resultNode.getRuleIdentifier());
        assertEquals(29L, resultNode.getWeight());
    }

    private List<TreeNode> assertAndGetChildNodes(final TreeNode node, final int expectedChildNodes) {
        final List<TreeNode> treeNodes = node.stream().collect(Collectors.toList());
        assertEquals(expectedChildNodes, treeNodes.size());

        return treeNodes;
    }
}