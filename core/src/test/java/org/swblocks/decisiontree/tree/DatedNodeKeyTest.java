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

package org.swblocks.decisiontree.tree;

import java.time.Instant;

import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.jbl.util.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link DatedNodeKey}.
 */
public class DatedNodeKeyTest {
    @Test
    public void equalsCorrect() {
        DatedNodeKey key = new DatedNodeKey("test1", new Range<>(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX));
        final DatedNodeKey sameKey = new DatedNodeKey("test1",
                new Range<>(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX));

        assertTrue(key.equals(key));
        assertTrue(key.equals(sameKey));
        assertFalse(key.equals(null));
        assertFalse(key.equals(new Integer(1)));

        DatedNodeKey otherKeyValue = new DatedNodeKey("test2",
                new Range<>(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX));
        assertFalse(key.equals(otherKeyValue));

        otherKeyValue = new DatedNodeKey("test1", new Range<>(Instant.now(), DecisionTreeRule.MAX));
        assertFalse(key.equals(otherKeyValue));

        otherKeyValue = new DatedNodeKey("test1", new Range<>(DecisionTreeRule.EPOCH, Instant.now()));
        assertFalse(key.equals(otherKeyValue));

        otherKeyValue = new DatedNodeKey("test1", new Range<>(DecisionTreeRule.EPOCH, null));
        assertFalse(key.equals(otherKeyValue));

        otherKeyValue = new DatedNodeKey("test1", new Range<>(null, DecisionTreeRule.MAX));
        assertFalse(key.equals(otherKeyValue));

        otherKeyValue = new DatedNodeKey("test1", null);
        assertFalse(key.equals(otherKeyValue));

        key = new DatedNodeKey("test1", null);
        assertTrue(key.equals(otherKeyValue));

        otherKeyValue = new DatedNodeKey("test2", new Range<>(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX));
        assertFalse(key.equals(otherKeyValue));
    }

    @Test
    public void hashCodeCorrect() {
        final DatedNodeKey key = new DatedNodeKey("test1", new Range<>(DecisionTreeRule.EPOCH,
                DecisionTreeRule.MAX));
        final DatedNodeKey other = new DatedNodeKey("test1", new Range<>(DecisionTreeRule.EPOCH,
                DecisionTreeRule.MAX));
        assertEquals(key.hashCode(), other.hashCode());
    }
}
