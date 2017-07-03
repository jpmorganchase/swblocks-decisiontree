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

import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test classes for {@link Input}.
 */
public class InputTest {
    @Test
    public void testConstructionOfInput() {
        final Instant evaluationTime = Instant.parse("2013-03-28T00:00:00Z");
        final Input input = Input.create("TestRuleSetName",
                DecisionTreeRuleSet.convertNamesToWeightedDrivers(Arrays.asList("driver1", "driver2")),
                evaluationTime);
        assertEquals("TestRuleSetName", input.getRuleSetName());

        assertEquals("*", input.getValueForDriverName("driver1"));
        assertEquals("*", input.getValueForDriverName("driver2"));
        assertEquals(evaluationTime, input.getEvaluationDate());

        assertEquals(Arrays.asList("*", "*"), input.getEvaluationInputs());

        assertEquals("Input{driverMap={WeightedDriver{name='driver1', weight=4}=*, " +
                "WeightedDriver{name='driver2', weight=2}=*}, " +
                "ruleSetName='TestRuleSetName', " +
                "evaluationDate=2013-03-28T00:00:00Z}", input.toString());
    }

    @Test
    public void testAddingInputs() {
        final Input input = Input.create("TestRuleSetName",
                DecisionTreeRuleSet.convertNamesToWeightedDrivers(Arrays.asList("driver1", "driver2")));
        assertTrue(input.putValueForDriverName("driver1", "testValue"));
        assertTrue(input.putBlankValueForDriverName("driver2"));
        assertFalse(input.putValueForDriverName("NotADriver", "testValue1"));
        assertEquals("testValue", input.getValueForDriverName("driver1"));
        assertEquals("", input.getValueForDriverName("driver2"));
        assertEquals(Arrays.asList("testValue", ""), input.getEvaluationInputs());
    }

    @Test
    public void testConstructionInputs() {
        final Input input = Input.create("TestRuleSetName",
                DecisionTreeRuleSet.convertNamesToWeightedDrivers(
                        Arrays.asList("driver1", "driver2")), "testValue", "testValue1");
        assertEquals("testValue", input.getValueForDriverName("driver1"));
        assertEquals("testValue1", input.getValueForDriverName("driver2"));
    }
}