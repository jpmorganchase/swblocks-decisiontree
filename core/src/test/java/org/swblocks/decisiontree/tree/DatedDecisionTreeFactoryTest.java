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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test cases for {@link DecisionTreeFactory} for a dated decision tree.
 */
public class DatedDecisionTreeFactoryTest {
    private static final Instant NOW = Instant.now();

    @Test
    public void rulesOverlap() {
        final Instant firstStart = NOW.minus(12, ChronoUnit.DAYS);
        final Instant firstFinish = NOW.plus(12, ChronoUnit.DAYS);

        final Instant secondStart = NOW.minus(4, ChronoUnit.DAYS);
        final Instant secondFinish = NOW.plus(4, ChronoUnit.DAYS);

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", firstStart,
                firstFinish, 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", secondStart,
                secondFinish, 2L, "1.2");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        // EXMETHOD
        final List<TreeNode> rootNodes = assertAndGetChildNodes(node, 1);
        BaseTreeNode treeNode = (BaseTreeNode) rootNodes.get(0);
        checkNodeValue(treeNode, "*", 1, firstStart, firstFinish);

        // EXCHANGE
        List<TreeNode> treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "CME", 2, firstStart, firstFinish);

        // PRODUCT
        final List<TreeNode> productNodes = assertAndGetChildNodes(treeNode, 2);

        final BaseTreeNode wildCardNode;
        final BaseTreeNode spNode;

        if (productNodes.get(0).getValue().equals("*")) {
            wildCardNode = (BaseTreeNode) productNodes.get(0);
            spNode = (BaseTreeNode) productNodes.get(1);
        } else {
            wildCardNode = (BaseTreeNode) productNodes.get(1);
            spNode = (BaseTreeNode) productNodes.get(0);
        }

        // "*" PATH - PRODUCT
        treeNode = wildCardNode;
        checkNodeValue(treeNode, "*", 3, firstStart, firstFinish);

        // "*" PATH - REGION
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, firstStart, firstFinish);

        // "*" PATH - ASSET
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "INDEX", 5, firstStart, firstFinish);

        // S_AND_P PATH - PRODUCT
        treeNode = spNode;
        checkNodeValue(treeNode, "S&P", 3, secondStart, secondFinish);

        // S_AND_P PATH - REGION
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, secondStart, secondFinish);

        // S_AND_P PATH - ASSET
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "INDEX", 5, secondStart, secondFinish);
    }

    @Test
    public void oneRuleInFuture() {
        final Instant firstStart = NOW.minus(4, ChronoUnit.DAYS);
        final Instant firstFinish = NOW.plus(4, ChronoUnit.DAYS);

        final Instant secondStart = NOW.plus(4, ChronoUnit.DAYS);
        final Instant secondFinish = NOW.plus(12, ChronoUnit.DAYS);

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", firstStart,
                firstFinish, 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", secondStart,
                secondFinish, 2L, "1.2");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        // EXMETHOD
        List<TreeNode> treeNodes = assertAndGetChildNodes(node, 1);

        BaseTreeNode treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 1, firstStart, secondFinish);

        // EXCHANGE
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "CME", 2, firstStart, secondFinish);

        // SIDE 1 - REGION
        final List<TreeNode> rootNodes = assertAndGetChildNodes(treeNode, 2);
        final BaseTreeNode wildcardNode = getNode(rootNodes, true);
        final BaseTreeNode regionNode = getNode(rootNodes, false);

        treeNode = wildcardNode;
        checkNodeValue(treeNode, "*", 3, firstStart, firstFinish);

        // SIDE 1 - ASSET
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, firstStart, firstFinish);

        // SIDE 1 - INDEX
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "INDEX", 5, firstStart, firstFinish);

        // SIDE 2 - REGION
        treeNode = regionNode;
        checkNodeValue(treeNode, "S&P", 3, secondStart, secondFinish);

        // SIDE 2 - ASSET
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, secondStart, secondFinish);

        // SIDE 2 - INDEX
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "INDEX", 5, secondStart, secondFinish);
    }

    @Test
    public void threeRulesWithSameDateRange() {
        final Instant start = NOW.minus(4, ChronoUnit.DAYS);
        final Instant end = NOW.plus(4, ChronoUnit.DAYS);

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX", start, end, 1L, "1.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", start, end, 2L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE", start, end, 3L, "1.4");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        // EXMETHOD
        final List<TreeNode> topNode = assertAndGetChildNodes(node, 2);
        final BaseTreeNode topWildCardNode = getNode(topNode, true);
        final BaseTreeNode topVoiceNode = getNode(topNode, false);

        BaseTreeNode treeNode = topWildCardNode;
        checkNodeValue(treeNode, "*", 1, start, end);

        // EXCHANGE
        List<TreeNode> treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "CME", 2, start, end);

        // SIDE 1 - REGION
        final List<TreeNode> rootNodes = assertAndGetChildNodes(treeNode, 2);
        final BaseTreeNode wildcardNode = getNode(rootNodes, true);
        final BaseTreeNode spNode = getNode(rootNodes, false);

        treeNode = wildcardNode;
        checkNodeValue(treeNode, "*", 3, start, end);

        // SIDE 1 - ASSET
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, start, end);

        // SIDE 1 - INDEX
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "INDEX", 5, start, end);

        // SIDE 2 - REGION
        treeNode = spNode;
        checkNodeValue(treeNode, "S&P", 3, start, end);

        // SIDE 2 - ASSET
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, start, end);

        // SIDE 2 - INDEX
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "INDEX", 5, start, end);

        // RULE 3 - EXMETHOD
        treeNode = topVoiceNode;
        checkNodeValue(treeNode, "VOICE", 1, start, end);

        // RULE 3 - EXCHANGE
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "CME", 2, start, end);

        // RULE 3 - PRODUCT
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "ED", 3, start, end);

        // RULE 3 - REGION
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, start, end);

        // RULE 3 - ASSET
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "RATE", 5, start, end);
    }

    @Test
    public void twoIdenticalRules() {
        final Instant start = NOW.minus(4, ChronoUnit.DAYS);
        final Instant end = NOW.plus(4, ChronoUnit.DAYS);

        // can be collapsed except last node - each has a different rule id so must fork.
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", start, end, 1L, "1.2");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", start, end, 2L, "1.2");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();
        final TreeNode node = constructTree(ruleSet);
        Assert.assertNotNull(node);

        // EXMETHOD
        List<TreeNode> treeNodes = assertAndGetChildNodes(node, 1);
        BaseTreeNode treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 1, start, end);

        // EXCHANGE
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "CME", 2, start, end);

        // PRODUCT
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "S&P", 3, start, end);

        // REGION
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "*", 4, start, end);

        // ASSET - 2 NODES
        /*
        TODO
        final List<TreeNode> rootNodes = assertAndGetChildNodes(treeNode, 2);

        treeNode = (BaseTreeNode) rootNodes.get(0);
        checkNodeValue(treeNode, INDEX, 5, start, end);

        treeNode = (BaseTreeNode) rootNodes.get(1);
        checkNodeValue(treeNode, INDEX, 5, start, end);
        */
    }

    private void checkNodeValue(final BaseTreeNode treeNode,
                                final String value,
                                final int level,
                                final Instant start,
                                final Instant end) {
        assertEquals(value, treeNode.getValue());
        assertEquals(level, treeNode.getDriverLevel());
        final DateRange dateRange = treeNode.getDateRange();
        assertEquals(start, dateRange.getStart());
        assertEquals(end, dateRange.getFinish());
        assertFalse(treeNode.getFailureNode().isPresent());
    }

    private TreeNode constructTree(final DecisionTreeRuleSet ruleSet) {
        return DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTreeType.DATED);
    }

    private List<TreeNode> assertAndGetChildNodes(final TreeNode node, final int expectedChildNodes) {
        final List<TreeNode> treeNodes = node.stream().collect(Collectors.toList());
        assertEquals(expectedChildNodes, treeNodes.size());

        return treeNodes;
    }

    private BaseTreeNode getNode(final List<TreeNode> nodes, final boolean isWildcard) {
        if ((nodes.get(0)).getValue().equals("*")) {
            return isWildcard ? (BaseTreeNode) nodes.get(0) : (BaseTreeNode) nodes.get(1);
        } else {
            return isWildcard ? (BaseTreeNode) nodes.get(1) : (BaseTreeNode) nodes.get(0);
        }
    }
}
