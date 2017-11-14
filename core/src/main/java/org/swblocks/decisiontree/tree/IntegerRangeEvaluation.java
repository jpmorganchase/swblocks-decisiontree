package org.swblocks.decisiontree.tree;

import java.util.function.Predicate;

import org.swblocks.jbl.util.Range;

public class IntegerRangeEvaluation extends GenericRangeEvaluation<Integer> implements Predicate<String> {
    IntegerRangeEvaluation(String name, Range<Integer> range) {
        super(name, range);
    }

    @Override
    protected Integer parse(String value) {
        return Integer.parseInt(value);
    }
}
