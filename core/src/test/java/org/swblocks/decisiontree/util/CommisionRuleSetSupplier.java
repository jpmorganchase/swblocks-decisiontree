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

package org.swblocks.decisiontree.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swblocks.decisiontree.Loader;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.InputBuilder;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.DecisionTreeFactory;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.eh.Result;
import org.swblocks.jbl.monitoring.Timer;

/**
 * Utility class to create different types of rule sets.
 */
public class CommisionRuleSetSupplier implements Loader<DecisionTreeRuleSet> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommisionRuleSetSupplier.class);

    /**
     * Creates the basic Commission ruleset.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> getCommisionRuleSet() {
        final UUID id = new UUID(0, 1);
        final Set<ValueGroup> groups = Collections.singleton(new ValueGroup(id, "CMEGroup",
                Arrays.asList("CME", "CBOT"), ValueGroup.DEFAULT_DATE_RANGE));

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator("commissions",
                Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        ruleSetBuilder.with(RuleSetBuilder::groups, groups);
        addRule(ruleSetBuilder, "*", GroupDriver.VG_PREFIX + id, "*", "*", "INDEX", null, null, 0, "1.1");
        addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX", null, null, 1, "1.2");
        addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE", null, null, 2, "1.4");
        addRule(ruleSetBuilder, "VOICE", "*", "*", "US", "*", null, null, 3, "1.5");
        addRule(ruleSetBuilder, "*", "*", "*", "US", "*", null, null, 4, "1.2");
        addRule(ruleSetBuilder, "*", "*", "*", "UK", "*", null, null, 5, "1.1");
        return ruleSetBuilder;
    }

    /**
     * Creates the basic Commission ruleset with additional regex rules.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> getCommissionRuleSetWithRegex() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = getCommisionRuleSet();
        addRule(ruleSetBuilder, "VOICE", "CME", "NDK", "AP.?C", "INDEX", null, null, 6, "1.1");
        addRule(ruleSetBuilder, "*", "C.?E", "S&P", "US", "INDEX", null, null, 7, "1.7");
        addRule(ruleSetBuilder, "*", "CME",
                InputBuilder.regExInput("^[A-Z]{1,2}[A-Z][0-9]{1,2}$"),
                "US", "*", null, null, 8, "1.8");

        return ruleSetBuilder;
    }

    /**
     * Creates a dated Commission ruleset for testing dated evaluations.
     *
     * <p>Slices
     * 28-03 -> 01-04 - rules 1, 9
     * 01-04 -> 04-04 - rules 2, 3, 9
     * 04-04 -> 06-04 - rules 2, 4, 5, 9
     * 06-04 -> 08-04 - rules 4, 5, 7, 9
     * 08-04 -> 12-04 - rules 4, 6, 9
     * 12-04 -> 13-04 - rules 6, 9
     * 13-04 -> 15-04 - rule 9
     * 15-04 -> end - rules 8, 9
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> getSlicedRuleSet() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        addRule(ruleSetBuilder, "*", "CME", "*", "*", "INDEX",
                Instant.parse("2013-03-28T00:00:00Z"), Instant.parse("2013-04-01T00:00:00Z"), 1L, "1.1");
        addRule(ruleSetBuilder, "VOICE", "CME", "*", "*", "INDEX",
                Instant.parse("2013-04-01T00:00:01Z"), Instant.parse("2013-04-06T00:00:00Z"), 2L, "1.2");
        addRule(ruleSetBuilder, "*", "*", "S&P", "*", "INDEX",
                Instant.parse("2013-04-01T00:00:00Z"), Instant.parse("2013-04-04T00:00:00Z"), 3L, "1.3");
        addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX",
                Instant.parse("2013-04-04T00:00:01Z"), Instant.parse("2013-04-12T00:00:00Z"), 4L, "1.4");
        addRule(ruleSetBuilder, "VOICE", "*", "ED", "*", "RATE",
                Instant.parse("2013-04-04T00:00:00Z"), Instant.parse("2013-04-08T00:00:00Z"), 5L, "1.5");
        addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE",
                Instant.parse("2013-04-08T00:00:01Z"), Instant.parse("2013-04-13T00:00:00Z"), 6L, "1.6");
        addRule(ruleSetBuilder, "VOICE", "*", "*", "US", "*",
                Instant.parse("2013-04-06T00:00:00Z"), Instant.parse("2013-04-08T00:00:00Z"), 7L, "1.7");
        addRule(ruleSetBuilder, "VOICE", "*", "*", "UK", "*",
                Instant.parse("2013-04-15T00:00:00Z"), Instant.parse("2023-01-01T00:00:00Z"), 8L, "1.8");
        addRule(ruleSetBuilder, "*", "*", "*", "US", "*",
                Instant.parse("2013-03-28T00:00:00Z"), null, 9L, "1.9");

        return ruleSetBuilder;
    }

    /**
     * Duplicates the getSlicedRuleSet test using a DATE_RANGE on a Single version tree over sliced trees.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> getCommissionRuleSetWithDateRanges() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator("commissions",
                Arrays.asList("RANGE", "EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        addDateRangeRule(ruleSetBuilder, "2013-03-28T00:00:00Z", "2013-04-01T00:00:00Z",
                "*", "CME", "*", "*", "INDEX",
                null, null, 1L, "1.1");
        addDateRangeRule(ruleSetBuilder, "2013-04-01T00:00:01Z", "2013-04-06T00:00:00Z",
                "VOICE", "CME", "*", "*", "INDEX",
                null, null, 2L, "1.2");
        addDateRangeRule(ruleSetBuilder, "2013-04-01T00:00:00Z", "2013-04-04T00:00:00Z",
                "*", "*", "S&P", "*", "INDEX",
                null, null, 3L, "1.3");
        addDateRangeRule(ruleSetBuilder, "2013-04-04T00:00:01Z", "2013-04-12T00:00:00Z",
                "*", "CME", "S&P", "*", "INDEX",
                null, null, 4L, "1.4");
        addDateRangeRule(ruleSetBuilder, "2013-04-04T00:00:00Z", "2013-04-08T00:00:00Z",
                "VOICE", "*", "ED", "*", "RATE",
                null, null, 5L, "1.5");
        addDateRangeRule(ruleSetBuilder, "2013-04-08T00:00:01Z", "2013-04-13T00:00:00Z",
                "VOICE", "CME", "ED", "*", "RATE",
                null, null, 6L, "1.6");
        addDateRangeRule(ruleSetBuilder, "2013-04-06T00:00:00Z", "2013-04-08T00:00:00Z",
                "VOICE", "*", "*", "US", "*",
                null, null, 7L, "1.7");
        addDateRangeRule(ruleSetBuilder, "2013-04-15T00:00:00Z", "2023-01-01T00:00:00Z",
                "VOICE", "*", "*", "UK", "*",
                null, null, 8L, "1.8");
        addDateRangeRule(ruleSetBuilder, "2013-03-28T00:00:00Z", "3023-01-01T00:00:00Z",
                "*", "*", "*", "US", "*",
                null, null, 9L, "1.9");
        return ruleSetBuilder;
    }

    /**
     * Helper method to add a DateRange Commission Rule to the Commission ruleset.
     * Should only be called from getCommissionRuleSetWithDateRanges.
     */
    private static Builder<RuleSetBuilder, DecisionTreeRuleSet> addDateRangeRule(
            final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder,
            final String startDate, final String endDate,
            final String exmethod, final String exchange,
            final String product, final String region,
            final String asset, final Instant start,
            final Instant finish, final long ruleId, final String rate) {
        return ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("DR:" + startDate + "|" + endDate,
                        exmethod, exchange, product, region, asset))
                .with(RuleBuilder::start, start)
                .with(RuleBuilder::end, finish)
                .with(RuleBuilder::setId, new UUID(0L, ruleId))
                .with(RuleBuilder::setCode, new UUID(0L, ruleId))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", rate)));
    }

    /**
     * Creates the basic Commission ruleset.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> getCommisionRuleSetWithNotionalRanges() {
        final UUID id = new UUID(0, 1);
        final Set<ValueGroup> groups = Collections.singleton(new ValueGroup(id, "CMEGroup",
                Arrays.asList("CME", "CBOT"), ValueGroup.DEFAULT_DATE_RANGE));

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator("commissions",
                Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET", "NOTIONAL"));
        ruleSetBuilder.with(RuleSetBuilder::groups, groups);
        addRule(ruleSetBuilder, "*", GroupDriver.VG_PREFIX + id, "*", "*", "INDEX",
                "IR:1000|5000", null, null, 0, "1.11");
        addRule(ruleSetBuilder, "*", GroupDriver.VG_PREFIX + id, "*", "*", "INDEX",
                "IR:5000|10000", null, null, 1, "1.12");
        addRule(ruleSetBuilder, "*", "CME", "S&P", "*", "INDEX",
                "", null, null, 2, "1.2");
        addRule(ruleSetBuilder, "VOICE", "CME", "ED", "*", "RATE",
                "", null, null, 3, "1.4");
        addRule(ruleSetBuilder, "VOICE", "*", "*", "US", "*",
                "", null, null, 4, "1.5");
        addRule(ruleSetBuilder, "*", "*", "*", "US", "*",
                "", null, null, 5, "1.2");
        addRule(ruleSetBuilder, "*", "*", "*", "UK", "*",
                "", null, null, 6, "1.1");
        addRule(ruleSetBuilder, "*", "*", "*", "UK", "*",
                "IR:1000|50000", null, null, 7, "1.17");
        addRule(ruleSetBuilder, "*", "*", "*", "UK", "*",
                "IR:500|5000", null, null, 8, "1.18");
        return ruleSetBuilder;
    }

    /**
     * Creates a large ruleset based on the inputs.
     *
     * <p>It is possible that random number generation would create duplicate rules, so that the final total number
     * of rules in the tree is less than the value of numberOfRules.
     *
     * @param numberOfRules          total number of rules to try and create.
     * @param numberOfInputDrivers   number of input drivers each rule has
     * @param varianceInInputDrivers array with one entry per input driver
     * @param percentageOfWildCards  a percentage (0-100) which indicates a probability that a driver will be a
     *                               wildcard
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> getSimpleTestRuleSet(final int numberOfRules,
                                                                                    final int ruleIdOffset,
                                                                                    final int numberOfInputDrivers,
                                                                                    final int[] varianceInInputDrivers,
                                                                                    final int percentageOfWildCards) {
        final List<String> drivers = new ArrayList<>(numberOfInputDrivers);

        for (int i = 0; i < numberOfInputDrivers; i++) {
            drivers.add("Driver" + i);
        }

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator(drivers);
        final Random randomGenerator = new Random();

        for (long i = ruleIdOffset; i < numberOfRules + ruleIdOffset; i++) {
            final List<String> inputs = new ArrayList<>(numberOfInputDrivers);
            boolean hasWildcard = false;

            for (int j = 0; j < numberOfInputDrivers; j++) {
                final boolean wildcard = (percentageOfWildCards % 100 + randomGenerator.nextInt(100)) > 100;
                if (wildcard && !hasWildcard) {
                    inputs.add("*");
                    hasWildcard = true;
                } else {
                    inputs.add("input" + i % varianceInInputDrivers[j]);
                }
            }

            ruleSetBuilder.with(RuleSetBuilder::rule,
                    RuleBuilder.creator().with(RuleBuilder::input, inputs)
                            .with(RuleBuilder::setId, new UUID(0, i)));
        }

        return ruleSetBuilder;
    }

    /**
     * Helper method to add a Commission Rule to the Commission ruleset.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> addRule(
            final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder,
            final String exmethod, final String exchange,
            final String product, final String region,
            final String asset, final Instant start,
            final Instant finish, final long ruleId, final String rate) {
        return ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList(exmethod, exchange, product, region, asset))
                .with(RuleBuilder::start, start)
                .with(RuleBuilder::end, finish)
                .with(RuleBuilder::setId, new UUID(0L, ruleId))
                .with(RuleBuilder::setCode, new UUID(0L, ruleId))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", rate)));
    }

    /**
     * Helper method to add a Commission Rule to the Commission ruleset with a notional range.
     */
    public static Builder<RuleSetBuilder, DecisionTreeRuleSet> addRule(
            final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder,
            final String exmethod, final String exchange,
            final String product, final String region,
            final String asset, final String notionalRange,
            final Instant start,
            final Instant finish, final long ruleId, final String rate) {
        return ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList(exmethod, exchange, product, region, asset, notionalRange))
                .with(RuleBuilder::start, start)
                .with(RuleBuilder::end, finish)
                .with(RuleBuilder::setId, new UUID(0L, ruleId))
                .with(RuleBuilder::setCode, new UUID(0L, ruleId))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", rate)));
    }

    @Ignore
    @Test
    public void testGetSimpleRuleSet() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleset1 = getSimpleTestRuleSet(500, 0, 10,
                new int[]{500, 500, 500, 500, 500, 500, 500, 500, 500, 500}, 0);
        Assert.assertNotNull(ruleset1);
        final DecisionTreeRuleSet ruleSet = ruleset1.build();

        final Timer timer = Timer.createStarted();

        DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTreeType.SINGLE);

        LOGGER.info("result {}", timer.stopAndPrintAsReadableString());
    }

    @Override
    public boolean test(final Result result) {
        return false;
    }

    @Override
    public Result<DecisionTreeRuleSet> get() {
        return Result.success(getCommisionRuleSet().build());
    }
}