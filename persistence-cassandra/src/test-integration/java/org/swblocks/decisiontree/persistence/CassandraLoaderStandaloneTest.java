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

package org.swblocks.decisiontree.persistence;

import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.swblocks.decisiontree.change.domain.Audit;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.ChangeSet;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.change.domain.builder.ChangeBuilder;
import org.swblocks.decisiontree.change.domain.builder.RuleChangeBuilder;
import org.swblocks.decisiontree.change.domain.builder.ValueGroupChangeBuilder;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSetMatcher;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;
import org.swblocks.jbl.util.DateRange;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Integration Test class for {@link CassandraLoader}.
 * This uses the embedded Cassandra server to run real tests against an instance.
 */
public class CassandraLoaderStandaloneTest {
    private static final String RULE_SET_NAME = "commissions";
    private static final String CQL_RESOURCE = "decisionTree.cql";
    private static final Instant NOW = Instant.now();

    static {
        EhSupport.propagate(() -> {
            System.setProperty("cassandra.unsafesystem", "true");
            System.setProperty("com.datastax.driver.USE_NATIVE_CLOCK", "false");
            EmbeddedCassandraServerHelper.startEmbeddedCassandra("dt-cassandra.yaml", 1200000L);
        });
    }

    private CassandraLoader cassandraLoader;
    private Session session;
    private Cluster cluster;

    @Before
    public void before() {
        this.cluster = EmbeddedCassandraServerHelper.getCluster();
        this.session = EmbeddedCassandraServerHelper.getSession();
    }

    @After
    public void after() {
        assertNotNull(this.cassandraLoader);
        this.cassandraLoader.stop();
        this.cassandraLoader = null;
    }

    @Test
    public void testSavingAndLoadingDecisionTree() {
        final CQLDataLoader dataLoader = new CQLDataLoader(this.session);
        dataLoader.load(new ClassPathCQLDataSet(CQL_RESOURCE, "loadSavedecisiontree"));

        this.cassandraLoader = CassandraLoader.instanceOf(this.cluster, "loadSavedecisiontree", RULE_SET_NAME);
        final DecisionTreeRuleSet commissions = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        assertNotNull(commissions);

        this.cassandraLoader.put(commissions);
        final Result<DecisionTreeRuleSet> decisionTreeRuleSetResult = this.cassandraLoader.get();
        assertTrue(decisionTreeRuleSetResult.isSuccess());
        final DecisionTreeRuleSet decisionTreeRuleSet = decisionTreeRuleSetResult.getData();
        assertNotNull(decisionTreeRuleSet);
        assertNotNull(decisionTreeRuleSet.getRules());
        assertThat(decisionTreeRuleSet, DecisionTreeRuleSetMatcher.isSame(commissions));
    }

    @Test
    public void testNoRuleSetInKeySpace() {
        // Try to load a missing ruleset and confirm it reports a failure.
        final CQLDataLoader dataLoader = new CQLDataLoader(this.session);
        dataLoader.load(new ClassPathCQLDataSet(CQL_RESOURCE, "not_a_ruleset"));
        this.cassandraLoader = CassandraLoader.instanceOf(this.cluster, "not_a_ruleset", "NOT_A_RULESET");

        final Result<DecisionTreeRuleSet> results = this.cassandraLoader.get();
        assertFalse(results.isSuccess());
        assertFalse(this.cassandraLoader.test(results));
    }

    @Test
    public void testPersistingChangeSets() {
        final CQLDataLoader dataLoader = new CQLDataLoader(this.session);
        dataLoader.load(new ClassPathCQLDataSet(CQL_RESOURCE, "decisiontreechange"));

        this.cassandraLoader = CassandraLoader.instanceOf(this.cluster, "decisiontreechange", RULE_SET_NAME);

        final DecisionTreeRuleSet commissions = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        assertNotNull(commissions);

        // Find default UK rule, id 5 and increase output rate to 1.3
        final DecisionTreeRule ukRule = commissions.getRules().get(new UUID(0, 5));
        assertNotNull(ukRule);

        // change is created for the date range of the UK rule - this is the change period
        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(commissions);
        builder.with(ChangeBuilder::changeRange, new DateRange(ukRule.getStart(), ukRule.getEnd()));
        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        builder.with(ChangeBuilder::activation, NOW);

        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(ukRule.getRuleCode())
                .with(RuleChangeBuilder::output, Collections.singletonList("Rate:1.3")));

        final ChangeSet change = new ChangeSet(UUID.randomUUID(), "updateUKRate",
                Collections.singleton(builder.build()));

        this.cassandraLoader.put(change);

