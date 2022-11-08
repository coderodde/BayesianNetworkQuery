package net.coderodde.ai.bayesiannetwork.commands;

import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import static net.coderodde.ai.bayesiannetwork.Utils.error;
import static net.coderodde.ai.bayesiannetwork.Utils.isValidIdentifier;
import static net.coderodde.ai.bayesiannetwork.Utils.parseProbability;

public final class CreateNewNodeExecutableCommand 
extends AbstractExecutableCommand {

    private final App app;
    
    public CreateNewNodeExecutableCommand(App app) {
        this.app = app;
    }
    
    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command);
        
        if (tokens.length < 3) {
            error("Cannot parse 'new' command.");
            return;
        }
        
        if (tokens.length >= 4 && !tokens[3].startsWith(COMMENT_BEGIN_TEXT)) {
            error("Bad comment format.");
            return;
        }
    
        String nodeName = tokens[1];
        
        if (!isValidIdentifier(nodeName)) {
            error("\"" + nodeName + "\" is a bad node identifier.");
            return;
        }
        
        String probabilityString = tokens[2];
        double probability;
        
        try {
            probability = parseProbability(probabilityString);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
            return;
        }
        
        DirectedGraphNode node;
        
        if (app.getNodeMap().containsKey(nodeName)) {
            node = app.getNodeMap().get(nodeName);
        } else {
            node = new DirectedGraphNode(nodeName, probability);
            app.getNodeMap().put(nodeName, node);
        }
        
        app.getProbabilityMap().put(node, probability);
    }
}
