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

package org.swblocks.decisiontree.tree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.swblocks.decisiontree.domain.DecisionTreeRuleSet;
import org.swblocks.decisiontree.util.CommisionRuleSetSupplier;
import org.swblocks.jbl.eh.EhSupport;
import org.swblocks.jbl.eh.Result;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

import static org.junit.Assert.assertEquals;

/**
 * Test to ensure Kryo serialization of {@link DecisionTreeRuleSet} and {@link TreeNode} does not fail.
 */
public class TreeSerializationTest {
    @Test
    public void ruleSet() {
        final Result<DecisionTreeRuleSet> result = (new CommisionRuleSetSupplier()).get();

        DecisionTreeRuleSet ruleSet = null;
        if (result.isSuccess()) {
            ruleSet = result.getData();
        }

        final Kryo kryo = new Kryo();
        // no default no-arg constructors
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        // NPE - collections not initialised correctly
        final InstantiatorStrategy defaultInstantiatorStrategy = new Kryo.DefaultInstantiatorStrategy();
        kryo.getRegistration(ArrayList.class)
                .setInstantiator(defaultInstantiatorStrategy.newInstantiatorOf(ArrayList.class));
        kryo.getRegistration(ConcurrentHashMap.KeySetView.class)
                .setInstantiator(ConcurrentHashMap::newKeySet);
        kryo.getRegistration(HashSet.class)
                .setInstantiator(defaultInstantiatorStrategy.newInstantiatorOf(HashSet.class));
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Output output = new Output(out);
        kryo.writeObject(output, ruleSet);
        output.flush();
        output.close();

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());

        final Input kryoInput = new Input(inputStream);
        kryo.readObject(kryoInput, DecisionTreeRuleSet.class);
    }

    @Test
    public void treeNode() {
        final Result<DecisionTreeRuleSet> result = (new CommisionRuleSetSupplier()).get();
        EhSupport.ensure(result.isSuccess(), "Could not create decision tree");
        final DecisionTreeRuleSet ruleSet = result.getData();

        final TreeNode node = DecisionTreeFactory.constructDecisionTree(ruleSet, DecisionTreeType.SINGLE);

        final Kryo kryo = new Kryo();
        // no default no-arg constructors
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        final InstantiatorStrategy defaultInstantiatorStrategy = new Kryo.DefaultInstantiatorStrategy();
        kryo.getRegistration(ArrayList.class)
                .setInstantiator(defaultInstantiatorStrategy.newInstantiatorOf(ArrayList.class));
        kryo.getRegistration(HashSet.class)
                .setInstantiator(defaultInstantiatorStrategy.newInstantiatorOf(HashSet.class));
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Output output = new Output(out);
        kryo.writeObject(output, node);
        output.flush();
        output.close();

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        final Input kryoInput = new Input(inputStream);
        final TreeNode tree = kryo.readObject(kryoInput, BaseTreeNode.class);

        final SingleDecisionTreeFactoryTest test = new SingleDecisionTreeFactoryTest();
        test.checkTreeNode(tree, ruleSet);

        assertEquals(node, tree);
    }
}