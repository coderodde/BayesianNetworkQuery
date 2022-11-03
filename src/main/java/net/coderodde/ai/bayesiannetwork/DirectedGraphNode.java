package net.coderodde.ai.bayesiannetwork;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class implements directed graph nodes.
 * 
 * @author Rodion "rodde" Efremov
 * @versoin 1.61 (Nov 3, 2022)
 * @version 1.6 (Sep 14, 2015)
 * @since 1.6 (Sep 4, 2015)
 */
public class DirectedGraphNode implements Comparable<DirectedGraphNode> {

    private final String name;
    private final double probability;
    private final Set<DirectedGraphNode> children = new LinkedHashSet<>();
    private final Set<DirectedGraphNode> parents  = new LinkedHashSet<>();

    public DirectedGraphNode(String name, double probability) {
        this.name = Objects.requireNonNull(name, "The node name is null.");
        this.probability = checkProbability(probability);
    }

    public void addChild(DirectedGraphNode child) {
        Objects.requireNonNull(child, "The child node is null.");

        if (child == this) {
            return;
        }

        children.add(child);
        child.parents.add(this);
    }

    public String getName() {
        return name;
    }

    public double getProbability() {
        return probability;
    }
    
    public boolean hasChild(DirectedGraphNode child) {
        return children.contains(child);
    }

    public void removeChild(DirectedGraphNode child) {
        if (children.contains(child)) {
            children.remove(child);
            child.parents.remove(this);
        }
    }

    public void clear() {
        children.stream().forEach((child) -> {
            child.parents.remove(this);
        });

        parents.stream().forEach((parent) -> {
            parent.children.remove(this);
        });

        children.clear();
        parents.clear();
    }

    public Set<DirectedGraphNode> children() {
        return Collections.<DirectedGraphNode>unmodifiableSet(children);
    }

    public Set<DirectedGraphNode> parents() {
        return Collections.<DirectedGraphNode>unmodifiableSet(parents);
    }

    @Override
    public String toString() {
        return "[" + name + ", p = " + probability + "]";
    }

    @Override
    public int compareTo(DirectedGraphNode other) {
        return name.compareTo(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DirectedGraphNode)) {
            return false;
        }

        return name.equals(((DirectedGraphNode) o).name);
    }
    
    private static double checkProbability(double probability) {
        if (Double.isNaN(probability)) {
            throw new IllegalArgumentException(
                    "Probability is NaN. " + 
                    "Must be between the range [0, 1], inclusively.");
        }
        
        if (probability < 0.0) {
            throw new IllegalArgumentException(
                    "Probability is negative: " 
                            + probability
                            + ". Must be at least 0 (zero).");
        }
        
        if (probability > 1.0) {
            throw new IllegalArgumentException(
                    "Probability is too large: " 
                            + probability 
                            + ". Must be at most 1 (one).");
        }
        
        return probability;
    }
}
