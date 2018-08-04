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

package org.swblocks.decisiontree.domain.builders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.jbl.builders.Builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link RuleBuilder}.
 */
public class RuleBuilderTest {
    @Test
    public void testBasicConstruction() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        assertNotNull(ruleBuilder);

        final List<String> testInputs = Arrays.asList("test1", "test2");
        ruleBuilder.with(RuleBuilder::input, testInputs);
        ruleBuilder.with(RuleBuilder::setDriverCount, 2L);
        ruleBuilder.with(RuleBuilder::setId, new UUID(0, 1));
        ruleBuilder.with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result"));
        final DecisionTreeRule rule = ruleBuilder.build();
        assertNotNull(rule);

        final InputDriver[] derivedInputDrivers = rule.getDrivers();

        assertEquals("test1", derivedInputDrivers[0].getValue());
        assertEquals(InputValueType.STRING, derivedInputDrivers[0].getType());

        assertEquals("test2", derivedInputDrivers[1].getValue());
        assertEquals(InputValueType.STRING, derivedInputDrivers[1].getType());

        assertEquals(new UUID(0, 1), rule.getRuleIdentifier());
        assertFalse(rule.getEvaluations().isPresent());
        assertEquals("result", rule.getOutputs().get("outputDriver"));
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
    }

    @Test
    public void testBasicConstructionWithTokenedOutput() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        assertNotNull(ruleBuilder);

        final List<String> testInputs = Arrays.asList("test1", "test2");
        ruleBuilder.with(RuleBuilder::input, testInputs);
        ruleBuilder.with(RuleBuilder::setDriverCount, 2L);
        ruleBuilder.with(RuleBuilder::setId, new UUID(0, 3));
        ruleBuilder.with(RuleBuilder::output, Collections.singletonList("outputDriver:result"));
        final DecisionTreeRule rule = ruleBuilder.build();
        assertNotNull(rule);

        final InputDriver[] derivedInputDrivers = rule.getDrivers();

        assertEquals("test1", derivedInputDrivers[0].getValue());
        assertEquals(InputValueType.STRING, derivedInputDrivers[0].getType());

        assertEquals("test2", derivedInputDrivers[1].getValue());
        assertEquals(InputValueType.STRING, derivedInputDrivers[1].getType());

        assertEquals(new UUID(0, 3), rule.getRuleIdentifier());
        assertFalse(rule.getEvaluations().isPresent());
        assertEquals("result", rule.getOutputs().get("outputDriver"));
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
    }

    @Test
    public void testBasicConstructionWithRegexTypes() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        assertNotNull(ruleBuilder);

        final List<String> testInputs = Arrays.asList("te.?t1", "test2.*");
        ruleBuilder.with(RuleBuilder::input, testInputs);
        ruleBuilder.with(RuleBuilder::setDriverCount, 2L);
        ruleBuilder.with(RuleBuilder::setId, new UUID(0, 1));
        ruleBuilder.with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result"));
        final DecisionTreeRule rule = ruleBuilder.build();
        assertNotNull(rule);

        final InputDriver[] derivedInputDrivers = rule.getDrivers();

        assertEquals("te.?t1", derivedInputDrivers[0].getValue());
        assertEquals(InputValueType.REGEX, derivedInputDrivers[0].getType());

        assertEquals("test2.*", derivedInputDrivers[1].getValue());
        assertEquals(InputValueType.REGEX, derivedInputDrivers[1].getType());
        assertFalse(rule.getEvaluations().isPresent());

        assertEquals(new UUID(0, 1), rule.getRuleIdentifier());
        assertEquals("result", rule.getOutputs().get("outputDriver"));
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
    }

    @Test
    public void testBasicConstructionForValueGroups() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        assertNotNull(ruleBuilder);

        final List<String> testInputs = Arrays.asList("VG:VG1:test1:test2:test3:test4",
                "VG:VG2:test10:test20:test30:test40:VG:VG3:test50:test9.?:test200.*");
        ruleBuilder.with(RuleBuilder::input, testInputs);
        ruleBuilder.with(RuleBuilder::setDriverCount, 2L);
        ruleBuilder.with(RuleBuilder::setId, new UUID(0, 1));
        ruleBuilder.with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result"));
        final DecisionTreeRule rule = ruleBuilder.build();
        assertNotNull(rule);

        final InputDriver[] derivedInputDrivers = rule.getDrivers();

        assertEquals("VG1", derivedInputDrivers[0].getValue());
        assertEquals(InputValueType.VALUE_GROUP, derivedInputDrivers[0].getType());

        assertEquals("VG2", derivedInputDrivers[1].getValue());
        assertEquals(InputValueType.VALUE_GROUP, derivedInputDrivers[1].getType());

        assertEquals(new UUID(0, 1), rule.getRuleIdentifier());
        assertEquals("result", rule.getOutputs().get("outputDriver"));
        assertFalse(rule.getEvaluations().isPresent());
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
    }

    @Test
    public void testDatedRuleConstruction() {
        final Instant startTime = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant endTime = Instant.now().plus(1, ChronoUnit.DAYS);

        final DecisionTreeRule rule = RuleBuilder.creator().with(RuleBuilder::input, Arrays.asList("test1", "test2"))
                .with(RuleBuilder::setDriverCount, 2L)
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result"))
                .with(RuleBuilder::start, startTime)
                .with(RuleBuilder::end, endTime)
                .build();

        assertNotNull(rule);
    }

    @Test
    public void testConstructionWithEvaluations() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        assertNotNull(ruleBuilder);

        final List<String> testInputs = Arrays.asList("test1", "test2");
        ruleBuilder.with(RuleBuilder::input, testInputs);
        ruleBuilder.with(RuleBuilder::setDriverCount, 2L);
        ruleBuilder.with(RuleBuilder::evaluations, Arrays.asList("Eval1", "Eval2"));
        ruleBuilder.with(RuleBuilder::setId, new UUID(0, 1));
        ruleBuilder.with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result"));
        final DecisionTreeRule rule = ruleBuilder.build();
        assertNotNull(rule);

        assertTrue(rule.getEvaluations().isPresent());
        final InputDriver[] derivedEvaluations = rule.getEvaluations().get();

        assertEquals("Eval1", derivedEvaluations[0].getValue());
        assertEquals(InputValueType.STRING, derivedEvaluations[0].getType());

        assertEquals("Eval2", derivedEvaluations[1].getValue());
        assertEquals(InputValueType.STRING, derivedEvaluations[1].getType());

        assertEquals(new UUID(0, 1), rule.getRuleIdentifier());
        assertEquals("result", rule.getOutputs().get("outputDriver"));
        assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
        assertEquals(DecisionTreeRule.MAX, rule.getEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectNumberOfDrivers() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        assertNotNull(ruleBuilder);

        final List<String> testInputs = Arrays.asList("test1", "test2");
        ruleBuilder.with(RuleBuilder::input, testInputs);
        ruleBuilder.with(RuleBuilder::setDriverCount, 1L);
        ruleBuilder.build();
    }

    @Test
    public void testInvalidRuleConstructionIfEmptyInputs() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        assertNotNull(ruleBuilder);
        assertNull(ruleBuilder.build());
    }
}