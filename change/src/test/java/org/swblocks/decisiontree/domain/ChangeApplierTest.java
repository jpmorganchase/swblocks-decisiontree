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

package org.swblocks.decisiontree.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.jbl.test.utils.JblTestClassUtils;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * Test cases for {@link ChangeApplier}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DecisionTreeRuleSet.class, Change.class})
public class ChangeApplierTest {
    @Test
    public void hasPrivateConstructor() {
        JblTestClassUtils.assertConstructorIsPrivate(ChangeApplier.class);
    }

    @Test
    public void testAppliesChangeToRuleSet() {
        final DecisionTreeRuleSet ruleSet = mock(DecisionTreeRuleSet.class);
        final Change change = mock(Change.class);
        ChangeApplier.apply(ruleSet, change);
        verify(ruleSet).updateRules(change);
    }
}