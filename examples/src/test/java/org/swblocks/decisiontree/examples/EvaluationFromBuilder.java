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
import org.swblocks.decisiontree.DecisionTree;
import org.swblocks.decisiontree.Input;
import org.swblocks.decisiontree.OutputResults;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.jbl.builders.Builder;

/**
 * Example for the Decision Tree which generates a DecisionTree from the {@link Builder}.
 * The {@link Builder} is a programmic way for creating complex structures via a simple API.
 */
public class EvaluationFromBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationFromBuilder.class);

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
    public void createTreeFromCsvTextAndEvaluate() {
        /**
         * Create a decision tree which is not time aware {@link DecisionTreeType#SINGLE}.
         */
        final DecisionTree decisionTree = DecisionTree.instanceOf(BuilderLoader.instanceOf(this.builder),
                DecisionTreeType.SINGLE);

        // Matches rule inputs [ "VOICE", "CME", "ED", "*", "RATE" ]
        Input input = decisionTree.createInputs("VOICE", "CME", "ED", "US", "RATE");
        Optional<OutputResults> evaluationFor = decisionTree.getEvaluationFor(input);
        logResult(input, evaluationFor);

        // Matches rule inputs [ "*", "VG:CMEGroup", "*", "*", "INDEX" ]
        // This matches the Value group which contains CME and CBOT
        input = decisionTree.createInputs("DMA", "CME", "ED", "US", "INDEX");
        evaluationFor = decisionTree.getEvaluationFor(input);
        logResult(input, evaluationFor);

        // Identical result from the other side of the value group
        input = decisionTree.createInputs("DMA", "CBOT", "ED", "US", "INDEX");
        evaluationFor = decisionTree.getEvaluationFor(input);
        logResult(input, evaluationFor);

        // Matches rule inputs [ "*", "*", "*", "UK", "*" ] with two outputs
        input = decisionTree.createInputs("DMA", "LSE", "I", "UK", "INDEX");
        evaluationFor = decisionTree.getEvaluationFor(input);
        logResult(input, evaluationFor);

        // No match
        input = decisionTree.createInputs("DMA", "TSE", "NK", "JP", "INDEX");
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
