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

import java.util.Arrays;
import java.util.List;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.tree.RegexDriver;
import org.swblocks.decisiontree.tree.StringDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link DriverCache}.
 */
public class DriverCacheTest {
    @Test
    public void cacheObject() {
        final DriverCache cache = new DriverCache();

        final String value = "test";
        final InputDriver driver = new StringDriver(value);

        assertEquals(null, cache.get(value, InputValueType.STRING));

        cache.put(driver);
        assertTrue(cache.contains("test"));
        assertEquals(driver, cache.get(value, InputValueType.STRING));
    }

    @Test
    public void testFindByType() {
        final DriverCache cache = new DriverCache();
        final InputDriver stringDriver = new StringDriver("testString1");
        final InputDriver stringDriver2 = new StringDriver("testString2");
        final InputDriver regexDriver = new RegexDriver("tes.?");
        final InputDriver groupDriver = new GroupDriver("testGroup", Arrays.asList(
                new StringDriver("testSub1"), new StringDriver("testSub2")));
        cache.put(stringDriver);
        cache.put(stringDriver2);
        cache.put(regexDriver);
        cache.put(groupDriver);
        final List<InputDriver> regexResults = cache.findByInputDriverType(InputValueType.REGEX);
        assertNotNull(regexResults);
        assertEquals(regexDriver, regexResults.get(0));
        final List<InputDriver> groupDrivers = cache.findByInputDriverType(InputValueType.VALUE_GROUP);
        assertEquals(groupDriver, groupDrivers.get(0));
        final List<InputDriver> stringDrivers = cache.findByInputDriverType(InputValueType.STRING);
        assertThat(stringDrivers, IsCollectionContaining.hasItems(stringDriver, stringDriver2));
    }
}
