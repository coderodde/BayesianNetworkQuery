package net.coderodde.ai.bayesiannetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class implements the classification result of the Bayes network.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.618 (Sep 18, 2015)
 */
public class ClassificationResult {

    private final List<SystemState> systemStateList = new ArrayList<>();
    private List<DirectedGraphNode> nodeList;

    /**
     * Computes the probability of posteriori variables given the apriori
     * variables.
     * 
     * @param posterioriVariableMap a map mapping each posteriori variable to 
     *                              its state.
     * @param aprioriVariableMap    a map mapping each apriori variable to its
     *                              state.
     * @return the probability of expression.
     */
    public double query(Map<DirectedGraphNode, Boolean> posterioriVariableMap,
                        Map<DirectedGraphNode, Boolean> aprioriVariableMap) {
        Objects.requireNonNull(posterioriVariableMap, 
                               "The posteriori variable map is null.");
        Objects.requireNonNull(aprioriVariableMap,
                               "The apriori variable map is null.");

        if (mapKeyIntersect(posterioriVariableMap, aprioriVariableMap)) {
            throw new IllegalArgumentException(
                    "Posteriori and apriori variable lists have a common " +
                     "variable.");
        }

        Map<DirectedGraphNode, Boolean> combinedVariableMap = 
                new HashMap<>(posterioriVariableMap);

        combinedVariableMap.putAll(aprioriVariableMap);

        double aprioriProbability    = 0.0;
        double posterioriProbability = 0.0;

        for (SystemState state : systemStateList) {
            if (state.stateContainsSubstate(aprioriVariableMap)) {
                aprioriProbability += state.getProbability();

                if (state.stateContainsSubstate(combinedVariableMap)) {
                    posterioriProbability += state.getProbability();
                }
            }
        }

        return aprioriProbability == 0.0 ? 0.0 : posterioriProbability / 
                                                 aprioriProbability;
    }

    void setNodeList(List<DirectedGraphNode> nodeList) {
        this.nodeList = nodeList;
    }

    /**
     * This method returns the sum of probabilities over all system states, and
     * it <b>must</b> return <b>1.0</b> under any circumstances.
     * 
     * @return the sum of probabilities.
     */
    public double getSumOfProbabilities() {
        double probability = 0.0;

        probability = systemStateList.stream()
                                      .map((state) -> state.getProbability())
                                      .reduce(probability, 
                                             (accumulator, _item) -> 
                                                     accumulator + _item);

        return probability;
    }

    public int getNumberOfStates() {
        return systemStateList.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        int i = 0;

        for (DirectedGraphNode node : nodeList) {
            sb.append(node.getName());

            if (i < nodeList.size() - 1) {
                sb.append(", ");
            }

            ++i;
        }

        sb.append(")\n");

        systemStateList.stream().forEach((state) -> {
            sb.append(state).append('\n');
        });

        return sb.toString();
    }

    void addSystemState(SystemState state) {
        systemStateList.add(state);
    }

    List<DirectedGraphNode> getNodeList() {
        return nodeList;
    }

    /**
     * Checks whether the two input maps have common keys.
     * 
     * @param <K>  the key type.
     * @param <V>  the value type.
     * @param map1 the first map.
     * @param map2 the second map.
     * @return {@code true} if maps share at least one common key.
     */
    private static <K, V> boolean mapKeyIntersect(Map<K, V> map1, 
                                                  Map<K, V> map2) {
        Map<K, V> smallerMap;
        Map<K, V> largerMap;

        if (map1.size() < map2.size()) {
            smallerMap = map1;
            largerMap  = map2;
        } else {
            smallerMap = map2;
            largerMap  = map1;
        }

        return smallerMap.keySet()
                         .stream()
                         .anyMatch((key) -> (largerMap.containsKey(key)));
    }
}
