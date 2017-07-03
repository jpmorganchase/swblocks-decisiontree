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

import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swblocks.decisiontree.DecisionTree;
import org.swblocks.decisiontree.FileLoader;
import org.swblocks.decisiontree.Input;
import org.swblocks.decisiontree.OutputResults;
import org.swblocks.decisiontree.domain.builders.JsonJacksonParser;
import org.swblocks.decisiontree.tree.DecisionTreeType;

/**
 * Example for the Decision Tree which loads a json file from the classfile and then runs a few evaluations on it.
 */
public class EvaluationFromJsonFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationFromJsonFile.class);

    @Test
    public void loadFileAndEvaluate() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(FileLoader.zippedJsonLoader("", "COMMISSIONS",
                new JsonJacksonParser()), DecisionTreeType.SINGLE);

        // Matches rule inputs [ "VOICE", "CME", "ED", "*", "RATE" ]
        Input input = decisionTree.createInputs("VOICE", "CME", "ED", "US", "RATE");
        Optional<OutputResults> evaluationFor = decisionTree.getEvaluationFor(input);
        logResult(input, evaluationFor);

        // Matches rule inputs [ "*", "*", "*", "US", "*" ]
        input = decisionTree.createInputs("DMA", "TSE", "NK", "JP", "INDEX");
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
