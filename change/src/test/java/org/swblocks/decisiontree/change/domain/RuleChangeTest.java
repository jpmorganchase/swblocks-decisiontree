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

package org.swblocks.decisiontree.change.domain;

import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.StringDriver;
import org.swblocks.jbl.test.utils.JblTestClassUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link RuleChange}.
 */
public class RuleChangeTest {
    private final Map<String, Object> injectedValues = new HashMap<>();
    private RuleChange bean;

    @Before
    public void setup() {
        this.injectedValues.clear();
        setBean(null);
    }

    @Test
    public void testConstruction() {
        final Instant start = Instant.now().minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final InputDriver[] drivers = getInputDriverArray("Test1", "Test2", "Test3");
        final Map<String, String> outputs = Collections.singletonMap("outputDriver", "result");
        final DecisionTreeRule rule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), drivers, outputs, start, end);

        final RuleChange change = new RuleChange(Type.ORIGINAL, rule);
        setBean(change);

        this.injectedValues.put("type", Type.ORIGINAL);
        this.injectedValues.put("rule", rule);

        JblTestClassUtils.assertGetterCorrectForConstructorInjection(this.injectedValues, getBean());
    }

    @Test
    public void equalsCorrect() {
        final Instant start = Instant.now().minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final InputDriver[] drivers = getInputDriverArray("Test1", "Test2", "Test3");
        final Map<String, String> outputs = Collections.singletonMap("outputDriver", "result");

        final DecisionTreeRule rule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), drivers, outputs, start, end);
        final RuleChange ruleChange = new RuleChange(Type.ORIGINAL, rule);

        assertTrue(ruleChange.equals(ruleChange));
        assertFalse(ruleChange.equals(null));
        assertFalse(ruleChange.equals(Boolean.TRUE));

        DecisionTreeRule otherRule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), drivers, outputs, start, end);
        RuleChange otherChange = new RuleChange(Type.ORIGINAL, otherRule);
        assertTrue(ruleChange.equals(otherChange));

        otherRule = new DecisionTreeRule(new UUID(0, 2), new UUID(0, 2), drivers, outputs, start, end);
        otherChange = new RuleChange(Type.ORIGINAL, otherRule);
        assertFalse(ruleChange.equals(otherChange));
    }

    @Test
    public void hashCodeCorrect() {
        final Instant start = Instant.now().minus(Period.ofWeeks(-1));
        final Instant end = start.plus(Period.ofWeeks(5));
        final InputDriver[] drivers = getInputDriverArray("Test1", "Test2", "Test3");
        final Map<String, String> outputs = Collections.singletonMap("outputDriver", "result");

        final DecisionTreeRule rule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), drivers, outputs, start, end);
        final RuleChange ruleChange = new RuleChange(Type.ORIGINAL, rule);

        final DecisionTreeRule otherRule =
                new DecisionTreeRule(new UUID(0, 1), new UUID(0, 2), drivers, outputs, start, end);
        final RuleChange otherRuleChange = new RuleChange(Type.ORIGINAL, otherRule);

        assertEquals(ruleChange.hashCode(), otherRuleChange.hashCode());
    }

    private InputDriver[] getInputDriverArray(final String... inputs) {
        final InputDriver[] drivers = new InputDriver[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            drivers[i] = new StringDriver(inputs[i]);
        }

        return drivers;
    }

    private RuleChange getBean() {
        return this.bean;
    }

    private void setBean(final RuleChange bean) {
        this.bean = bean;
    }
}
