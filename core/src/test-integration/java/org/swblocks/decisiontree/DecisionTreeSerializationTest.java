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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Test;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.swblocks.decisiontree.tree.DecisionTreeType;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

import static org.junit.Assert.assertTrue;

/**
 * Test to ensure that Kryo serialization of {@link DecisionTree} does not fail.
 */
public class DecisionTreeSerializationTest {
    @Test
    public void decisionTree() {
        final DecisionTree decisionTree = DecisionTree.instanceOf(new CommisionRuleSetSupplier(),
                DecisionTreeType.SINGLE);
        decisionTree.getEvaluationFor(decisionTree.createInputs());

        final Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        final InstantiatorStrategy defaultInstantiatorStrategy = new Kryo.DefaultInstantiatorStrategy();
        kryo.getRegistration(ArrayList.class)
                .setInstantiator(defaultInstantiatorStrategy.newInstantiatorOf(ArrayList.class));
        kryo.getRegistration(HashSet.class)
                .setInstantiator(defaultInstantiatorStrategy.newInstantiatorOf(HashSet.class));
        kryo.getRegistration(ConcurrentHashMap.KeySetView.class)
                .setInstantiator(ConcurrentHashMap::newKeySet);
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Output output = new Output(out);
        kryo.writeObject(output, decisionTree);
        output.close();

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        final Input kryoInput = new Input(inputStream);
        final DecisionTree tree = kryo.readObject(kryoInput, DecisionTree.class);

        final org.swblocks.decisiontree.Input input = decisionTree.createInputs("VOICE", "CME", "ED", "US", "RATE");

        final Optional<OutputResults> results = tree.getEvaluationFor(input);
        assertTrue(results.isPresent());
        Assert.assertEquals("1.4", results.get().results().get("Rate"));
    }
}