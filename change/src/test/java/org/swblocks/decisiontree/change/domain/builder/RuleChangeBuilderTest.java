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
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 * Test class for {@link RuleChangeBuilder}.
 */
public class RuleChangeBuilderTest {
    private static final UUID GROUP_ID = new UUID(0, 8);
    private static final Instant NOW = Instant.now();

    private Builder<RuleChangeBuilder, List<RuleChange>> builder;
    private DecisionTreeRuleSet ruleSet;

    static List<RuleChange> getChangesByType(final List<RuleChange> changes, final Type type) {
        return changes.stream().filter(change -> change.getType() == type)
                .sorted(Comparator.comparing(change2 -> change2.getRule().getStart()))
                .collect(Collectors.toList());
    }

    static void assertRuleChange(final RuleChange change,
                                 final Type type,
                                 final UUID id,
                                 final UUID code,
                                 final String[] drivers,
                                 final Map<String, String> outputs,
                                 final Instant start,
                                 final Instant finish) {
        assertEquals(type, change.getType());

        final DecisionTreeRule rule = change.getRule();

        if (id == null) {
            assertNotNull(rule.getRuleIdentifier());
        } else {
            assertEquals(id, rule.getRuleIdentifier());
        }

        assertEquals(code, rule.getRuleCode());

        final List<String> ruleDrivers = new ArrayList<>(1);
        Arrays.stream(rule.getDrivers()).forEach(inputDriver -> ruleDrivers.add(inputDriver.toString()));
        assertThat(ruleDrivers, contains(drivers));

        assertEquals(outputs, rule.getOutputs());
        if (start != null) {
            assertEquals(start, rule.getStart());
        } else {
            assertNotNull(rule.getStart());
        }
        assertEquals(finish, rule.getEnd());
    }

    @Before
    public void setup() {
        this.ruleSet = getRuleSetBuilder().build();
        this.builder = RuleChangeBuilder.creator(new UUID(0, 2))
                .with(RuleChangeBuilder::ruleSet, this.ruleSet);
    }

    @Test(expected = IllegalStateException.class)
    public void noDriversOrOutputs() {
        this.builder.build();
    }

    @Test
    public void createsNewRule() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = getRuleSetBuilder();

        final DriverCache cache = new DriverCache();
        final Set<ValueGroup> groups = ConcurrentHashMap.newKeySet();
        final ValueGroup group = new ValueGroup("CMEGroup", Arrays.asList("CME", "CBOT"));
        groups.add(group);
        RuleSetBuilder.addValueGroupsToDriverCache(cache, groups);

        ruleSetBuilder.with(RuleSetBuilder::setCache, cache);
        ruleSetBuilder.with(RuleSetBuilder::groups, groups);
        this.ruleSet = ruleSetBuilder.build();

