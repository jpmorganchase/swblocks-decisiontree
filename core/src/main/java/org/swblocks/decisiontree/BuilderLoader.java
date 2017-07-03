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

import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleSetBuilder;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.eh.Result;

/**
 * Class for wrapping a {@link RuleSetBuilder} as a {@link Loader} for use in the {@link DecisionTree} access class.
 */
public class BuilderLoader implements Loader<DecisionTreeRuleSet> {
    private final Builder<RuleSetBuilder, DecisionTreeRuleSet> builder;

    private BuilderLoader(final Builder<RuleSetBuilder, DecisionTreeRuleSet> builder) {
        this.builder = builder;
    }

    /**
     * Create a {@link Loader} instance to load from a {@link RuleSetBuilder}.
     *
     * @param builder The builder to use to create the {@link DecisionTreeRuleSet} when loading.
     * @return instance of the {@link BuilderLoader}
     */
    public static BuilderLoader instanceOf(final Builder<RuleSetBuilder, DecisionTreeRuleSet> builder) {
        return new BuilderLoader(builder);
    }

    /**
     * @return {@link Result} wrapping the generated {@link DecisionTreeRuleSet}.
     */
    @Override
    public Result<DecisionTreeRuleSet> get() {
        try {
            return Result.success(this.builder.build());
        } catch (final Exception exception) {
            return Result.failure(() -> exception);
        }
    }

    /**
     * Test if the load should be retried on failure.  For {@link BuilderLoader} we would never retry.
     *
     * @param result {@link Result} from bulding the ruleset.
     * @return hardcoded to false for this class.
     */
    @Override
    public boolean test(final Result<DecisionTreeRuleSet> result) {
        return false;
    }
}
