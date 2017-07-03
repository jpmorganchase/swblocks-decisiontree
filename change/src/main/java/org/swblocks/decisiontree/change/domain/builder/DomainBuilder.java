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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;

/**
 * Abstract class that is used by {@link RuleChangeBuilder} and {@link ValueGroupChangeBuilder} to build the original
 * and new items that form the change.
 *
 * @param <C> the change object type
 * @param <D> the domain object type
 */
abstract class DomainBuilder<C, D> extends BaseBuilder<C, D> {
    /**
     * Default changeRange range for a change - from now until our maximum time.
     */
    private static final DateRange DEFAULT = new DateRange(Instant.now(), DecisionTreeRule.MAX);
    UUID id;
    DateRange range;

    /**
     * Static method that builds the changes.
     *
     * <p>This method checks if a domain object id has been specified to determine if the change applies to an
     * individual segment. If not, an overall change is applied based on the change range and the inputs and outputs
     * supplied to the builder.
     *
     * <p>First, a list of {@link DateRange} objects are created from the start and end times of the segments as will
     * as the start and end time of the change. Then for each date range, the corresponding segment is found.
     *
     * <p>If no segment is found but the date range is within the change period, a new segment is created. If a segment
     * is found and the date range is in the change period, the existing segment will be marked for removal. New
     * segments are created that reflect the change. Where a segment is on the change boundary, new segments will be
     * created based on the existing segment data.
     *
     * @param builder  the builder
     * @param segments the list of segments for the domain object
     * @param <C>      the type of change object that will be returned
     * @param <D>      the type of domain object that is being used to generate the change for
     * @return the list of changes
     */
    static <C, D> List<C> builds(final DomainBuilder<C, D> builder,
                                 final List<D> segments) {
        final List<C> changes = new ArrayList<>(1);

        if (builder.id != null) {
            EhSupport.ensure(builder.range != null, "No range supplied to alter segment for id %s ", builder.id);

            final Optional<D> amended = builder.getSegment(segments, builder.id);
            EhSupport.ensure(amended.isPresent(), "No segment found for id %s", builder.id);
            final D segment = amended.get();

            if (DEACTIVATE_SEGMENT.test(builder.range)) {
                return Collections.singletonList(builder.getOriginalChange(segment));
            }

            builder.range =
                    getEffectiveChange(builder.range, builder.getStart(segment), builder.getFinish(segment));
            builder.amendInputsAndOutputs(builder, segment);
            segments.remove(segment);
            changes.add(builder.getOriginalChange(segment));
        }

        builder.checkInputsAndOutputs(builder);

        if (builder.range == null) {
            builder.range = DEFAULT;
        }

        buildChanges(builder, segments, changes);
        return changes;
    }

    private static <C, D> void buildChanges(final DomainBuilder<C, D> builder,
                                            final List<D> segments,
                                            final List<C> changes) {
        final DateRange change = builder.range;
        EhSupport.ensure(change.getStart().isBefore(change.getFinish()),
                "Start of change %s is not before end of change %s", change.getStart(), change.getFinish());

        final List<DateRange> slices = getSegmentDateRanges(builder, segments, change);
        final Set<D> removedSegments = new HashSet<>();
        final List<D> createdSegments = new ArrayList<>(1);

        // iterate over slices - add correct properties if there is a change involved
        for (final DateRange slice : slices) {
            final Instant start = slice.getStart();
            final Instant end = slice.getFinish();

            final Optional<D> matching = segments.stream().filter(segment -> SLICE_IN_SEGMENT
                    .test(slice, new DateRange(builder.getStart(segment), builder.getFinish(segment)))).findFirst();

            final D segment = matching.isPresent() ? matching.get() : null;
            // we have no segment but the slice is in the change as start or end match times match
            if (segment == null && (SLICE_START_OR_END_MATCH_CHANGE.test(slice, change) ||
                    SLICE_IN_CHANGE.test(slice, change))) {
                // create a new segment with the change data - all data must be present as there is no segment
                createdSegments.add(builder.getNewSegmentFromChangeInput(builder, start, end));
            } else if (segment != null && isChangeRequired(builder, change, slice, segment)) {
                // New segments are required if:
                // 1. slice start or end dates match the change start or end dates OR
                // 2. slice start date is after change start date and slice end date is before the change end date OR
                // 3. (change starts at slice end date (slice end date is not segment end date OR
                //      change finishes and slice start date (slice start date is not segment start date)

                // segment will be removed
                removedSegments.add(segment);
                // "Split up" existing segment
                if (TIME_MATCH.test(change.getStart(), slice.getFinish()) ||
                        TIME_MATCH.test(change.getFinish(), slice.getStart())) {
                    // new inputs and outputs derived from the existing segment
                    createdSegments.add(builder.getNewSegmentBasedOnExisting(builder, start, end, segment));
                } else {
                    // new  segment with data from change if supplied - otherwise defaults to segment data
                    createdSegments.add(builder.getNewSegmentBasedOnChange(builder, start, end, segment));
                }
            }
        }

        removedSegments.forEach(segment -> changes.add(builder.getOriginalChange(segment)));
        mergeAndCreateNewChanges(builder, createdSegments, changes);
    }

