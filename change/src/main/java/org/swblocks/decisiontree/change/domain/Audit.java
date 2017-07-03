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
import java.util.Objects;

/**
 * Class storing audit information for a {@link Change}.
 */
public final class Audit {
    private final String initiator;
    private final Instant initiatorTime;
    private final String authoriser;
    private final Instant authoriserTime;

    /**
     * Constructor initialising audit information for a {@link Change}.
     *
     * @param initiator      the change initiator
     * @param initiatorTime  the time when the change was initiated
     * @param authoriser     the change authoriser
     * @param authoriserTime the time at which the change was authorised
     */
    public Audit(final String initiator,
                 final Instant initiatorTime,
                 final String authoriser,
                 final Instant authoriserTime) {
        this.initiator = initiator;
        this.initiatorTime = initiatorTime;
        this.authoriser = authoriser;
        this.authoriserTime = authoriserTime;
    }

    public String getInitiator() {
        return this.initiator;
    }

    public Instant getInitiatorTime() {
        return this.initiatorTime;
    }

    public String getAuthoriser() {
        return this.authoriser;
    }

    public Instant getAuthoriserTime() {
        return this.authoriserTime;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final Audit audit = (Audit) other;

        return Objects.equals(this.initiator, audit.getInitiator()) &&
                Objects.equals(this.initiatorTime, audit.getInitiatorTime()) &&
                Objects.equals(this.authoriser, audit.getAuthoriser()) &&
                Objects.equals(this.authoriserTime, audit.getAuthoriserTime());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.initiator) + Objects.hashCode(this.initiatorTime) +
                Objects.hashCode(this.authoriser) + Objects.hashCode(this.authoriserTime);
    }
}
