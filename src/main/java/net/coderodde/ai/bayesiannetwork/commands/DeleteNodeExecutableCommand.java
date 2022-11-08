package net.coderodde.ai.bayesiannetwork.commands;

import net.coderodde.ai.bayesiannetwork.App;
import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.Utils;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

public class DeleteNodeExecutableCommand extends AbstractExecutableCommand {

    private final App app;
    
    public DeleteNodeExecutableCommand(App app) {
        this.app = app;
    }
    
    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command);
        
        if (tokens.length < 2) {
            error("Invalid delete command. " + 
                  "Expected \"" + tokens[0] + " <nodeName>");
            return;
        }
        
        String nodeName = tokens[1];
        
        if (!Utils.isValidIdentifier(nodeName)) {
            error("\"" + nodeName + "\" is not a valid node name.");
            return;
        }
        
        if (tokens.length > 2 && !tokens[2].equals(COMMENT_BEGIN_TEXT)) {
            StringBuilder sb = new StringBuilder();
            sb.append(tokens[0])
              .append(" ")
              .append(tokens[1])
              .append(" #");
            
            for (int i = 2; i < tokens.length; ++i) {
                sb.append(" ").append(tokens[i]);
            }
            
            error("Bad command format. Did you mean \"" + 
                  sb.toString() + "\"?");
            
            return;
        }
        
        DirectedGraphNode removedNode = app.getNodeMap().remove(nodeName);
        
        if (removedNode != null) {
            removedNode.clear(); // Unlink the removed node from its neighbors.
            app.getProbabilityMap().remove(removedNode);
            app.setModificationState(true);
            System.out.println("Removed node \"" + nodeName + "\".");
        }
    }
}
