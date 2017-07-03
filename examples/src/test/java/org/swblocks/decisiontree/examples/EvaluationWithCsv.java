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

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swblocks.decisiontree.DecisionTree;
import org.swblocks.decisiontree.Input;
import org.swblocks.decisiontree.OutputResults;
import org.swblocks.decisiontree.StreamLoader;
import org.swblocks.decisiontree.tree.DecisionTreeType;

/**
 * Example for the Decision Tree which generates a DecisionTree from a CSV input stream.
 *
 * <p>Format for CSV String is:
 *
 * <p>Comma separated Input names (header row)
 *
 * <p>Repeating rules as
 * Comma separated input strings matching the header row, then comma separated Output Pairs for the results
 */
public class EvaluationWithCsv {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationWithCsv.class);
    final String testFile = "\nEXMETHOD,EXCHANGE,PRODUCT,REGION,ASSET\n\"" +
            "*, VG:CMEGroup:CME:CBOT,*,*,INDEX, Rate:1.1\n" +
            "*, CME, S&P, *, INDEX, Rate:1.2\n" +
            "VOICE, CME, ED,*,RATE, Rate:1.4\n" +
            "*, *, *,US,*, Rate:1.5\n" +
            "*, *, *,UK,*, Rate:1.1, Unit:£\n";

    @Test
    public void createTreeFromCsvTextAndEvaluate() {
        /**
         * Create a decision tree which is not time aware {@link DecisionTreeType#SINGLE}.
         */
        final DecisionTree decisionTree = DecisionTree.instanceOf(StreamLoader.csvLoader("COMMISSIONS",
                new ByteArrayInputStream(this.testFile.getBytes())),
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
