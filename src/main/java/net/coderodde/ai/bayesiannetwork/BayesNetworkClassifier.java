package net.coderodde.ai.bayesiannetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class implements the binary Bayes network classifier.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.61 (Sep 18, 2015)
 */
public class BayesNetworkClassifier {

    private final List<DirectedGraphNode> network;
    private final ProbabilityMap<DirectedGraphNode> probabilityMap;
    private final Map<DirectedGraphNode, Boolean> onoffMap;
    private final ClassificationResult result;
    private final Set<DirectedGraphNode> visited;
    private final List<DirectedGraphNode> tuple;
    
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
        
        this.result = new ClassificationResult();
        this.visited = new HashSet<>(network.size());
        this.tuple = new ArrayList<>(network.size());
    }
    
    /**
     * Implements the actual classification.
     * 
     * @return the classification result structure.
     */
    private ClassificationResult classify() {
        classify(getRootSet(network), 1.0f);
        result.setNodeList(tuple);
        return result;
    }
    
    /**
     * Returns the set of nodes that have no parents.
     * 
     * @param network the Bayes network to process.
     * @return the set of root nodes.
     */
    private Set<DirectedGraphNode> getRootSet(List<DirectedGraphNode> network) {
        Set<DirectedGraphNode> set = new TreeSet<>();

        for (DirectedGraphNode node : network) {
            if (isIndependent(node)) {
                set.add(node);
            }
        }
        
        return set;
    }

    /**
     * Returns {@code true} if the input node has a parent whose current state 
     * is "off".
     * 
     * @param node the node to check.
     * @return {@code true} if the input node has a turned off parent.
     */
    private boolean nodeHasOffParent(DirectedGraphNode node) {
        for (DirectedGraphNode parent : node.parents()) {
            if (onoffMap.get(parent).equals(Boolean.FALSE)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Implements the actual compilation from a Bayesian network to the list of
     * system states. This algorithm builds a tree whose leafs are all possible 
     * system states, yet it prunes the search by skipping the nodes whose state
     * cannot vary, thus implementing "branch-and-bound" technique.
     * 
     * @param levelSet    the set of nodes on current level.
     * @param probability the accumulated probability.
     */
    private void classify(Set<DirectedGraphNode> levelSet,
                          double probability) {
        if (levelSet.isEmpty()) {
            // End the recursion, record the tuple and its probability as a new
            // system state.
            Map<DirectedGraphNode, Boolean> variableMap = new HashMap<>();

            for (DirectedGraphNode node : tuple) {
                variableMap.put(node, onoffMap.get(node));
            }

            result.addSystemState(new SystemState(variableMap, 
                                                  result, 
                                                  probability));
            return;
        }

        Iterator<DirectedGraphNode> iterator = levelSet.iterator();

        // Remove the nodes from 'levelSet' whose some parents are not yet
        // processed. We will get to them in deeper recursion depth.
        outer:
        while (iterator.hasNext()) {
            for (DirectedGraphNode parent : iterator.next().parents()) {
                if (!visited.contains(parent)) {
                    iterator.remove();
                    continue outer;
                }
            }
        }

        // Set of nodes whose on/off status cannot vary, namely the nodes whose
        // probability is 0 or whose some parents are off, or nodes with no off
        // parents and probability 1.0.
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

        // Add the level to visited set and load the node at current level that
        // can vary.
        for (DirectedGraphNode node : levelSet) {
            if (!skipSet.contains(node)) {
                nodeArray[i++] = node;
            }

            visited.add(node);

            if (tuple.size() < visited.size()) {
                tuple.add(node);
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

        // For each combination of variable state nodes, recur.
        while (getNextCombination(nodeArray, doInit)) {
            doInit = false;

            classify(nextLevelSet,
                         probability * computeProbability(nodeArray));
        }

        // Clean up the state for further recursion.
        for (DirectedGraphNode node : levelSet) {
            visited.remove(node);
        }
    }

    /**
     * Computes the probability of configuration consisting of nodes in 
     * {@code nodeArray}.
     * 
     * @param nodeArray the array of nodes whose probabilities to accumulate.
     * @return the probability value.
     */
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

    /**
     * Generates the next combination of states for nodes in {@code nodes}.
     * 
     * @param nodes  the array of nodes for which we want to generate all 
     *               possible states.
     * @param doInit indicate whether the states should be all initialized to 
     *               "off".
     * @return {@code false} if upon entry to this method all states in 
     *         {@code nodes} is "on", which indicates there is no more state
     *         combinations.
     */
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

    private static void checkNetworkNotEmpty(List<DirectedGraphNode> network) {
        if (network.isEmpty()) {
            throw new IllegalArgumentException("The input network is empty.");
        }
    }

    private static void checkNetworkIsAcyclic(List<DirectedGraphNode> network) {
        if (!Utils.graphIsAcyclic(network.get(0))) {
            throw new IllegalArgumentException(
                    "The current network contains cycles.");
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
    
    private static boolean isIndependent(DirectedGraphNode node) {
        return node.parents().isEmpty();
    }
}
