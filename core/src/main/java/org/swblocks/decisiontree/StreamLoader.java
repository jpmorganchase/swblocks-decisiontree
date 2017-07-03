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

import java.io.InputStream;

import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.domain.builders.CsvParser;
import org.swblocks.decisiontree.domain.builders.RuleBuilderParser;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;

/**
 * Base class for loading a JSON {@link java.io.InputStream}.
 */
public final class StreamLoader implements Loader<DecisionTreeRuleSet>, AutoCloseable {
    private final InputStream stream;
    private final RuleBuilderParser parser;
    private Result<DecisionTreeRuleSet> result;

    private StreamLoader(final InputStream stream, final RuleBuilderParser parser) {
        EhSupport.ensureArg(stream != null, "ruleset input stream is null.");
        this.stream = stream;
        this.parser = parser;
    }

    public static StreamLoader jsonLoader(final InputStream json, final RuleBuilderParser parser) {
        return new StreamLoader(json, parser);
    }

    public static StreamLoader csvLoader(final String name, final InputStream csvInput) {
        return new StreamLoader(csvInput, CsvParser.instanceOf(name));
    }

    /**
     * Test if the load should be retried on failure.  Never retry for  {@link StreamLoader}.
     *
     * @param result {@link Result} from loading the file.
     * @return hardcoded to false for this class.
     */
    @Override
    public boolean test(final Result result) {
        return false;
    }

    @Override
    public Result<DecisionTreeRuleSet> get() {
        if (this.result != null) {
            return this.result;
        }

        try (final InputStream input = this.stream) {
            this.result = Result.success(this.parser.parseRuleSet(input));
        } catch (final Exception exception) {
            this.result = Result.failure(() -> exception);
        }

        return this.result;
    }

    @Override
    public void close() throws Exception {
        this.stream.close();
    }
}
