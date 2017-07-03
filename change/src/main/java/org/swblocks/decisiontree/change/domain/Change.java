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

package org.swblocks.decisiontree.change.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.TreeChange;
import org.swblocks.decisiontree.TreeRule;
import org.swblocks.decisiontree.TreeValueGroup;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;

import static java.util.Optional.ofNullable;

/**
 * Domain object representing a Change.
 */
public final class Change implements TreeChange {
    private final UUID id;
    private final String ruleSetName;
    private final Set<RuleChange> ruleChanges;
    private final Set<ValueGroupChange> valueGroupChanges;
    private final Instant activationTime;
    private final DateRange changeRange;
    private Audit audit;

    /**
     * Constructor initialising the Change.
     *
     * @param id                the id of the change
     * @param ruleSetName       the rule set associated with the change
     * @param activationTime    the time that the change is activated (if null - change is not active)
     * @param changeRange       the time period that the change affects
     * @param audit             the audit information associated with a change
     * @param ruleChanges       the set of {@link RuleChange} associated with the change
     * @param valueGroupChanges the set of {@link ValueGroupChange} associated with the change
     */
    public Change(final UUID id,
                  final String ruleSetName,
                  final Instant activationTime,
                  final DateRange changeRange,
                  final Audit audit,
                  final Set<RuleChange> ruleChanges,
                  final Set<ValueGroupChange> valueGroupChanges) {
        this.id = id;
        this.ruleSetName = ruleSetName;
        this.activationTime = activationTime;
        this.changeRange = changeRange;
        this.audit = audit;
        this.ruleChanges = ofNullable(ruleChanges).orElse(Collections.emptySet());
        this.valueGroupChanges = ofNullable(valueGroupChanges).orElse(Collections.emptySet());
    }

    public UUID getId() {
        return this.id;
    }

    public String getRuleSetName() {
        return this.ruleSetName;
    }

    public Instant getActivationTime() {
        return this.activationTime;
    }

    public DateRange getChangeRange() {
        return this.changeRange;
    }

    public Set<RuleChange> getRuleChanges() {
        return Collections.unmodifiableSet(this.ruleChanges);
    }

    @Override
    public Map<UUID, Optional<TreeRule>> getRules() {
        return this.ruleChanges.stream().collect(
                Collectors.toMap((RuleChange ruleChanges) -> ruleChanges.getRule().getRuleIdentifier(),
                        ruleChange -> Type.NEW.equals(ruleChange.getType()) ?
                                Optional.of(ruleChange.getRule()) : Optional.empty(),
                        (map1Value, map2Value) -> (map1Value.isPresent() ? map1Value : map2Value)));
    }

    @Override
    public Map<UUID, Optional<TreeValueGroup>> getGroups() {
        return this.valueGroupChanges.stream().collect(
                Collectors.toMap((ValueGroupChange valueGroupChange) -> valueGroupChange.getValueGroup().getId(),
                        valueGroupChange -> Type.NEW.equals(valueGroupChange.getType()) ?
                                Optional.of(valueGroupChange.getValueGroup()) : Optional.empty(),
                        (map1Value, map2Value) -> (map1Value.isPresent() ? map1Value : map2Value)));
    }

    public Set<ValueGroupChange> getValueGroupChanges() {
        return Collections.unmodifiableSet(this.valueGroupChanges);
    }

    public boolean addRuleChange(final RuleChange change) {
        EhSupport.ensureArg(change != null, "Cannot add a null rule change");
        return this.ruleChanges.add(change);
    }

    public boolean addRuleChange(final Set<RuleChange> changes) {
        EhSupport.ensureArg(changes != null, "Cannot add a null rule change");
        return this.ruleChanges.addAll(changes);
    }

    public boolean removeRuleChange(final RuleChange change) {
        return this.ruleChanges.remove(change);
    }

    public boolean addValueGroupChange(final ValueGroupChange change) {
        EhSupport.ensureArg(change != null, "Cannot add a null value group change");
        return this.valueGroupChanges.add(change);
    }

    public boolean removeValueGroupChange(final ValueGroupChange change) {
        return this.valueGroupChanges.remove(change);
    }

    public Audit getAudit() {
        return this.audit;
    }

    public void setAudit(final Audit audit) {
        this.audit = audit;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final Change otherChange = (Change) other;
        return this.id.equals(otherChange.getId()) && this.ruleSetName.equals(otherChange.getRuleSetName());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode() + this.ruleSetName.hashCode();
    }
}
