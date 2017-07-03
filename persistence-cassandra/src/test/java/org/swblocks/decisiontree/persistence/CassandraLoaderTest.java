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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.swblocks.decisiontree.change.domain.Audit;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.ChangeSet;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.change.domain.builder.ChangeBuilder;
import org.swblocks.decisiontree.change.domain.builder.RuleChangeBuilder;
import org.swblocks.decisiontree.change.domain.builder.ValueGroupChangeBuilder;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSetMatcher;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.decisiontree.domain.builders.DomainSerialiser;
import org.swblocks.decisiontree.domain.builders.RuleBuilder;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.eh.Result;
import org.swblocks.jbl.util.DateRange;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link CassandraLoader}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CodecRegistry.class)
public class CassandraLoaderTest {
    private static final String RULE_SET_NAME = "ruleSetName";
    private static final Instant NOW = Instant.now();
    private static final UUID ID = new UUID(0L, 1L);
    private static final List<String> NEW_OUTPUT = Collections.singletonList("Rate:1.3");
    private static final UUID CHANGESET_ID = UUID.randomUUID();
    private static final String CHANGE_NAME = "testChange";
    private static final UUID CODE = new UUID(1L, 0L);
    private static final UUID CHANGEID = UUID.randomUUID();
    private static final List<String> OUTPUTS = Collections.singletonList("Rate:1.0");
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final List<String> DRIVER_NAMES =
            Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET");
    private static final List<String> INPUTS = Arrays.asList("VOICE", "VG:" + GROUP_ID, "ED", "US", "RATE");
    private static final Instant START = Instant.parse("2013-04-04T00:00:00Z");
    private static final Instant FINISH = Instant.parse("2013-04-08T00:00:00Z");
    private static final String GROUP_NAME = "CMEGroup";
    private static final List<String> GROUP_DRIVERS = Arrays.asList("CME", "CBOT");
    private static final ValueGroup group =
            new ValueGroup(GROUP_ID, GROUP_NAME, GROUP_DRIVERS, ValueGroup.DEFAULT_DATE_RANGE);

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Cluster cluster;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Session session;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private BoundStatement boundStatement;
    @Mock
    private ResultSet rulesetResults;
    @Mock
    private Row rulesetRow;
    @Mock
    private ResultSet ruleResults;
    @Mock
    private Row ruleRow;
    @Mock
    private ResultSet groupResults;
    @Mock
    private Row groupRow;
    @Mock
    private ResultSet changeResults;
    @Mock
    private Row changeRow1;
    @Mock
    private Row changeRow2;
    @Mock
    private Row changeRow3;

    @Before
    public void setup() {
        when(this.cluster.connect(anyString())).thenReturn(this.session);
        when(this.session.prepare(anyString())).thenReturn(this.preparedStatement);
        when(this.preparedStatement.bind()).thenReturn(this.boundStatement);
        when(this.boundStatement.setString(anyInt(), anyString())).thenReturn(this.boundStatement);
        when(this.boundStatement.setUUID(anyInt(), anyObject())).thenReturn(this.boundStatement);
        when(this.boundStatement.setList(anyInt(), anyListOf(String.class))).thenReturn(this.boundStatement);
        when(this.boundStatement.setList(anyInt(), anyListOf(UUID.class))).thenReturn(this.boundStatement);
        when(this.boundStatement.set(anyInt(), anyObject(), any(InstantCodec.class))).thenReturn(this.boundStatement);
        when(this.boundStatement.setToNull(anyInt())).thenReturn(this.boundStatement);
        when(this.boundStatement.setString(anyString(), anyString())).thenReturn(this.boundStatement);
        when(this.boundStatement.setUUID(anyString(), anyObject())).thenReturn(this.boundStatement);
        when(this.boundStatement.setList(anyString(), anyListOf(String.class))).thenReturn(this.boundStatement);
        when(this.boundStatement.setList(anyString(), anyListOf(UUID.class))).thenReturn(this.boundStatement);
        when(this.boundStatement.set(anyString(), anyObject(), any(InstantCodec.class)))
                .thenReturn(this.boundStatement);
        when(this.boundStatement.setToNull(anyString())).thenReturn(this.boundStatement);
    }

