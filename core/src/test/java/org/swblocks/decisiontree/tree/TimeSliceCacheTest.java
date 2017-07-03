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
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.Test;
import org.swblocks.jbl.util.DateRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test cases for {@link TimeSliceCache}.
 */
public class TimeSliceCacheTest {
    @Test
    public void testCreateTimeSlices() {
        final Instant now = Instant.now();

        final TimeSliceCache timeSliceCache = TimeSliceCache.getInstance();
        final TreeNode node1 = NodeSupplier.createTreeNode(new StringDriver("Test1"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final Optional<DateRange> range1 = Optional.of(new DateRange(now.minus(10, ChronoUnit.DAYS),
                now.minus(5, ChronoUnit.DAYS)));
        final TreeNode node2 = NodeSupplier.createTreeNode(new StringDriver("Test2"),
                NodeSupplier.ROOT_NODE_LEVEL).get();
        final Optional<DateRange> range2 = Optional.of(new DateRange(now.plus(1, ChronoUnit.DAYS),
                now.plus(10, ChronoUnit.DAYS)));
        final Optional<DateRange> range3 = Optional.of(new DateRange(now.plus(11, ChronoUnit.DAYS),
                now.plus(20, ChronoUnit.DAYS)));

        timeSliceCache.put(range1.get(), Optional.of(node1));
        timeSliceCache.put(range2.get(), Optional.of(node2));

        assertEquals(node1, timeSliceCache.get(range1).get());
        assertEquals(node2, timeSliceCache.get(range2).get());
        assertFalse(timeSliceCache.get(range3).isPresent());

        final DateRange[] sliceSet = timeSliceCache.keys();
        assertEquals(sliceSet.length, 2);
    }
}