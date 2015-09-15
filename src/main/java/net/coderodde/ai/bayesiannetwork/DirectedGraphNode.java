package net.coderodde.ai.bayesiannetwork;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class implements directed graph nodes.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Sep 14, 2015)
 */
public class DirectedGraphNode implements Comparable<DirectedGraphNode> {
   
    private final String name;
    private final Set<DirectedGraphNode> children = new LinkedHashSet<>();
    private final Set<DirectedGraphNode> parents  = new LinkedHashSet<>();
    
    public DirectedGraphNode(String name) {
        Objects.requireNonNull(name, "The node name is null.");
        this.name = name;
    }
    
    public void addChild(DirectedGraphNode child) {
        Objects.requireNonNull(child, "The child node is null.");
        
        if (child == this) {
            return;
        }
        
        children.add(child);
        child.parents.add(this);
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
    
    public boolean isIndependent() {
        return parents.isEmpty();
    }
    
    public Set<DirectedGraphNode> children() {
        return Collections.<DirectedGraphNode>unmodifiableSet(children);
    }
    
    public Set<DirectedGraphNode> parents() {
        return Collections.<DirectedGraphNode>unmodifiableSet(parents);
    }
    
    @Override
    public String toString() {
        return name;
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
}
