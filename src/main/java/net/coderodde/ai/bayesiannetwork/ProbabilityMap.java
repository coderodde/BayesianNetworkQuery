package net.coderodde.ai.bayesiannetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class maps objects to their probabilities.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Sep 15, 2015)
 * @param <T> the object implementation type.
 */
public class ProbabilityMap<T> {

    private final Map<T, Double> map = new HashMap<>();

    public void put(T object, double probability) {
        Objects.requireNonNull(object, "The input node is null.");
        checkProbability(probability);
        map.put(object, probability);
    }

    public double get(T node) {
        Objects.requireNonNull(node, "The query node is null.");

        if (!map.containsKey(node)) {
            throw new IllegalStateException(
                    "There is no mapping for node " + node);
        }

        return map.get(node);
    }

    public boolean contains(T node) {
        return map.containsKey(node);
    }

    public void remove(T object) {
        map.remove(object);
    }

    private static void checkProbability(double probability) {
        if (Double.isNaN(probability)) {
            throw new IllegalArgumentException("The input probability is NaN.");
        }

        if (probability < 0.0) {
            throw new IllegalArgumentException(
                    "The input probability is too small: " + probability);
        }

        if (probability > 1.0) {
            throw new IllegalArgumentException(
                    "The input probability is too large: " + probability);
        }
    }
}
