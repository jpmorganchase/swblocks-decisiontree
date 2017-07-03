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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSetMatcher;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.jbl.eh.EhSupport;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link JsonJacksonParser}.
 */
public class JsonJacksonParserTest {
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidJson() {
        final String invalidJsonStr = "  \"name\": \"FNC_CALCULATION_METHOD_RULESET\",\n" +
                "  \"drivers\": [\n" +
                "    \"CALCULATION_METHOD_NAME\"\n" +
                "  ],\n";
        new JsonJacksonParser().parseRuleSet(new ByteArrayInputStream(invalidJsonStr.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void initialisesIdenticalRuleSetsFromFile() {
        assertThat(getRuleSet("commissions"), DecisionTreeRuleSetMatcher.isSame(getRuleSet("commissions")));
    }

    private DecisionTreeRuleSet getRuleSet(final String name) {
        return EhSupport.propagateFn(() -> {
            try (final InputStream is =
                         getClass().getClassLoader().getResourceAsStream(name + ".json")) {
                assertNotNull(is);
                return new JsonJacksonParser().parseRuleSet(is);
            }
        });
    }

    @Test
    public void parseRuleSetWithMultipleValueGroups() {
        EhSupport.propagate(() -> {
            try (final InputStream is = getClass().getClassLoader().getResourceAsStream("commissions.json")) {
                assertNotNull(is);
                final DecisionTreeRuleSet deserializedRuleSet = new JsonJacksonParser().parseRuleSet(is);
                assertNotNull(deserializedRuleSet);
                assertEquals("commissions", deserializedRuleSet.getName());

                assertValueGroups(deserializedRuleSet);

                assertThat(deserializedRuleSet.getRules().values(), hasSize(7));

                final DriverCache cache = deserializedRuleSet.getDriverCache();
                assertTrue(cache.contains(GroupDriver.VG_PREFIX + (new UUID(0, 6)).toString()));
                assertTrue(cache.contains(GroupDriver.VG_PREFIX + (new UUID(0, 7)).toString()));
            }
        });
    }

    private void assertValueGroups(final DecisionTreeRuleSet deserializedRuleSet) {
        final Set<ValueGroup> groups = deserializedRuleSet.getValueGroups();
        assertThat(groups, hasSize(2));

        ValueGroup group = groups.stream().filter(valueGroup ->
                valueGroup.getId().equals(new UUID(0, 6))).findFirst().get();
        assertThat(group.getName(), equalTo("CMEGroup"));
        assertThat(group.getValues(), contains("CME", "CBOT"));
        assertThat(group.getRange().getStart(), equalTo(DecisionTreeRule.EPOCH));

        group = groups.stream().filter(valueGroup ->
                valueGroup.getId().equals(new UUID(0, 7))).findFirst().get();
        assertThat(group.getName(), equalTo("CMEGroup"));
        assertThat(group.getValues(), contains("CME", "CBOT", "NYMEX"));
        assertThat(group.getRange().getFinish(), equalTo(DecisionTreeRule.MAX));
    }
}