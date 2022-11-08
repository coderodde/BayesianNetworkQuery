package net.coderodde.ai.bayesiannetwork.commands;

import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import net.coderodde.ai.bayesiannetwork.Utils;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Nov 8, 2022)
 * @since 1.6 (Nov 8, 2022)
 */
public final class ArcQueryExecutableCommand extends AbstractExecutableCommand {

    private final App app;
    
    public ArcQueryExecutableCommand(App app) {
        this.app = app;
    }
    
    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command.trim());
        
        if (tokens.length < 5
                || !tokens[2].equals("is")
                || !tokens[3].equals("connected")) {
            error("Bad format. Must be '<nodeA> is connected to <nodeB>'");
            return;
        }
        
        if (tokens.length > 5 && !tokens[5].equals(App.COMMENT_BEGIN_TEXT)) {
            error("Bad format. Must be '<nodeA> is connected to <nodeB> " + 
                  "# Comment words.'");
            return;
        }
        
        String tailNodeName = tokens[1];
        String headNodeName = tokens[4];
        
        if (!Utils.isValidIdentifier(tailNodeName)) {
            error("Bad tail node name: \"" + tailNodeName + "\".");
            return;
        }
        
        if (!Utils.isValidIdentifier(headNodeName)) {
            error("Bad head node name: \"" + headNodeName + "\".");
            return;
        }
        
        if (!app.getNodeMap().containsKey(tailNodeName)) {
            error("No node \"" + tailNodeName + "\".");
            return;
        }
        
        if (!app.getNodeMap().containsKey(headNodeName)) {
            error("No node \"" + headNodeName + "\".");
            return;
        }
        
        DirectedGraphNode tail = app.getNodeMap().get(tailNodeName);
        DirectedGraphNode head = app.getNodeMap().get(headNodeName);
        
        System.out.println(tail.hasChild(head));
    }
}
