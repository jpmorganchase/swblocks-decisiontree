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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.swblocks.jbl.collections.CollectionUtils;
import org.swblocks.jbl.eh.EhSupport;

/**
 * Domain object for a Change Set that can contain many {@link Change} objects.
 */
public final class ChangeSet {
    private final UUID id;
    private final String name;
    private final Set<Change> changes = new HashSet<>();

    /**
     * Constructor initialising a change set.
     *
     * @param id      the id of the change set
     * @param name    the name of the change set
     * @param changes the collection of {@link Change}
     */
    public ChangeSet(final UUID id, final String name, final Set<Change> changes) {
        EhSupport.ensureArg(id != null && name != null && CollectionUtils.isNotEmpty(changes),
                "Change set cannot have any null values");
        this.id = id;
        this.name = name;
        this.changes.addAll(changes);
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Set<Change> getChanges() {
        return Collections.unmodifiableSet(this.changes);
    }

    public boolean addChange(final Change change) {
        EhSupport.ensureArg(change != null, "Cannot add a null change");
        return this.changes.add(change);
    }

    public boolean removeChange(final Change change) {
        return this.changes.remove(change);
    }
}
