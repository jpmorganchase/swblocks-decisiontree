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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.tree.DecisionTreeFactory;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.tree.TreeNode;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test Cases for {@link Evaluator} using the {@link DecisionTreeFactory} creating a time sliced decision tree.
 *
 * <p>Following date ranges are created in this test:
 * <blockquote><pre>
 *      2013-03-28T00:00:00Z to 2013-04-01T00:00:00Z
 *      2013-04-01T00:00:00Z to 2013-04-01T00:00:01Z
 *      2013-04-01T00:00:01Z to 2013-04-04T00:00:00Z
 *      2013-04-04T00:00:00Z to 2013-04-04T00:00:01Z
 *      2013-04-04T00:00:01Z to 2013-04-06T00:00:00Z
 *      2013-04-06T00:00:00Z to 2013-04-08T00:00:00Z
 *      2013-04-08T00:00:00Z to 2013-04-08T00:00:01Z
 *      2013-04-08T00:00:01Z to 2013-04-12T00:00:00Z
 *      2013-04-12T00:00:00Z to 2013-04-13T00:00:00Z
 *      2013-04-13T00:00:00Z to 2013-04-15T00:00:00Z
 *      2013-04-15T00:00:00Z to 2023-01-01T00:00:00Z
 *      2023-01-01T00:00:00Z to +1000000000-12-31T23:59:59.999999999Z
 * </pre></blockquote>
 */
public class TimeSlicedEvaluationTest {
    private final TreeNode rootNode =
            DecisionTreeFactory.constructDecisionTree(CommisionRuleSetSupplier.getSlicedRuleSet().build(),
                    DecisionTreeType.SLICED);
    private List<String> inputA;
    private List<String> inputB;

    /**
     * Setup test rule set and input drivers.
     */
    @Before
    public void setup() {

        this.inputA = Arrays.asList("VOICE", "CME", "ED", "US", "RATE");
        this.inputB = Arrays.asList("*", "CME", "ED", "UK", "*");
    }

    @Test
    public void firstEvaluation() {
        /**
         * Applicable time slice: 2013-04-08T00:00:01Z to 2013-04-12T00:00:00Z.
         */
        final Instant evaluation = Instant.parse("2013-04-10T00:00:00.Z");

        final Optional<UUID> result = Evaluator.evaluate(this.inputA, evaluation, this.rootNode);
        assertTrue(result.isPresent());

        assertEquals(new UUID(0, 6L), result.get());
    }

    @Test
    public void secondEvaluation() {
        /**
         * Applicable time slice: 2013-04-06T00:00:00Z to 2013-04-08T00:00:00Z.
         */
        final Instant evaluation = Instant.parse("2013-04-07T05:00:00.Z");

        final Optional<UUID> result = Evaluator.evaluate(this.inputA, evaluation, this.rootNode);
        assertTrue(result.isPresent());

        assertEquals(new UUID(0, 5L), result.get());
    }

    @Test
    public void thirdEvaluation() {
        /**
         * Applicable time slice: 2013-04-01T00:00:00Z to 2013-04-01T00:00:01Z.
         */
        final Instant evaluation = Instant.parse("2013-04-01T00:00:00.Z");
        final Optional<UUID> result = Evaluator.evaluate(this.inputA, evaluation, this.rootNode);
        assertTrue(result.isPresent());

        assertEquals(new UUID(0, 9L), result.get());
    }

    @Test
    public void fourthEvaluation() {
        /**
         * Applicable time slice: 2013-04-15T00:00:00Z to 2023-01-01T00:00:00Z.
         */
        final Instant evaluation = Instant.parse("2016-12-01T00:00:00.Z");
        final Optional<UUID> result = Evaluator.evaluate(this.inputB, evaluation, this.rootNode);
        assertFalse(result.isPresent());
    }

}
