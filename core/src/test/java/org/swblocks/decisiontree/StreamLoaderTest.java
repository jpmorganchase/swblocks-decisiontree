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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilderParser;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test class for {@link StreamLoader}.
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamLoaderTest {
    @Mock
    private RuleBuilderParser parser;

    @Before
    public void setUp() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        when(parser.parseRuleSet(any())).thenReturn(ruleSet);
    }

    @Test
    public void nullInputStream() {
        String message = null;
        try {
            StreamLoader.jsonLoader(null, parser);
        } catch (final Exception exception) {
            message = exception.getMessage();
        }

        assertEquals("ruleset input stream is null.", message);
    }

    @Test
    public void parsesStream() {
        final StreamLoader loader = StreamLoader.jsonLoader(
                getClass().getClassLoader().getResourceAsStream("commissions.json"), parser);
        Result<DecisionTreeRuleSet> result = EhSupport.propagateFn(() -> {
            try (final StreamLoader local = loader) {
                return local.get();
            }
        });

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("commissions", result.getData().getName());

        result = loader.get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("commissions", result.getData().getName());
    }

    @Test
    public void invalidStream() {
        final RuleBuilderParser failingParser = mock(RuleBuilderParser.class);
        when(failingParser.parseRuleSet(any())).thenThrow(new IllegalArgumentException());
        final Result<DecisionTreeRuleSet> result = EhSupport.propagateFn(() -> {
            try (final StreamLoader loader = StreamLoader.jsonLoader(
                    new ByteArrayInputStream("INVALID".getBytes(StandardCharsets.UTF_8)), failingParser)) {
                return loader.get();
            }
        });

        assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
    }

    @Test
    public void neverRetries() {
        EhSupport.propagateFn(() -> {
            try (final StreamLoader loader = StreamLoader.jsonLoader(
                    new ByteArrayInputStream("INVALID".getBytes(StandardCharsets.UTF_8)), parser)) {
                assertFalse(loader.test(null));
                return loader.get();
            }
        });
    }
}
