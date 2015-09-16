package net.coderodde.ai.bayesiannetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encodes a particular state of the Bayes network and its 
 * probability.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.61 (Sep 16, 2015)
 */
final class SystemState {

    private final Map<DirectedGraphNode, Boolean> map = new HashMap<>();
    private final List<DirectedGraphNode> nodeList = new ArrayList<>();
    private final double probability;

    SystemState(Map<DirectedGraphNode, Boolean> map, 
                List<DirectedGraphNode> nodeList,
                double probability) {
        this.map.putAll(map);
        this.nodeList.addAll(nodeList);
        this.probability = probability;
    }

    boolean stateContainsSubstate(
            Map<DirectedGraphNode, Boolean> substate) {
        for (Map.Entry<DirectedGraphNode, Boolean> entry : 
                substate.entrySet()) {
            if (!entry.getValue().equals(map.get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }

    double getProbability() {
        return probability;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        int i = 0;

        for (DirectedGraphNode node : nodeList) {
            int fieldLength = node.toString().length();
            String field = String.format("%" + fieldLength + "s",
                                         map.get(node) ? "1" : "0");
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