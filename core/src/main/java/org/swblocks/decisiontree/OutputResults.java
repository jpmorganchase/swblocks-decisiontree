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

package org.swblocks.decisiontree;

import java.util.Map;

import org.swblocks.decisiontree.domain.DecisionTreeRule;

/**
 * Holder for the results from an evaluation.
 *
 * <p>This is the external API for accessing the results.
 */
public class OutputResults {
    private final DecisionTreeRule rule;

    OutputResults(final DecisionTreeRule rule) {
        this.rule = rule;
    }

    public Map<String, String> results() {
        return this.rule.getOutputs();
    }
}