        final Result<ChangeSet> loadedChange = this.cassandraLoader.getChange("updateUKRate");
        assertNotNull(loadedChange);
        if (!loadedChange.isSuccess()) {
            assertTrue(loadedChange.getException().getMessage(), loadedChange.isSuccess());
        }
    }

    @Test
    public void persistingChangeSetsWithValueGroups() {
        final CQLDataLoader dataLoader = new CQLDataLoader(this.session);
        dataLoader.load(new ClassPathCQLDataSet(CQL_RESOURCE, "decisiontreechangegroup"));

        this.cassandraLoader = CassandraLoader.instanceOf(this.cluster, "decisiontreechangegroup", RULE_SET_NAME);
        final DecisionTreeRuleSet commissions = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        assertNotNull(commissions);
        final DecisionTreeRuleSet ruleSet = commissions;
        this.cassandraLoader.put(ruleSet);

        final Set<ValueGroup> current = ruleSet.getValueGroups();
        final Optional<ValueGroup> matching = current.stream().filter(valueGroup ->
                valueGroup.getId().equals(new UUID(0, 1))).findFirst();

        assertTrue(matching.isPresent());

        final ValueGroup group = matching.get();
        final Instant end = group.getRange().getFinish();
        final DateRange changeRange = new DateRange(NOW.minus(Period.ofWeeks(20)), NOW.plus(Period.ofWeeks(20)));
        final List<String> drivers = Arrays.asList("CME", "KCBOT");

        // Change the value groups
        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator(group.getName());
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, ruleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::drivers, drivers);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeRange, changeRange);

        // persist the change...
        final Builder<ChangeBuilder, Change> changeBuilder = ChangeBuilder.creator(ruleSet);
        changeBuilder.with(ChangeBuilder::changeRange, changeRange);
        changeBuilder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        changeBuilder.with(ChangeBuilder::activation, NOW);
        changeBuilder.with(ChangeBuilder::valueGroupChange, valueGroupChangeBuilder);

        final ChangeSet changeSet = new ChangeSet(UUID.randomUUID(), "updateValueGroups",
                Collections.singleton(changeBuilder.build()));
        this.cassandraLoader.put(changeSet);

        final Result<ChangeSet> loadedChange = this.cassandraLoader.getChange("updateValueGroups");
        assertNotNull(loadedChange);
        if (!loadedChange.isSuccess()) {
            assertTrue(loadedChange.getException().getMessage(), loadedChange.isSuccess());
        }
    }

    @Test
    public void persistsChangeSetWithNewValueGroup() {
        final CQLDataLoader dataLoader = new CQLDataLoader(this.session);
        dataLoader.load(new ClassPathCQLDataSet(CQL_RESOURCE, "decisiontreechangegroupnew"));

        this.cassandraLoader = CassandraLoader.instanceOf(this.cluster, "decisiontreechangegroupnew",
                "commissions_no_groups");
        final DecisionTreeRuleSet commissions = CommisionRuleSetSupplier.getCommisionRuleSet().build();
        this.cassandraLoader.put(commissions);

        final Set<ValueGroup> current = commissions.getValueGroups();
//        assertTrue(current.isEmpty());

        // Change the value groups
        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator("CMEGroup");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, commissions);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT"));
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeRange,
                new DateRange(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX));
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::driver, "EXCHANGE");
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleCodes, Collections.singletonList(new UUID(0, 1)));

        // persist change
        final Builder<ChangeBuilder, Change> changeBuilder = ChangeBuilder.creator(commissions);
        changeBuilder.with(ChangeBuilder::changeRange, new DateRange(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX));
        changeBuilder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        changeBuilder.with(ChangeBuilder::activation, NOW);
        changeBuilder.with(ChangeBuilder::valueGroupChange, valueGroupChangeBuilder);

        final ChangeSet changeSet = new ChangeSet(UUID.randomUUID(), "updateValueGroups",
                Collections.singleton(changeBuilder.build()));
        this.cassandraLoader.put(changeSet);

        final Result<ChangeSet> loadedChange = this.cassandraLoader.getChange("updateValueGroups");
        assertNotNull(loadedChange);
        if (!loadedChange.isSuccess()) {
            assertTrue(loadedChange.getException().getMessage(), loadedChange.isSuccess());
        }

        final Set<Change> changes = loadedChange.getData().getChanges();
        assertThat(changes, hasSize(1));

        final Optional<Change> change = changes.stream().findFirst();
        assertTrue(change.isPresent());
        assertThat(change.get().getRuleChanges(), hasSize(2));

        final Set<ValueGroupChange> groupChanges = change.get().getValueGroupChanges();
        assertThat(groupChanges, hasSize(2));

        final Optional<ValueGroupChange> groupChange = groupChanges.stream()
                .filter(groupChangeFilter -> groupChangeFilter.getType().equals(Type.NEW)).findFirst();
        assertTrue(groupChange.isPresent());
        assertEquals("EXCHANGE", groupChange.get().getValueGroup().getDriverName());
        assertThat(groupChange.get().getValueGroup().getRuleCodes(), contains(new UUID(0, 1)));
    }
}