package net.coderodde.ai.bayesiannetwork.commands;

import java.util.Map;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 ()
 * @since 1.6 ()
 */
public final class PrintNodesExecutableCommand 
        extends AbstractExecutableCommand {

    private final App app;
    
    public PrintNodesExecutableCommand(App app) {
        this.app = app;
    }
    
    @Override
    public void execute(String command) {
        if (app.getNodeMap().isEmpty()) {
            System.out.println("No nodes yet.");
            return;
        }
        
        int maximumNodeNameLength = computeMaximumNodeNameLength();
        String format = "%" + maximumNodeNameLength + "s %f";
        
        for (Map.Entry<String, DirectedGraphNode> entry 
                : app.getNodeMap().entrySet()) {
            System.out.println(
                    String.format(
                            format, 
                            entry.getValue().getName(),
                            entry.getValue().getProbability()
                            ));
        }
    }
    
    private int computeMaximumNodeNameLength() {
        int maximumLength = Integer.MIN_VALUE;
        
        for (String nodeName : app.getNodeMap().keySet()) {
            maximumLength = Math.max(maximumLength, nodeName.length());
        }
        
        return maximumLength;
    }
}
