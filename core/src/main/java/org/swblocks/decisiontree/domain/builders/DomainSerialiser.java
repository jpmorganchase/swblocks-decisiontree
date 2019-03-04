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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.agrona.Strings;
import org.swblocks.decisiontree.domain.DriverCache;
import org.swblocks.decisiontree.tree.DateRangeDriver;
import org.swblocks.decisiontree.tree.GroupDriver;
import org.swblocks.decisiontree.tree.InputDriver;
import org.swblocks.decisiontree.tree.InputValueType;
import org.swblocks.decisiontree.tree.IntegerRangeDriver;
import org.swblocks.decisiontree.tree.RegexDriver;
import org.swblocks.decisiontree.tree.StringDriver;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.util.Range;

/**
 * Utility methods to convert the DecisionTree domain objects to and from their string format.
 *
 * <p>This is used for retrieving and storing in Json or other persistent stores.
 */
public final class DomainSerialiser {
    private static final Function<String, InputValueType> INPUT_VALUE_TYPE_FUNCTION = driverValue -> {
        if (driverValue.startsWith(GroupDriver.VG_PREFIX)) {
            return InputValueType.VALUE_GROUP;
        } else if (driverValue.startsWith(DateRangeDriver.DR_PREFIX)) {
            return InputValueType.DATE_RANGE;
        } else if (driverValue.startsWith(IntegerRangeDriver.IR_PREFIX)) {
            return InputValueType.INTEGER_RANGE;
        } else if (driverValue.startsWith(RegexDriver.REGEX_PREFIX) ||
                driverValue.contains(".?") || driverValue.contains(".*")) {
            return InputValueType.REGEX;
        }
        return InputValueType.STRING;
    };
    private static final Integer MIN_INTEGER = new Integer(Integer.MIN_VALUE);
    private static final Integer MAX_INTEGER = new Integer(Integer.MAX_VALUE);

    /**
     * Private constructor to enforce static use.
     */
    private DomainSerialiser() {
    }

    /**
     * Converts the List of {@link InputDriver} into a String form for serialising out.
     *
     * <p>The order of the drivers is maintained.
     *
     * @param drivers Array of {@link InputDriver} to convert
     * @return String form of inputs.
     */
    public static List<String> convertDrivers(final InputDriver[] drivers) {
        return Arrays.stream(drivers).map(InputDriver::toString).collect(Collectors.toList());
    }

    /**
     * Converts the List of {@link InputDriver} into a String form for serialising out.
     *
     * <p>The order of the drivers is maintained.
     *
     * @param drivers Array of {@link InputDriver} to convert
     * @return String form of inputs.
     */
    public static List<String> convertDriversWithSubGroups(final List<InputDriver> drivers) {
        final List<String> driverList = new ArrayList<>(drivers.size());
        GroupDriver.convertDriversIntoDriversAndGroups(drivers, driverList, driverList);
        return driverList;
    }

    /**
     * Converts the Map of Output name value pairs into a String form for serialising out.
     *
     * @param outputMap Map of Strings name value pairs
     * @return List of outputs
     */
    public static List<String> convertOutputs(final Map<String, String> outputMap) {
        return outputMap.entrySet().stream().map(
                entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.toList());
    }

    /**
     * Converts the list of Output name value pairs to a map format.
     *
     * @param outputs the list of name value pairs
     * @return the map of outputs
     */
    public static Map<String, String> convertOutputs(final List<String> outputs) {
        return outputs.stream().filter(item -> item.contains(":"))
                .map(item -> item.split(":", 2)).collect(Collectors.toMap(item -> item[0], item -> item[1],
                        (key, value) -> key));
    }

    /**
     * Creates a {@link Supplier} to provide an implementation of an {@link InputDriver} for the serialised String.
     *
     * @param currentDriver Serialised form of the driver definition.
     * @param cache         {@link DriverCache} cache of {@link InputDriver}
     * @return {@link Supplier} of {@link InputDriver}
     */
    public static Supplier<InputDriver> createInputDriver(final String currentDriver, final DriverCache cache) {
        return () -> {
            final InputValueType type = INPUT_VALUE_TYPE_FUNCTION.apply(currentDriver);
            final InputDriver inputDriver;
            if (InputValueType.VALUE_GROUP.equals(type)) {
                inputDriver = getValueGroupDriver(currentDriver, cache, type);
            } else {
                inputDriver = getSingleDriver(currentDriver, cache, type);
            }
            return inputDriver;
        };
    }

