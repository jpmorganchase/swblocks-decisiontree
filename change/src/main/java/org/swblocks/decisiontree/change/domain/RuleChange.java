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

import org.swblocks.decisiontree.domain.DecisionTreeRule;

/**
 * Domain class for a RuleChange.
 */
public final class RuleChange {
    private final Type type;
    private final DecisionTreeRule rule;

    public RuleChange(final Type type, final DecisionTreeRule rule) {
        this.type = type;
        this.rule = rule;
    }

    public Type getType() {
        return this.type;
    }

    public DecisionTreeRule getRule() {
        return this.rule;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final RuleChange otherChange = (RuleChange) other;
        return this.type == otherChange.getType() && this.getRule().equals(otherChange.getRule());
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() + this.rule.hashCode();
    }
}
