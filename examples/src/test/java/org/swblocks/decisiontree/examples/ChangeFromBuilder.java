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

package org.swblocks.decisiontree.examples;

import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swblocks.decisiontree.BuilderLoader;
import org.swblocks.decisiontree.ChangeableDecisionTree;
import org.swblocks.decisiontree.Input;
import org.swblocks.decisiontree.OutputResults;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.builder.ChangeBuilder;
import org.swblocks.decisiontree.change.domain.builder.ValueGroupChangeBuilder;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;

/**
 * Example for the Decision Tree which generates a DecisionTree from the {@link Builder}.
 * The {@link Builder} is a programmic way for creating complex structures via a simple API.
 */
public class ChangeFromBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeFromBuilder.class);
    private static final Instant NOW = Instant.now();

    private Builder<RuleSetBuilder, DecisionTreeRuleSet> builder;

    @Before
    public void setUp() {
        this.builder = RuleSetBuilder.creator("commissions",
                Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));

        this.builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "VG:CMEGroup:CME:CBOT", "*", "*", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.1")));

        this.builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "CME", "S&P", "*", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.2")));

        this.builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "*", "RATE"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.4")));

        this.builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "*", "*", "US", "*"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.5")));

        this.builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "*", "*", "UK", "*"))
                .with(RuleBuilder::output, Stream.of(
                        new SimpleEntry<>("Rate", "1.1"),
                        new SimpleEntry<>("Unit", "£"))
                        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))));
    }

    @Test
    public void changeTreeWithNewRule() {
        /**
         * Create a decision tree which is not time aware {@link DecisionTreeType#SINGLE}.
         */
        final ChangeableDecisionTree decisionTree = ChangeableDecisionTree.instanceOf(
                BuilderLoader.instanceOf(this.builder), DecisionTreeType.SINGLE, null);

        // Identical result from the other side of the value group
        final Input input = decisionTree.createInputs("DMA", "CBT", "ED", "US", "INDEX");
        Optional<OutputResults> evaluationFor = decisionTree.getEvaluationFor(input);
        logResult(input, evaluationFor);

        final Builder<ChangeBuilder, Change> builder = decisionTree.createChange("USER1",
                new DateRange(NOW, DecisionTreeRule.MAX));

        builder.with(ChangeBuilder::valueGroupChange,
                ValueGroupChangeBuilder.creator("CMEGroup")
                        .with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT", "CBT")));
        decisionTree.applyChange(builder, "USER2");
        evaluationFor = decisionTree.getEvaluationFor(input);
        logResult(input, evaluationFor);
    }

    private void logResult(final Input input, final Optional<OutputResults> evaluationFor) {
        if (evaluationFor.isPresent()) {
            LOGGER.info("Found evaluation for {} in tree, the result is {}", input, evaluationFor.get().results());
        } else {
            LOGGER.info("No evaluation for {} in tree", input);
        }
    }
}
