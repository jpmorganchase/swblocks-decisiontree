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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.RuleBuilderSerialiser;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.eh.EhSupport;

import static org.junit.Assert.assertNotNull;

/**
 * Test cases for {@link FilePersister}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FilePersisterTest {
    @Mock
    private RuleBuilderSerialiser builderSerialiser;

    @Test
    public void testWriteFileToDisk() {
        final DecisionTreeRuleSet result = CommisionRuleSetSupplier.getCommisionRuleSet().build();

        final FilePersister persister = FilePersister.instanceOf("build", "commissions", builderSerialiser);
        persister.put(result);

        EhSupport.propagate(() -> {
            try (final InputStream input = Files.newInputStream(
                    Paths.get("build" + File.separator + "commissions.json"),
                    StandardOpenOption.READ)) {
                assertNotNull(input);
            }
        });
    }

    @Test
    public void testWriteZipFileToDisk() {
        final DecisionTreeRuleSet result = CommisionRuleSetSupplier.getCommisionRuleSet().build();

        final FilePersister persister = FilePersister.zippedInstanceOf("build", "commissions", builderSerialiser);
        persister.put(result);

        EhSupport.propagate(() -> {
            try (final InputStream input = Files.newInputStream(
                    Paths.get("build" + File.separator + "commissions.zip"),
                    StandardOpenOption.READ)) {
                final ZipInputStream zipInputStream = new ZipInputStream(input);
                zipInputStream.getNextEntry();
                assertNotNull(zipInputStream);
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullRuleSet() {
        final FilePersister persister = FilePersister.instanceOf("build", "commissions", builderSerialiser);
        persister.put(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullZippedRuleSet() {
        final FilePersister persister = FilePersister.zippedInstanceOf("build", "commissions", builderSerialiser);
        persister.put(null);
    }
}