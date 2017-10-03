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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.jbl.util.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link TimeSlicedRootNode} when a string tree node is created using {@link NodeSupplier}.
 */
public class TimeSlicedRootNodeTest {
    private static final Instant NOW = Instant.now();

    @Test
    public void testConstructingTimeSlicesFromRuleSet() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getSlicedRuleSet().build();
        final TreeNode node = NodeSupplier.createTimeSlicedRootNode(ruleSet).get();
        Assert.assertNotNull(node);

        final TimeSlicedRootNode timeSlicedRootNode = (TimeSlicedRootNode) node;
        final List<Instant> times = timeSlicedRootNode.getTimes();
        Assert.assertNotNull(times);

        final List<Instant> expectedTimes = Arrays.asList(
                Instant.parse("2013-03-28T00:00:00Z"),
                Instant.parse("2013-04-01T00:00:00Z"),
                Instant.parse("2013-04-01T00:00:01Z"),
                Instant.parse("2013-04-04T00:00:00Z"),
                Instant.parse("2013-04-04T00:00:01Z"),
                Instant.parse("2013-04-06T00:00:00Z"),
                Instant.parse("2013-04-08T00:00:00Z"),
                Instant.parse("2013-04-08T00:00:01Z"),
                Instant.parse("2013-04-12T00:00:00Z"),
                Instant.parse("2013-04-13T00:00:00Z"),
                Instant.parse("2013-04-15T00:00:00Z"),
                Instant.parse("2023-01-01T00:00:00Z"),
                DecisionTreeRule.MAX
        );
        assertEquals(expectedTimes, times);
    }

    @Test
    public void testConstructingIdentifyingTimeSlicesFromRuleSet() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getSlicedRuleSet().build();
        final TimeSlicedRootNode timeSlicedRootNode = (TimeSlicedRootNode)
                NodeSupplier.createTimeSlicedRootNode(ruleSet).get();
        Assert.assertNotNull(timeSlicedRootNode);

        assertEquals(new Range<>(Instant.parse("2013-03-28T00:00:00Z"), Instant.parse("2013-04-01T00:00:00Z")),
                timeSlicedRootNode.getActiveRange(Instant.parse("2013-03-28T01:00:00Z")).get());

        assertEquals(new Range<>(Instant.parse("2013-04-15T00:00:00Z"), Instant.parse("2023-01-01T00:00:00Z")),
                timeSlicedRootNode.getActiveRange(Instant.parse("2016-01-01T00:00:00Z")).get());

        assertEquals(new Range<>(Instant.parse("2013-04-15T00:00:00Z"), Instant.parse("2023-01-01T00:00:00Z")),
                timeSlicedRootNode.getActiveRange(Instant.parse("2013-04-15T00:00:00Z")).get());
        assertEquals(new Range<>(Instant.parse("2013-04-15T00:00:00Z"), Instant.parse("2023-01-01T00:00:00Z")),
                timeSlicedRootNode.getActiveRange(Instant.parse("2022-12-31T23:59:59.999Z")).get());

        assertFalse(timeSlicedRootNode.getActiveRange(Instant.parse("1972-12-31T23:59:59.999Z")).isPresent());
    }

    @Test
    public void testGeneratingSlicedRuleSets() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getSlicedRuleSet().build();
        final TimeSlicedRootNode timeSlicedRootNode = (TimeSlicedRootNode)
                NodeSupplier.createTimeSlicedRootNode(ruleSet).get();
        assertNotNull(timeSlicedRootNode);

        final Optional<DecisionTreeRuleSet> slicedRuleSet = timeSlicedRootNode.getSlicedRuleSet(
                Instant.parse("2013-03-28T01:00:00Z"));
        assertTrue(slicedRuleSet.isPresent());
        assertNotNull(slicedRuleSet.get().getRules());
        assertEquals(Integer.valueOf(2), Integer.valueOf(slicedRuleSet.get().getRules().size()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentException() {
        final TreeNode node = NodeSupplier.createTimeSlicedRootNode(null).get();
        node.getEvaluatedNodes(null, null);
    }

    @Test
    public void constructsTreeFromRule() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "APAC", "INDEX", NOW,
                NOW.plus(Period.ofWeeks(2)), 1L, "1.9");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "EMAIL", "S&P", "ED", "US", "INDEX",
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)), 2L, "2.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "ELECTRONIC", "CBOT", "EB", "UK", "INDEX",
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)), 3L, "1.1");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        final TimeSlicedRootNode slicedRoodNode = (TimeSlicedRootNode) DecisionTreeFactory.constructDecisionTree(
                ruleSet, DecisionTreeType.SLICED);
        assertNotNull(slicedRoodNode);

        final TreeNode root = slicedRoodNode.getTreeNodeForTime(NOW.plus(Period.ofWeeks(2))).get();
        assertNotNull(root);

        // RULE 3
        List<TreeNode> treeNodes = assertAndGetChildNodes(root, 1);

        // EXMETHOD
        BaseTreeNode treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "ELECTRONIC", 1);

        // EXCHANGE
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "CBOT", 2);

        // PRODUCT
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "EB", 3);

        // REGION
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "UK", 4);

        // REGION
        treeNodes = assertAndGetChildNodes(treeNode, 1);
        treeNode = (BaseTreeNode) treeNodes.get(0);
        checkNodeValue(treeNode, "INDEX", 5);

        // also a result node
        final ResultNode resultNode = (ResultNode) treeNodes.get(0);
        assertEquals(new UUID(0, 3L), resultNode.getRuleIdentifier());
        assertEquals(31L, resultNode.getWeight());
    }

    @Test
    public void testTreeWithOverlappingRules() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "APAC", "INDEX", NOW,
                NOW.plus(Period.ofWeeks(5)), 1L, "2.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "EMAIL", "S&P", "ED", "US", "INDEX",
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(6)), 2L, "2.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "ELECTRONIC", "CBOT", "EB", "UK", "INDEX",
                NOW.plus(Period.ofWeeks(3)), NOW.plus(Period.ofWeeks(5)), 3L, "1.1");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        final TimeSlicedRootNode slicedRoodNode = (TimeSlicedRootNode) DecisionTreeFactory.constructDecisionTree(
                ruleSet, DecisionTreeType.SLICED);
        assertNotNull(slicedRoodNode);

        final TreeNode root = slicedRoodNode.getTreeNodeForTime(NOW.plus(Period.ofWeeks(4))).get();
        assertNotNull(root);

        // 3 separate trees as all rules overlap at 4 weeks
        final List<TreeNode> treeNodes = root.stream().collect(Collectors.toList());
        assertEquals(3, treeNodes.size());
    }

    @Test
    public void testTreeWithSameRuleDates() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "APAC", "INDEX", NOW,
                NOW.plus(Period.ofWeeks(5)), 1L, "2.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "EMAIL", "S&P", "ED", "US", "INDEX", NOW,
                NOW.plus(Period.ofWeeks(5)), 2L, "2.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "ELECTRONIC", "CBOT", "EB", "UK", "INDEX", NOW,
                NOW.plus(Period.ofWeeks(5)), 3L, "1.1");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        // Date before start - no nodes
        final TimeSlicedRootNode slicedRoodNode = (TimeSlicedRootNode) DecisionTreeFactory.constructDecisionTree(
                ruleSet, DecisionTreeType.SLICED);
        assertNotNull(slicedRoodNode);

        final Optional<TreeNode> root = slicedRoodNode.getTreeNodeForTime(NOW.minus(Period.ofWeeks(1)));
        assertFalse(root.isPresent());
    }

    @Test
    public void usesCache() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "VOICE", "CME", "ED", "APAC", "INDEX", NOW,
                NOW.plus(Period.ofWeeks(5)), 1L, "2.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "EMAIL", "S&P", "ED", "US", "INDEX",
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(6)), 2L, "2.1");
        CommisionRuleSetSupplier.addRule(ruleSetBuilder, "ELECTRONIC", "CBOT", "EB", "UK", "INDEX",
                NOW.plus(Period.ofWeeks(3)), NOW.plus(Period.ofWeeks(5)), 3L, "1.1");

        final DecisionTreeRuleSet ruleSet = ruleSetBuilder.build();

        final TimeSlicedRootNode slicedRoodNode = (TimeSlicedRootNode) DecisionTreeFactory.constructDecisionTree(
                ruleSet, DecisionTreeType.SLICED);
        assertNotNull(slicedRoodNode);

        final Optional<TreeNode> first = slicedRoodNode.getTreeNodeForTime(NOW.plus(Period.ofWeeks(1)));
        assertTrue(first.isPresent());
        final Optional<TreeNode> cachedFirst = slicedRoodNode.getTreeNodeForTime(NOW.plus(Period.ofWeeks(1)));
        assertTrue(cachedFirst.isPresent());

        List<TreeNode> nonCachedNodes = first.get().stream().collect(Collectors.toList());
        List<TreeNode> cachedNodes = cachedFirst.get().stream().collect(Collectors.toList());
        checkNodesEqual(nonCachedNodes, cachedNodes, 1);

        assertEquals(nonCachedNodes.get(0), cachedNodes.get(0));

        final Optional<TreeNode> second = slicedRoodNode.getTreeNodeForTime(NOW.plus(Period.ofWeeks(4)));
        assertTrue(first.isPresent());
        final Optional<TreeNode> cachedSecond = slicedRoodNode.getTreeNodeForTime(NOW.plus(Period.ofWeeks(4)));
        assertTrue(cachedSecond.isPresent());

        nonCachedNodes = second.get().stream().collect(Collectors.toList());
        cachedNodes = cachedSecond.get().stream().collect(Collectors.toList());
        checkNodesEqual(nonCachedNodes, cachedNodes, 3);
    }

    @Test
    public void stubbedMethods() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getSlicedRuleSet().build();
        final TimeSlicedRootNode timeSlicedRootNode = (TimeSlicedRootNode)
                NodeSupplier.createTimeSlicedRootNode(ruleSet).get();
        Assert.assertNotNull(timeSlicedRootNode);

        assertNull(timeSlicedRootNode.stream());
        assertFalse(timeSlicedRootNode.evaluate(null));
        assertEquals(Optional.empty(), timeSlicedRootNode.getExactNode(null));
        assertNull(timeSlicedRootNode.addNode(timeSlicedRootNode));
        assertEquals(InputValueType.STRING, timeSlicedRootNode.getDriverType());
        assertEquals("SLICED_ROOT_NODE", timeSlicedRootNode.getValue());
        assertNull(timeSlicedRootNode.getDateRange());
        assertNull(timeSlicedRootNode.getDriver());
    }

    private void checkNodesEqual(final List<TreeNode> expected, final List<TreeNode> actual, final int size) {
        assertTrue(expected.size() == size);
        assertTrue(actual.size() == size);

        for (int i = 0; i < size; ++i) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }

    private void checkNodeValue(final BaseTreeNode treeNode, final String value, final int level) {
        assertEquals(value, treeNode.getValue());
        assertEquals(level, treeNode.getDriverLevel());
        assertFalse(treeNode.getFailureNode().isPresent());
    }

    private List<TreeNode> assertAndGetChildNodes(final TreeNode node, final int expectedChildNodes) {
        final List<TreeNode> treeNodes = node.stream().collect(Collectors.toList());
        assertEquals(expectedChildNodes, treeNodes.size());

        return treeNodes;
    }
}