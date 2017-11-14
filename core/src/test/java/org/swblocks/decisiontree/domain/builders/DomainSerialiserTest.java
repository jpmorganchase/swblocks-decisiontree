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

package org.swblocks.decisiontree.domain.builders;

import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Ignore;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.tree.StringDriver;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.test.utils.JblTestClassUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DomainSerialiser}.
 */
public class DomainSerialiserTest {
    @Test
    public void hasPrivateConstructor() {
        JblTestClassUtils.assertConstructorIsPrivate(DomainSerialiser.class);
    }

    @Test
    public void convertStringToStringDriver() {
        testStringConversionToInputDriver("TestString", InputValueType.STRING);
    }

    @Test
    public void convertStringToComplexRegExDriver() {
        testStringConversionToInputDriver(InputBuilder.RegExInput("^[A-Z]{1,2}[A-Z][0-9]{1,2}$"),
                "^[A-Z]{1,2}[A-Z][0-9]{1,2}$", InputValueType.REGEX);
    }

    @Test
    public void convertStringToRegExDriver() {
        testStringConversionToInputDriver("Tes.?", InputValueType.REGEX);
    }

    @Test
    public void convertStringToDateRangeDriver() {
        testStringConversionToInputDriver("DR:2017-07-04T16:00:00.000Z|2017-07-10T16:00:00.000Z",
                InputValueType.DATE_RANGE);
    }

    @Test
    public void convertStringToIntegerRangeDriver() {
        testStringConversionToInputDriver("IR:500|12345",
                InputValueType.INTEGER_RANGE);
    }

    @Test
    public void convertStringToDateRangeDriverAndTestDates() {
        final DriverCache cache = new DriverCache();
        final String testString = "DR:2017-07-04T16:00:00.000Z|2017-07-10T16:00:00.000Z";

        final Supplier<InputDriver> rangeSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver dateRangeDriver = rangeSupplier.get();
        assertNotNull(dateRangeDriver);
        assertThat(dateRangeDriver.getType(), is(InputValueType.DATE_RANGE));
        assertThat(dateRangeDriver.getValue(), is(testString));
        assertThat(dateRangeDriver.evaluate("2017-07-04T16:00:00.000Z"), is(true));
        assertThat(dateRangeDriver.evaluate("2017-07-10T16:00:00.000Z"), is(false));
    }

    @Test
    public void convertStringToIntegerRangeAndTestData() {
        final DriverCache cache = new DriverCache();
        final String testString = "IR:100|12345";
        final Supplier<InputDriver> rangeSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver intRangeDriver = rangeSupplier.get();
        assertNotNull(intRangeDriver);
        assertThat(intRangeDriver.getType(), is(InputValueType.INTEGER_RANGE));
        assertThat(intRangeDriver.getValue(), is(testString));
        assertThat(intRangeDriver.evaluate("123"), is(true));
        assertThat(intRangeDriver.evaluate("54321"), is(false));
    }

    @Test
    public void convertStringToIntegerRangeWithMax() {
        final DriverCache cache = new DriverCache();
        final String testString = "IR:100|";
        final Supplier<InputDriver> rangeSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver intRangeDriver = rangeSupplier.get();
        assertNotNull(intRangeDriver);
        assertThat(intRangeDriver.getType(), is(InputValueType.INTEGER_RANGE));
        assertThat(intRangeDriver.getValue(), is(testString));
        assertThat(intRangeDriver.evaluate("123"), is(true));
        Integer maxInt = new Integer(Integer.MAX_VALUE);
        assertThat(intRangeDriver.evaluate(maxInt.toString()), is(false));
        Integer almostMaxInt = new Integer(Integer.MAX_VALUE - 1);
        assertThat(intRangeDriver.evaluate(almostMaxInt.toString()), is(true));
    }

