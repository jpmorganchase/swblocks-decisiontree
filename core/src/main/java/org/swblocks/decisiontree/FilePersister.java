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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilderSerialiser;
import org.swblocks.jbl.eh.EhSupport;

/**
 * Class to save a {@link DecisionTreeRuleSet} to the filesystem, either zipped or basic.
 */
public class FilePersister implements Persister {
    private static final String ZIP = ".zip";
    private static final String JSON = ".json";
    private final String fileName;
    private final RuleBuilderSerialiser serialiser;

    private FilePersister(final String path, final String name, final String extension,
                          final RuleBuilderSerialiser serialiser) {
        this.fileName = (path != null && !path.isEmpty() ? path + File.separator : "") + name + extension;
        this.serialiser = serialiser;
    }

    public static FilePersister zippedInstanceOf(final String path, final String name,
                                                 final RuleBuilderSerialiser serialiser) {
        return new FilePersister(path, name, ZIP, serialiser);
    }

    public static FilePersister instanceOf(final String path, final String name,
                                           final RuleBuilderSerialiser serialiser) {
        return new FilePersister(path, name, JSON, serialiser);
    }

    @Override
    public void put(final DecisionTreeRuleSet ruleSet) {
        if (this.fileName.endsWith(ZIP)) {
            writeZippedFile(ruleSet);
        } else {
            writeFile(ruleSet);
        }
    }

    private void writeZippedFile(final DecisionTreeRuleSet ruleSet) {
        EhSupport.ensureArg(ruleSet != null, "Cannot persist a null ruleset");
        EhSupport.propagate(() -> {
            try (
                    final OutputStream stream = new BufferedOutputStream(
                            Files.newOutputStream(Paths.get(this.fileName), StandardOpenOption.CREATE));
                    final CheckedOutputStream checksum = new CheckedOutputStream(stream, new CRC32());
                    final ZipOutputStream zipOutputStream = new ZipOutputStream(checksum)) {
                final ZipEntry entry = new ZipEntry(ruleSet.getName() + ".json");
                zipOutputStream.putNextEntry(entry);
                this.serialiser.serialiseRuleSet(zipOutputStream, ruleSet);
            }
        });
    }

    private void writeFile(final DecisionTreeRuleSet ruleSet) {
        EhSupport.ensureArg(ruleSet != null, "Cannot persist a null ruleset");
        EhSupport.propagate(() -> {
            try (final OutputStream stream = new BufferedOutputStream(
                    Files.newOutputStream(Paths.get(this.fileName), StandardOpenOption.CREATE))) {
                this.serialiser.serialiseRuleSet(stream, ruleSet);
            }
        });
    }
}
