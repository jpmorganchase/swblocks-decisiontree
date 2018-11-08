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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.tree.StringDriver;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for <code>DecisionTreeRule</code>.
 */
public class DecisionTreeRuleTest {
    private final Map<String, String> outputDriver = Collections.singletonMap("outputDriver", "result");

    @Test
    public void testConstructor() {
        final InputDriver[] testInputDriver = getInputDriverArray("input1");
        final InputDriver[] testEvaluationDriver = getInputDriverArray("output1");
        final Instant startInstance = Instant.now();
        final Instant endInstance = startInstance.plus(1, ChronoUnit.DAYS);
        final DecisionTreeRule decisionTreeRule = new DecisionTreeRule(new UUID(0, 1),
                new UUID(0, 2), testInputDriver, testEvaluationDriver, this.outputDriver,
                startInstance, endInstance);

        assertArrayEquals(testInputDriver, decisionTreeRule.getDrivers());
        assertTrue(decisionTreeRule.getEvaluations().isPresent());
        assertArrayEquals(testEvaluationDriver, decisionTreeRule.getEvaluations().get());
        assertEquals(new UUID(0, 1), decisionTreeRule.getRuleIdentifier());
        assertEquals(new UUID(0, 2), decisionTreeRule.getRuleCode());
        assertEquals("result", decisionTreeRule.getOutputs().get("outputDriver"));
        assertEquals(startInstance, decisionTreeRule.getStart());
        assertEquals(endInstance, decisionTreeRule.getEnd());
    }

    @Test
    public void testConstructorWithBlankEvaluations() {
        final InputDriver[] testInputDriver = getInputDriverArray("input1");
        final Instant startInstance = Instant.now();
        final Instant endInstance = startInstance.plus(1, ChronoUnit.DAYS);
        final DecisionTreeRule decisionTreeRule = new DecisionTreeRule(new UUID(0, 1),
                new UUID(0, 2), testInputDriver, this.outputDriver,
                startInstance, endInstance);

        assertArrayEquals(testInputDriver, decisionTreeRule.getDrivers());
        assertFalse(decisionTreeRule.getEvaluations().isPresent());
        assertEquals(new UUID(0, 1), decisionTreeRule.getRuleIdentifier());
        assertEquals(new UUID(0, 2), decisionTreeRule.getRuleCode());
        assertEquals("result", decisionTreeRule.getOutputs().get("outputDriver"));
        assertEquals(startInstance, decisionTreeRule.getStart());
        assertEquals(endInstance, decisionTreeRule.getEnd());
    }

