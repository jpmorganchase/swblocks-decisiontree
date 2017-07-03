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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.StringDriver;

import static org.junit.Assert.assertThat;

/**
 * Matcher implementation for matching {@link DecisionTreeRule}.
 */
public class DecisionTreeRuleMatcher {
    /**
     * Compares two {@link DecisionTreeRule} objects for unit testing.
     *
     * @param otherRule Rule to test
     * @return {@link Matcher}
     */
    public static Matcher<DecisionTreeRule> isSame(final DecisionTreeRule otherRule) {
        return new BaseMatcher<DecisionTreeRule>() {
            @Override
            public boolean matches(final Object item) {
                final DecisionTreeRule rule = (DecisionTreeRule) item;
                return rule.equals(otherRule) && rule.isDuplicateRule(otherRule);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Rule should be ").appendValue(otherRule.toString());
            }
        };
    }

    @Test
    public void testRuleMatcher() {
        final Map<String, String> outputDriver = Collections.singletonMap("outputDriver", "result");

        final DecisionTreeRule decisionTreeRule1 =
                new DecisionTreeRule(new UUID(0, 1), UUID.randomUUID(),
                        new InputDriver[]{new StringDriver("Test1"), new StringDriver("Test2")},
                        outputDriver, null, null);

        assertThat(decisionTreeRule1, isSame(decisionTreeRule1));
        assertThat(decisionTreeRule1, isSame(new DecisionTreeRule(new UUID(0, 1),
                UUID.randomUUID(), new InputDriver[]{new StringDriver("Test1"), new StringDriver("Test2")},
                outputDriver, null, null)));
    }
}