    private static <C, D> List<DateRange> getSegmentDateRanges(final DomainBuilder<C, D> builder,
                                                               final List<D> segments,
                                                               final DateRange change) {
        // add all distinct start and end times for segments
        final Set<Instant> times = new TreeSet<>();
        segments.forEach(segment -> {
            times.add(builder.getStart(segment));
            times.add(builder.getFinish(segment));
        });
        // times that the change applies
        times.add(change.getStart());
        times.add(change.getFinish());

        // construct a list of date range objects for the times
        final List<Instant> orderedTimes = new ArrayList<>(times);
        final List<DateRange> slices = new ArrayList<>(1);
        for (int i = 0; i < times.size() - 1; ++i) {
            final DateRange dateTimeSlice = new DateRange(orderedTimes.get(i), orderedTimes.get(i + 1));
            slices.add(dateTimeSlice);
        }
        return slices;
    }

    private static DateRange getEffectiveChange(final DateRange range,
                                                final Instant domainStart,
                                                final Instant domainFinish) {
        final Instant start = range.getStart();
        final Instant finish = range.getFinish();

        final DateRange effective;

        if (start == null) {
            effective = new DateRange(domainStart, finish);
        } else if (finish == null) {
            effective = new DateRange(start, domainFinish);
        } else {
            effective = range;
        }

        return effective;
    }

    /**
     * Get the segment for the specified id.
     *
     * @param segments the list of domain segments
     * @param id       the id of the segment
     * @return the segment
     */
    abstract Optional<D> getSegment(final List<D> segments, final UUID id);

    /**
     * Creates the domain object based on the the change input first, then the domain object data.
     *
     * @param builder the builder
     * @param start   the start date of the segment
     * @param end     the end date of the segment
     * @param segment the current domain object
     * @return the new domain segment
     */
    abstract D getNewSegmentBasedOnChange(final DomainBuilder<C, D> builder,
                                          final Instant start,
                                          final Instant end,
                                          final D segment);

    /**
     * Creates a new segment based on the existing segmeent.
     *
     * @param builder the builder
     * @param start   the start date of the segment
     * @param end     the end date of the segment
     * @param segment the current segment
     * @return the new segment
     */
    abstract D getNewSegmentBasedOnExisting(final DomainBuilder<C, D> builder,
                                            final Instant start,
                                            final Instant end,
                                            final D segment);

    /**
     * Method to create a new segment based on the change input only.
     *
     * @param builder the builder
     * @param start   the start date of the domain object
     * @param end     the end date of the domain object
     * @return the new segment
     */
    abstract D getNewSegmentFromChangeInput(final DomainBuilder<C, D> builder,
                                            final Instant start,
                                            final Instant end);

    /**
     * Wraps the existing segment which is being replaced in an original change as the {@link Type#ORIGINAL}.
     *
     * @param segment the segment being replaced
     * @return the original change
     */
    abstract C getOriginalChange(final D segment);

    /**
     * Amend the data in the change if required using the segment.
     *
     * @param builder the builder
     * @param segment the domain segment
     */
    abstract void amendInputsAndOutputs(final DomainBuilder<C, D> builder, final D segment);

    /**
     * Validate that the change data is present.
     *
     * @param builder the builder
     */
    abstract void checkInputsAndOutputs(final DomainBuilder<C, D> builder);
}