    @Test
    public void testPutRuleSet() {
        final CassandraLoader testInstance = CassandraLoader.instanceOf(this.cluster, "keyspace", RULE_SET_NAME);
        assertNotNull(testInstance);
        verify(this.cluster, times(1)).getConfiguration();

        final DecisionTreeRuleSet commissionRuleSet = createSingleRuleRuleSet();

        testInstance.put(commissionRuleSet);
        verify(this.boundStatement, times(2)).setString(0, RULE_SET_NAME);
        verify(this.boundStatement).setUUID(1, ID);
        verify(this.boundStatement).setUUID(2, CODE);
        verify(this.boundStatement).setList(3, INPUTS);
        verify(this.boundStatement).set(4, START, InstantCodec.instance);
        verify(this.boundStatement).set(5, FINISH, InstantCodec.instance);
        verify(this.boundStatement).setList(6, OUTPUTS);

        verify(this.boundStatement).setUUID(1, GROUP_ID);
        verify(this.boundStatement).setString(2, GROUP_NAME);
        verify(this.boundStatement).setList(3, GROUP_DRIVERS);
        verify(this.boundStatement).set(4, DecisionTreeRule.EPOCH, InstantCodec.instance);
        verify(this.boundStatement).set(5, DecisionTreeRule.MAX, InstantCodec.instance);

        verifyNoMoreInteractions(this.boundStatement);

        verify(this.session, times(2)).execute(this.boundStatement);
    }

    @Test
    public void testGetRuleSet() {
        when(this.session.execute(CassandraLoader.CQL_GET_RULESET_FROM_RULESETNAME, "DEFAULT", RULE_SET_NAME))
                .thenReturn(this.rulesetResults);
        when(this.rulesetResults.isExhausted()).thenReturn(false, true);
        when(this.rulesetResults.one()).thenReturn(this.rulesetRow);
        when(this.session.execute(CassandraLoader.CQL_GET_RULES_FOR_RULESET, RULE_SET_NAME))
                .thenReturn(this.ruleResults);
        when(this.session.execute(CassandraLoader.CQL_GET_VALUE_GROUPS_FOR_RULESET, RULE_SET_NAME))
                .thenReturn(this.groupResults);

        final Iterator<Row> groupIterator = Collections.singletonList(this.groupRow).iterator();
        when(this.groupResults.iterator()).thenReturn(groupIterator);
        when(this.groupRow.getUUID(0)).thenReturn(GROUP_ID);
        when(this.groupRow.getString(1)).thenReturn(GROUP_NAME);
        when(this.groupRow.getList(2, String.class)).thenReturn(GROUP_DRIVERS);
        when(this.groupRow.get(3, Instant.class)).thenReturn(DecisionTreeRule.EPOCH);
        when(this.groupRow.get(4, Instant.class)).thenReturn(DecisionTreeRule.MAX);

        final Iterator<Row> rowInterator = Collections.singletonList(this.ruleRow).iterator();
        when(this.ruleResults.iterator()).thenReturn(rowInterator);
        when(this.ruleRow.get("id", UUID.class)).thenReturn(ID);
        when(this.ruleRow.get("code", UUID.class)).thenReturn(CODE);
        when(this.ruleRow.get("start", Instant.class)).thenReturn(START);
        when(this.ruleRow.get("end", Instant.class)).thenReturn(FINISH);
        when(this.ruleRow.getList("drivers", String.class)).thenReturn(INPUTS);
        when(this.ruleRow.getList("outputs", String.class)).thenReturn(OUTPUTS);

        final CassandraLoader testInstance = CassandraLoader.instanceOf(this.cluster, "keyspace", RULE_SET_NAME);
        assertNotNull(testInstance);
        verify(this.cluster, times(1)).getConfiguration();
        when(this.rulesetRow.getList("drivers", String.class)).thenReturn(DRIVER_NAMES);
        when(this.rulesetRow.getList("groups", String.class)).thenReturn(Collections.emptyList());

        final Result<DecisionTreeRuleSet> commissionRuleSet = testInstance.get();
        if (!commissionRuleSet.isSuccess()) {
            assertTrue(commissionRuleSet.getException().getMessage(), commissionRuleSet.isSuccess());
        }

        verify(this.rulesetRow, times(1)).getList("drivers", String.class);

        verify(this.groupRow, times(1)).getUUID(0);
        verify(this.groupRow, times(1)).getString(1);
        verify(this.groupRow, times(1)).getList(2, String.class);
        verify(this.groupRow, times(1)).get(3, Instant.class);
        verify(this.groupRow, times(1)).get(4, Instant.class);

        verify(this.ruleRow, times(1)).get("id", UUID.class);
        verify(this.ruleRow, times(1)).get("code", UUID.class);
        verify(this.ruleRow, times(1)).get("start", Instant.class);
        verify(this.ruleRow, times(1)).get("end", Instant.class);
        verify(this.ruleRow, times(1)).getList("drivers", String.class);
        verify(this.ruleRow, times(1)).getList("outputs", String.class);

        assertThat(commissionRuleSet.getData(), DecisionTreeRuleSetMatcher.isSame(createSingleRuleRuleSet()));
    }

