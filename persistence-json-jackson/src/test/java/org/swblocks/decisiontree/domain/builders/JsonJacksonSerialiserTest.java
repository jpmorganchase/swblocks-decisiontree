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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swblocks.decisiontree.FileLoader;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleMatcher;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.eh.Result;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Test cases for {@link JsonJacksonSerialiser}.
 */
public class JsonJacksonSerialiserTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonJacksonSerialiserTest.class);

    @Test
    public void testJsonGenerationWithNonUuidValueGroups() throws IOException {
        final DecisionTreeRuleSet originalRuleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        assertNotNull(originalRuleSet);
        assertNotNull(originalRuleSet.getRules());
        assertNotNull(originalRuleSet.getValueGroups());

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new JsonJacksonSerialiser().serialiseRuleSet(stream, originalRuleSet);
        final String jsonString = stream.toString("UTF-8");
        assertNotNull(jsonString);
        LOGGER.debug("JsonString is \n{}", jsonString);

        final JsonJacksonParser parser = new JsonJacksonParser();
        final DecisionTreeRuleSet serialisedRuleSet = parser.parseRuleSet(
                new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(serialisedRuleSet);
        assertNotNull(serialisedRuleSet.getRules());

        assertThat(serialisedRuleSet.getValueGroups().size(), equalTo(originalRuleSet.getValueGroups().size()));
        assertThat(serialisedRuleSet.getValueGroups().stream().findFirst().get(),
                equalTo(originalRuleSet.getValueGroups().stream().findFirst().get()));

        assertThat(serialisedRuleSet.getDriversByType(InputValueType.STRING),
                IsCollectionContaining.hasItems(originalRuleSet.getDriversByType(InputValueType.STRING)
                        .toArray(new InputDriver[originalRuleSet.getDriversByType(InputValueType.STRING)
                                .size()])));

        assertThat(serialisedRuleSet.getDriversByType(InputValueType.REGEX),
                IsCollectionContaining.hasItems(originalRuleSet.getDriversByType(InputValueType.REGEX)
                        .toArray(new InputDriver[originalRuleSet.getDriversByType(InputValueType.REGEX)
                                .size()])));

        for (final DecisionTreeRule rule : originalRuleSet.getRules().values()) {
            if (Arrays.stream(rule.getDrivers()).noneMatch(inputDriver ->
                    InputValueType.VALUE_GROUP == inputDriver.getType())) {
                assertThat(rule, DecisionTreeRuleMatcher.isSame(
                        serialisedRuleSet.getRules().get(rule.getRuleIdentifier())));
            } else {
                // simple input has now been transformed for value group
                final DecisionTreeRule other = serialisedRuleSet.getRules().get(rule.getRuleIdentifier());

                final InputDriver[] drivers = rule.getDrivers();
                final InputDriver[] otherDrivers = other.getDrivers();
                for (int i = 0; i < drivers.length; ++i) {
                    final InputDriver driver = drivers[i];
                    if (InputValueType.VALUE_GROUP == driver.getType()) {
                        final ValueGroup group = serialisedRuleSet.getValueGroups().stream().findFirst().get();
                        assertNotNull(group);
                        assertThat(driver.getValue(), equalTo(group.getId().toString()));
                    } else {
                        assertThat(driver, equalTo(otherDrivers[i]));
                    }
                }
            }
        }
    }

    @Test
    public void testJsonGenerationForUuidFormattedValueGroups() throws IOException {
        final Result<DecisionTreeRuleSet> result = FileLoader.jsonLoader("", "commissions", new JsonJacksonParser())
                .get();
        assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        final DecisionTreeRuleSet originalRuleSet = result.getData();

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new JsonJacksonSerialiser().serialiseRuleSet(stream, originalRuleSet);
        final String jsonString = stream.toString("UTF-8");
        assertNotNull(jsonString);
        LOGGER.debug("JsonString is \n{}", jsonString);

        final JsonJacksonParser parser = new JsonJacksonParser();
        final DecisionTreeRuleSet serialisedRuleSet = parser.parseRuleSet(
                new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(serialisedRuleSet);
        assertNotNull(serialisedRuleSet.getRules());
        assertThat(serialisedRuleSet.getDriversByType(InputValueType.VALUE_GROUP),
                IsCollectionContaining.hasItems(originalRuleSet.getDriversByType(InputValueType.VALUE_GROUP)
                        .toArray(new InputDriver[originalRuleSet.getDriversByType(InputValueType.VALUE_GROUP)
                                .size()])));

        assertThat(serialisedRuleSet.getDriversByType(InputValueType.STRING),
                IsCollectionContaining.hasItems(originalRuleSet.getDriversByType(InputValueType.STRING)
                        .toArray(new InputDriver[originalRuleSet.getDriversByType(InputValueType.STRING)
                                .size()])));

        assertThat(serialisedRuleSet.getDriversByType(InputValueType.REGEX),
                IsCollectionContaining.hasItems(originalRuleSet.getDriversByType(InputValueType.REGEX)
                        .toArray(new InputDriver[originalRuleSet.getDriversByType(InputValueType.REGEX)
                                .size()])));

        for (final DecisionTreeRule rule : originalRuleSet.getRules().values()) {
            assertThat(rule, DecisionTreeRuleMatcher.isSame(
                    serialisedRuleSet.getRules().get(rule.getRuleIdentifier())));
        }
    }

    @Ignore
    @Test
    public void testJsonWriteToFile() throws IOException {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();

        try (final OutputStream stream = Files.newOutputStream(Paths.get("COMMISSIONS.zip"), StandardOpenOption.CREATE);
             final CheckedOutputStream checksum = new CheckedOutputStream(stream, new CRC32());
             final ZipOutputStream zipOutputStream = new ZipOutputStream(checksum)) {
            final ZipEntry entry = new ZipEntry(ruleSet.getName() + ".json");
            zipOutputStream.putNextEntry(entry);
            new JsonJacksonSerialiser().serialiseRuleSet(zipOutputStream, ruleSet);
        }
    }
}