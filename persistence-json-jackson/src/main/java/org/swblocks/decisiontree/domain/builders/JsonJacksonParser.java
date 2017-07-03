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

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;
import org.swblocks.jbl.util.DateRange;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Utility class to load a {@link DecisionTreeRuleSet} from an input stream.
 */
public class JsonJacksonParser implements RuleBuilderParser {
    /**
     * Constructs a {@link DecisionTreeRuleSet} from a {@link InputStream}.
     *
     * @param inputStream of the json to process
     * @return Constructed {@link DecisionTreeRuleSet}
     */
    @Override
    public DecisionTreeRuleSet parseRuleSet(final InputStream inputStream) {
        return EhSupport.propagateFn(() -> {
            try (JsonParser parser = new JsonFactory().createParser(inputStream)) {

                EhSupport.ensureArg(parser.nextToken() == JsonToken.START_OBJECT, "InputStream is not valid JSON");

                String ruleSetName = "";
                final List<String> drivers = new ArrayList<>();
                final List<DecisionTreeRule> rules = new ArrayList<>(1);
                final DriverCache cache = new DriverCache();
                final Set<ValueGroup> groups = ConcurrentHashMap.newKeySet();

                while (!parser.isClosed()) {
                    final JsonToken token = parser.nextToken();

                    if (JsonToken.FIELD_NAME.equals(token)) {
                        final String fieldName = parser.getCurrentName();
                        switch (fieldName) {
                            case "name":
                                parser.nextToken();
                                ruleSetName = parser.getText();
                                break;
                            case "drivers":
                                parseArrayOfStrings(parser, drivers);
                                break;
                            case "groups":
                                parseGroups(parser, cache, groups);
                                break;
                            case "rules":
                                parser.nextToken();
                                while (parser.nextToken() != JsonToken.END_ARRAY) {
                                    final DecisionTreeRule rule = parseRule(parser, cache, groups);
                                    if (rule != null) {
                                        rules.add(rule);
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                return new DecisionTreeRuleSet(ruleSetName, rules.stream().collect(
                        Collectors.toMap(DecisionTreeRule::getRuleIdentifier, r -> r)), drivers, cache, groups);
            }
        });
    }

    private void parseGroups(final JsonParser parser, final DriverCache cache, final Set<ValueGroup> groups) {
        EhSupport.propagate(() -> {
            parser.nextToken();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                parseValueGroup(parser, groups);
            }
            checkNestedGroupDriverValues(groups);
            RuleSetBuilder.addValueGroupsToDriverCache(cache, groups);
        });
    }

    private void parseValueGroup(final JsonParser parser,
                                 final Set<ValueGroup> valueGroups) {
        EhSupport.propagate(() -> {
            Optional<UUID> uuid = Optional.empty();
            String name = null;
            final List<String> valueGroup = new ArrayList<>();
            Instant start = DecisionTreeRule.EPOCH;
            Instant end = DecisionTreeRule.MAX;

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                final String ruleFieldName = parser.getCurrentName();
                switch (ruleFieldName) {
                    case "uuid":
                        parser.nextToken();
                        uuid = Optional.of(getUuid(parser));
                        break;
                    case "name":
                        parser.nextToken();
                        name = parser.getText();
                        break;
                    case "values":
                        parseArrayOfStrings(parser, valueGroup);
                        break;
                    case "start":
                        parser.nextToken();
                        start = Instant.ofEpochMilli(parser.getLongValue());
                        break;
                    case "end":
                        parser.nextToken();
                        end = Instant.ofEpochMilli(parser.getLongValue());
                        break;
                    default:
                        break;
                }
            }
            if (name != null) {
                if (valueGroup.isEmpty()) {
                    valueGroup.add(name);
                }
                valueGroups.add(new ValueGroup(uuid.isPresent() ? uuid.get() : UUID.randomUUID(), name, valueGroup,
                        new DateRange(start, end)));
            }
        });
    }

    private void checkNestedGroupDriverValues(final Set<ValueGroup> valueGroups) {
        final Map<String, UUID> unmatched = new HashMap<>();
        valueGroups.forEach(currentGroup -> {
            final List<String> drivers = currentGroup.getValues();
            final Set<String> unconverted =
                    drivers.stream().filter(driver -> driver.startsWith(GroupDriver.VG_PREFIX) &&
                            !isUuid(driver.split(":")[1])).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(unconverted)) {
                final List<String> amended = new ArrayList<>(drivers.size());

                drivers.forEach(driver -> {
                    if (unconverted.contains(driver)) {
                        if (unmatched.containsKey(driver)) {
                            amended.add(GroupDriver.VG_PREFIX + unmatched.get(driver));
                        } else {
                            final String groupName = driver.split(":")[1];
                            final Optional<ValueGroup> group = valueGroups.stream().filter(valueGroup ->
                                    valueGroup.getName().equals(groupName)).findFirst();
                            EhSupport.ensure(group.isPresent(), "Could not find a value group %s", driver);

                            final UUID id = group.get().getId();
                            unmatched.put(driver, id);
                            amended.add(GroupDriver.VG_PREFIX + id);
                        }
                    } else {
                        amended.add(driver);
                    }
                });
                currentGroup.updateValues(amended);
            }
        });
    }

    private DecisionTreeRule parseRule(final JsonParser parser, final DriverCache cache, final Set<ValueGroup> groups) {
        return EhSupport.propagateFn(() -> {
            UUID uuid = null;
            String code = null;
            final List<String> inputs = new ArrayList<>();
            final List<String> outputs = new ArrayList<>();
            Instant start = null;
            Instant end = null;

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                final String ruleFieldName = parser.getCurrentName();
                switch (ruleFieldName) {
                    case "id":
                        parser.nextToken();
                        final long id = parser.getLongValue();
                        uuid = new UUID(0, id);
                        break;
                    case "uuid":
                        parser.nextToken();
                        uuid = getUuid(parser);
                        break;
                    case "cd":
                        parser.nextToken();
                        code = parser.getText();
                        break;
                    case "in":
                        parseInputArrayOfStrings(parser, inputs, cache, groups);
                        break;
                    case "out":
                        parseArrayOfStrings(parser, outputs);
                        break;
                    case "start":
                        parser.nextToken();
                        start = Instant.ofEpochMilli(parser.getLongValue());
                        break;
                    case "end":
                        parser.nextToken();
                        end = Instant.ofEpochMilli(parser.getLongValue());
                        break;
                    default:
                        break;
                }
            }

            final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator()
                    .with(RuleBuilder::cache, cache)
                    .with(RuleBuilder::input, inputs)
                    .with(RuleBuilder::output, outputs)
                    .with(RuleBuilder::setId, uuid)
                    .with(RuleBuilder::setCode,
                            code != null ? UUID.fromString(code) : uuid != null ? uuid : UUID.randomUUID())
                    .with(RuleBuilder::start, start)
                    .with(RuleBuilder::end, end);
            return ruleBuilder.build();
        });
    }

    private UUID getUuid(final JsonParser parser) {
        return EhSupport.propagateFn(() -> {
            final String uuidStr = parser.getText();
            UUID uuid = null;
            if (uuidStr != null) {
                uuid = UUID.fromString(uuidStr);
            }
            return uuid;
        });
    }

    private void parseArrayOfStrings(final JsonParser parser, final List<String> outputs) {
        EhSupport.propagate(() -> {
            parser.nextToken();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                outputs.add(parser.getText());
            }
        });
    }

    private void parseInputArrayOfStrings(final JsonParser parser,
                                          final List<String> inputs,
                                          final DriverCache cache,
                                          final Set<ValueGroup> groups) {
        EhSupport.propagate(() -> {
            parser.nextToken();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                String value = parser.getText();
                if (value.startsWith(GroupDriver.VG_PREFIX) && !cache.contains(value)) {
                    final String groupName = value.split(":")[1];
                    final Optional<ValueGroup> group =
                            groups.stream().filter(valueGroup -> valueGroup.getName().equals(groupName)).findFirst();
                    if (group.isPresent()) {
                        value = GroupDriver.VG_PREFIX + group.get().getId();
                    } else {
                        inputs.add(value);
                    }
                }
                inputs.add(value);
            }
        });
    }

    private boolean isUuid(final String uuid) {
        Result<UUID> result;
        try {
            result = Result.success(UUID.fromString(uuid));
        } catch (final Exception exception) {
            result = Result.failure(() -> exception);
        }
        return result.isSuccess();
    }
}
