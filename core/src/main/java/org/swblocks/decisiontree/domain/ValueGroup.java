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

package org.swblocks.decisiontree.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.swblocks.decisiontree.TreeValueGroup;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.DateRange;

/**
 * ValueGroup defines the values that are used to create a {@link GroupDriver}.
 *
 * <p>If one of the driver values is also a value group, then the value should be prefixed with "VG:".
 */
public final class ValueGroup implements TreeValueGroup {
    public static final DateRange DEFAULT_DATE_RANGE = new DateRange(DecisionTreeRule.EPOCH, DecisionTreeRule.MAX);
    private final UUID id;
    private final String name;
    private final DateRange range;
    private final List<UUID> ruleCodes = new ArrayList<>(1);
    private final List<String> values = new ArrayList<>(1);
    private String driverName;

    /**
     * Constructor taking a name and values only.
     *
     * @param name   the value group name
     * @param values the list of driver values
     */
    public ValueGroup(final String name, final List<String> values) {
        this(UUID.randomUUID(), name, values, DEFAULT_DATE_RANGE);
    }

    /**
     * Constructor initialising all variables.
     *
     * @param id     the {@code UUID} id for the value group
     * @param name   the name of the value group
     * @param values the list of driver values
     * @param range  the date range that the value group is active
     */
    public ValueGroup(final UUID id, final String name, final List<String> values, final DateRange range) {
        EhSupport.ensureArg(!(id == null || name == null || values == null || range == null),
                "ValueGroup cannot be initialised with any null values %s, %s, %s, %s", id, name, values, range);
        this.id = id;
        this.name = name;
        this.values.addAll(values);
        this.range = DEFAULT_DATE_RANGE.equals(range) ? DEFAULT_DATE_RANGE : range;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<String> getValues() {
        return CollectionUtils.unmodifiableList(this.values);
    }

    @Override
    public DateRange getRange() {
        return this.range;
    }

    /**
     * Method to allow over-writing of the current values.
     *
     * @param amended the amended list of driver values
     */
    public void updateValues(final List<String> amended) {
        if (CollectionUtils.isNotEmpty(amended)) {
            this.values.clear();
            this.values.addAll(amended);
        }
    }

    /**
     * Set the rule data where this value group is a brand new value group.
     *
     * @param driverName the rule driver name
     * @param ruleCodes  the rule codes
     */
    public void setNewRuleData(final String driverName, final List<UUID> ruleCodes) {
        EhSupport.ensureArg(driverName != null, "Driver name cannot be null");
        EhSupport.ensureArg(CollectionUtils.isNotEmpty(ruleCodes), "Rule codes have not been provided");

        this.driverName = driverName;
        this.ruleCodes.addAll(ruleCodes);
    }

    public List<UUID> getRuleCodes() {
        return this.ruleCodes;
    }

    public String getDriverName() {
        return this.driverName;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final ValueGroup otherGroup = (ValueGroup) other;
        return getId().equals(otherGroup.getId()) && getName().equals(otherGroup.getName()) &&
                getValues().equals(otherGroup.getValues()) && getRange().equals(otherGroup.getRange());
    }

    @Override
    public int hashCode() {
        return getId().hashCode() + getName().hashCode() + getValues().hashCode() + getRange().hashCode();
    }

    @Override
    public String toString() {
        return GroupDriver.VG_PREFIX + getId();
    }
}
