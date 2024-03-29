package net.coderodde.ai.bayesiannetwork.commands;

import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import net.coderodde.ai.bayesiannetwork.Utils;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

public final class DisconnectNodePairExecutableCommand 
extends AbstractExecutableCommand {
    
    private final App app;
    
    public DisconnectNodePairExecutableCommand(App app) {
        this.app = app;
    }

    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command);
        
        if (tokens.length < 4) {
            error("Missing required tokens.");
            return;
        }
        
        if (!tokens[2].equals("from")) {
            error("Command format error. The 3rd token should be \"from\".");
            return;
        }
        
        if (tokens.length > 4 && !tokens[4].equals(COMMENT_BEGIN_TEXT)) {
            error("The command does not immediately end with a comment.");
            return;
        }
        
        String tailNodeName = tokens[1];
        String headNodeName = tokens[3];
        
        if (!Utils.isValidIdentifier(tailNodeName)) {
            error("Bad tail node name: \"" + tailNodeName + "\".");
            return;
        }
        
        if (!Utils.isValidIdentifier(headNodeName)) {
            error("Bad head node name: \"" + headNodeName + "\".");
            return;
        }
        
        if (!app.getNodeMap().containsKey(tailNodeName)) {
            error("No node with name \"" + tailNodeName + "\".");
            return;
        }
        
        if (!app.getNodeMap().containsKey(headNodeName)) {
            error("No node with name \"" + headNodeName + "\".");
            return;
        }
        
        if (tailNodeName.equals(headNodeName)) {
            return;
        }
        
        DirectedGraphNode tail = app.getNodeMap().get(tailNodeName);
        DirectedGraphNode head = app.getNodeMap().get(headNodeName);
        
        if (tail.hasChild(head)) {
            tail.removeChild(head);
            app.setModificationState(true);
            System.out.println(
                    "Removed the child \"" 
                            + headNodeName
                            + "\" from the parent \"" 
                            + tailNodeName + "\".");
        }
    }
}
