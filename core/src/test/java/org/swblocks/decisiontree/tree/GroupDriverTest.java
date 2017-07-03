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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link GroupDriver}.
 */
public class GroupDriverTest {
    private static InputDriver createInputDriver(final String driverName, final String prefix) {
        return new GroupDriver(driverName, createSubInputDrivers(prefix));
    }

    static List<InputDriver> createSubInputDrivers(final String prefix) {
        final List<InputDriver> drivers = new ArrayList<>(4);
        drivers.add(new StringDriver(prefix + "1"));
        drivers.add(new StringDriver(prefix + "2"));
        drivers.add(new StringDriver(prefix + "3"));
        drivers.add(new RegexDriver(prefix.substring(0, prefix.length() - 1) + ".?4"));
        return drivers;
    }

    @Test
    public void correctValue() {
        final InputDriver driver = createInputDriver("Test1", "test");
        assertEquals("Test1", driver.getValue());
    }

    @Test
    public void correctType() {
        final InputDriver driver = createInputDriver("Test1", "test");
        assertEquals(InputValueType.VALUE_GROUP, driver.getType());
    }

    @Test
    public void evaluation() {
        final InputDriver driver = createInputDriver("Test1", "test");
        assertTrue(driver.evaluate("test1"));
        assertFalse(driver.evaluate("non-existant"));
        assertTrue(driver.evaluate("test4"));
    }

    @Test
    public void hashCodeValue() {
        final InputDriver driver = createInputDriver("Test1", "test");
        assertEquals(80698815, driver.hashCode());
    }

    @Test
    public void testOnlyStoreUniqueDrivers() {
        final List<InputDriver> subDriverList = createSubInputDrivers("test");
        final List<InputDriver> duplicateSubDriverList = createSubInputDrivers("test");
        final InputDriver dupDriver = new GroupDriver("dup1", duplicateSubDriverList);
        subDriverList.add(dupDriver);
        final InputDriver subDriver = new GroupDriver("sub1", subDriverList);

        final List<InputDriver> drivers = Collections.singletonList(subDriver);
        final List<InputDriver> inputs = GroupDriver.getSubInputDrivers(drivers, true);
        assertNotNull(inputs);

        // We have created 10 drivers, of which 4 are duplicates.
        // So should be 6 unique items, test1, test2, test3, test.?4, VG:dup1, VG:sub1
        assertEquals(6, inputs.size());
        inputs.forEach(input -> assertThat(1, Matchers.equalTo(Collections.frequency(inputs, input))));

    }

    @Test
    public void testGetSubDrivers() {
        final List<InputDriver> subDriverList = createSubInputDrivers("test");
        final List<InputDriver> sub2DriverList = createSubInputDrivers("subtest");
        final InputDriver sub2Driver = new GroupDriver("sub2", sub2DriverList);
        subDriverList.add(sub2Driver);
        final InputDriver subDriver = new GroupDriver("sub1", subDriverList);

        final List<InputDriver> drivers = Collections.singletonList(subDriver);

        final GroupDriver group = new GroupDriver("group", drivers);
        assertNotNull(group);

        InputDriver[] inputs = group.getSubDrivers(false);
        assertNotNull(inputs);
        assertArrayEquals(new InputDriver[]{subDriver}, inputs);

        inputs = ((GroupDriver) subDriver).getSubDrivers(false);
        assertNotNull(inputs);
        assertThat(subDriverList, IsCollectionContaining.hasItems(inputs));

        final List<InputDriver> everyDriver = new ArrayList<>(drivers);
        everyDriver.addAll(sub2DriverList);
        everyDriver.addAll(subDriverList);

        inputs = group.getSubDrivers(true);
        assertNotNull(inputs);
        assertThat(everyDriver, IsCollectionContaining.hasItems(inputs));
    }

    @Test
    public void testSplittingDrivers() {
        final List<InputDriver> subDriverList = createSubInputDrivers("test");
        final InputDriver subDriver = new GroupDriver("sub1", subDriverList);

        final List<InputDriver> drivers = new ArrayList<>(2);
        drivers.add(subDriver);
        drivers.add(new StringDriver("string1"));
        drivers.add(new RegexDriver("regex.?"));

        final GroupDriver group = new GroupDriver("group", drivers);
        assertNotNull(group);

        assertThat(group.convertDrivers(),
                IsCollectionContaining.hasItems("string1", "regex.?", "VG:sub1:test1:test2:test3:tes.?4"));

        final List<String> nonGroupDrivers = new ArrayList<>();
        final List<String> groupDrivers = new ArrayList<>();

        GroupDriver.convertDriversIntoDriversAndGroups(Arrays.asList(group.getSubDrivers(false)),
                nonGroupDrivers, groupDrivers);
        assertEquals("VG:sub1:test1:test2:test3:tes.?4", groupDrivers.get(0));
        assertThat(nonGroupDrivers, IsCollectionContaining.hasItems("string1", "regex.?"));
    }

    @Test
    public void driverEquals() {
        final InputDriver driver = createInputDriver("Test1", "test");
        assertTrue(driver.equals(driver));
        assertFalse(driver.equals(null));
        assertFalse(driver.equals(new Integer(1)));

        InputDriver other = createInputDriver("Test1", "test");
        assertTrue(driver.equals(other));

        other = createInputDriver("Feast", "test");
        assertFalse(driver.equals(other));

        other = new RegexDriver("Test1");
        assertFalse(driver.equals(other));
    }

    @Test
    public void testRecursiveSubInputsAreNotDuplicated() {
        final List<InputDriver> subDriverList = createSubInputDrivers("test");
        final List<InputDriver> subSubDriverList = createSubInputDrivers("testSub");
        final InputDriver subSubDriver = new GroupDriver("sub1.1", subSubDriverList);
        subDriverList.add(subSubDriver);
        final InputDriver subDriver = new GroupDriver("sub1", subDriverList);
        subSubDriverList.add(subDriver);

        ((GroupDriver) subDriver).setSubValues(subDriverList);
        ((GroupDriver) subSubDriver).setSubValues(subSubDriverList);

        final List<InputDriver> drivers = Arrays.asList(subDriver, subSubDriver);
        final List<InputDriver> inputs = GroupDriver.getSubInputDrivers(drivers, true);
        assertNotNull(inputs);
        inputs.forEach(input -> assertThat(1, Matchers.equalTo(Collections.frequency(inputs, input))));
    }

    @Test
    public void testSameNameOfValueGroupAsStringInGroup() {
        final List<InputDriver> subDriverList = createSubInputDrivers("test");
        subDriverList.add(new StringDriver("test"));

        final GroupDriver group = new GroupDriver("test", subDriverList);
        assertNotNull(group);
        assertTrue(group.evaluate("test"));

        final List<InputDriver> inputs = GroupDriver.getSubInputDrivers(subDriverList, true);
        assertNotNull(inputs);
    }
}