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

import org.junit.Test;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.eh.Result;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link BuilderLoader}.
 */
public class BuilderLoaderTest {
    @Test
    public void testCreatingFromBuilder() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> commisionRuleSetSupplier =
                CommisionRuleSetSupplier.getCommisionRuleSet();
        final BuilderLoader builderLoader = BuilderLoader.instanceOf(commisionRuleSetSupplier);
        final Result<DecisionTreeRuleSet> result = builderLoader.get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("commissions", result.getData().getName());
        assertFalse(builderLoader.test(result));
    }

    @Test
    public void testFailure() {
        final BuilderLoader builderLoader = BuilderLoader.instanceOf(null);
        final Result<DecisionTreeRuleSet> result = builderLoader.get();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }
}