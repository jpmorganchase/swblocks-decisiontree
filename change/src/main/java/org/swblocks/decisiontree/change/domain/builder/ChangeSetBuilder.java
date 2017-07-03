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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.swblocks.decisiontree.change.domain.Change;
import org.swblocks.decisiontree.change.domain.ChangeSet;
import org.swblocks.jbl.builders.Builder;
import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;

/**
 * ChangeSetBuilder groups a set of {@link Change} in a new {@link ChangeSet}.
 *
 * <p>Example usage:
 * <blockquote><pre>
 *  public ChangeSet createChangeSet(List&lt;Change&gt; changes) {
 *      final Builder&lt;ChangeSetBuilder, ChangeSet&gt; builder = ChangeSetBuilder.creator("MyChangeSet");
 *      builder.with(ChangeSetBuilder::changes, changes);
 *
 *      return builder.build();
 *  }
 * </pre></blockquote>
 */
public final class ChangeSetBuilder {
    private final String name;
    private final Set<Change> changes = new HashSet<>();

    private ChangeSetBuilder(final String name) {
        EhSupport.ensureArg(name != null && !name.trim().isEmpty(),
                "Change name cannot be missing from a change");
        this.name = name;
    }

    private static Supplier<ChangeSetBuilder> create(final String name) {
        return () -> new ChangeSetBuilder(name);
    }

    private static Predicate<ChangeSetBuilder> validate() {
        return builder -> builder.name != null && CollectionUtils.isNotEmpty(builder.changes);
    }

    /**
     * Static method to create the {@link Builder} with this class as the domain builder and the {@link ChangeSet} class
     * returned as the result.
     *
     * @param name the name of the change set
     * @return the {@code Builder} used to build the {@code Change}
     */
    public static Builder<ChangeSetBuilder, ChangeSet> creator(final String name) {
        return Builder.instanceOf(ChangeSetBuilder.create(name), ChangeSetBuilder.validate(),
                ChangeSetBuilder::builds);
    }

    /**
     * Static method that builds the change set.
     *
     * @param builder the builder
     * @return the {@code ChangeSet}
     */
    private static ChangeSet builds(final ChangeSetBuilder builder) {
        return new ChangeSet(UUID.randomUUID(), builder.name, builder.changes);
    }

    /**
     * Method to add a single {@link Change} to the {@link ChangeSetBuilder#changes} collection.
     *
     * @param change the change to add
     * @return this builder
     */
    public ChangeSetBuilder change(final Change change) {
        if (change != null) {
            this.changes.add(change);
        }
        return this;
    }

    /**
     * Method to a add a collection of {@link Change} to the {@link ChangeSetBuilder#changes} collection.
     *
     * @param changes the collection of changes
     * @return this builder
     */
    public ChangeSetBuilder changes(final Collection<Change> changes) {
        if (CollectionUtils.isNotEmpty(changes)) {
            this.changes.addAll(changes);
        }
        return this;
    }
}
