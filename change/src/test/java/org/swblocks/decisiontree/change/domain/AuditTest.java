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

package org.swblocks.decisiontree.change.domain;

import java.time.Instant;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.swblocks.jbl.test.utils.JblTestClassUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link Audit}.
 */
public class AuditTest {
    private final Map<String, Object> injectedValues = new HashMap<>();
    private Audit bean;

    @Before
    public void setup() {
        this.injectedValues.clear();
    }

    @Test
    public void testConstruction() {
        final Instant initiatorTime = Instant.now();
        final Instant authoriserTime = initiatorTime.plus(Period.ofWeeks(1));

        final Audit audit = new Audit("USER1", initiatorTime, "USER2", authoriserTime);
        setBean(audit);

        this.injectedValues.put("initiator", "USER1");
        this.injectedValues.put("initiatorTime", initiatorTime);
        this.injectedValues.put("authoriser", "USER2");
        this.injectedValues.put("authoriserTime", authoriserTime);

        JblTestClassUtils.assertGetterCorrectForConstructorInjection(this.injectedValues, getBean());
    }

    private Audit getBean() {
        return this.bean;
    }

    private void setBean(final Audit audit) {
        this.bean = audit;
    }

    @Test
    public void equalsCorrect() {
        final Instant now = Instant.now();
        final Audit audit = new Audit("USER1", now, "USER2", now);

        assertTrue(audit.equals(audit));
        assertFalse(audit.equals(null));
        assertFalse(audit.equals(Boolean.TRUE));

        Audit other = new Audit("USER1", now, "USER2", now);
        assertTrue(audit.equals(other));

        other = new Audit(null, now, "USER2", now);
        assertFalse(audit.equals(other));

        other = new Audit(null, null, "USER2", now);
        assertFalse(audit.equals(other));

        other = new Audit(null, null, null, now);
        assertFalse(audit.equals(other));

        other = new Audit(null, null, null, null);
        assertFalse(audit.equals(other));
    }

    @Test
    public void hashCodeCorrect() {
        final Instant now = Instant.now();
        final Audit audit = new Audit("USER1", now, "USER2", now);
        final Audit other = new Audit("USER1", now, "USER2", now);

        assertEquals(audit.hashCode(), other.hashCode());
    }
}
