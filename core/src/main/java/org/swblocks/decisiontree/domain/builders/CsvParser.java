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

package org.swblocks.decisiontree.domain.builders;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.jbl.builders.Builder;

/**
 * Utility class to load a {@link DecisionTreeRuleSet} from a basic CSV file defaulting a lot of information.
 * All rules are assumed to be from {@link DecisionTreeRule#EPOCH} to {@link DecisionTreeRule#MAX}
 *
 * <p>The CSV file has the format of header line with the input driver names and each following row containing the rules
 * with the drivers and then the name:value pairs of the outputs.
 *
 * <p>For example
 * DRIVER1, DRVIER2, DRIVER3
 * Input1, *, Input2, Out1:1, Out2:2
 * This will create a new ruleset with 3 drivers and 1 rule with the inputs to the rule being Input1, * and Input2.
 * The output returned will be the map of [(Out1,1)(Out2,2)].
 */
public class CsvParser implements RuleBuilderParser {

    private static final String DELIMINATOR = ",";
    private final String ruleSetName;

    private CsvParser(final String ruleSetName) {
        this.ruleSetName = ruleSetName;
    }

    public static CsvParser instanceOf(final String ruleSetName) {
        return new CsvParser(ruleSetName);
    }

    @Override
    public DecisionTreeRuleSet parseRuleSet(final InputStream inputStream) {
        return processCsvStream(new BufferedReader(new InputStreamReader(inputStream)).lines());
    }

    /**
     * processes a Stream of Strings into a List of List of Strings, broken by lines and then by {@link #DELIMINATOR}.
     *
     * @param lines Stream of Strings
     * @return Constructed {@link DecisionTreeRuleSet}
     */
    public DecisionTreeRuleSet processCsvStream(final Stream<String> lines) {
        final List<List<String>> values = lines.map(
                line -> Arrays.stream(line.split(DELIMINATOR))
                        .map(String::trim)
                        .map(s -> s.replaceAll("^\"|\"$", ""))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()))
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        final DriverCache cache = new DriverCache();
        final List<String> drivers = new ArrayList<>(values.get(0));
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(this.ruleSetName, drivers);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i).size() > drivers.size()) {
                ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                        .with(RuleBuilder::cache, cache)
                        .with(RuleBuilder::input, values.get(i).subList(0, drivers.size()))
                        .with(RuleBuilder::output, values.get(i).subList(drivers.size(), values.get(i).size()))
                        .with(RuleBuilder::setId, UUID.randomUUID())
                        .with(RuleBuilder::setCode, UUID.randomUUID())
                        .with(RuleBuilder::start, DecisionTreeRule.EPOCH)
                        .with(RuleBuilder::end, DecisionTreeRule.MAX));
            }
        }

        return ruleSetBuilder.build();
    }
}
