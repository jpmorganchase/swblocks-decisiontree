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

package org.swblocks.decisiontree.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.swblocks.decisiontree.TreeChange;
import org.swblocks.decisiontree.TreeRule;
import org.swblocks.decisiontree.TreeValueGroup;
import org.swblocks.decisiontree.tree.DecisionTreeFactory;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.RepeatingRule;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.google.code.tempusfugit.concurrency.annotations.Repeating;

/**
 * Runs Concurrent Integration Tests for adding/removing rules from a {@link DecisionTreeRuleSet} while also building a
 * evaluation tree to force iteration of the underlying data.
 */
public class DecisionTreeRuleSetConcurrentIntegrationTest {
    private static DecisionTreeRuleSet ruleSet1;
    private static TreeChange updateChange;
    private static TreeChange deleteChange;

    private static Map<UUID, DecisionTreeRule> deleteSet;
    @Rule
    public ConcurrentRule concurrentRule = new ConcurrentRule();
    @Rule
    public RepeatingRule repeatingRule = new RepeatingRule();

    // synchronized to comply with internal sonar rules
    @BeforeClass
    public static synchronized void setup() {
        ruleSet1 = CommisionRuleSetSupplier.getSimpleTestRuleSet(5000, 0, 10,
                new int[]{500, 500, 500, 500, 500, 500, 500, 500, 500, 500}, 0).build();
        final DecisionTreeRuleSet ruleSet2 = CommisionRuleSetSupplier.getSimpleTestRuleSet(5, 75, 10,
                new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 0).build();
        // Now remove them
        deleteSet = new HashMap<>();
        ruleSet2.getRules().entrySet().forEach(entity -> deleteSet.put(entity.getKey(), null));

        updateChange = new TreeChange() {
            @Override
            public Map<UUID, Optional<TreeRule>> getRules() {
                final HashMap<UUID, Optional<TreeRule>> rules = new HashMap<>();
                for (final Map.Entry<UUID, DecisionTreeRule> entry : ruleSet2.getRules().entrySet()) {
                    rules.put(entry.getKey(), Optional.of(entry.getValue()));
                }
                return rules;
            }

            @Override
            public Map<UUID, Optional<TreeValueGroup>> getGroups() {
                return new HashMap<>();
            }
        };

        deleteChange = new TreeChange() {
            @Override
            public Map<UUID, Optional<TreeRule>> getRules() {
                final HashMap<UUID, Optional<TreeRule>> rules = new HashMap<>();
                for (final Map.Entry<UUID, DecisionTreeRule> entry : ruleSet2.getRules().entrySet()) {
                    rules.put(entry.getKey(), Optional.empty());
                }
                return rules;
            }

            @Override
            public Map<UUID, Optional<TreeValueGroup>> getGroups() {
                return new HashMap<>();
            }
        };

    }

    @Test
    @Concurrent(count = 10)
    @Repeating(repetition = 200)
    public void runsMultipleTimes() {
        ruleSet1.updateRules(updateChange);
        DecisionTreeFactory.constructDecisionTree(ruleSet1, DecisionTreeType.SINGLE);
        ruleSet1.updateRules(deleteChange);
        DecisionTreeFactory.constructDecisionTree(ruleSet1, DecisionTreeType.SINGLE);
    }
}
