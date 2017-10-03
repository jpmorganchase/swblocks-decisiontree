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
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.Range;

/**
 * Time Sliced Root Node that retrieves and stores cached tree nodes based on a time slice.
 *
 * <p>Stored nodes are {@link DecisionTreeType#SINGLE} decision trees.
 */
public final class TimeSlicedRootNode implements TreeNode {
    private static final BiPredicate<Instant, Instant> PREDICATE_RULE_START_CHECK = (rangeStart, ruleStart) ->
            ruleStart.compareTo(rangeStart) <= 0;
    private static final BiPredicate<Instant, Instant> PREDICATE_RULE_END_CHECK = (rangeEnd, ruleEnd) ->
            ruleEnd.compareTo(rangeEnd) >= 0;

    private final TimeSliceCache cache = TimeSliceCache.getInstance();
    private final DecisionTreeRuleSet ruleSet;

    TimeSlicedRootNode(final DecisionTreeRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    List<Instant> getTimes() {
        final Set<Instant> times = new TreeSet<>();

        this.ruleSet.getRules().forEach((ruleId, rule) -> {
            times.add(rule.getStart());
            times.add(rule.getEnd());
        });

        return Collections.unmodifiableList(new ArrayList<>(times));
    }

    Optional<Range<Instant>> getActiveRange(final Instant time) {
        final List<Range<Instant>> slices = new ArrayList<>(1);
        final List<Instant> times = getTimes();

        for (int i = 0; i < times.size() - 1; i++) {
            final Range<Instant> dateTimeSlice = new Range<>(times.get(i), times.get(i + 1));
            slices.add(dateTimeSlice);
        }

        Range<Instant> activeRange = null;
        for (final Range<Instant> range : slices) {
            if (Range.RANGE_CHECK.test(range, time)) {
                activeRange = range;
                break;
            }
        }

        return Optional.ofNullable(activeRange);
    }

    Optional<DecisionTreeRuleSet> getSlicedRuleSet(final Instant time) {
        final Map<UUID, DecisionTreeRule> ruleMap = new HashMap<>();

        final Optional<Range<Instant>> activeRange = getActiveRange(time);

        if (activeRange.isPresent()) {
            this.ruleSet.getRules().forEach((ruleId, rule) -> {
                if (PREDICATE_RULE_START_CHECK.test(activeRange.get().getStart(), rule.getStart()) &&
                        PREDICATE_RULE_END_CHECK.test(activeRange.get().getFinish(), rule.getEnd())) {
                    ruleMap.put(ruleId, rule);
                }
            });
            return Optional.of(new DecisionTreeRuleSet(this.ruleSet.getName(), ruleMap, this.ruleSet.getDriverNames(),
                    this.ruleSet.getDriverCache(), this.ruleSet.getValueGroups()));
        }
        return Optional.empty();
    }

    @Override
    public Stream<TreeNode> stream() {
        return null;
    }

    @Override
    public boolean evaluate(final String input) {
        return false;
    }

    @Override
    public List<TreeNode> getEvaluatedNodes(final List<String> inputs, final Instant time) {
        EhSupport.ensureArg(time != null, "Time sliced decision tree has %s time", time);

        final Optional<TreeNode> rootSlicedNode = getTreeNodeForTime(time);
        EhSupport.ensure(rootSlicedNode.isPresent(), "No slice node found");
        return rootSlicedNode.get().getEvaluatedNodes(inputs, time);
    }

    /**
     * Gets the {@code DecisionTreeType.SINGLE} decision tree for the time given.
     *
     * @param time Evaluation time
     * @return {@link Optional} of the root node for the decision tree.
     */
    Optional<TreeNode> getTreeNodeForTime(final Instant time) {
        Optional<Range<Instant>> activeRange = Optional.empty();

        for (final Range<Instant> range : this.cache.keys()) {
            if (Range.RANGE_CHECK.test(range, time)) {
                activeRange = Optional.of(range);
                break;
            }
        }

        if (activeRange.isPresent()) {
            return this.cache.get(activeRange);
        }

        Optional<TreeNode> rootSlicedNode = Optional.empty();
        activeRange = getActiveRange(time);

        final Optional<DecisionTreeRuleSet> decisionTreeRuleSet = getSlicedRuleSet(time);
        if (decisionTreeRuleSet.isPresent()) {
            final TreeNode newNode = DecisionTreeFactory.constructDecisionTree(decisionTreeRuleSet.get(),
                    DecisionTreeType.SINGLE);
            rootSlicedNode = Optional.of(newNode);
            this.cache.put(activeRange.get(), rootSlicedNode);
        }

        return rootSlicedNode;
    }

    @Override
    public Optional<TreeNode> getExactNode(final TreeNode inputValue) {
        return Optional.empty();
    }

    @Override
    public TreeNode addNode(final TreeNode newNode) {
        return null;
    }

    @Override
    public InputValueType getDriverType() {
        return InputValueType.STRING;
    }

    @Override
    public String getValue() {
        return "SLICED_ROOT_NODE";
    }

    @Override
    public InputDriver getDriver() {
        return null;
    }

    @Override
    public Range<Instant> getDateRange() {
        return null;
    }
}