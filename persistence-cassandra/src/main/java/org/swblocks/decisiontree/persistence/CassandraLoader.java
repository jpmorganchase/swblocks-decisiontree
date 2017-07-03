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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.Loader;
import org.swblocks.decisiontree.Persister;
import org.swblocks.decisiontree.change.domain.Audit;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.ChangeSet;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.DomainSerialiser;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;
import org.swblocks.jbl.lifecycle.ComponentLifecycle;
import org.swblocks.jbl.util.DateRange;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;

/**
 * Persists a {@link DecisionTreeRuleSet} to and from Cassandra.
 */
public final class CassandraLoader implements Loader<DecisionTreeRuleSet>, Persister, ComponentLifecycle {
    static final String CQL_GET_RULES_FOR_RULESET = "select id, code, version, drivers, start, end, outputs " +
            "from active_rules_by_rulesetname where rulesetname = ?";
    static final String CQL_GET_RULESET_FROM_RULESETNAME = "select drivers from ruleset_details " +
            "where application = ? and rulesetname = ?";
    static final String CQL_GET_VALUE_GROUPS_FOR_RULESET = "select id, name, drivers, start, end " +
            "from active_groups_by_rulesetname where rulesetname = ?";
    static final String CQL_GET_ACTIVE_CHANGE = "select id, changeid, start, end, rulesetname, initiator, " +
            "initiatortime, approver, approvertime, activationtime, rulechangetype, ruleid, rulecode, " +
            "ruledrivers, ruleoutputs, rulestart, ruleend," +
            "vgchangetype, vgid, vgname, vgdrivers, vgstart, vgend, vgdrivername, vgrulecodes " +
            "from active_changesets where changesetname = ?";
    private static final String CQL_INSERT_RULESET =
            "INSERT INTO ruleset_details (application, rulesetname, drivers) VALUES (?, ?, ?)";
    private static final String CQL_INSERT_RULES = "INSERT INTO active_rules_by_rulesetname " +
            "(rulesetname, id, code, version, drivers, start, end, outputs) values (?, ?, ?, 1, ?, ?, ?, ?)";
    private static final String CQL_INSERT_GROUPS = "INSERT INTO active_groups_by_rulesetname" +
            "(rulesetname, id, name, drivers, start, end) values (?, ?, ?, ?, ?, ?)";
    private static final String CQL_INSERT_CHANGE = "INSERT INTO active_changesets " +
            "(changesetname, id, changeid, start, end, rulesetname, initiator, initiatortime, approver, approvertime," +
            "activationTime, " +
            "rulechangetype, ruleid, rulecode, ruledrivers, ruleoutputs, rulestart, ruleend, " +
            "vgchangetype, vgid, vgname, vgdrivers, vgstart, vgend, vgdrivername, vgrulecodes) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEFAULT_APPLICATION = "DEFAULT";
    private final String ruleSetName;
    private final String keyspace;
    private final Session session;

