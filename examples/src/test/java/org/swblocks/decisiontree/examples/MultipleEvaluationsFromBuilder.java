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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.core.IsCollectionContaining;
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
import org.swblocks.jbl.collections.CollectionUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Example for the Decision Tree which generates a DecisionTree from the {@link Builder}. The example uses overlapping
 * inputs to generate multiple results for a set of inputs.
 *
 * <p>The {@link Builder} is a programmic way for creating complex structures via a simple API.
 */
public class MultipleEvaluationsFromBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleEvaluationsFromBuilder.class);

    private Builder<RuleSetBuilder, DecisionTreeRuleSet> builder;

    @Before
    public void setUp() {
        builder = RuleSetBuilder.creator("commissions",
                Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET", "NOTIONAL"));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "VG:CMEGroup:CME:CBOT", "*", "*", "INDEX", "*"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.19")));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "VG:CMEGroup:CME:CBOT", "*", "*", "INDEX", "IR:1000|5000"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.17")));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input,
                        Arrays.asList("*", "VG:CMEGroup:CME:CBOT", "*", "*", "INDEX", "IR:5000|100000"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.15")));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input,
                        Arrays.asList("*", "VG:CMEGroup:CME:CBOT", "*", "*", "INDEX", "IR:5000|70000"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.13")));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "CME", "S&P", "*", "INDEX", "*"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.2")));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "*", "RATE", "*"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.4")));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "*", "*", "US", "*", "*"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.5")));

        builder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("*", "*", "*", "UK", "*", "*"))
                .with(RuleBuilder::output, Stream.of(
                        new SimpleEntry<>("Rate", "1.1"),
                        new SimpleEntry<>("Unit", "£"))
                        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))));
    }

    @Test
    public void createTreeFromBuilderTextAndEvaluate() {
        /**
         * Create a decision tree which is not time aware {@link DecisionTreeType#SINGLE}.
         */
        final DecisionTree decisionTree = DecisionTree.instanceOf(BuilderLoader.instanceOf(builder),
                DecisionTreeType.SINGLE);

        // Matches rule inputs [ "*", "VG:CMEGroup", "*", "*", "INDEX", "*" ]
        Input input = decisionTree.createInputs("VOICE", "CBOT", "S&P", "US", "INDEX", "100");
        final Optional<OutputResults> evaluationFor = decisionTree.getSingleEvaluationFor(input);
        logResult(input, evaluationFor);
        assertThat(evaluationFor.isPresent(), is(true));
        assertThat(evaluationFor.get().results().get("Rate"), is("1.19"));

        // Matches rule inputs [ "*", "VG:CMEGroup", "*", "*", "INDEX", "1000|5000" ]
        input = decisionTree.createInputs("VOICE", "CBOT", "S&P", "US", "INDEX", "1100");
        List<OutputResults> evaluationsFor = decisionTree.getEvaluationsFor(input);
        logResult(input, evaluationsFor);
        assertThat(CollectionUtils.isNotEmpty(evaluationsFor), is(true));

        assertThat(evaluationsFor.stream().map(
                outputResults -> outputResults.results().get("Rate")).collect(Collectors.toList()),
                IsCollectionContaining.hasItems("1.17"));

        // Matches rule inputs [ "*", "VG:CMEGroup", "*", "*", "INDEX", "5000|10000" ]
        // AND Matches rule inputs [ "*", "VG:CMEGroup", "*", "*", "INDEX", "5000|7000" ]
        input = decisionTree.createInputs("VOICE", "CBOT", "S&P", "US", "INDEX", "6100");
        evaluationsFor = decisionTree.getEvaluationsFor(input);
        logResult(input, evaluationsFor);
        assertThat(CollectionUtils.isNotEmpty(evaluationsFor), is(true));

        assertThat(evaluationsFor.stream().map(
                outputResults -> outputResults.results().get("Rate")).collect(Collectors.toList()),
                IsCollectionContaining.hasItems("1.13", "1.15"));
    }

    private void logResult(final Input input, final Optional<OutputResults> evaluationFor) {
        if (evaluationFor.isPresent()) {
            LOGGER.info("Found evaluation for {} in tree, the result is {}", input, evaluationFor.get().results());
        } else {
            LOGGER.info("No evaluation for {} in tree", input);
        }
    }

    private void logResult(final Input input, final List<OutputResults> evaluationsFor) {
        if (CollectionUtils.isNotEmpty(evaluationsFor)) {
            evaluationsFor.forEach(output -> LOGGER.info("Found evaluation for {} in tree, the result is {}",
                    input, output.results()));
        } else {
            LOGGER.info("No evaluation for {} in tree", input);
        }
    }
}
