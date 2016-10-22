package net.coderodde.ai.bayesiannetwork;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains various utility methods.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Sep 15, 2015)
 */
public class Utils {

    /**
     * Given the start node {@code start}, this static method treats each 
     * directed arc as an undirected arc, and crawls entire graph reachable from
     * {@code start}.
     * 
     * @param  start the node whose graph we want to crawl.
     * @return the list of nodes reachable from {@code start} if each directed
     *         arc is considered to be undirected.
     */
    public static List<DirectedGraphNode> 
        findEntireGraph(DirectedGraphNode start) {
        Deque<DirectedGraphNode> queue = new ArrayDeque<>();
        Set<DirectedGraphNode> visited = new HashSet<>();

        queue.addLast(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            DirectedGraphNode current = queue.removeFirst();

            current.children()
                   .stream()
                   .filter((child) -> (!visited.contains(child)))
                   .map((child) -> {
                visited.add(child);
                return child;
            }).forEach((child) -> {
                queue.addLast(child);
            });

            current.parents()
                   .stream()
                   .filter((parent) -> (!visited.contains(parent)))
                   .map((parent) -> {
                visited.add(parent);
                return parent;
            }).forEach((parent) -> {
                queue.addLast(parent);
            });
        }

        return new ArrayList<>(visited);
    }

    /**
     * Checks that the entire graph reachable from {code start} is acyclic.
     * 
     * @param start a representative node of a graph.
     * @return {@code true} only if the graph has directed cycles.
     */
    public static boolean graphIsAcyclic(DirectedGraphNode start) {
        List<DirectedGraphNode> nodeList = findEntireGraph(start);
        Map<DirectedGraphNode, NodeColor> map = new HashMap<>();

        nodeList.stream().forEach((node) -> {
            map.put(node, NodeColor.WHITE);
        });

        if (!nodeList.stream().noneMatch((node) -> (dfs(node, map)))) {
            return false;
        }

        return true;
    }

    private enum NodeColor {
        WHITE,
        GRAY,
        BLACK
    }

    /**
     * Implements the cycle detection algorithm, which is depth-first search.
     * 
     * @param node the node to start the search from.
     * @param map  the color map.
     * @return {@code true} if there is a cycle in the graph. {@code false} 
     *         otherwise.
     */
    private static boolean dfs(DirectedGraphNode node, 
                               Map<DirectedGraphNode, NodeColor> map) {
        map.put(node, NodeColor.GRAY);

        for (DirectedGraphNode child : node.children()) {
            if (map.get(child).equals(NodeColor.WHITE)) {
                // The color of 'child' is WHITE.
                if (dfs(child, map)) {
                    return true;
                }
            }

            if (map.get(child).equals(NodeColor.GRAY)) {
                // Found a cycle.
                return true;
            }
        }

        map.put(node, NodeColor.BLACK);
        return false;
    }
    
    public static void error(String message) {
        System.err.println("ERROR: " + message);
    }
    
    /**
     * Checks that an identifier is a valid Java identifier.
     * 
     * @param identifier the identifier to check.
     * @return {@code true} only if the input identifier is valid.
     */
    public static boolean isValidIdentifier(String identifier) {
        if (identifier.isEmpty()) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }

        for (int i = 1; i < identifier.length(); ++i) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }

        return true;
    }
    
    public static double parseProbability(String probabilityString) {
        double probability;
        
        try {
           probability = Double.parseDouble(probabilityString);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "The input probability \"" + probabilityString + "\" is " +
                    "not a floating point number.");
        }
        
        if (Double.isNaN(probability)) {
            throw new IllegalArgumentException(
                    "The input probability is NaN.");
        }
        
        if (probability < 0.0) {
            throw new IllegalArgumentException(
                    "The input probability \"" + probabilityString + "\" is " +
                    "negative. Should be at least 0.");
        }
        
        if (probability > 1.0) {
            throw new IllegalArgumentException(
                    "The input probability \"" + probabilityString + "\" is " + 
                    "too large. Should be at most 1.");
        }
        
        return probability;
    }
}