    @Test
    public void convertStringToIntegerRangeWithMin() {
        final DriverCache cache = new DriverCache();
        final String testString = "IR:|100";
        final Supplier<InputDriver> rangeSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver intRangeDriver = rangeSupplier.get();
        assertNotNull(intRangeDriver);
        assertThat(intRangeDriver.getType(), is(InputValueType.INTEGER_RANGE));
        assertThat(intRangeDriver.getValue(), is(testString));
        assertThat(intRangeDriver.evaluate("12"), is(true));
        Integer minInt = new Integer(Integer.MIN_VALUE);
        assertThat(intRangeDriver.evaluate(minInt.toString()), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void convertStringToIntegerRangeWithMissingDeliminator() {
        final DriverCache cache = new DriverCache();
        final String testString = "IR:1223";
        final Supplier<InputDriver> rangeSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver intRangeDriver = rangeSupplier.get();
    }

    private void testStringConversionToInputDriver(String inputString,
                                                   InputValueType expectedType) {
        testStringConversionToInputDriver(inputString, inputString, expectedType);
    }

    private void testStringConversionToInputDriver(String inputString, String resultsString,
                                                   InputValueType expectedType) {
        final DriverCache cache = new DriverCache();
        final Supplier<InputDriver> driverSupplier = DomainSerialiser.createInputDriver(inputString, cache);
        final InputDriver driver = driverSupplier.get();
        assertNotNull(driver);
        assertEquals(resultsString, driver.getValue());
        assertEquals(expectedType, driver.getType());
        assertEquals(driver, cache.get(resultsString, expectedType));

        final List<String> serialisedDrivers = DomainSerialiser.convertDrivers(new InputDriver[]{driver});
        assertNotNull(serialisedDrivers);
        assertEquals(1, serialisedDrivers.size());
        assertEquals(resultsString, serialisedDrivers.get(0));
    }

    @Test (expected = IllegalStateException.class)
    public void failInvalidStringToDateRangeDriver() {
        DomainSerialiser.createInputDriver("DR:2017-07-04T16:00:00.000Z2017-07-10T16:00:00.000Z",
                new DriverCache()).get();
    }

    @Test (expected = DateTimeParseException.class)
    public void failInvalidDateStringToDateRangeDriver() {
        DomainSerialiser.createInputDriver("DR:2017-04T16:00:00.000Z|2017-07-10T16:00:00.000Z",
                new DriverCache()).get();
    }

    @Test
    public void convertStringToGroupDriver() {
        final DriverCache cache = new DriverCache();
        final String testString = "VG:TestGroup:Test1:Test2:Test3";
        final Supplier<InputDriver> groupSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver groupDriver = groupSupplier.get();
        assertNotNull(groupDriver);
        assertEquals("TestGroup", groupDriver.getValue());
        assertEquals(InputValueType.VALUE_GROUP, groupDriver.getType());
        assertEquals(groupDriver, cache.get("TestGroup", InputValueType.VALUE_GROUP));
        final InputDriver[] drivers = ((GroupDriver) groupDriver).getSubDrivers(false);
        final InputDriver[] expected = {new StringDriver("Test1"), new StringDriver("Test2"),
                new StringDriver("Test3")};
        assertThat(Arrays.asList(drivers), IsCollectionContaining.hasItems(expected));

        List<String> serialisedDrivers = DomainSerialiser.convertDriversWithSubGroups(Arrays.asList(groupDriver));
        assertNotNull(serialisedDrivers);
        assertEquals(1, serialisedDrivers.size());
        assertEquals(testString, serialisedDrivers.get(0));

        serialisedDrivers = DomainSerialiser.convertDrivers(new InputDriver[]{groupDriver});
        assertNotNull(serialisedDrivers);
        assertEquals(1, serialisedDrivers.size());
        assertEquals(groupDriver.toString(), serialisedDrivers.get(0));
    }

    @Test
    public void convertBrokenEmptyStrings() {
        final DriverCache cache = new DriverCache();
        final String testString = "VG:1::";
        final Supplier<InputDriver> groupSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver groupDriver = groupSupplier.get();
        assertNotNull(groupDriver);
        final InputDriver[] drivers = ((GroupDriver) groupDriver).getSubDrivers(false);
        assertEquals(0, drivers.length);
    }

    @Test
    public void testEmptyTokensInStringToGroupDriver() {
        final DriverCache cache = new DriverCache();
        final String testString = "VG:TestGroup:Test1::Test2:Test3:VG:SubGroup::Test4:Test5";
        final Supplier<InputDriver> groupSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver groupDriver = groupSupplier.get();
        assertNotNull(groupDriver);
        final InputDriver[] drivers = ((GroupDriver) groupDriver).getSubDrivers(false);
        final InputDriver[] expectedList = new InputDriver[]{
                new StringDriver("Test1"), new StringDriver(""), new StringDriver("Test2"),
                new StringDriver("Test3"),
                new GroupDriver("SubGroup",
                        Arrays.asList(new StringDriver(""), new StringDriver("Test4"),
                                new StringDriver("Test5")))};
        assertEquals(expectedList.length, drivers.length);
        assertThat(Arrays.asList(drivers), IsCollectionContaining.hasItems(expectedList));

        List<String> serialisedDrivers = DomainSerialiser.convertDriversWithSubGroups(Arrays.asList(groupDriver));
        assertNotNull(serialisedDrivers);
        assertEquals(1, serialisedDrivers.size());
        assertEquals(testString, serialisedDrivers.get(0));

        serialisedDrivers = DomainSerialiser.convertDrivers(new InputDriver[]{groupDriver});
        assertNotNull(serialisedDrivers);
        assertEquals(1, serialisedDrivers.size());
        assertEquals(groupDriver.toString(), serialisedDrivers.get(0));

    }

    @Test
    @Ignore("The support for a blank token at the end is not working.  Putting in test and will return to it.")
    public void testEmptyTokensAtEndOfGroupDriver() {
        final DriverCache cache = new DriverCache();
        final String testString = "VG:TestGroup:Test1:Test2:Test3::VG:SubGroup:Test4:Test5:";
        final Supplier<InputDriver> groupSupplier = DomainSerialiser.createInputDriver(testString, cache);
        final InputDriver groupDriver = groupSupplier.get();
        assertNotNull(groupDriver);
        final InputDriver[] drivers = ((GroupDriver) groupDriver).getSubDrivers(false);
        final InputDriver[] expectedList = new InputDriver[]{
                new StringDriver("Test1"), new StringDriver("Test2"), new StringDriver("Test3"),
                new StringDriver(""),
                new GroupDriver("SubGroup",
                        Arrays.asList(new StringDriver("Test4"), new StringDriver("Test5"),
                                new StringDriver("")))};
        assertEquals(expectedList.length, drivers.length);
        assertThat(Arrays.asList(drivers), IsCollectionContaining.hasItems(expectedList));

        final List<String> serialisedDrivers = DomainSerialiser.convertDrivers(new InputDriver[]{groupDriver});
        assertEquals(testString, serialisedDrivers.get(0));
    }

    @Test
    public void convertStringToGroupDriverWithSubGroups() {
        final DriverCache cache = new DriverCache();
        final Supplier<InputDriver> groupSupplier = DomainSerialiser.createInputDriver("VG:TestGroup:Test", cache);
        final InputDriver groupDriver = groupSupplier.get();
        assertNotNull(groupDriver);
        assertEquals("TestGroup", groupDriver.getValue());
        assertEquals(InputValueType.VALUE_GROUP, groupDriver.getType());
        assertEquals(groupDriver, cache.get("TestGroup", InputValueType.VALUE_GROUP));
    }

    @Test
    public void testConvertGroupDrivers() {
        final Builder<RuleBuilder, DecisionTreeRule> ruleBuilder = RuleBuilder.creator();
        final DriverCache cache = new DriverCache();
        final List<String> testInputs = Arrays.asList("VG:VG1:test1:test2:test3:test4", "singleTest",
                "VG:VG2:test10:test20:test30:test40:VG:VG3:test50:test9.?:test200.*");
        ruleBuilder.with(RuleBuilder::input, testInputs);
        ruleBuilder.with(RuleBuilder::cache, cache);
        ruleBuilder.with(RuleBuilder::setDriverCount, 3L);
        ruleBuilder.with(RuleBuilder::setId, new UUID(0, 1));
        ruleBuilder.with(RuleBuilder::output, Collections.singletonMap("outputDriver", "result"));
        final DecisionTreeRule rule = ruleBuilder.build();
        assertNotNull(rule);
        final InputDriver[] derivedInputDrivers = rule.getDrivers();

        List<String> result = DomainSerialiser.convertDriversWithSubGroups(Arrays.asList(derivedInputDrivers));
        assertNotNull(result);
        assertEquals(testInputs, result);

        result = DomainSerialiser.convertDrivers(derivedInputDrivers);
        assertNotNull(result);
        assertThat(result, IsCollectionContaining.hasItems("VG:VG1", "singleTest", "VG:VG2"));
    }

    @Test
    public void testOutputConversion() {
        final Map<String, String> outputMap = Stream.of(new AbstractMap.SimpleEntry<>("Driver1", "Value1"),
                new AbstractMap.SimpleEntry<>("Driver2", "Value2"),
                new AbstractMap.SimpleEntry<>("Driver3", "Value3"))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        assertThat(DomainSerialiser.convertOutputs(outputMap), IsCollectionContaining.hasItems(
                "Driver1:Value1", "Driver2:Value2", "Driver3:Value3"));
    }

    @Test
    public void testOutputConversionToMap() {
        final List<String> outputlist = new ArrayList<>();
        outputlist.add("Driver1:Value1");
        outputlist.add("Driver2:Value2");
        outputlist.add("Driver3:Value3");

        final Map<String, String> outputMap = DomainSerialiser.convertOutputs(outputlist);
        assertEquals(3, outputMap.size());
        assertThat(outputMap, IsMapContaining.hasEntry("Driver1", "Value1"));
        assertThat(outputMap, IsMapContaining.hasEntry("Driver2", "Value2"));
        assertThat(outputMap, IsMapContaining.hasEntry("Driver3", "Value3"));
    }

    @Test
    public void testInvalidOutputConversion() {
        final List<String> outputlist = new ArrayList<>();
        outputlist.add("Driver1:Value1");
        outputlist.add("Driver2Value2");
        outputlist.add("Driver3:Value3");

        final Map<String, String> outputMap = DomainSerialiser.convertOutputs(outputlist);
        assertEquals(2, outputMap.size());
        assertThat(outputMap, IsMapContaining.hasEntry("Driver1", "Value1"));
        assertThat(outputMap, IsMapContaining.hasEntry("Driver3", "Value3"));
    }
}