    @Test
    public void testPutChange() {
        final CassandraLoader testInstance = CassandraLoader.instanceOf(this.cluster, "keyspace", RULE_SET_NAME);
        assertNotNull(testInstance);
        verify(this.cluster, times(1)).getConfiguration();
        final DecisionTreeRuleSet decisionTreeRuleSet = createSingleRuleRuleSet();
        final ChangeSet commissionChangeSet = createSingleChangeSet(decisionTreeRuleSet);

        testInstance.put(commissionChangeSet);
        // The ChangeSet and Change are repeated for each RuleChange, so are called twice.
        verify(this.boundStatement, times(2)).setString(0, CHANGE_NAME);
        verify(this.boundStatement, times(2)).setUUID(1, CHANGESET_ID);
        // 2 Change ID
        verify(this.boundStatement, times(2)).set(3, START, InstantCodec.instance);
        verify(this.boundStatement, times(2)).set(4, FINISH, InstantCodec.instance);
        verify(this.boundStatement, times(2)).setString(5, RULE_SET_NAME);
        verify(this.boundStatement, times(2)).setString(6, "USER1");
        verify(this.boundStatement, times(2)).set(7, NOW, InstantCodec.instance);
        verify(this.boundStatement, times(2)).setString(8, "USER2");
        verify(this.boundStatement, times(2)).set(9, NOW, InstantCodec.instance);
        // Activation
        verify(this.boundStatement, times(2)).set(10, NOW, InstantCodec.instance);

        final Change change = commissionChangeSet.getChanges().iterator().next();
        assertNotNull(change);
        assertEquals(2, change.getRuleChanges().size());
        final RuleChange ruleChange1 = change.getRuleChanges().iterator().next();
        final RuleChange ruleChange2 = change.getRuleChanges().iterator().next();

        verify(this.boundStatement, times(1)).setString(11, ruleChange1.getType().toString());
        verify(this.boundStatement, times(1)).setString(11, ruleChange2.getType().toString());

        verify(this.boundStatement, times(1)).setUUID(12, ruleChange1.getRule().getRuleIdentifier());
        verify(this.boundStatement, times(1)).setUUID(12, ruleChange2.getRule().getRuleIdentifier());

        // Both RulesChanges have the same RuleCode as they relate to one rule
        verify(this.boundStatement, times(2)).setUUID(13, ruleChange1.getRule().getRuleCode());

        // Both RulesChanges have the same Inputs as the change edits outputs
        verify(this.boundStatement, times(2)).setList(14,
                DomainSerialiser.convertDrivers(ruleChange1.getRule().getDrivers()));
        verify(this.boundStatement, times(1)).setList(15,
                DomainSerialiser.convertOutputs(ruleChange1.getRule().getOutputs()));
        verify(this.boundStatement, times(1)).setList(15,
                DomainSerialiser.convertOutputs(ruleChange2.getRule().getOutputs()));
        verify(this.boundStatement, times(2)).set(16, START, InstantCodec.instance);
        verify(this.boundStatement, times(2)).set(17, FINISH, InstantCodec.instance);

        verify(this.session, times(2)).execute(this.boundStatement);
    }

