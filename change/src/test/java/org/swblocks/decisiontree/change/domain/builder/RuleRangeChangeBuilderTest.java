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
import java.util.stream.Collectors;

import org.junit.Assert;
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
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.util.DateRange;
import junit.framework.TestCase;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Test class for {@link RuleChangeBuilder} - de-activation and amending range of an exising rule segment.
 */
public class RuleRangeChangeBuilderTest {
    private static final Instant NOW = Instant.now();

    private Builder<RuleChangeBuilder, List<RuleChange>> builder;
    private DecisionTreeRuleSet ruleSet;

    @Before
    public void setup() {
        createRuleSet();
        assertNotNull(this.ruleSet);
    }

    @Test(expected = IllegalStateException.class)
    public void noRuleFoundForRuleIdWhenNull() {
        createBuilder(null, new DateRange(null, null));
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void noRuleFoundForRuleId() {
        createBuilder(new UUID(0, 10), new DateRange(null, null));
        this.builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void noRangeSupplied() {
        createBuilder(new UUID(0, 1), null);
        this.builder.build();
    }

    @Test
    public void deActivatesFirstRuleSegment() {
        createBuilder(new UUID(0, 1), new DateRange(null, null));
        final List<RuleChange> changes = this.builder.build();
        assertThat(changes, hasSize(1));

        final RuleChange change = changes.get(0);
        TestCase.assertEquals(Type.ORIGINAL, change.getType());
        TestCase.assertEquals(new UUID(0, 1), change.getRule().getRuleIdentifier());
    }

    @Test
    public void amendsStartDateOfFirstSegment() {
        createBuilder(new UUID(0, 1), new DateRange(NOW.plus(Period.ofWeeks(1)), null));
        final List<RuleChange> changes = this.builder.build();
        assertThat(changes, hasSize(2));

        final List<RuleChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)).minusMillis(1L));

