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

import org.swblocks.decisiontree.domain.ValueGroup;

/**
 * Domain object for a ValueGroup.
 */
public final class ValueGroupChange {
    private final Type type;
    private final ValueGroup valueGroup;

    public ValueGroupChange(final Type type, final ValueGroup valueGroup) {
        this.type = type;
        this.valueGroup = valueGroup;
    }

    public Type getType() {
        return this.type;
    }

    public ValueGroup getValueGroup() {
        return this.valueGroup;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final ValueGroupChange change = (ValueGroupChange) other;
        return this.type == change.getType() && this.valueGroup.equals(change.getValueGroup());
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() + this.valueGroup.hashCode();
    }
}

