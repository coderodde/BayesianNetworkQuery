package net.coderodde.ai.bayesiannetwork;

import java.util.HashMap;
import java.util.Map;

/**
 * This inner static class encodes a particular state of the Bayes network
 * and its probability.
 */
final class SystemState {

    private final Map<DirectedGraphNode, Boolean> map = new HashMap<>();
    private final double probability;

    SystemState(Map<DirectedGraphNode, Boolean> map, double probability) {
        this.map.putAll(map);
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
        
        for (Map.Entry<DirectedGraphNode, Boolean> entry : map.entrySet()) {
            sb.append(entry.getValue().equals(Boolean.TRUE) ? "1" : "0");
            
            if (i < map.size() - 1) {
                sb.append(", ");
            }
            
            ++i;
        }
        
        return sb.append("): ")
                 .append(probability)
                 .toString();
    }
}