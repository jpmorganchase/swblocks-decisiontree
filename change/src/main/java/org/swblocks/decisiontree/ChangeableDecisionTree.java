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

package org.swblocks.decisiontree;

import java.time.Instant;

import org.swblocks.decisiontree.change.domain.Audit;
import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.builder.ChangeBuilder;
import org.swblocks.decisiontree.domain.ChangeApplier;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;
import org.swblocks.jbl.util.DateRange;
import org.swblocks.jbl.util.retry.ActionRetrier;
import org.swblocks.jbl.util.retry.Retrier;

/**
 * Extends the {@link DecisionTree} access class to allow for modification to the underlying ruleset.
 *
 * <p>Provides additional access method to modify the underlying data in the {@link DecisionTreeRuleSet}.
 */
public class ChangeableDecisionTree extends DecisionTree {
    private final Persister persister;

    private ChangeableDecisionTree(final Loader<DecisionTreeRuleSet> loader, final DecisionTreeType type,
                                   final DecisionTreeRuleSet decisionTreeRuleSet,
                                   final Persister persister) {
        super(loader, type, decisionTreeRuleSet);
        this.persister = persister;
    }

    /**
     * Creates an instance of the {@link ChangeableDecisionTree} with the specified {@link Loader}
     *
     * @param loader    {@link Loader} to use to instantiate the underlying {@link DecisionTreeRuleSet}
     * @param type      {@link DecisionTreeType} Type of evaluation tree to create.
     * @param persister Optional {@link Persister} to write the new rule set to
     * @return instance of {@link ChangeableDecisionTree}
     */
    public static ChangeableDecisionTree instanceOf(final Loader<DecisionTreeRuleSet> loader,
                                                    final DecisionTreeType type,
                                                    final Persister persister) {
        final ActionRetrier<DecisionTreeRuleSet> retrier = Retrier.createNonRetrier();
        final Result<DecisionTreeRuleSet> result = retrier.run(loader, booleanResult -> false);
        EhSupport.checkResult(result);

        return new ChangeableDecisionTree(loader, type, result.getData(), persister);
    }

    /**
     * Creates a new Change Builder for modifying the {@link DecisionTreeRuleSet}, using the {@link Builder}.
     *
     * @param user  User who is creating the change
     * @param range {@link DateRange} the change is to apply against.
     * @return A Change Builder to modify the Rules and/or ValueGroups
     */
    public Builder<ChangeBuilder, Change> createChange(final String user, final DateRange range) {
        final Builder<ChangeBuilder, Change> builder = ChangeBuilder.creator(this.ruleSet);

        builder.with(ChangeBuilder::audit, new Audit(user, Instant.now(), null, null));
        builder.with(ChangeBuilder::changeRange, range);
        return builder;
    }

    /**
     * Applies the change to the {@link DecisionTreeRuleSet}.
     *
     * @param builder  Change builder containing the Rule and/or ValueGroup changes.
     * @param approver User who is approving this change.
     * @return The generated {@link Change} from the {@link Builder}
     */
    public Change applyChange(final Builder<ChangeBuilder, Change> builder, final String approver) {
        builder.with(ChangeBuilder::audit, new Audit(null, null, approver, Instant.now()));
        final Change change = builder.build();
        ChangeApplier.apply(this.ruleSet, change);
        initialiseRootNode();

        if (this.persister != null) {
            this.persister.put(this.ruleSet);
        }
        return change;
    }
}
