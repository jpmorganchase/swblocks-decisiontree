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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.tree.RegexDriver;
import org.swblocks.decisiontree.tree.StringDriver;

/**
 * Class that caches {@link InputDriver} objects so that they can be shared across rules during ruleset creation.
 */
public final class DriverCache {
    /**
     * Map that caches {@link StringDriver}, {@link RegexDriver} and {@link GroupDriver} objects.
     */
    final Map<String, InputDriver> drivers = new HashMap<>();

    public DriverCache() {
        // default empty constructor
    }

    /**
     * Gets an {@link InputDriver} from the cache, if it exists.
     *
     * @param name Name of the driver to search for.
     * @param type Type of driver.
     * @return {@link InputDriver} found in the cache or null otherwise.
     */
    public InputDriver get(final String name, final InputValueType type) {
        return this.drivers.get(getKey(name, type));
    }

    public boolean contains(final String key) {
        return this.drivers.containsKey(key);
    }

    /**
     * Puts an {@link InputDriver} into the cache, replacing any existing driver.
     *
     * @param driver {@link InputDriver} to add.
     */
    public void put(final InputDriver driver) {
        final String key = driver.toString();
        this.drivers.put(key, driver);
    }

    private String getKey(final String name, final InputValueType type) {
        return (type == InputValueType.VALUE_GROUP) ? GroupDriver.VG_PREFIX + name : name;
    }

    /**
     * Returns a list of {@link InputDriver} where they match on {@link InputValueType}.
     *
     * @param type Type to search for
     * @return {@link List} of matching drivers.
     */
    public List<InputDriver> findByInputDriverType(final InputValueType type) {
        return this.drivers.values().stream().filter(i -> i.getType() == type).collect(Collectors.toList());
    }

    public List<InputDriver> findByActualName(final String name) {
        return this.drivers.values().stream().filter(i -> name.equals(i.getValue())).collect(Collectors.toList());
    }

}