    private static InputDriver getSingleDriver(final String currentDriver,
                                               final DriverCache cache,
                                               final InputValueType type) {
        InputDriver inputDriver = cache.get(currentDriver, type);

        if (inputDriver == null) {
            switch (type) {
                case STRING:
                    inputDriver = new StringDriver(currentDriver);
                    break;
                case REGEX:
                    inputDriver = new RegexDriver(currentDriver.replace(RegexDriver.REGEX_PREFIX + ":", ""));
                    break;
                case DATE_RANGE:
                    final StringTokenizer tokenizer = new StringTokenizer(
                            currentDriver.replace(DateRangeDriver.DR_PREFIX + ":",""), "|", false);
                    EhSupport.ensure(tokenizer.countTokens() == 2, "DateRange driver incorrectly formatted");
                    inputDriver = new DateRangeDriver(currentDriver, new Range<>(Instant.parse(tokenizer.nextToken()),
                            Instant.parse(tokenizer.nextToken())));
                    break;
                case INTEGER_RANGE:
                    final StringTokenizer intTokenizer = new StringTokenizer(
                            currentDriver.replace(IntegerRangeDriver.IR_PREFIX + ":", ""), "|", true);
                    EhSupport.ensure(intTokenizer.countTokens() == 2 || intTokenizer.countTokens() == 3,
                            "Integer Range driver incorrectly formatted");
                    if (intTokenizer.countTokens() == 3) {
                        final String minValue = intTokenizer.nextToken();
                        intTokenizer.nextToken();
                        final String maxValue = intTokenizer.nextToken();
                        inputDriver = new IntegerRangeDriver(currentDriver, new Range<>(Integer.parseInt(minValue),
                                Integer.parseInt(maxValue)));
                    } else {
                        final String firstToken = intTokenizer.nextToken();
                        if ("|".equals(firstToken)) {
                            inputDriver = new IntegerRangeDriver(currentDriver, new Range<>(MIN_INTEGER,
                                    Integer.parseInt(intTokenizer.nextToken())));
                        } else {
                            inputDriver = new IntegerRangeDriver(currentDriver,
                                    new Range<>(Integer.parseInt(firstToken),
                                    MAX_INTEGER));
                        }
                    }
                    break;
                default:
                    inputDriver = null;
                    break;
            }
            if (inputDriver != null) {
                cache.put(inputDriver);
            }
        }
        return inputDriver;
    }

    private static InputDriver getValueGroupDriver(final String currentDriver,
                                                   final DriverCache cache,
                                                   final InputValueType type) {
        final String[] tokensGroup = currentDriver.split(GroupDriver.VG_PREFIX);
        final String value = tokensGroup[1].split(":")[0];

        InputDriver inputDriver = cache.get(value, type);
        if (inputDriver == null) {
            final List<InputDriver> topLevelDriver = new ArrayList<>(16);
            for (int i = 2; i < tokensGroup.length; i++) {
                if (Strings.isEmpty(tokensGroup[i])) {
                    continue;
                }
                final String name = tokensGroup[i].split(":")[0];

                InputDriver subGroup = cache.get(name, type);
                if (subGroup == null) {
                    final List<InputDriver> subDrivers = getGroupDrivers(tokensGroup[i], cache);
                    subGroup = new GroupDriver(name, subDrivers);
                    topLevelDriver.add(subGroup);
                    cache.put(subGroup);
                }
            }

            topLevelDriver.addAll(getGroupDrivers(tokensGroup[1], cache));
            inputDriver = new GroupDriver(value, topLevelDriver);
            cache.put(inputDriver);
        }
        return inputDriver;
    }

    private static List<InputDriver> getGroupDrivers(final String currentToken,
                                                     final DriverCache cache) {
        final List<InputDriver> drivers = new ArrayList<>();

        if (Strings.isEmpty(currentToken)) {
            return Collections.emptyList();
        }

        final String[] tokens = currentToken.split(":", 0);

        // Position 0 contains a name
        for (int i = 1; i < tokens.length; i++) {
            final String tokenValue = tokens[i];
            final InputValueType tokenType = INPUT_VALUE_TYPE_FUNCTION.apply(tokenValue);
            final InputDriver driver = getSingleDriver(tokenValue, cache, tokenType);
            drivers.add(driver);
        }
        return drivers;
    }
}