    private CassandraLoader(final Cluster cluster, final String keyspace, final String ruleSetName) {
        this.keyspace = keyspace;
        this.ruleSetName = ruleSetName;
        cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance);
        this.session = cluster.connect(this.keyspace);
    }

    /**
     * Creates an instance of {@link CassandraLoader} using the created Cassandra {@link Cluster}.
     *
     * @param cluster     Cassandra Cluster
     * @param keyspace    Keyspace within Cassandra
     * @param ruleSetName RuleSet to load
     * @return {@link CassandraLoader} for loading the ruleset.
     */
    public static CassandraLoader instanceOf(final Cluster cluster, final String keyspace, final String ruleSetName) {
        return new CassandraLoader(cluster, keyspace, ruleSetName);
    }

    /**
     * Loads a {@link DecisionTreeRuleSet} from Cassandra using the defined keyspace.
     *
     * @return {@link Result} indicating if the load succeeded, storing any exception if the load failed.
     */
    @Override
    public Result<DecisionTreeRuleSet> get() {
        try {
            ResultSet resultSet = this.session.execute(CQL_GET_RULESET_FROM_RULESETNAME,
                    "DEFAULT", this.ruleSetName);
            EhSupport.ensure(!resultSet.isExhausted(), "RuleSet %s does not exist in %s.", this.ruleSetName,
                    this.keyspace);

            final Row resultsRow = resultSet.one();
            final List<String> driverList = resultsRow.getList("drivers", String.class);
            final DriverCache driverCache = new DriverCache();

            resultSet = this.session.execute(CQL_GET_RULES_FOR_RULESET, this.ruleSetName);
            final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder = RuleSetBuilder.creator(driverList);
            ruleSetBuilder.with(RuleSetBuilder::setName, this.ruleSetName);
            ruleSetBuilder.with(RuleSetBuilder::groups, getValueGroupsForRuleSet(this.ruleSetName));
            ruleSetBuilder.with(RuleSetBuilder::setCache, driverCache);

            resultSet.iterator().forEachRemaining(row -> ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                    .with(RuleBuilder::cache, driverCache)
                    .with(RuleBuilder::setId, row.get("id", UUID.class))
                    .with(RuleBuilder::setCode, row.get("code", UUID.class))
                    .with(RuleBuilder::start, row.get("start", Instant.class))
                    .with(RuleBuilder::end, row.get("end", Instant.class))
                    .with(RuleBuilder::input, row.getList("drivers", String.class))
                    .with(RuleBuilder::output, row.getList("outputs", String.class))
            ));

            final DecisionTreeRuleSet loadedRuleSet = ruleSetBuilder.build();
            return Result.success(loadedRuleSet);
        } catch (final Exception exception) {
            return Result.failure(() -> exception);
        }
    }

    /**
     * Persists a {@link DecisionTreeRuleSet} into Cassandra.
     *
     * @param ruleset RuleSet to persist.
     */
    @Override
    public void put(final DecisionTreeRuleSet ruleset) {
        this.session.execute(CQL_INSERT_RULESET, DEFAULT_APPLICATION, ruleset.getName(), ruleset.getDriverNames());

        if (CollectionUtils.isNotEmpty(ruleset.getValueGroups())) {
            final PreparedStatement prepared = this.session.prepare(CQL_INSERT_GROUPS);

            ruleset.getValueGroups().forEach(group -> {
                final BoundStatement bound = prepared.bind()
                        .setString(0, ruleset.getName())
                        .setUUID(1, group.getId())
                        .setString(2, group.getName())
                        .setList(3, group.getValues())
                        .set(4, group.getRange().getStart(), InstantCodec.instance)
                        .set(5, group.getRange().getFinish(), InstantCodec.instance);
                this.session.execute(bound);
            });
        }

        final PreparedStatement prepared = this.session.prepare(CQL_INSERT_RULES);
        for (final DecisionTreeRule rule : ruleset.getRules().values()) {
            final BoundStatement bound = prepared.bind()
                    .setString(0, ruleset.getName())
                    .setUUID(1, rule.getRuleIdentifier())
                    .setUUID(2, rule.getRuleCode())
                    .setList(3, DomainSerialiser.convertDrivers(rule.getDrivers()))
                    .set(4, rule.getStart(), InstantCodec.instance)
                    .set(5, rule.getEnd(), InstantCodec.instance)
                    .setList(6, DomainSerialiser.convertOutputs(rule.getOutputs()));
            this.session.execute(bound);
        }
    }

    public void put(final ChangeSet changeSet) {
        final PreparedStatement insertChange = this.session.prepare(CQL_INSERT_CHANGE);
        for (final Change change : changeSet.getChanges()) {
            for (final RuleChange ruleChange : change.getRuleChanges()) {
                final BoundStatement boundStatement = insertChange.bind()
                        .setString(0, changeSet.getName())
                        .setUUID(1, changeSet.getId())
                        .setUUID(2, change.getId())
                        .set(3, change.getChangeRange().getStart(), InstantCodec.instance)
                        .set(4, change.getChangeRange().getFinish(), InstantCodec.instance)
                        .setString(5, change.getRuleSetName())
                        .setString(6, change.getAudit().getInitiator())
                        .set(7, change.getAudit().getInitiatorTime(), InstantCodec.instance)
                        .setString(8, change.getAudit().getAuthoriser())
                        .set(9, change.getAudit().getAuthoriserTime(), InstantCodec.instance)
                        .set(10, change.getActivationTime(), InstantCodec.instance)
                        .setString(11, ruleChange.getType().name())
                        .setUUID(12, ruleChange.getRule().getRuleIdentifier())
                        .setUUID(13, ruleChange.getRule().getRuleCode())
                        .setList(14, DomainSerialiser.convertDrivers(ruleChange.getRule().getDrivers()))
                        .setList(15, DomainSerialiser.convertOutputs(ruleChange.getRule().getOutputs()))
                        .set(16, ruleChange.getRule().getStart(), InstantCodec.instance)
                        .set(17, ruleChange.getRule().getEnd(), InstantCodec.instance)
                        .setString(18, Type.NONE.name())
                        .setUUID(19, UUID.randomUUID());
                this.session.execute(boundStatement);
            }
            for (final ValueGroupChange vgChange : change.getValueGroupChanges()) {
                final BoundStatement boundStatement = insertChange.bind()
                        .setString(0, changeSet.getName())
                        .setUUID(1, changeSet.getId())
                        .setUUID(2, change.getId())
                        .set(3, change.getChangeRange().getStart(), InstantCodec.instance)
                        .set(4, change.getChangeRange().getFinish(), InstantCodec.instance)
                        .setString(5, change.getRuleSetName())
                        .setString(6, change.getAudit().getInitiator())
                        .set(7, change.getAudit().getInitiatorTime(), InstantCodec.instance)
                        .setString(8, change.getAudit().getAuthoriser())
                        .set(9, change.getAudit().getAuthoriserTime(), InstantCodec.instance)
                        .set(10, change.getActivationTime(), InstantCodec.instance)
                        .setString(11, Type.NONE.name())
                        .setUUID(12, UUID.randomUUID())
                        .setToNull(13)
                        .setToNull(14)
                        .setToNull(15)
                        .setToNull(16)
                        .setToNull(17)
                        .setString(18, vgChange.getType().name())
                        .setUUID(19, vgChange.getValueGroup().getId())
                        .setString(20, vgChange.getValueGroup().getName())
                        .setList(21, vgChange.getValueGroup().getValues())
                        .set(22, vgChange.getValueGroup().getRange().getStart(), InstantCodec.instance)
                        .set(23, vgChange.getValueGroup().getRange().getFinish(), InstantCodec.instance)
                        .setString(24, vgChange.getValueGroup().getDriverName() == null ? "" :
                                vgChange.getValueGroup().getDriverName())
                        .setList(25, vgChange.getValueGroup().getRuleCodes());
                this.session.execute(boundStatement);
            }
        }
    }

    public Result<ChangeSet> getChange(final String changeSetName) {
        try {
            final ResultSet resultSet = this.session.execute(CQL_GET_ACTIVE_CHANGE, changeSetName);
            EhSupport.ensure(!resultSet.isExhausted(), "ChangeSet %s does not exist in %s.", changeSetName,
                    this.keyspace);
            UUID changeSetId = null;
            final Map<Change, List<Change>> changes = new HashMap<>();

            for (final Row row : resultSet) {
                changeSetId = row.getUUID("id");

                final UUID changeid = row.getUUID("changeid");
                final String rulesetname = row.getString("rulesetname");
                final Instant activationTime = row.get("activationtime", Instant.class);
                final DateRange changeRange = new DateRange(row.get("start", Instant.class),
                        row.get("end", Instant.class));
                final Change newchange = new Change(changeid, rulesetname, activationTime, changeRange,
                        getAuditForChange(row), getRuleChangeForChange(row), getValueGroupChange(row));

                final List<Change> internalChanges = changes.computeIfAbsent(newchange, value -> new ArrayList<>());
                internalChanges.add(newchange);
            }
            return Result.success(new ChangeSet(changeSetId, changeSetName, mergeChangesIntoSet(changes)));
        } catch (final Exception exception) {
            return Result.failure(() -> exception);
        }
    }

    private Set<ValueGroup> getValueGroupsForRuleSet(final String ruleSetName) {
        return EhSupport.propagateFn(() -> {
            final ResultSet resultSet = this.session.execute(CQL_GET_VALUE_GROUPS_FOR_RULESET, ruleSetName);
            final Set<ValueGroup> groups = ConcurrentHashMap.newKeySet();

            for (final Row row : resultSet) {
                final UUID id = row.getUUID(0);
                final String name = row.getString(1);
                final List<String> drivers = row.getList(2, String.class);
                final DateRange range = new DateRange(row.get(3, Instant.class), row.get(4, Instant.class));

                final ValueGroup group = new ValueGroup(id, name, drivers, range);
                groups.add(group);
            }

            return groups;
        });
    }

    private Set<RuleChange> getRuleChangeForChange(final Row row) {
        final String type = row.getString("rulechangetype");
        if (type != null && Type.valueOf(type) != Type.NONE) {
            final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator()
                    .with(RuleBuilder::input, row.getList("ruledrivers", String.class))
                    .with(RuleBuilder::output, row.getList("ruleoutputs", String.class))
                    .with(RuleBuilder::setId, row.getUUID("ruleid"))
                    .with(RuleBuilder::setCode, row.getUUID("rulecode"))
                    .with(RuleBuilder::start, row.get("rulestart", Instant.class))
                    .with(RuleBuilder::end, row.get("ruleend", Instant.class));

            final DecisionTreeRule rule = ruleBuilder.build();
            return Collections.singleton(new RuleChange(Type.valueOf(type), rule));
        }

        return Collections.emptySet();
    }

    private DecisionTreeRule getRuleFromExistingRule(final DecisionTreeRule rule, final DriverCache cache) {
        final List<String> drivers = new ArrayList<>(rule.getDrivers().length);
        Arrays.stream(rule.getDrivers()).forEach(inputDriver -> drivers.add(inputDriver.toString()));

        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator()
                .with(RuleBuilder::cache, cache)
                .with(RuleBuilder::setId, rule.getRuleIdentifier())
                .with(RuleBuilder::setCode, rule.getRuleCode())
                .with(RuleBuilder::input, drivers)
                .with(RuleBuilder::output, rule.getOutputs())
                .with(RuleBuilder::start, rule.getStart())
                .with(RuleBuilder::end, rule.getEnd());

        return ruleBuilder.build();
    }

    private Set<ValueGroupChange> getValueGroupChange(final Row row) {
        final String type = row.getString("vgchangetype");
        if (type != null && Type.valueOf(type) != Type.NONE) {
            final ValueGroup group =
                    new ValueGroup(row.getUUID("vgid"), row.getString("vgname"),
                            row.getList("vgdrivers", String.class),
                            new DateRange(row.get("vgstart", Instant.class), row.get("vgend", Instant.class)));

            final String driver = row.getString("vgdrivername");
            if (driver != null && !driver.isEmpty()) {
                group.setNewRuleData(driver, row.getList("vgrulecodes", UUID.class));
            }

            return Collections.singleton(new ValueGroupChange(Type.valueOf(type), group));
        }

        return Collections.emptySet();
    }

    private Audit getAuditForChange(final Row row) {
        return new Audit(row.getString("initiator"),
                row.get("initiatortime", Instant.class), row.getString("approver"),
                row.get("approvertime", Instant.class));
    }

    private Set<Change> mergeChangesIntoSet(final Map<Change, List<Change>> changes) {
        final Set<Change> changeSet = new HashSet<>();
        // Convert Map to Set for final construction
        for (final Map.Entry<Change, List<Change>> changeGroup : changes.entrySet()) {
            final Set<RuleChange> ruleChanges = changeGroup.getValue().stream().flatMap(change ->
                    change.getRuleChanges().stream()).collect(Collectors.toSet());
            final Set<ValueGroupChange> groupChanges = changeGroup.getValue().stream().flatMap(change ->
                    change.getValueGroupChanges().stream()).collect(Collectors.toSet());

            final Change finalChange = changeGroup.getKey();

            // Get the value groups for the rule set and add to the cache to ensure rules are correct
            final Set<ValueGroup> ruleSetGroups = getValueGroupsForRuleSet(finalChange.getRuleSetName());
            groupChanges.forEach(valueGroupChange -> ruleSetGroups.add(valueGroupChange.getValueGroup()));

            final DriverCache cache = new DriverCache();
            RuleSetBuilder.addValueGroupsToDriverCache(cache, ruleSetGroups);

            final Set<RuleChange> finalRuleChanges = new HashSet<>(ruleChanges.size());
            ruleChanges.forEach(ruleChange -> finalRuleChanges.add(new RuleChange(ruleChange.getType(),
                    getRuleFromExistingRule(ruleChange.getRule(), cache))));

            changeSet.add(new Change(finalChange.getId(), finalChange.getRuleSetName(),
                    finalChange.getActivationTime(), finalChange.getChangeRange(),
                    finalChange.getAudit(), finalRuleChanges, groupChanges));
        }
        return changeSet;
    }


    /**
     * Test if the load should be retried on failure. Initially {@link CassandraLoader} we would never retry. //TODO
     * Update to check result to see if we need to retry.
     *
     * @param result {@link Result} from loading the file.
     * @return hardcoded to false for this class.
     */
    @Override
    public boolean test(final Result result) {
        return false;
    }

    @Override
    public boolean isRunning() {
        return this.session != null && !this.session.isClosed();
    }

    @Override
    public void stop() {
        if (isRunning()) {
            this.session.close();
        }
    }
}
