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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test cases for {@link CsvParser}.
 */
public class CsvParserTest {
    @Test
    public void testReadCsvFileAsAReader() throws IOException {
        try (InputStream fileInputStream = new FileInputStream(
                getClass().getClassLoader().getResource("commissions.csv").getFile())) {

            final DecisionTreeRuleSet decisionTreeRuleSet = CsvParser.instanceOf("commissions")
                    .parseRuleSet(fileInputStream);
            assertNotNull(decisionTreeRuleSet);
            assertEquals(4, decisionTreeRuleSet.getRules().size());
            assertEquals(Arrays.asList("EXMETHOD", "EXCHANGE", "PRODUCT", "REGION", "ASSET"),
                    decisionTreeRuleSet.getDriverNames());

        }
    }

    @Test
    public void testMissingDriversOnARule() throws IOException {
        final String testFile = "\nEXMETHOD,EXCHANGE,PRODUCT,REGION,ASSET\n\"*\", \"CME\", \"S&P\", \"*\"";
        try (InputStream fileInputStream = new ByteArrayInputStream(testFile.getBytes())) {

            final DecisionTreeRuleSet decisionTreeRuleSet = CsvParser.instanceOf("commissions")
                    .parseRuleSet(fileInputStream);
            assertNotNull(decisionTreeRuleSet);
            assertEquals(0, decisionTreeRuleSet.getRules().size());
        }
    }

    @Test
    public void testRuleCreatedCorrectly() throws IOException {
        final String testFile = "\nEXMETHOD,EXCHANGE,PRODUCT,REGION,ASSET\n\"*\", \"CME\", " +
                "\"S&P\", \"*\", \"INDEX\" , \"Rate:1.2\"";
        try (InputStream fileInputStream = new ByteArrayInputStream(testFile.getBytes())) {

            final DecisionTreeRuleSet decisionTreeRuleSet = CsvParser.instanceOf("commissions")
                    .parseRuleSet(fileInputStream);
            assertNotNull(decisionTreeRuleSet);
            // Rule is not created.
            assertEquals(1, decisionTreeRuleSet.getRules().size());
            final DecisionTreeRule rule = decisionTreeRuleSet.getRules().values().iterator().next();
            Assert.assertEquals(Arrays.asList(new String[]{"*", "CME", "S&P", "*", "INDEX"}),
                    Arrays.stream(rule.getDrivers()).map(driver -> driver.getValue())
                            .collect(Collectors.toList()));
            assertEquals(Collections.singletonMap("Rate", "1.2"), rule.getOutputs());
            assertEquals(DecisionTreeRule.EPOCH, rule.getStart());
            assertEquals(DecisionTreeRule.MAX, rule.getEnd());
        }
    }
}
