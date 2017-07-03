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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilderParser;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.eh.Result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests cases for {@link FileLoader}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FileLoaderTest {
    @Mock
    private RuleBuilderParser parser;

    @Before
    public void setUp() {
        final DecisionTreeRuleSet ruleSet = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        when(parser.parseRuleSet(any())).thenReturn(ruleSet);
    }

    @Test
    public void testSuccessResult() {
        final Result<DecisionTreeRuleSet> result = FileLoader.jsonLoader("", "commissions_no_uuid", parser).get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("commissions", result.getData().getName());
    }

    @Test
    public void testSuccessResultMultipleValueGroups() {
        final Result<DecisionTreeRuleSet> result = FileLoader.jsonLoader("", "commissions", parser).get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("commissions", result.getData().getName());
    }

    @Test
    public void testSuccessZipResult() {
        final Result<DecisionTreeRuleSet> result = FileLoader.zippedJsonLoader("", "COMMISSIONS", parser).get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("commissions", result.getData().getName());
    }

    @Test
    public void testSuccessZipCsvResult() {
        final Result<DecisionTreeRuleSet> result = FileLoader.csvLoader("", "commissions").get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("commissions", result.getData().getName());
    }

    @Test
    public void testMissingFileResult() {
        final Result<DecisionTreeRuleSet> result = FileLoader.jsonLoader("", "I_DO_NOT_EXIST", parser).get();
        assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(new IllegalArgumentException("File I_DO_NOT_EXIST.json does not exist.").getMessage(),
                result.getException().getMessage());
    }

    @Test
    public void testRetryingPredicate() {
        final FileLoader loader = FileLoader.jsonLoader("", "I_DO_NOT_EXIST", parser);
        final Result<DecisionTreeRuleSet> result = loader.get();
        assertFalse(loader.test(result));
    }
}