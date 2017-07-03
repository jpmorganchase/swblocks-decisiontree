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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Evaluation class for evaluating groups of input drivers.
 *
 * <p>The class contains a set of string values and a list of other input driver value types.
 */
final class GroupEvaluation implements Predicate<String> {
    private final String value;
    private Set<InputDriver> drivers;
    private Set<String> group;

    /**
     * Constructs a {@link GroupEvaluation}
     *
     * @param value        Name of the group
     * @param inputDrivers {@link List} of {@link InputDriver} which make up the group.
     */
    GroupEvaluation(final String value, final List<InputDriver> inputDrivers) {
        this.value = value;
        this.drivers = new HashSet<>(inputDrivers.size(), 1F);
        this.group = new HashSet<>(inputDrivers.size(), 1F);
        setSubValues(inputDrivers);
    }

    /**
     * Replaces the internal drivers with new ones.
     *
     * @param inputDrivers new drivers for the group
     */
    public void setSubValues(final List<InputDriver> inputDrivers) {
        final Set<String> localGroup = new HashSet<>(inputDrivers.size(), 1F);
        final Set<InputDriver> localDrivers = new HashSet<>(inputDrivers.size(), 1F);

        splitOutStringDriversAndOthers(inputDrivers, localGroup, localDrivers);

        if (!localGroup.isEmpty()) {
            this.group = new HashSet<>(localGroup.size(), 1F);
            this.group.addAll(localGroup);
        } else {
            this.group = Collections.emptySet();
        }

        if (!localDrivers.isEmpty()) {
            this.drivers = new HashSet<>(localDrivers.size(), 1F);
            this.drivers.addAll(localDrivers);
        } else {
            this.drivers = Collections.emptySet();
        }
    }

    private void splitOutStringDriversAndOthers(final List<InputDriver> drivers, final Set<String> localGroup,
                                                final Set<InputDriver> localDrivers) {

        GroupDriver.getSubInputDrivers(drivers, true).forEach(inputDriver -> {
            switch (inputDriver.getType()) {
                case STRING:
                    localGroup.add(inputDriver.getValue());
                    break;
                case REGEX:
                    localDrivers.add(inputDriver);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public boolean test(final String input) {
        if (this.group.contains(input)) {
            return true;
        }

        for (final InputDriver driver : this.drivers) {
            if (driver.evaluate(input)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return this.value.equals(((GroupEvaluation) other).value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
}
