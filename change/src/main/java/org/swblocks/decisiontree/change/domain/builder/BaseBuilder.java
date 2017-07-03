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

package org.swblocks.decisiontree.change.domain.builder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.jbl.util.DateRange;

/**
 * Abstract class that is used by classes extending {@link DomainBuilder} and {@link RuleGroupChangeBuilder}.
 *
 * <p>Used to define common functionality used by all builder sub-classes.
 *
 * @param <C> the change object type
 * @param <D> the domain object type
 */
abstract class BaseBuilder<C, D> {
    static final BiPredicate<Instant, Instant> TIME_MATCH = Instant::equals;
    static final BiPredicate<DateRange, DateRange> SLICE_START_OR_END_MATCH_CHANGE = (slice, change) ->
            TIME_MATCH.test(slice.getStart(), change.getStart()) ||
                    TIME_MATCH.test(slice.getFinish(), change.getFinish());
    static final BiPredicate<DateRange, DateRange> SLICE_IN_CHANGE = (slice, change) ->
            change.getStart().isBefore(slice.getStart()) && change.getFinish().isAfter(slice.getFinish());
    static final Predicate<DateRange> DEACTIVATE_SEGMENT =
            changeRange -> changeRange != null && changeRange.getStart() == null && changeRange.getFinish() == null;
    private static final BiPredicate<DateRange, Instant> FINISHES_IN_SEGMENT = (segment, time) ->
            time.isAfter(segment.getStart()) && time.compareTo(segment.getFinish()) <= 0;
    /**
     * A slice is in the segment if one of the conditions below apply.
     *
     * <p>Slice start is in segment range: slice start date &gt;= segment start date AND
     * slice start date &lt; segment end date
     *
     * <p>Slice end date is in segment range: slice end date &gt; segment start date AND slice end date &lt;= segment
     * end date
     */
    static final BiPredicate<DateRange, DateRange> SLICE_IN_SEGMENT = (slice, segment) ->
            DateRange.RANGE_CHECK.test(segment, slice.getStart()) ||
                    FINISHES_IN_SEGMENT.test(segment, slice.getFinish());

    static <C, D> void mergeAndCreateNewChanges(final BaseBuilder<C, D> builder,
                                                final List<D> createdSegments,
                                                final List<C> changes) {
        // merge any segments created that can be considered to be identical - properties are the
        // same and the end date of one segment is the start date of another
        final List<D> merged = new ArrayList<>(1);
        for (final D created : createdSegments) {
            if (merged.isEmpty()) {
                merged.add(created);
                continue;
            }
            final int indexOfLastMerged = merged.size() - 1;
            final D latest = merged.get(indexOfLastMerged);

            if (builder.segmentsMatch(latest, created)) {
                merged.remove(indexOfLastMerged);
                merged.add(builder.getMergedSegment(builder, latest, created));
            } else {
                merged.add(created);
            }
        }

        merged.forEach(segment -> changes.add(builder.getNewChange(segment)));
    }

    static <C, D> boolean isChangeRequired(final BaseBuilder<C, D> builder,
                                           final DateRange change,
                                           final DateRange slice,
                                           final D segment) {
        return SLICE_START_OR_END_MATCH_CHANGE.test(slice, change) || SLICE_IN_CHANGE.test(slice, change) ||
                isExistingSegmentModified(builder, change, slice, segment);
    }

    private static <C, D> boolean isExistingSegmentModified(final BaseBuilder<C, D> builder,
                                                            final DateRange change,
                                                            final DateRange slice,
                                                            final D segment) {
        return isSliceAdjacentChange(change.getStart(), slice.getFinish(), builder.getFinish(segment)) ||
                isSliceAdjacentChange(change.getFinish(), slice.getStart(), builder.getStart(segment));
    }

    private static boolean isSliceAdjacentChange(final Instant changeTime,
                                                 final Instant sliceTime,
                                                 final Instant segmentTime) {
        return TIME_MATCH.test(changeTime, sliceTime) && !TIME_MATCH.test(sliceTime, segmentTime);
    }

    /**
     * Wraps the existing segment which is being replaced in an original change as the {@link Type#NEW}.
     *
     * @param segment the segment being replaced
     * @return the new change
     */
    abstract C getNewChange(final D segment);

    /**
     * Check whether the latest segment can be merged with the created segment.
     *
     * @param latestSegment  the latest merged segment
     * @param createdSegment the current segment
     * @return the new segment
     */
    abstract boolean segmentsMatch(final D latestSegment, final D createdSegment);

    /**
     * Merges the latest segment with the current created segment.
     *
     * @param builder        the builder
     * @param latestSegment  the latest segment that was merged
     * @param createdSegment the new segment
     * @return the merged segment
     */
    abstract D getMergedSegment(final BaseBuilder<C, D> builder, final D latestSegment, final D createdSegment);

    abstract Instant getStart(final D segment);

    abstract Instant getFinish(final D segment);
}