    @Test
    public void testGetChange() {
        setUpPutChangeMocks(false);

        final CassandraLoader testInstance = CassandraLoader.instanceOf(this.cluster, "keyspace", RULE_SET_NAME);
        assertNotNull(testInstance);
        verify(this.cluster, times(1)).getConfiguration();
        final Result<ChangeSet> loadedChangeSet = testInstance.getChange(CHANGE_NAME);
        assertNotNull(loadedChangeSet);
        if (!loadedChangeSet.isSuccess()) {
            assertTrue(loadedChangeSet.getException().getMessage(), loadedChangeSet.isSuccess());
        }
        verifyRuleChanges();
    }

    private void verifyRuleChanges() {
        verify(this.changeRow1, times(1)).getUUID("id");
        verify(this.changeRow2, times(1)).getUUID("id");
        verify(this.changeRow1, times(1)).getUUID("changeid");
        verify(this.changeRow2, times(1)).getUUID("changeid");
        verify(this.changeRow1, times(1)).getString("rulesetname");
        verify(this.changeRow2, times(1)).getString("rulesetname");
        verify(this.changeRow1, times(1)).get("activationtime", Instant.class);
        verify(this.changeRow2, times(1)).get("activationtime", Instant.class);
        verify(this.changeRow1, times(1)).get("start", Instant.class);
        verify(this.changeRow2, times(1)).get("end", Instant.class);
        verify(this.changeRow1, times(1)).get("start", Instant.class);
        verify(this.changeRow2, times(1)).get("end", Instant.class);
        verify(this.changeRow1, times(1)).get("initiatortime", Instant.class);
        verify(this.changeRow2, times(1)).get("initiatortime", Instant.class);
        verify(this.changeRow1, times(1)).get("approvertime", Instant.class);
        verify(this.changeRow2, times(1)).get("approvertime", Instant.class);
        verify(this.changeRow1, times(1)).getString("initiator");
        verify(this.changeRow2, times(1)).getString("initiator");
        verify(this.changeRow1, times(1)).getString("approver");
        verify(this.changeRow2, times(1)).getString("approver");
        verify(this.changeRow1, times(1)).getString("rulechangetype");
        verify(this.changeRow2, times(1)).getString("rulechangetype");
        verify(this.changeRow1, times(1)).getUUID("ruleid");
        verify(this.changeRow2, times(1)).getUUID("ruleid");
        verify(this.changeRow1, times(1)).getUUID("rulecode");
        verify(this.changeRow2, times(1)).getUUID("rulecode");
        verify(this.changeRow1, times(1)).getList("ruledrivers", String.class);
        verify(this.changeRow2, times(1)).getList("ruledrivers", String.class);
        verify(this.changeRow1, times(1)).getList("ruleoutputs", String.class);
        verify(this.changeRow2, times(1)).getList("ruleoutputs", String.class);
        verify(this.changeRow1, times(1)).get("rulestart", Instant.class);
        verify(this.changeRow2, times(1)).get("rulestart", Instant.class);
        verify(this.changeRow1, times(1)).get("ruleend", Instant.class);
        verify(this.changeRow2, times(1)).get("ruleend", Instant.class);
    }

