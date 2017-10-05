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
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.RepeatingRule;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.google.code.tempusfugit.concurrency.annotations.Repeating;
import org.swblocks.jbl.util.Range;

/**
 * Concurrent test of the {@link TimeSliceCache} to ensure no concurrent modification exceptions are thrown.
 */
public class TimeSliceCacheConcurrencyTest {
    private final TimeSliceCache cache = TimeSliceCache.getInstance();

    @Rule
    public ConcurrentRule concurrentRule = new ConcurrentRule();
    @Rule
    public RepeatingRule repeatingRule = new RepeatingRule();

    @Test
    @Concurrent(count = 20)
    @Repeating(repetition = 200)
    public void cacheAccess() {
        final Instant now = Instant.now();
        final int start = (int) (Math.random() * 100);
        final int end = start + (int) (Math.random() * 100);

        final Range<Instant> dateRange = new Range<>(now.plus(Period.ofWeeks(start)), now.plus(Period.ofWeeks(end)));
        final TreeNode node = NodeSupplier.createTreeNode(
                new StringDriver(UUID.randomUUID().toString()), NodeSupplier.ROOT_NODE_LEVEL).get();

        final Optional<TreeNode> treeNode = Optional.of(node);
        this.cache.put(dateRange, treeNode);

        for (final Range<Instant> dr : this.cache.keys()) {
            this.cache.get(Optional.of(dr));
        }

        this.cache.get(Optional.of(dateRange));
    }
}
