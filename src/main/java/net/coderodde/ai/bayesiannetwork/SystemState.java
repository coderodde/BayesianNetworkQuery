package net.coderodde.ai.bayesiannetwork;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class encodes a particular state of the Bayes network and its 
 * probability.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.618 (Sep 18, 2015)
 */
final class SystemState {

    /**
     * The set of nodes in this state whose state is "on". All nodes in the
     * network that are not in this list are considered to have state "off". 
     */
    private final Set<DirectedGraphNode> onSet = new HashSet<>();

    /**
     * The probability of this state.
     */
    private final double probability;

    /**
     * The {@code ClassificationResult} this state belongs to.
     */
    private final ClassificationResult owner;

    SystemState(Map<DirectedGraphNode, Boolean> map, 
                ClassificationResult result,
                double probability) {
        map.entrySet()
           .stream()
           .filter((entry) -> (entry.getValue().equals(Boolean.TRUE)))
           .forEach((entry) -> {
            onSet.add(entry.getKey());
        });

        this.probability = probability;
        this.owner = result;
    }

    boolean stateContainsSubstate(Map<DirectedGraphNode, Boolean> substate) {
        for (Map.Entry<DirectedGraphNode, Boolean> entry : 
                substate.entrySet()) {
            if (entry.getValue()) {
                if (!onSet.contains(entry.getKey())) {
                    return false;
                }
            } else {
                if (onSet.contains(entry.getKey())) {
                    return false;
                }
            }
        }

        return true;
    }

    double getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        List<DirectedGraphNode> nodeList = owner.getNodeList();
        int i = 0;

        for (DirectedGraphNode node : nodeList) {
            int fieldLength = node.toString().length();
            String field = String.format("%" + fieldLength + "s",
                                         onSet.contains(node) ? "1" : "0");
            sb.append(field);

            if (i < nodeList.size() - 1) {
                sb.append(", ");
                i++;
            }
        }

        return sb.append("): ")
                 .append(probability)
                 .toString();
    }
}