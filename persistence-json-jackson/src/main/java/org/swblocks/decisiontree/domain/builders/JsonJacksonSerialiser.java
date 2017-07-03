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

import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Utility class to serialise a {@link DecisionTreeRuleSet} to an output stream.
 */
public class JsonJacksonSerialiser implements RuleBuilderSerialiser {
    /**
     * Serialises the {@link DecisionTreeRuleSet} into the {@link OutputStream} in Json format.
     *
     * <p>Any exceptions are wrapped and thrown via the {@link EhSupport} API.
     *
     * @param out     {@link OutputStream} to write into
     * @param ruleSet {@link DecisionTreeRuleSet} to serialise
     */
    public static void toJsonWriter(final OutputStream out, final DecisionTreeRuleSet ruleSet) {
        EhSupport.propagate(() -> {
            try (JsonGenerator generator = new JsonFactory().createGenerator(out, JsonEncoding.UTF8)) {
                generator.useDefaultPrettyPrinter();
                generator.writeStartObject();
                generator.writeStringField("name", ruleSet.getName());
                writeRuleSetDrivers(generator, ruleSet);
                writeRuleSetGroups(generator, ruleSet.getValueGroups());
                writeRules(generator, ruleSet);

                generator.writeEndObject();
                generator.flush();
            }
        });
    }

    private static void writeRules(final JsonGenerator generator, final DecisionTreeRuleSet ruleSet) {
        EhSupport.propagate(() -> {
            if (!ruleSet.getRules().isEmpty()) {
                generator.writeArrayFieldStart("rules");
                for (final DecisionTreeRule rule : ruleSet.getRules().values()) {
                    generator.writeStartObject();
                    generator.writeStringField("uuid", rule.getRuleIdentifier().toString());
                    if (rule.getRuleCode() != null) {
                        generator.writeStringField("cd", rule.getRuleCode().toString());
                    }
                    writeRuleInputs(generator, rule);
                    writeRuleOutput(generator, rule);
                    writeRuleTimes(generator, rule);
                    generator.writeEndObject();
                }
                generator.writeEndArray();
            }
        });
    }

    private static void writeRuleInputs(final JsonGenerator generator,
                                        final DecisionTreeRule rule) {
        EhSupport.propagate(() -> {
            generator.writeArrayFieldStart("in");
            for (final InputDriver input : rule.getDrivers()) {
                generator.writeString(input.toString());
            }
            generator.writeEndArray();
        });
    }

    private static void writeRuleTimes(final JsonGenerator generator, final DecisionTreeRule rule) {
        EhSupport.propagate(() -> {
            generator.writeNumberField("start", rule.getStart().toEpochMilli());
            generator.writeNumberField("end", rule.getEnd().toEpochMilli());
        });
    }

    private static void writeRuleOutput(final JsonGenerator generator, final DecisionTreeRule rule) {
        EhSupport.propagate(() -> {
            if (CollectionUtils.isNotEmpty(rule.getOutputs())) {
                generator.writeArrayFieldStart("out");
                for (final Map.Entry<String, String> output : rule.getOutputs().entrySet()) {
                    generator.writeString(output.getKey() + ":" + output.getValue());
                }
                generator.writeEndArray();
            }
        });
    }

    private static void writeRuleSetDrivers(final JsonGenerator generator, final DecisionTreeRuleSet ruleSet) {
        EhSupport.propagate(() -> {
            generator.writeArrayFieldStart("drivers");
            for (final String name : ruleSet.getDriverNames()) {
                generator.writeString(name);
            }
            generator.writeEndArray();
        });
    }

    private static void writeRuleSetGroups(final JsonGenerator generator, final Set<ValueGroup> groups) {
        EhSupport.propagate(() -> {
            if (CollectionUtils.isNotEmpty(groups)) {
                generator.writeArrayFieldStart("groups");

                for (final ValueGroup group : groups) {
                    generator.writeStartObject();
                    generator.writeStringField("uuid", group.getId().toString());
                    generator.writeStringField("name", group.getName());
                    generator.writeArrayFieldStart("values");
                    for (final String entry : group.getValues()) {
                        generator.writeString(entry);
                    }
                    generator.writeEndArray();

                    writeValueGroupTimes(generator, group.getRange());
                    generator.writeEndObject();
                }
                generator.writeEndArray();
            }
        });
    }

    private static void writeValueGroupTimes(final JsonGenerator generator, final DateRange range) {
        EhSupport.propagate(() -> {
            generator.writeNumberField("start", range.getStart().toEpochMilli());
            generator.writeNumberField("end", range.getFinish().toEpochMilli());
        });
    }

    @Override
    public void serialiseRuleSet(final OutputStream outputStream, final DecisionTreeRuleSet decisionTreeRuleSet) {
        toJsonWriter(outputStream, decisionTreeRuleSet);
    }
}
