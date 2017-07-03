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

package org.swblocks.decisiontree.domain;

import java.util.Objects;

/**
 * POJO class to link a driver name with a corresponding weight, which gives the precedence in the decision tree.
 *
 * <p>The {@code WeightedDriver} objects are comparable based one their weight and this provides ordering.
 *
 * <p>The {@code WeightedDriver} objects use the name as the Equals and HashCode.
 */
public final class WeightedDriver implements Comparable<WeightedDriver> {
    private final String name;
    private final int weight;

    /**
     * Constructor taking the name and the weight.
     *
     * @param name   name of driver
     * @param weight weight of driver
     */
    public WeightedDriver(final String name, final int weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName() {
        return this.name;
    }

    public int getWeight() {
        return this.weight;
    }

    @Override
    public int compareTo(final WeightedDriver other) {
        return this.weight > other.getWeight() ? -1 : this.weight == other.getWeight() ? 0 : 1;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final WeightedDriver that = (WeightedDriver) other;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    @Override
    public String toString() {
        return "WeightedDriver{" +
                "name='" + this.name + '\'' +
                ", weight=" + this.weight +
                '}';
    }
}