    @Test
    public void testDefaultDatedRangeConstructor() {
        final DecisionTreeRule decisionTreeRule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), getInputDriverArray("input1", "input2"),
                        this.outputDriver, null, null);

        assertEquals(DecisionTreeRule.EPOCH, decisionTreeRule.getStart());
        assertEquals(DecisionTreeRule.MAX, decisionTreeRule.getEnd());
        assertEquals("DecisionTreeRule{ruleIdentifier=00000000-0000-0000-0000-000000000001, " +
                        "ruleCode=00000000-0000-0000-0000-000000000002, " +
                        "drivers=[input1, input2], " +
                        "outputs={outputDriver=result}, " +
                        "start=1970-01-01T00:00:00Z, end=+292278994-08-17T07:12:55.807Z}",
                decisionTreeRule.toString());
    }

    @Test
    public void testIsActiveWithinDateRange() {
        final Instant startInstance = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant endInstance = startInstance.plus(2, ChronoUnit.DAYS);
        final DecisionTreeRule decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), this.outputDriver, startInstance, endInstance);

        assertTrue(decisionTreeRule.isActiveAt(Instant.now()));
        Assert.assertFalse(decisionTreeRule.isActiveAt(Instant.now().minus(2, ChronoUnit.DAYS)));
        Assert.assertFalse(decisionTreeRule.isActiveAt(Instant.now().plus(2, ChronoUnit.DAYS)));
    }

    @Test
    public void testDateRange() {
        final Instant startInstance = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant endInstance = startInstance.plus(2, ChronoUnit.DAYS);
        final DecisionTreeRule decisionTreeRule =
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                        getInputDriverArray("DRIVER_ONE", "DRIVER_TWO"),
                        this.outputDriver, startInstance, endInstance);

        assertEquals(startInstance, decisionTreeRule.getRange().getStart());
        assertEquals(endInstance, decisionTreeRule.getRange().getFinish());
    }

    @Test
    public void testWeightedCalculations() {
        DecisionTreeRule decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1"), this.outputDriver, null, null);
        assertEquals(1, decisionTreeRule.getRuleWeight());

        decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), this.outputDriver, null, null);
        assertEquals(3, decisionTreeRule.getRuleWeight());

        decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "*"),
                this.outputDriver, null, null);
        assertEquals(2, decisionTreeRule.getRuleWeight());

        decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2", "input3"), this.outputDriver, null, null);
        assertEquals(7, decisionTreeRule.getRuleWeight());

        decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "*", "input3"), this.outputDriver, null, null);
        assertEquals(5, decisionTreeRule.getRuleWeight());

        decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("*", "*", "*"),
                this.outputDriver, null, null);
        assertEquals(0, decisionTreeRule.getRuleWeight());

        decisionTreeRule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "*", "*", "*"), this.outputDriver, null, null);
        assertEquals(8, decisionTreeRule.getRuleWeight());
    }

    @Test
    public void testEquals() {
        final DecisionTreeRule decisionTreeRule1 = new DecisionTreeRule(new UUID(0, 1),
                UUID.randomUUID(), getInputDriverArray("input1", "input2"), this.outputDriver, null, null);
        assertFalse(decisionTreeRule1.equals(null));
        assertFalse(decisionTreeRule1.equals(new Integer(1)));
        assertTrue(decisionTreeRule1.equals(decisionTreeRule1));
        assertTrue(decisionTreeRule1.equals(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"),
                this.outputDriver, null, null)));
        //Equals tested on ID only.
        assertTrue(decisionTreeRule1.equals(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input2", "input3"),
                this.outputDriver, null, null)));
        assertFalse(decisionTreeRule1.equals(new DecisionTreeRule(new UUID(0, 2), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"),
                this.outputDriver, null, null)));
    }

    @Test
    public void hashCodeCorrect() {
        final DecisionTreeRule rule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), this.outputDriver, null, null);
        final DecisionTreeRule other = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), this.outputDriver, null, null);

        assertTrue(rule.hashCode() == other.hashCode());
    }

    @Test
    public void testRuleDuplicateInputCheck() {
        final DecisionTreeRule rule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), this.outputDriver, null, null);

        assertTrue(rule.isDuplicateInputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateInputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input3"), this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateInputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1"), this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateInputData(null));
        assertFalse(rule.isDuplicateInputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray(), this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateInputData(null));
    }

    @Test
    public void testRuleDuplicateEvaluationCheck() {
        final DecisionTreeRule rule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), getInputDriverArray("eval1", "eval2"),
                this.outputDriver, null, null);

        assertTrue(rule.isDuplicateEvaluations(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), getInputDriverArray("eval1", "eval2"),
                this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateEvaluations(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input3"), getInputDriverArray("input1", "input2"),
                this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateEvaluations(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), getInputDriverArray("eval1"),
                this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateEvaluations(null));
        assertFalse(rule.isDuplicateEvaluations(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), this.outputDriver, null, null)));
        assertFalse(rule.isDuplicateEvaluations(null));
    }

    @Test
    public void testRuleDuplicateOutputCheck() {
        final DecisionTreeRule rule = new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), Collections.singletonMap("outputDriver", "result"), null,
                null);
        assertTrue(rule.isDuplicateOutputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input1", "input2"), Collections.singletonMap("outputDriver", "result"), null,
                null)));
        assertTrue(rule.isDuplicateOutputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input3", "input4"), Collections.singletonMap("outputDriver", "result"), null,
                null)));
        assertFalse(rule.isDuplicateOutputData(null));
        assertFalse(rule.isDuplicateOutputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input3", "input4"), Collections.singletonMap("outputDriver", "result1"), null,
                null)));
        assertFalse(rule.isDuplicateOutputData(new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                getInputDriverArray("input3", "input4"), Collections.singletonMap("outputDriver1", "result1"), null,
                null)));
        assertFalse(rule.isDuplicateOutputData(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input3", "input4"),
                        Stream.of(new AbstractMap.SimpleEntry<>("outputDriver", "result1"),
                                new AbstractMap.SimpleEntry<>("outputDriver2", "result2"))
                                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey,
                                        AbstractMap.SimpleEntry::getValue)), null, null)));
    }

    @Test
    public void testDuplicateDateRange() {
        final Instant startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        final Instant endTime = Instant.now().plus(180, ChronoUnit.DAYS);
        final DecisionTreeRule rule =
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                        getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), startTime, endTime);

        assertFalse(rule.isDuplicateDateRange(null));
        assertTrue(rule.isDuplicateDateRange(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), startTime, endTime)));
        assertTrue(rule.isDuplicateDateRange(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input3", "input4"),
                        Collections.singletonMap("outputDriver2", "result2"), startTime, endTime)));
        assertFalse(rule.isDuplicateDateRange(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), null, endTime)));
        assertFalse(rule.isDuplicateDateRange(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), startTime, null)));
        assertFalse(rule.isDuplicateDateRange(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), Instant.now(), endTime)));
    }

    @Test
    public void testDuplicateRuleCheck() {
        final Instant startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        final Instant endTime = Instant.now().plus(180, ChronoUnit.DAYS);
        final DecisionTreeRule rule =
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), startTime, endTime);

        assertFalse(rule.isDuplicateRule(null));
        assertTrue(rule.isDuplicateRule(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), startTime, endTime)));
        assertFalse(rule.isDuplicateRule(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input3"),
                        Collections.singletonMap("outputDriver", "result"), startTime, endTime)));
        assertFalse(rule.isDuplicateRule(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result1"), startTime, endTime)));
        assertFalse(rule.isDuplicateRule(
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(), getInputDriverArray("input1", "input2"),
                        Collections.singletonMap("outputDriver", "result"), startTime, null)));
    }

    @Test
    public void testReplaceDriversFromCache() {
        final DecisionTreeRule decisionTreeRule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), getInputDriverArray("input1", "input2"),
                        this.outputDriver, null, null);
        final DriverCache cache = new DriverCache();
        cache.put(new StringDriver("input1"));
        final InputDriver originalDriver1 = decisionTreeRule.getDrivers()[0];
        final InputDriver cachedDriver1 = cache.get("input1", InputValueType.STRING);
        // They are not the same object.
        assertFalse(originalDriver1 == cachedDriver1);

        decisionTreeRule.replaceDriversFromCache(cache);
        // They are now the same object.
        assertTrue(decisionTreeRule.getDrivers()[0] == cachedDriver1);

        // DRIVER_TWO has been put into the cache.
        assertEquals(cache.get("input2", InputValueType.STRING), decisionTreeRule.getDrivers()[1]);
    }

    private InputDriver[] getInputDriverArray(final String... inputs) {
        final InputDriver[] drivers = new InputDriver[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            drivers[i] = new StringDriver(inputs[i]);
        }

        return drivers;
    }
}