        final List<RuleChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(4)).minusMillis(1L));

        Assert.assertNotEquals(originals.get(0).getRule().getRuleIdentifier(),
                newChanges.get(0).getRule().getRuleIdentifier());
    }

    @Test
    public void amendsEndDateOfFirstSegmentToFinishBeforeSecondSegmentStarts() {
        createBuilder(new UUID(0, 1), new DateRange(null, NOW.plus(Period.ofWeeks(3))));
        final List<RuleChange> changes = this.builder.build();
        assertThat(changes, hasSize(2));

        final List<RuleChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(1));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)).minusMillis(1L));

        final List<RuleChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(3)));
    }

    @Test
    public void amendsFirstSegmentToFinishAfterLastRuleSegment() {
        createBuilder(new UUID(0, 1), new DateRange(NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(9))));
        final List<RuleChange> changes = this.builder.build();
        assertThat(changes, hasSize(4));

        final List<RuleChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(3));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)).minusMillis(1L));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)).minusMillis(1L));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(9)));
    }

    @Test
    public void amendsFirstSegmentToFinishAfterLastRuleSegmentWithNewInputsAndOutputs() {
        createBuilder(new UUID(0, 1), new DateRange(NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(9))));
        this.builder.with(RuleChangeBuilder::input, Arrays.asList("VOICE", "CME", "ED", "APAC", "INDEX"));
        this.builder.with(RuleChangeBuilder::output, Collections.singletonMap("Rate", "2.0"));

        final List<RuleChange> changes = this.builder.build();
        assertThat(changes, hasSize(4));

        final List<RuleChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(3));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)).minusMillis(1L));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)).minusMillis(1L));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(1));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "APAC", "INDEX"}, Collections.singletonMap("Rate", "2.0"),
                NOW.plus(Period.ofWeeks(1)), NOW.plus(Period.ofWeeks(9)));
    }

    @Test
    public void amendsStartAndFinishOfSecondSegment() {
        createBuilder(new UUID(0, 2), new DateRange(NOW.plus(Period.ofWeeks(3)), NOW.plus(Period.ofWeeks(7))));
        final List<RuleChange> changes = this.builder.build();
        assertThat(changes, hasSize(6));

        final List<RuleChange> originals = getChangesByType(changes, Type.ORIGINAL);
        assertThat(originals, hasSize(3));
        originals.sort(Comparator.comparing(change -> change.getRule().getStart()));

        assertRuleChange(originals.get(0), Type.ORIGINAL, new UUID(0, 1), new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(4)).minusMillis(1L));

        assertRuleChange(originals.get(1), Type.ORIGINAL, new UUID(0, 2), new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(4)), NOW.plus(Period.ofWeeks(6)).minusMillis(1L));

        assertRuleChange(originals.get(2), Type.ORIGINAL, new UUID(0, 3), new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(6)), NOW.plus(Period.ofWeeks(8)));

        final List<RuleChange> newChanges = getChangesByType(changes, Type.NEW);
        assertThat(newChanges, hasSize(3));
        newChanges.sort(Comparator.comparing(change -> change.getRule().getStart()));

        assertRuleChange(newChanges.get(0), Type.NEW, null, new UUID(0, 2),
                new String[]{"VOICE", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.1"),
                NOW.plus(Period.ofWeeks(2)), NOW.plus(Period.ofWeeks(3)));

        assertRuleChange(newChanges.get(1), Type.NEW, null, new UUID(0, 2),
                new String[]{"EMAIL", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.2"),
                NOW.plus(Period.ofWeeks(3)), NOW.plus(Period.ofWeeks(7)));

        assertRuleChange(newChanges.get(2), Type.NEW, null, new UUID(0, 2),
                new String[]{"ELECTRONIC", "CME", "ED", "US", "INDEX"}, Collections.singletonMap("Rate", "1.3"),
                NOW.plus(Period.ofWeeks(7)), NOW.plus(Period.ofWeeks(8)));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotAmendSecondSegmentWhenNewValueGroupBoundaryExceeded() {
        createRuleSetWithValueGroups();
        createBuilder(new UUID(0, 2), new DateRange(NOW.plus(Period.ofWeeks(3)), NOW.plus(Period.ofWeeks(7))));
        this.builder.build();
    }

    private List<RuleChange> getChangesByType(final List<RuleChange> changes, final Type type) {
        return changes.stream().filter(change -> change.getType() == type)
                .sorted(Comparator.comparing(change2 -> change2.getRule().getStart()))
                .collect(Collectors.toList());
    }

    private void assertRuleChange(final RuleChange change,
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
            TestCase.assertNotNull(rule.getRuleIdentifier());
        } else {
            assertEquals(id, rule.getRuleIdentifier());
        }

        assertEquals(code, rule.getRuleCode());

        final List<String> ruleDrivers = new ArrayList<>(1);
        Arrays.stream(rule.getDrivers()).forEach(inputDriver -> ruleDrivers.add(inputDriver.toString()));
        assertThat(ruleDrivers, contains(drivers));

        assertEquals(outputs, rule.getOutputs());
        assertEquals(start, rule.getStart());
        assertEquals(finish, rule.getEnd());
    }

    private void createBuilder(final UUID id, final DateRange range) {
        this.builder = RuleChangeBuilder.creator(this.ruleSet, new UUID(0, 2));
        this.builder.with(RuleChangeBuilder::changeRange, range);
        this.builder.with(RuleChangeBuilder::ruleId, id);
    }

    private void createRuleSet() {
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
    }

    private void createRuleSetWithValueGroups() {
        final DriverCache cache = new DriverCache();
        final Set<ValueGroup> groups = new HashSet<>(2);

        final UUID groupOneId = new UUID(0, 8);
        final UUID groupTwoId = new UUID(0, 9);
        final String groupName = "CMEGroup";

        final ValueGroup groupOne = new ValueGroup(groupOneId, groupName, Arrays.asList("CME", "CBOT"),
                new DateRange(NOW, NOW.plus(Period.ofWeeks(6))));
        groups.add(groupOne);
        final ValueGroup groupTwo = new ValueGroup(groupTwoId, groupName, Arrays.asList("CME", "NYMEX"),
                new DateRange(NOW.plus(Period.ofWeeks(6)), DecisionTreeRule.MAX));
        groups.add(groupTwo);

        RuleSetBuilder.addValueGroupsToDriverCache(cache, groups);

        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"));
        ruleSetBuilder.with(RuleSetBuilder::groups, groups);
        ruleSetBuilder.with(RuleSetBuilder::setCache, cache);

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::cache, cache)
                .with(RuleBuilder::setId, new UUID(0, 1))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input,
                        Arrays.asList("VOICE", GroupDriver.VG_PREFIX + groupOne.getId(), "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.1"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(2)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(4)).minusMillis(1L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::cache, cache)
                .with(RuleBuilder::setId, new UUID(0, 2))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input,
                        Arrays.asList("EMAIL", GroupDriver.VG_PREFIX + groupOne.getId(), "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.2"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(4)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(6)).minusMillis(1L)));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::cache, cache)
                .with(RuleBuilder::setId, new UUID(0, 3))
                .with(RuleBuilder::setCode, new UUID(0, 2))
                .with(RuleBuilder::input,
                        Arrays.asList("ELECTRONIC", GroupDriver.VG_PREFIX + groupTwo.getId(), "ED", "US", "INDEX"))
                .with(RuleBuilder::output, Collections.singletonMap("Rate", "1.3"))
                .with(RuleBuilder::start, NOW.plus(Period.ofWeeks(6)))
                .with(RuleBuilder::end, NOW.plus(Period.ofWeeks(8))));

        this.ruleSet = ruleSetBuilder.build();
    }
}
