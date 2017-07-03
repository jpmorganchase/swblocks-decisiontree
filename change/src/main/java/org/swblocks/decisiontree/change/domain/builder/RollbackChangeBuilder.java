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

package org.swblocks.decisiontree.change.domain.builder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.swblocks.decisiontree.change.domain.Audit;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.RuleChange;
import org.swblocks.decisiontree.change.domain.Type;
import org.swblocks.decisiontree.change.domain.ValueGroupChange;
import org.swblocks.decisiontree.domain.DecisionTreeRule;
import org.swblocks.decisiontree.domain.ValueGroup;
import org.swblocks.jbl.builders.Builder;

/**
 * RollbackChangeBuilder that returns a {@link Change} reverting a previous {@code Change}.
 *
 * <p>Example usage to produce a {@code Change} for rollback:
 * <blockquote><pre>
 *  public Change getRollBack(Change implemented,
 *                            Audit audit) {
 *      final Builder&lt;RollbackChangeBuilder, Change&gt; builder = RollbackChangeBuilder.creator();
 *
 *      builder.with(RollbackChangeBuilder::change, implemented);
 *      builder.with(ChangeBuilder::audit, audit);
 *
 *      return builder.build();
 *  }
 * </pre></blockquote>
 */
public final class RollbackChangeBuilder {
    private Change change;
    private Audit audit;
    private Instant activationTime;

    private static Supplier<RollbackChangeBuilder> create() {
        return RollbackChangeBuilder::new;
    }

    private static Predicate<RollbackChangeBuilder> validate() {
        return builder -> builder.change != null;
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the {@link Change} class
     * returned as the result.
     *
     * @return the {@link Builder} used to build the {@link Change}
     */
    public static Builder<RollbackChangeBuilder, Change> creator() {
        return Builder.instanceOf(RollbackChangeBuilder.create(), RollbackChangeBuilder.validate(),
                RollbackChangeBuilder::builds);
    }

    /**
     * Static method that builds the change object to that will be used in a rollback situation.
     *
     * @param builder the builder
     * @return the {@link Change}
     */
    private static Change builds(final RollbackChangeBuilder builder) {
        final Set<RuleChange> ruleChanges = new HashSet<>(1);
        final Change change = builder.change;
        for (final RuleChange ruleChange : change.getRuleChanges()) {
            final DecisionTreeRule rule = ruleChange.getRule();
            final DecisionTreeRule changed = new DecisionTreeRule(UUID.randomUUID(), rule.getRuleCode(),
                    rule.getDrivers(), rule.getOutputs(), rule.getStart(), rule.getEnd());

            switch (ruleChange.getType()) {
                case ORIGINAL:
                    ruleChanges.add(new RuleChange(Type.NEW, changed));
                    break;
                case NEW:
                    ruleChanges.add(new RuleChange(Type.ORIGINAL, changed));
                    break;
                default:
                    break;
            }
        }

        final Set<ValueGroupChange> valueGroupChanges = new HashSet<>(1);
        for (final ValueGroupChange valueGroupChange : change.getValueGroupChanges()) {
            final ValueGroup valueGroup = valueGroupChange.getValueGroup();
            final ValueGroup changed = new ValueGroup(UUID.randomUUID(), valueGroup.getName(), valueGroup.getValues(),
                    valueGroup.getRange());

            switch (valueGroupChange.getType()) {
                case ORIGINAL:
                    valueGroupChanges.add(new ValueGroupChange(Type.NEW, changed));
                    break;
                case NEW:
                    valueGroupChanges.add(new ValueGroupChange(Type.ORIGINAL, changed));
                    break;
                default:
                    break;
            }
        }

        return new Change(UUID.randomUUID(), change.getRuleSetName(), builder.activationTime, change.getChangeRange(),
                builder.audit, ruleChanges, valueGroupChanges);
    }

    /**
     * Method to set the change that will be used to create a rollback change.
     *
     * @param change the change that will be reverted
     * @return this builder
     */
    public RollbackChangeBuilder change(final Change change) {
        this.change = change;
        return this;
    }

    /**
     * Method to set the activation time.
     *
     * @param activationTime the time at which the change is activated.
     * @return this builder
     */
    public RollbackChangeBuilder date(final Instant activationTime) {
        if (activationTime != null) {
            this.activationTime = activationTime;
        }
        return this;
    }


    /**
     * Method to set the audit information in the change.
     *
     * @param audit the {@link Audit} object
     * @return this builder
     */
    public RollbackChangeBuilder audit(final Audit audit) {
        this.audit = audit;
        return this;
    }
}
