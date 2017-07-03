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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.swblocks.jbl.cache.LruCache;
import org.swblocks.jbl.util.DateRange;

/**
 * LRUCache storing a map with a key of {@link DateRange} hashCode and an {@link Optional} value of {@link TreeNode}.
 *
 * <p>Used for storing time sliced trees once they have been built.
 */
public class TimeSliceCache {
    private static final int MAX_CACHE_SIZE = 20;
    private final Map<DateRange, Optional<TreeNode>> cache;

    private TimeSliceCache() {
        this.cache = Collections.synchronizedMap(LruCache.getCache(MAX_CACHE_SIZE, MAX_CACHE_SIZE, 0.75F));
    }

    public static TimeSliceCache getInstance() {
        return new TimeSliceCache();
    }

    public void put(final DateRange range, final Optional<TreeNode> node) {
        this.cache.putIfAbsent(range, node);
    }

    /**
     * Gets the {@link Optional} {@link TreeNode} from the cache which matches the {@link DateRange} passed in.
     *
     * @param range {@link DateRange} to match against.
     * @return The {@link Optional} {@link TreeNode} from the cache, {@code Optional.empty()} if not in cache.
     */
    public Optional<TreeNode> get(final Optional<DateRange> range) {
        if (range.isPresent()) {
            final Optional<TreeNode> treeNode = this.cache.get(range.get());
            if (treeNode != null) {
                return treeNode;
            }
        }
        return Optional.empty();
    }

    public DateRange[] keys() {
        final Set<DateRange> keys = this.cache.keySet();
        return keys.toArray(new DateRange[keys.size()]);
    }
}