        this.builder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 9));
        this.builder.with(RuleChangeBuilder::input,
                Arrays.asList("VOICE", "CME", "ED", "UK", GroupDriver.VG_PREFIX + group.getId()));
        this.builder.with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.0"));
        this.builder.with(RuleChangeBuilder::changeRange,
                new DateRange(NOW.minus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(1))));
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(1));

        final RuleChange change = ruleChanges.get(0);
        assertEquals(Type.NEW, change.getType());

        assertRuleChange(change, Type.NEW, null, new UUID(0, 9),
                new String[]{"VOICE", "CME", "ED", "UK", GroupDriver.VG_PREFIX + group.getId().toString()},
                Collections.singletonMap("Rate", "2.0"), NOW.minus(Period.ofWeeks(1)),
                NOW.plus(Period.ofWeeks(1)));

        final List<String> driverValues =
                Arrays.stream(change.getRule().getDrivers()).map(InputDriver::toString).collect(Collectors.toList());
        assertThat(driverValues, contains("VOICE", "CME", "ED", "UK", GroupDriver.VG_PREFIX + group.getId()));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreateNewRuleWithoutDrivers() {
        this.ruleSet = getRuleSetBuilder().build();

        this.builder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 9));
        this.builder.with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.0"));
        this.builder.with(RuleChangeBuilder::changeRange,
                new DateRange(NOW.minus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(1))));
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreateNewRuleWithoutOutputs() {
        this.ruleSet = getRuleSetBuilder().build();

        this.builder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 9));
        this.builder.with(RuleChangeBuilder::input, Arrays.asList("VOICE", "CME", "ED", "UK", "INDEX"));
        this.builder.with(RuleChangeBuilder::changeRange,
                new DateRange(NOW.minus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(1))));
        this.builder.build();
    }

    @Test
    public void createsNewChangeFinishingBeforeFirstRuleSegmentStarts() {
        addChange(NOW, NOW.plus(Period.ofWeeks(1)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(1));

        final RuleChange change = ruleChanges.get(0);

        assertRuleChange(change, Type.NEW, null, new UUID(0, 2), new String[]{"VOICE", "CME", "ED", "UK", "INDEX"},
                Collections.singletonMap("Rate", "2.0"), NOW, NOW.plus(Period.ofWeeks(1)));
    }

    @Test
    public void createsNewChangeFinishingOnFirstRuleSegmentStart() {
        addChange(NOW, NOW.plus(Period.ofWeeks(2)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(1));

        final RuleChange change = ruleChanges.get(0);

        assertRuleChange(change, Type.NEW, null, new UUID(0, 2), new String[]{"VOICE", "CME", "ED", "UK", "INDEX"},
                Collections.singletonMap("Rate", "2.0"), NOW, NOW.plus(Period.ofWeeks(2)));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreateNewChangeBeforeFirstRuleSegmentWithoutDrivers() {
        addChange(NOW, NOW.plus(Period.ofWeeks(1)), false, true);
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCreateNewChangeBeforeFirstRuleSegmentWithoutOutputs() {
        addChange(NOW, NOW.plus(Period.ofWeeks(1)), true, false);
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void failsWhenDatesNotInChronologicalOrder() {
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(1)), true, false);
        this.builder.build();
    }

    @Test
    public void createsChangeUsingDefaultPeriod() {
        addChange(null, NOW.plus(Period.ofWeeks(1)), true, true);

        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(4));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(3));

        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                null, DecisionTreeRule.MAX);
    }

    private void assertOriginalSegmentsRemoved(final List<RuleChange> originals) {
        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void createChangeStartingBeforeAndFinishingInFirstSegment() {
        addChange(NOW, NOW.plus(Period.ofWeeks(3)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(3)));
        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(3)), NOW.plus(Period.ofWeeks(4)));

        assertNotEquals(originals.get(0).getRule().getRuleIdentifier(),
                newChanges.get(0).getRule().getRuleIdentifier());
        assertNotEquals(newChanges.get(0).getRule().getRuleIdentifier(),
                newChanges.get(1).getRule().getRuleIdentifier());
    }

    @Test
    public void createChangeStartingBeforeAndFinishingAtEndOfFirstSegment() {
        addChange(NOW, NOW.plus(Period.ofWeeks(4)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(2));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(4)));

        assertNotEquals(originals.get(0).getRule().getRuleIdentifier(),
                newChanges.get(0).getRule().getRuleIdentifier());
    }

    @Test
    public void createChangeStartingBeforeAndFinishingAtEndOfFirstSegmentWithDiscreteGaps() {
        createBuilderWithDiscreteGapsBetweenSegments();

        addChange(NOW, NOW.plus(Period.ofWeeks(4)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(2));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)).minusMillis(1L));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(4)));

        assertNotEquals(originals.get(0).getRule().getRuleIdentifier(),
                newChanges.get(0).getRule().getRuleIdentifier());
    }

    @Test
    public void createChangeStartingBeforeFirstSegmentAndFinishingInSecondSegment() {
        addChange(NOW, NOW.plus(Period.ofWeeks(5)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(4));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(2));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));

        assertNotEquals(originals.get(1).getRule().getRuleIdentifier(),
                newChanges.get(1).getRule().getRuleIdentifier());
    }

    @Test
    public void createChangeStartingBeforeFirstSegmentAndFinishingAtEndOfSecondSegment() {
        addChange(NOW, NOW.plus(Period.ofWeeks(6)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(2));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(6)));
    }

    @Test
    public void createChangeStartingBeforeFirstSegmentAndFinishingInThirdSegment() {
        addChange(NOW, NOW.plus(Period.ofWeeks(7)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(5));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(3));

        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void createChangeStartingBeforeFirstSegmentAndFinishingAtEndOfThirdSegment() {
        addChange(NOW, NOW.plus(Period.ofWeeks(8)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(4));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(3));

        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void createChangeStartingBeforeFirstSegmentAndFinishingAfterEndOfThirdSegment() {
        addChange(NOW, NOW.plus(Period.ofWeeks(9)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(4));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(3));

        assertOriginalSegmentsRemoved(originals);

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW, NOW.plus(Period.ofWeeks(9)));
    }

    @Test
    public void addsChangeInbetweenFirstAndThirdSegmentOnExactDates() {
        createBuilderWithSecondRuleSegmentOmitted();
        addChange(NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(7)), true, true);

        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(1));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(0));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(7)));
    }

    @Test
    public void addsChangeInbetweenFirstAndThirdSegment() {
        createBuilderWithSecondRuleSegmentOmitted();
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)), true, true);

        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(1));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotAddChangeInbetweenFirstAndThirdSegmentWithoutDrivers() {
        createBuilderWithSecondRuleSegmentOmitted();
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)), false, true);
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotAddChangeInbetweenFirstAndThirdSegmentWithoutOutputs() {
        createBuilderWithSecondRuleSegmentOmitted();
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)), true, false);
        this.builder.build();
    }

    // Starts and finishes within second rule segment
    @Test
    public void changeReplacesSecondSegment() {
        addChange(NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(2));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));
    }

    @Test
    public void changeReplacesSecondSegmentWhenSegmentsHaveGaps() {
        createBuilderWithDiscreteGapsBetweenSegments();

        addChange(NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)).minusMillis(1L), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(2));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)).minusMillis(1L));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)).minusMillis(1L));
    }

    @Test
    public void changeAddedToSecondSegmentAtStartAndFinishesWithinSecondSegment() {
        addChange(NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));
    }

    @Test
    public void changeAddedToSecondSegmentAtStartAndFinishesWithinSecondSegmentUsesExistingDrivers() {
        addChange(NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)), false, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));
    }

    @Test
    public void changeAddedToSecondSegmentAtStartAndFinishesWithinSecondSegmentUsesExistingOutputs() {
        addChange(NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)), true, false);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));
    }

    @Test
    public void changeAddedToSecondSegmentAfterStartAndFinishesWithinSecondSegment() {
        final Instant changeStart = NOW.plus(Period.ofWeeks(5).minusDays(2));
        final Instant changeEnd = NOW.plus(Period.ofWeeks(5).plusDays(2));
        addChange(changeStart, changeEnd, true, true);

        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(4));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(3));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), changeStart);

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                changeStart, changeEnd);

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                changeEnd, NOW.plus(Period.ofWeeks(6)));
    }

    @Test
    public void changeAddedToSecondSegmentAfterStartAndFinishesAtSecondSegmentEnd() {
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));
    }

    // Starts in second segment and finishes in next segment
    @Test
    public void changeStartsAtSecondSegmentStartAndFinishesBeforeThirdSegmentFinish() {
        addChange(NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(7)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(4));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(2));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void changeStartsAfterSecondSegmentStartAndFinishesBeforeThirdSegmentFinish() {
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(7)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(5));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(2));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(3));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void changeStartsAfterSecondSegmentStartAndFinishesBeforeThirdSegmentFinishNewOutputs() {
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(7)), false, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(6));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(2));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(4));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(3), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void changeStartsAfterSecondSegmentStartAndFinishesBeforeThirdSegmentFinishNewOutputsWithRuleGroups() {
        createRuleMapWithValueGroups();

        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(7)), false, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(6));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(2));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(4));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(3), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"},
                Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void changeStartsAfterSecondSegmentStartAndFinishesAtThirdSegmentFinish() {
        addChange(NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(8)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(4));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(2));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(5)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(5)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void changeStartsAtSecondSegmentFinishAndFinishesAtThirdSegmentFinish() {
        addChange(NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(2));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));
    }

    // Change added to third segment
    @Test
    public void changeAddedToThirdSegmentFinishingAtFinish() {
        addChange(NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test
    public void changeAddedToThirdSegmentFinishingAfterFinish() {
        addChange(NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(9)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(3));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(2));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(9)));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotAddChangeThirdSegmentFinishingAfterFinishWithoutDrivers() {
        addChange(NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(9)), false, true);
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotAddChangeThirdSegmentFinishingAfterFinishWithoutOutputs() {
        addChange(NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(9)), true, false);
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotAddChangeThirdSegmentFinishingAfterFinishWithoutDriversOrOutputs() {
        addChange(NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(9)), false, false);
        this.builder.build();
    }

    // Change added to end of last segment
    @Test
    public void addsChangeStartingAtExactEndOfThirdSegment() {
        this.builder.with(RuleChangeBuilder::input, Arrays.asList("VOICE", "CME", "ED", "UK", "INDEX"));
        this.builder.with(RuleChangeBuilder::output, Collections.singletonList("Rate:2.0"));
        this.builder.with(RuleChangeBuilder::changeRange,
                new DateRange(NOW.plus(Period.ofWeeks(8)), NOW.plus(Period.ofWeeks(9))));

        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(1));

        final List<RuleChange> originals = getChangesByType(ruleChanges, Type.ORIGINAL);
        assertThat(originals, hasSize(0));

        final List<RuleChange> newChanges = getChangesByType(ruleChanges, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "UK", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(8)), NOW.plus(Period.ofWeeks(9)));
    }

    @Test
    public void addsChangeStartingAfterEndOfThirdSegment() {
        addChange(NOW.plus(Period.ofWeeks(9)), NOW.plus(Period.ofWeeks(10)), true, true);
        final List<RuleChange> ruleChanges = this.builder.build();
        assertThat(ruleChanges, hasSize(1));

        final RuleChange change = ruleChanges.get(0);

        assertRuleChange(change, Type.NEW, null, new UUID(0, 2), new String[]{"VOICE", "CME", "ED", "UK", "INDEX"},
                Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(9)), NOW.plus(Period.ofWeeks(10)));

    }

    @Test(expected = IllegalStateException.class)
    public void cannotAddChangeToEndOfThirdSegmentWithoutDrivers() {
        addChange(NOW.plus(Period.ofWeeks(8)), NOW.plus(Period.ofWeeks(9)), false, true);
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotAddChangeToEndOfThirdSegmentWithoutOutputs() {
        addChange(NOW.plus(Period.ofWeeks(9)), NOW.plus(Period.ofWeeks(10)), true, false);
        this.builder.build();
    }

    private void addChange(final Instant start,
                           final Instant end,
                           final boolean withDrivers,
                           final boolean withOutputs) {
        if (withDrivers) {
            this.builder.with(RuleChangeBuilder::input, Arrays.asList("VOICE", "CME", "ED", "UK", "INDEX"));
        }
        if (withOutputs) {
            this.builder.with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.0"));
        }
        if (start != null) {
            this.builder.with(RuleChangeBuilder::changeRange, new DateRange(start, end));
        }
    }

    private Builder<RuleSetBuilder, DecisionTreeRuleSet> getRuleSetBuilder() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.1"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("EMAIL", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.2"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(6))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("ELECTRONIC", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 4))
                .with(RuleBuilder::setCode, new UUID(0, 3))
                .with(RuleBuilder::input, Arrays.asList("ELECTRONIC", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, NOW.minus(Period.ofWeeks(1)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(1))));

        return ruleSetBuilder;
    }

    private void createRuleMapWithValueGroups() {
        final String groupName = "CMEGroup";
        final Set<ValueGroup> groups = new HashSet<>(1);
        final ValueGroup group = new ValueGroup(GROUP_ID, groupName, Arrays.asList("CME", "CBOT"),
                new DateRange(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX));
        groups.add(group);

        final DriverCache cache = new DriverCache();
        RuleSetBuilder.addValueGroupsToDriverCache(cache, groups);

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        ruleSetBuilder.with(RuleSetBuilder::groups, groups);
        ruleSetBuilder.with(RuleSetBuilder::setCache, cache);

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("VOICE", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.1"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("EMAIL", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.2"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(6))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input,
                        Arrays.asList("ELECTRONIC", GroupDriver.VG_PREFIX + GROUP_ID, "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 4))
                .with(RuleBuilder::setCode, new UUID(0, 3))
                .with(RuleBuilder::input, Arrays.asList("ELECTRONIC", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, NOW.minus(Period.ofWeeks(1)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(1))));

        this.ruleSet = ruleSetBuilder.build();
        this.builder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2));
    }

    private void createBuilderWithSecondRuleSegmentOmitted() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.1"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4))));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("ELECTRONIC", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(7)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        this.ruleSet = ruleSetBuilder.build();
        this.builder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2));
    }

    private void createBuilderWithDiscreteGapsBetweenSegments() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.1"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4)).minusMillis(1L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("EMAIL", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.2"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(6)).minusMillis(1L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input, Arrays.asList("ELECTRONIC", "CME", "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        this.ruleSet = ruleSetBuilder.build();
        this.builder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2));
    }
}
