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
            
            for (DirectedGraphNode child : current.children()) {
                if (!visited.contains(child)) {
                    visited.add(child);
                    queue.addLast(child);
                }
            }
            
            for (DirectedGraphNode parent : current.parents()) {
                if (!visited.contains(parent)) {
                    visited.add(parent);
                    queue.addLast(parent);
                }
            }
        }
        
        return new ArrayList<>(visited);
    }
    
    public static boolean graphIsAcyclic(DirectedGraphNode start) {
        List<DirectedGraphNode> nodeList = findEntireGraph(start);
        Map<DirectedGraphNode, NodeColor> map = new HashMap<>();
        
        for (DirectedGraphNode node : nodeList) {
            map.put(node, NodeColor.WHITE);
        }
        
        for (DirectedGraphNode node : nodeList) {
            if (dfs(node, map)) {
                return false;
            }
        }
        
        return true;
    }
    
    private enum NodeColor {
        WHITE,
        GRAY,
        BLACK
    }
    
    /**
     * Implements the cycle detection algorithm.
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
}