    @Test
    public void putNewGroupChange() {
        final CassandraLoader testInstance = CassandraLoader.instanceOf(this.cluster, "keyspace", RULE_SET_NAME);
        assertNotNull(testInstance);
        verify(this.cluster, times(1)).getConfiguration();
        final DecisionTreeRuleSet decisionTreeRuleSet = createSingleRuleRuleSetWithoutValueGroup();

        final Builder<ValueGroupChangeBuilder, List<ValueGroupChange>> valueGroupChangeBuilder =
                ValueGroupChangeBuilder.creator(GROUP_NAME);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleSet, decisionTreeRuleSet);
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::changeRange, new DateRange(START, FINISH));
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::drivers, Arrays.asList("CME", "CBOT"));
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::ruleCodes, Collections.singletonList(CODE));
        valueGroupChangeBuilder.with(ValueGroupChangeBuilder::driver, "EXCHANGE");

        final Builder<ChangeBuilder, Change> changeBuilder = ChangeBuilder.creator(decisionTreeRuleSet);
        changeBuilder.with(ChangeBuilder::changeRange, new DateRange(START, FINISH));
        changeBuilder.with(ChangeBuilder::valueGroupChange, valueGroupChangeBuilder);
        changeBuilder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        changeBuilder.with(ChangeBuilder::activation, NOW);

        final ChangeSet changeSet =
                new ChangeSet(CHANGESET_ID, CHANGE_NAME, Collections.singleton(changeBuilder.build()));
        testInstance.put(changeSet);

        // common for rule changes and value group insert
        verify(this.boundStatement, times(3)).setString(0, CHANGE_NAME);
        verify(this.boundStatement, times(3)).setUUID(1, CHANGESET_ID);
        // 2 change id
        verify(this.boundStatement, times(3)).set(3, START, InstantCodec.instance);
        verify(this.boundStatement, times(3)).set(4, FINISH, InstantCodec.instance);
        verify(this.boundStatement, times(3)).setString(5, RULE_SET_NAME);
        verify(this.boundStatement, times(3)).setString(6, "USER1");
        verify(this.boundStatement, times(3)).set(7, NOW, InstantCodec.instance);
        verify(this.boundStatement, times(3)).setString(8, "USER2");
        verify(this.boundStatement, times(3)).set(9, NOW, InstantCodec.instance);
        // Activation
        verify(this.boundStatement, times(3)).set(10, NOW, InstantCodec.instance);

        final Change change = changeSet.getChanges().iterator().next();
        assertNotNull(change);
        assertEquals(2, change.getRuleChanges().size());
        final RuleChange ruleChange1 = change.getRuleChanges().iterator().next();
        final RuleChange ruleChange2 = change.getRuleChanges().iterator().next();

        verify(this.boundStatement, times(1)).setString(11, ruleChange1.getType().toString());
        verify(this.boundStatement, times(1)).setString(11, ruleChange2.getType().toString());

        verify(this.boundStatement, times(1)).setUUID(12, ruleChange1.getRule().getRuleIdentifier());
        verify(this.boundStatement, times(1)).setUUID(12, ruleChange2.getRule().getRuleIdentifier());

        // Both RulesChanges have the same RuleCode as they relate to one rule
        verify(this.boundStatement, times(2)).setUUID(13, ruleChange1.getRule().getRuleCode());

        // Both RulesChanges have different inputs and same outputs
        verify(this.boundStatement, times(1)).setList(14,
                DomainSerialiser.convertDrivers(ruleChange1.getRule().getDrivers()));
        verify(this.boundStatement, times(1)).setList(14,
                DomainSerialiser.convertDrivers(ruleChange2.getRule().getDrivers()));
        verify(this.boundStatement, times(2)).setList(15,
                DomainSerialiser.convertOutputs(ruleChange1.getRule().getOutputs()));
        verify(this.boundStatement, times(2)).setList(15,
                DomainSerialiser.convertOutputs(ruleChange2.getRule().getOutputs()));
        verify(this.boundStatement, times(2)).set(16, START, InstantCodec.instance);
        verify(this.boundStatement, times(2)).set(17, FINISH, InstantCodec.instance);

        // Empty value groups....
        verify(this.boundStatement, times(2)).setString(18, Type.NONE.name());
        // random UUID

        // ValueGroup....
        assertEquals(1, change.getValueGroupChanges().size());
        final ValueGroupChange groupChange = change.getValueGroupChanges().iterator().next();
        final ValueGroup added = groupChange.getValueGroup();

        // default rule data
        verify(this.boundStatement, times(1)).setString(11, Type.NONE.name());
        // rule random uuid
        verify(this.boundStatement, times(1)).setToNull(13);
        verify(this.boundStatement, times(1)).setToNull(14);
        verify(this.boundStatement, times(1)).setToNull(15);
        verify(this.boundStatement, times(1)).setToNull(16);
        verify(this.boundStatement, times(1)).setToNull(17);

        // specific value group data
        verify(this.boundStatement, times(1)).setString(18, groupChange.getType().name());
        verify(this.boundStatement, times(1)).setUUID(19, added.getId());
        verify(this.boundStatement, times(1)).setString(20, added.getName());
        verify(this.boundStatement, times(1)).setList(21, added.getValues());
        verify(this.boundStatement, times(1)).set(22, START, InstantCodec.instance);
        verify(this.boundStatement, times(1)).set(23, FINISH, InstantCodec.instance);
        verify(this.boundStatement, times(1)).setString(24, added.getDriverName());
        verify(this.boundStatement, times(1)).setList(25, added.getRuleCodes());
    }

    @Test
    public void getNewGroupChange() {
        setUpPutChangeMocks(true);

        final CassandraLoader testInstance = CassandraLoader.instanceOf(this.cluster, "keyspace", RULE_SET_NAME);
        assertNotNull(testInstance);
        verify(this.cluster, times(1)).getConfiguration();
        final Result<ChangeSet> loadedChangeSet = testInstance.getChange(CHANGE_NAME);
        assertNotNull(loadedChangeSet);
        if (!loadedChangeSet.isSuccess()) {
            assertTrue(loadedChangeSet.getException().getMessage(), loadedChangeSet.isSuccess());
        }

        verifyRuleChanges();

        verify(this.changeRow3, times(1)).getUUID("id");
        verify(this.changeRow3, times(1)).getUUID("changeid");
        verify(this.changeRow3, times(1)).getString("rulesetname");
        verify(this.changeRow3, times(1)).get("activationtime", Instant.class);
        verify(this.changeRow3, times(1)).get("start", Instant.class);
        verify(this.changeRow3, times(1)).get("end", Instant.class);
        verify(this.changeRow3, times(1)).get("initiatortime", Instant.class);
        verify(this.changeRow3, times(1)).get("approvertime", Instant.class);
        verify(this.changeRow3, times(1)).getString("initiator");
        verify(this.changeRow3, times(1)).getString("approver");
        verify(this.changeRow3, times(1)).getString("rulechangetype");
        verify(this.changeRow3, times(0)).getUUID("ruleid");
        verify(this.changeRow3, times(0)).getUUID("rulecode");
        verify(this.changeRow3, times(0)).getList("ruledrivers", String.class);
        verify(this.changeRow3, times(0)).getList("ruleoutputs", String.class);
        verify(this.changeRow3, times(0)).get("rulestart", Instant.class);
        verify(this.changeRow3, times(0)).get("ruleend", Instant.class);
        verify(this.changeRow3, times(1)).getString("vgchangetype");
        verify(this.changeRow3, times(1)).getUUID("vgid");
        verify(this.changeRow3, times(1)).getString("vgname");
        verify(this.changeRow3, times(1)).getList("vgdrivers", String.class);
        verify(this.changeRow3, times(1)).get("vgstart", Instant.class);
        verify(this.changeRow3, times(1)).get("vgend", Instant.class);
        verify(this.changeRow3, times(1)).getString("vgdrivername");
        verify(this.changeRow3, times(1)).getList("vgrulecodes", UUID.class);
    }

    private void setUpPutChangeMocks(final boolean isValueGroup) {
        when(this.session.execute(CassandraLoader.CQL_GET_ACTIVE_CHANGE, CHANGE_NAME)).thenReturn(this.changeResults);

        final Iterator<Row> rowInterator;
        if (isValueGroup) {
            when(this.changeResults.isExhausted()).thenReturn(false, false, true);
            rowInterator = Arrays.asList(this.changeRow1, this.changeRow2, this.changeRow3).iterator();
        } else {
            when(this.changeResults.isExhausted()).thenReturn(false, true);
            rowInterator = Arrays.asList(this.changeRow1, this.changeRow2).iterator();
        }

        when(this.changeResults.iterator()).thenReturn(rowInterator);
        when(this.changeRow1.getUUID("id")).thenReturn(CHANGESET_ID);
        when(this.changeRow2.getUUID("id")).thenReturn(CHANGESET_ID);
        when(this.changeRow1.getUUID("changeid")).thenReturn(CHANGEID);
        when(this.changeRow2.getUUID("changeid")).thenReturn(CHANGEID);
        when(this.changeRow1.getString("rulesetname")).thenReturn(RULE_SET_NAME);
        when(this.changeRow2.getString("rulesetname")).thenReturn(RULE_SET_NAME);
        when(this.changeRow1.get("activationtime", Instant.class)).thenReturn(NOW);
        when(this.changeRow2.get("activationtime", Instant.class)).thenReturn(NOW);
        when(this.changeRow1.get("start", Instant.class)).thenReturn(START);
        when(this.changeRow2.get("end", Instant.class)).thenReturn(FINISH);
        when(this.changeRow1.get("start", Instant.class)).thenReturn(START);
        when(this.changeRow2.get("end", Instant.class)).thenReturn(FINISH);
        when(this.changeRow1.get("initiatortime", Instant.class)).thenReturn(NOW);
        when(this.changeRow2.get("initiatortime", Instant.class)).thenReturn(NOW);
        when(this.changeRow1.get("approvertime", Instant.class)).thenReturn(NOW);
        when(this.changeRow2.get("approvertime", Instant.class)).thenReturn(NOW);
        when(this.changeRow1.getString("initiator")).thenReturn("USER1");
        when(this.changeRow2.getString("initiator")).thenReturn("USER1");
        when(this.changeRow1.getString("approver")).thenReturn("USER2");
        when(this.changeRow2.getString("approver")).thenReturn("USER2");
        when(this.changeRow1.getString("rulechangetype")).thenReturn("ORIGINAL");
        when(this.changeRow2.getString("rulechangetype")).thenReturn("NEW");
        when(this.changeRow1.getUUID("ruleid")).thenReturn(UUID.randomUUID());
        when(this.changeRow2.getUUID("ruleid")).thenReturn(UUID.randomUUID());
        when(this.changeRow1.getUUID("rulecode")).thenReturn(CODE);
        when(this.changeRow2.getUUID("rulecode")).thenReturn(CODE);
        when(this.changeRow1.getList("ruledrivers", String.class)).thenReturn(INPUTS);
        when(this.changeRow2.getList("ruledrivers", String.class)).thenReturn(INPUTS);
        when(this.changeRow1.getList("ruleoutputs", String.class)).thenReturn(OUTPUTS);
        when(this.changeRow2.getList("ruleoutputs", String.class)).thenReturn(NEW_OUTPUT);
        when(this.changeRow1.get("rulestart", Instant.class)).thenReturn(START);
        when(this.changeRow2.get("rulestart", Instant.class)).thenReturn(START);
        when(this.changeRow1.get("ruleend", Instant.class)).thenReturn(FINISH);
        when(this.changeRow2.get("ruleend", Instant.class)).thenReturn(FINISH);

        if (isValueGroup) {
            when(this.changeRow3.getUUID("id")).thenReturn(CHANGESET_ID);
            when(this.changeRow3.getUUID("changeid")).thenReturn(CHANGEID);
            when(this.changeRow3.getString("rulesetname")).thenReturn(RULE_SET_NAME);
            when(this.changeRow3.get("activationtime", Instant.class)).thenReturn(NOW);
            when(this.changeRow3.get("start", Instant.class)).thenReturn(START);
            when(this.changeRow3.get("end", Instant.class)).thenReturn(FINISH);
            when(this.changeRow3.get("initiatortime", Instant.class)).thenReturn(NOW);
            when(this.changeRow3.get("approvertime", Instant.class)).thenReturn(NOW);
            when(this.changeRow3.getString("initiator")).thenReturn("USER1");
            when(this.changeRow3.getString("approver")).thenReturn("USER2");
            when(this.changeRow3.getString("rulechangetype")).thenReturn("NONE");
            when(this.changeRow3.getString("vgchangetype")).thenReturn("NEW");
            when(this.changeRow3.getUUID("vgid")).thenReturn(new UUID(0, 1));
            when(this.changeRow3.getString("vgname")).thenReturn(GROUP_NAME);
            when(this.changeRow3.getList("vgdrivers", String.class)).thenReturn(Arrays.asList("CME", "CBOT"));
            when(this.changeRow3.get("vgstart", Instant.class)).thenReturn(START);
            when(this.changeRow3.get("vgend", Instant.class)).thenReturn(FINISH);
            when(this.changeRow3.getString("vgdrivername")).thenReturn("EXCHANGE");
            when(this.changeRow3.getList("vgrulecodes", UUID.class)).thenReturn(Collections.singletonList(CODE));
        }
    }

    private ChangeSet createSingleChangeSet(final DecisionTreeRuleSet decisionTreeRuleSet) {
        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(decisionTreeRuleSet);
        builder.with(ChangeBuilder::setId, CHANGEID);
        builder.with(ChangeBuilder::changeRange, new DateRange(START, FINISH));
        builder.with(ChangeBuilder::audit, new Audit("USER1", NOW, "USER2", NOW));
        builder.with(ChangeBuilder::activation, NOW);

        builder.with(ChangeBuilder::ruleChange, RuleChangeBuilder.creator(CODE)
                .with(RuleChangeBuilder::output, NEW_OUTPUT));

        return new ChangeSet(CHANGESET_ID, CHANGE_NAME, Collections.singleton(builder.build()));
    }

    private DecisionTreeRuleSet createSingleRuleRuleSet() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(RULE_SET_NAME, DRIVER_NAMES);
        ruleSetBuilder.with(RuleSetBuilder::groups, Collections.singleton(group));

        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, INPUTS)
                .with(RuleBuilder::start, START)
                .with(RuleBuilder::end, FINISH)
                .with(RuleBuilder::setId, ID)
                .with(RuleBuilder::setCode, CODE)
                .with(RuleBuilder::output, OUTPUTS));

        return ruleSetBuilder.build();
    }

    private DecisionTreeRuleSet createSingleRuleRuleSetWithoutValueGroup() {
        final Builder<RuleSetBuilder, DecisionTreeRuleSet> ruleSetBuilder =
                RuleSetBuilder.creator(RULE_SET_NAME, DRIVER_NAMES);
        ruleSetBuilder.with(RuleSetBuilder::rule, RuleBuilder.creator()
                .with(RuleBuilder::input, Arrays.asList("VOICE", "CME", "ED", "US", "RATE"))
                .with(RuleBuilder::start, START)
                .with(RuleBuilder::end, FINISH)
                .with(RuleBuilder::setId, ID)
                .with(RuleBuilder::setCode, CODE)
                .with(RuleBuilder::output, OUTPUTS));

        return ruleSetBuilder.build();
    }
}
