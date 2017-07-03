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

import java.util.Arrays;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;

import static org.junit.Assert.assertThat;

/**
 * Matcher implementation for matching {@link DecisionTreeRuleSet}.
 */
public class DecisionTreeRuleSetMatcher {
    /**
     * Compares two {@link DecisionTreeRuleSet} objects for unit testing.
     *
     * @param otherRuleSet Rule to test
     * @return {@link Matcher}
     */
    public static Matcher<DecisionTreeRuleSet> isSame(final DecisionTreeRuleSet otherRuleSet) {
        return new BaseMatcher<DecisionTreeRuleSet>() {
            private String errorString = "";

            @Override
            public boolean matches(final Object item) {
                boolean matches = true;
                final DecisionTreeRuleSet ruleSet = (DecisionTreeRuleSet) item;
                matches &= ruleSet.equals(otherRuleSet);
                matches &= ruleSet.getDriverNames().equals(otherRuleSet.getDriverNames());
                matches &= isInputDriversMatch(ruleSet, otherRuleSet);
                if (matches) {
                    for (final DecisionTreeRule otherRule : otherRuleSet.getRules().values()) {
                        final DecisionTreeRule rule = ruleSet.getRules().get(otherRule.getRuleIdentifier());
                        matches &= DecisionTreeRuleMatcher.isSame(otherRule).matches(rule);
                        if (!matches) {
                            this.errorString = "Rule " + rule + " does not match " + otherRule;
                            break;
                        }
                    }
                }
                return matches;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText(otherRuleSet.toString());
            }

            @Override
            public void describeMismatch(final Object item, final Description description) {
                description.appendText(this.errorString);
            }
        };
    }

    private static boolean isInputDriversMatch(final DecisionTreeRuleSet ruleSet,
                                               final DecisionTreeRuleSet otherRuleSet) {
        boolean matches = true;
        for (final InputValueType type : InputValueType.values()) {
            final List<InputDriver> ruleSetDriversForType = ruleSet.getDriversByType(type);
            final List<InputDriver> otherRuleSetDriversForType = otherRuleSet.getDriversByType(type);

            for (final InputDriver driver : ruleSetDriversForType) {
                final int index = otherRuleSetDriversForType.indexOf(driver);
                if (index != -1) {
                    final InputDriver otherDriver = otherRuleSetDriversForType.get(index);
                    if (InputValueType.VALUE_GROUP.equals(driver.getType())) {
                        // Need additional check for sub drivers list setup.
                        final List<InputDriver> subDrivers =
                                Arrays.asList(((GroupDriver) driver).getSubDrivers(true));
                        final List<InputDriver> otherSubDrivers =
                                Arrays.asList(((GroupDriver) otherDriver).getSubDrivers(true));
                        matches &= subDrivers.equals(otherSubDrivers);
                    }
                } else {
                    matches = false;
                    break;
                }
            }
        }
        return matches;
    }

    @Test
    public void testRuleSetMatcher() {
        final DecisionTreeRuleSet ruleSet1 = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        final DecisionTreeRuleSet ruleSet2 = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        assertThat(ruleSet1, isSame(ruleSet2));
    }
}
