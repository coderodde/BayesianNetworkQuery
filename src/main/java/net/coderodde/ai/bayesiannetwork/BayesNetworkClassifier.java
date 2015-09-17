package net.coderodde.ai.bayesiannetwork;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class implements the Bayes network classifier.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Sep 15, 2015)
 */
public class BayesNetworkClassifier {

    private final List<DirectedGraphNode> network;
    private final ProbabilityMap<DirectedGraphNode> probabilityMap;
    private final Map<DirectedGraphNode, Boolean> onoffMap;

    /**
     * Performs the actual classification task.
     * 
     * @param network        the list of (some) nodes of a network to classify.
     * @param probabilityMap the map mapping each node to its probability.
     * @return the data structure that facilitates queries.
     */
    public static ClassificationResult 
        classify(List<DirectedGraphNode> network,
                 ProbabilityMap<DirectedGraphNode> probabilityMap) {

        BayesNetworkClassifier bnc = 
                new BayesNetworkClassifier(network, probabilityMap);

        return bnc.classify();
    }

    /**
     * Implements the actual classification.
     * 
     * @return the classification result structure.
     */
    private ClassificationResult classify() {
        Deque<DirectedGraphNode> tuple = new ArrayDeque<>();
        Set<DirectedGraphNode> visited = new HashSet<>();
        Set<DirectedGraphNode> set = getRootSet(network);
        ClassificationResult result = new ClassificationResult();

        classifyImpl(tuple, visited, set, result, 1.0f);

        return result;

    }
    
    private Set<DirectedGraphNode> getRootSet(List<DirectedGraphNode> network) {
        Set<DirectedGraphNode> set = new TreeSet<>();

        for (DirectedGraphNode node : network) {
            if (node.isIndependent()) {
                set.add(node);
            }
        }
        
        return set;
    }

    private boolean nodeHasOffParent(DirectedGraphNode node) {
        for (DirectedGraphNode parent : node.parents()) {
            if (onoffMap.get(parent).equals(Boolean.FALSE)) {
                return true;
            }
        }

        return false;
    }

    private void classifyImpl(Deque<DirectedGraphNode> tuple,
                              Set<DirectedGraphNode> visited,
                              Set<DirectedGraphNode> levelSet,
                              ClassificationResult result,
                              double probability) {
        if (levelSet.isEmpty()) {
            // End the recursion, record the tuple and its probability as a new
            // system state.
            Map<DirectedGraphNode, Boolean> variableMap = new HashMap<>();

            for (DirectedGraphNode node : tuple) {
                variableMap.put(node, onoffMap.get(node));
            }

            result.addSystemState(new SystemState(variableMap, 
                                                  new ArrayList<>(tuple), 
                                                  probability));
            return;
        }

        Iterator<DirectedGraphNode> iterator = levelSet.iterator();

        // Remove the nodes from 'levelSet' whose some parents are not yet
        // processed.
        outer:
        while (iterator.hasNext()) {
            for (DirectedGraphNode parent : iterator.next().parents()) {
                if (!visited.contains(parent)) {
                    iterator.remove();
                    continue outer;
                }
            }
        }

        // Set of nodes whose on/off status cannot vary.
        Set<DirectedGraphNode> skipSet = new HashSet<>(levelSet.size());

        for (DirectedGraphNode node : levelSet) {
            if (nodeHasOffParent(node) || probabilityMap.get(node) == 0.0) {
                onoffMap.put(node, Boolean.FALSE);
                skipSet.add(node);
            } else if (probabilityMap.get(node) == 1.0) {
                onoffMap.put(node, Boolean.TRUE);
                skipSet.add(node);
            }
        }

        DirectedGraphNode[] nodeArray = new DirectedGraphNode[levelSet.size() -
                                                               skipSet.size()];

        int i = 0;

        // Add the level to visited set.
        for (DirectedGraphNode node : levelSet) {
            if (!skipSet.contains(node)) {
                nodeArray[i++] = node;
            }

            visited.add(node);

            if (tuple.size() < visited.size()) {
                tuple.addLast(node);
            }

            if (result.getNodeList().size() < visited.size()) {
                result.getNodeList().add(node);
            }
        }

        Set<DirectedGraphNode> nextLevelSet = new TreeSet<>();

        // Compute the next node level.
        for (DirectedGraphNode node : levelSet) {
            for (DirectedGraphNode child : node.children()) {
                nextLevelSet.add(child);
            }
        }

        boolean doInit = true;

        while (getNextCombination(nodeArray, doInit)) {
            doInit = false;

            classifyImpl(tuple, 
                         visited, 
                         nextLevelSet, 
                         result, 
                         probability * computeProbability(nodeArray));
        }

        // Clean up the state for further recursion.
        for (DirectedGraphNode node : levelSet) {
            visited.remove(node);
        }
    }

    private double computeProbability(DirectedGraphNode[] nodeArray) {
        double p = 1.0;

        for (DirectedGraphNode node : nodeArray) {
            if (onoffMap.get(node).equals(Boolean.TRUE)) {
                p *= probabilityMap.get(node);
            } else {
                p *= 1.0 - probabilityMap.get(node);
            }
        }

        return p;
    }

    private boolean getNextCombination(DirectedGraphNode[] nodes, 
                                       boolean doInit) {
        if (doInit) {
            for (DirectedGraphNode node : nodes) {
                onoffMap.put(node, Boolean.FALSE);
            }

            return true;
        }

        for (int i = nodes.length - 1; i >= 0; --i) {
            if (onoffMap.get(nodes[i]).equals(Boolean.FALSE)) {
                onoffMap.put(nodes[i], Boolean.TRUE);

                for (++i; i < nodes.length; ++i) {
                    onoffMap.put(nodes[i], Boolean.FALSE);
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Constructs the internal state of the classifier.
     * 
     * @param network        the list of (some) nodes in the Bayes network.
     * @param probabilityMap the map mapping each node to its probability.
     */
    private BayesNetworkClassifier(
            List<DirectedGraphNode> network,
            ProbabilityMap<DirectedGraphNode> probabilityMap) {
        Objects.requireNonNull(network, "The input network is null.");
        Objects.requireNonNull(probabilityMap, 
                               "The input probability map is null.");

        checkNetworkNotEmpty(network);
        network = Utils.findEntireGraph(network.get(0));
        checkNetworkIsAcyclic(network);
        checkProbabilityMap(probabilityMap, network);

        this.network = network;
        this.probabilityMap = probabilityMap;
        this.onoffMap = new HashMap<>();

        for (DirectedGraphNode node : network) {
            onoffMap.put(node, Boolean.FALSE);
        }
    }

    private static void checkNetworkNotEmpty(List<DirectedGraphNode> network) {
        if (network.isEmpty()) {
            throw new IllegalArgumentException("The input network is empty.");
        }
    }

    private static void checkNetworkIsAcyclic(List<DirectedGraphNode> network) {
        if (!Utils.graphIsAcyclic(network.get(0))) {
            throw new IllegalArgumentException(
                    "The input network contains cycles.");
        }
    }

    private static void checkProbabilityMap(
            ProbabilityMap<DirectedGraphNode> probabilityMap,
            List<DirectedGraphNode> nodeList) {
        for (DirectedGraphNode node : nodeList) {
            if (!probabilityMap.contains(node)) {
                throw new IllegalArgumentException(
                "The node " + node + " is not mapped in the probability map.");
            }
        }
    }
}
