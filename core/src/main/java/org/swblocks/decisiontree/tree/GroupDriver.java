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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Value group driver value that contains a set of string values and a list of other input driver value types.
 *
 * <p>The value group driver could be comprised of a group of strings and a regex pattern for example.
 */
public final class GroupDriver extends InputDriver {
    public static final String VG_PREFIX = "VG:";
    private List<InputDriver> originalDrivers;

    /**
     * Creates the {@code GroupDriver} from a list of {@link InputDriver}.
     *
     * @param value        Name of the group
     * @param inputDrivers List of input drivers, which can include other value groups
     */
    public GroupDriver(final String value, final List<InputDriver> inputDrivers) {
        super(value, InputValueType.VALUE_GROUP, new GroupEvaluation(value, inputDrivers));
        this.originalDrivers = new ArrayList<>(inputDrivers);
    }

    static List<InputDriver> getSubInputDrivers(final List<InputDriver> originalDrivers,
                                                final boolean recurse) {
        final Set<InputDriver> allDrivers = new HashSet<>(originalDrivers);
        for (final InputDriver driver : originalDrivers) {
            if (recurse && InputValueType.VALUE_GROUP.equals(driver.getType())) {
                getSubInputDrivers(((GroupDriver) driver).originalDrivers, allDrivers);
            }
        }
        return new ArrayList<>(allDrivers);
    }

    private static void getSubInputDrivers(final List<InputDriver> originalDrivers,
                                           final Set<InputDriver> allDrivers) {
        for (final InputDriver driver : originalDrivers) {
            if (InputValueType.VALUE_GROUP.equals(driver.getType()) && !allDrivers.contains(driver)) {
                getSubInputDrivers(((GroupDriver) driver).originalDrivers, allDrivers);
            }
        }
        allDrivers.addAll(originalDrivers);
    }

    /**
     * Populates two lists of non group and group drivers in String form.
     *
     * @param originalDrivers Drivers to convert to string form
     * @param drivers         {@link List} of non group drivers to be populated from the {@code originalDrivers}
     * @param groups          {@link List} of group drivers to be populated from the {@code originalDrivers}
     */
    public static void convertDriversIntoDriversAndGroups(final List<InputDriver> originalDrivers,
                                                          final List<String> drivers,
                                                          final List<String> groups) {
        for (final InputDriver value : originalDrivers) {
            if (InputValueType.VALUE_GROUP.equals(value.getType())) {
                final List<String> group = ((GroupDriver) value).convertDrivers();
                final String driverName = value.toString() + ":";
                groups.add(driverName + String.join(":", group));
            } else {
                drivers.add(value.getValue());
            }
        }
    }

    /**
     * Gets an array of {@link InputDriver} containing all the {@link InputDriver} items.
     *
     * @param recurse include {@link InputDriver} from sub group drivers.
     * @return Array of {@link InputDriver}
     */
    public InputDriver[] getSubDrivers(final boolean recurse) {
        final List<InputDriver> allDrivers = getSubInputDrivers(this.originalDrivers, recurse);
        return allDrivers.toArray(new InputDriver[allDrivers.size()]);
    }

    /**
     * Converts the internal drivers to String form for serialising out.
     *
     * <p>The order returned is non-group drivers and then group drivers to ensure that the result is parsed correctly.
     *
     * @return {@link List} of drivers in String form
     */
    public List<String> convertDrivers() {
        final List<String> driverList = new ArrayList<>(this.originalDrivers.size());
        final List<String> vgList = new ArrayList<>(this.originalDrivers.size());

        convertDriversIntoDriversAndGroups(this.originalDrivers, driverList, vgList);
        // Valuegroups are added after normal drivers.
        driverList.addAll(vgList);

        return driverList;
    }

    /**
     * Updates the internal store of the group drivers.
     *
     * @param subValues {@link List} of new drivers, replacing the old.
     */
    public void setSubValues(final List<InputDriver> subValues) {
        this.originalDrivers = new ArrayList<>(subValues);
        setEvaluation(new GroupEvaluation(getValue(), subValues));
    }

    @Override
    public String toString() {
        return VG_PREFIX + getValue();
    }

    /**
     * Over ride equals for Group Driver.
     *
     * <p>This has been added to address a sonar rule. In reality, {@link InputDriver#equals(Object)} fully implements
     * is required for this class. The field {@link GroupDriver#originalDrivers} plays no part in equality of the
     * object.
     *
     * <p>Note the presence of the hashcode method below - it invokes super.hashcode() to align with equals.
     *
     * @param other the object to compare
     * @return true if the objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}