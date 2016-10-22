package net.coderodde.ai.bayesiannetwork.commands;

import java.util.Map;
import java.util.Objects;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import static net.coderodde.ai.bayesiannetwork.Utils.error;
import static net.coderodde.ai.bayesiannetwork.Utils.isValidIdentifier;

public final class QueryArcExecutableCommand extends AbstractExecutableCommand {

    private final App app;
    private final Map<String, DirectedGraphNode> nodeMap;
    
    public QueryArcExecutableCommand(
            App app,
            Map<String, DirectedGraphNode> nodeMap) {
        this.app = 
                Objects.requireNonNull(app, 
                                       "The input application object is null.");
        
        this.nodeMap =
                Objects.requireNonNull(nodeMap, "The input node map is null.");
    }
    
    @Override
    public void execute(String command) {
        String[] words = splitToTokens(command);
        
        if (words.length < 5
                || !words[2].equals("connected")
                || !words[3].equals("to")) {
            error("Bad command format.");
            return;
        }
        
        String tailNodeName = words[1];
        String headNodeName = words[4];
        
        if (!isValidIdentifier(tailNodeName)) {
            error("Bad tail node name \"" + tailNodeName + "\".");
            return;
        }
        
        if (!isValidIdentifier(headNodeName)) {
            error("Bad head node name \"" + headNodeName + "\".");
            return;
        }
        
        if (!nodeMap.containsKey(tailNodeName)) {
            error("No node \"" + tailNodeName + "\".");
            return;
        }
        
        if (!nodeMap.containsKey(headNodeName)) {
            error("No node \"" + headNodeName + "\".");
            return;
        }
        
        DirectedGraphNode tail = nodeMap.get(tailNodeName);
        DirectedGraphNode head = nodeMap.get(headNodeName);
        
        if (tail.hasChild(head)) {
            tail.removeChild(head);
            app.setModificationState(true);
        }
    }
}
