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
import java.util.zip.ZipInputStream;

import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.CsvParser;
import org.swblocks.decisiontree.domain.builders.RuleBuilderParser;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;

/**
 * Class for loading a JSON format file, either zipped or json.
 */
public class FileLoader implements Loader<DecisionTreeRuleSet> {
    private static final String ZIP = ".zip";
    private static final String CSV = ".csv";
    private static final String JSON = ".json";
    private final String fileName;
    private final RuleBuilderParser parser;

    private FileLoader(final String path, final String name, final String extension, final RuleBuilderParser parser) {
        this.fileName = (path != null && !path.isEmpty() ? path + File.separator : "") + name + extension;
        this.parser = parser;
    }

    /**
     * Create a {@link Loader} instance to load from a zipped json file.
     *
     * @param path   path to the file  from a classpath root, blank if file is in the classpath
     * @param name   name of the file to load
     * @param parser {@link RuleBuilderParser} to use to process the data in the file
     * @return instance of the {@link FileLoader}
     */
    public static FileLoader zippedJsonLoader(final String path, final String name, final RuleBuilderParser parser) {
        return new FileLoader(path, name, ZIP, parser);
    }

    /**
     * Create a {@link Loader} instance to load from a CSV file.
     *
     * @param path path to the file from a classpath root, blank if file is in the classpath
     * @param name name of the file to load
     * @return instance of the {@link Loader}
     */
    public static FileLoader csvLoader(final String path, final String name) {
        return new FileLoader(path, name, CSV, CsvParser.instanceOf(name));
    }

    /**
     * Create a {@link Loader} instance to load from a json file.
     *
     * @param path   path to the file  from a classpath root, blank if file is in the classpath
     * @param name   name of the file to load
     * @param parser {@link RuleBuilderParser} to use to process the data in the file
     * @return instance of the {@link FileLoader}
     */
    public static FileLoader jsonLoader(final String path, final String name, final RuleBuilderParser parser) {
        return new FileLoader(path, name, JSON, parser);
    }

    @Override
    public Result<DecisionTreeRuleSet> get() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(this.fileName)) {
            EhSupport.ensureArg(is != null, "File %s does not exist.", this.fileName);
            if (this.fileName.endsWith(ZIP)) {
                final ZipInputStream zipInputStream = new ZipInputStream(is);
                zipInputStream.getNextEntry();
                return Result.success(this.parser.parseRuleSet(zipInputStream));
            }
            return Result.success(this.parser.parseRuleSet(is));
        } catch (final Exception exception) {
            return Result.failure(() -> exception);
        }
    }

    /**
     * Test if the load should be retried on failure.  For {@link FileLoader} we would never retry.
     *
     * @param result {@link Result} from loading the file.
     * @return hardcoded to false for this class.
     */
    @Override
    public boolean test(final Result result) {
        return false;
    }
}
