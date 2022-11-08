package net.coderodde.ai.bayesiannetwork.commands;

import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import static net.coderodde.ai.bayesiannetwork.Utils.error;
import static net.coderodde.ai.bayesiannetwork.Utils.isValidIdentifier;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 ()
 * @since 1.6 ()
 */
public final class ConnectNodePairExecutableCommand 
        extends AbstractExecutableCommand {

    private final App app;
    
    public ConnectNodePairExecutableCommand(App app) {
        this.app = app;
    }
    
    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command);
        
    if (tokens.length < 4) {
            error("Missing required tokens. " + 
                    "The proper format is " + 
                    "\"connect <tail_node> to <head_node> [# Comment text.]\"");
            return;
        }

        if (!tokens[2].equals("to")) {
            error("Format error. The proper format is " + 
                  "\"connect <tail_node> to <head_node> [# Comment text.]\"");
            return;
        }
        
        if (tokens.length > 4 && !tokens[4].equals(COMMENT_BEGIN_TEXT)) {
            error("Format error. The proper format is " + 
                  "\"connect <tail_node> to <head_node> # Comment text.\"");
            return;
        }

        String tailNodeName = tokens[1];
        String headNodeName = tokens[3];

        if (!isValidIdentifier(tailNodeName)) {
            error("Bad tail node name: \"" + tailNodeName + "\".");
            return;
        }

        if (!isValidIdentifier(headNodeName)) {
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
            error("Self-loops not allowed.");
            return;
        }

        DirectedGraphNode tail = app.getNodeMap().get(tailNodeName);
        DirectedGraphNode head = app.getNodeMap().get(headNodeName);

        if (!tail.hasChild(head)) {
            tail.addChild(head);
            app.setModificationState(true);
            System.out.println(
                    "Connected " + tailNodeName + " to " + headNodeName);
        } else {
            System.out.println(
                    tailNodeName + "is already connected to " + headNodeName);
        }
    }
}
