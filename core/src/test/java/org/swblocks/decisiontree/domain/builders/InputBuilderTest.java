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

import org.junit.Test;
import org.swblocks.jbl.test.utils.JblTestClassUtils;
import org.swblocks.jbl.util.Range;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link InputBuilder}.
 */
public class InputBuilderTest {
    @Test
    public void testPrivateConstructor() {
        assertTrue(JblTestClassUtils.assertConstructorIsPrivate(InputBuilder.class));
    }

    @Test
    public void testRegExInput() {
        assertThat(InputBuilder.regExInput(".RegEx"), is("RE:.RegEx"));
    }

    @Test
    public void testDateRangeInput() {
        final String startStr = "2013-03-28T00:00:00Z";
        final String endStr = "2015-06-01T10:50:00Z";
        final Range<Instant> range = new Range<>(Instant.parse(startStr), Instant.parse(endStr));
        assertThat(InputBuilder.dateRangeInput(range), is("DR:" + startStr + "|" + endStr));
    }

    @Test
    public void testIntegerRangeInput() {
        assertThat(InputBuilder.integerRangeInput(new Range<>(100, 500)), is("IR:100|500"));
    }
}