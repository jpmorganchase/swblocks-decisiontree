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

package org.swblocks.decisiontree;

import java.util.Optional;

import org.junit.Test;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.builder.ChangeBuilder;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ChangeableDecisionTreeTest}.
 */
public class ChangeableDecisionTreeTest {
    @Test
    public void testDecisionTreeEvaluation() {
        final DecisionTree decisionTree = ChangeableDecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE);
        assertNotNull(decisionTree);

        final Input input = decisionTree.createInputs("VOICE", "CME", "ED", "US", "RATE");

        final Optional<OutputResults> results = decisionTree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        assertEquals("1.4", results.get().results().get("Rate"));
    }

    @Test
    public void testCreatingChange() {
        final ChangeableDecisionTree decisionTree = ChangeableDecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE, null);
        assertNotNull(decisionTree);
        final DateRange dateRange = new DateRange(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX);

        final Builder<ChangeBuilder, Change> change = decisionTree.createChange("User1", dateRange);
        assertNotNull(change);
        final Change build = change.build();
        assertEquals("User1", build.getAudit().getInitiator());
        assertEquals(dateRange, build.getChangeRange());
    }

    @Test
    public void testApplyingChange() {
        final ChangeableDecisionTree decisionTree = ChangeableDecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE, new TestPersister());
        assertNotNull(decisionTree);
        final DateRange dateRange = new DateRange(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX);

        final Builder<ChangeBuilder, Change> changeBuilder = decisionTree.createChange("User1", dateRange);
        assertNotNull(changeBuilder);
        final Change change = decisionTree.applyChange(changeBuilder, "User2");
        assertNotNull(change);
        assertEquals("User2", change.getAudit().getAuthoriser());
    }

    static class TestPersister implements Persister {
        @Override
        public void put(final DecisionTreeRuleSet ruleSet) {
            assertThat(ruleSet, is(notNullValue()));
        }
    }
}