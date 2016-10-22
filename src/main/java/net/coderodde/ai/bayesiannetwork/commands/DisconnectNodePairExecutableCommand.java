package net.coderodde.ai.bayesiannetwork.commands;

import java.util.Map;
import java.util.Objects;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import net.coderodde.ai.bayesiannetwork.Utils;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

public final class DisconnectNodePairExecutableCommand 
extends AbstractExecutableCommand {
    
    private final App app;
    private final Map<String, DirectedGraphNode> nodeMap;
    
    public DisconnectNodePairExecutableCommand(
            App app,
            Map<String, DirectedGraphNode> nodeMap) {
        this.app = 
                Objects.requireNonNull(app, 
                                       "The input application object is null.");
        
        this.nodeMap = Objects.requireNonNull(nodeMap,
                                              "The input node map is null.");
    }

    @Override
    public void execute(String command) {
        String[] words = splitToTokens(command);
        
        if (words.length < 4) {
            error("Missing required tokens.");
            return;
        }
        
        if (!words[2].equals("from")) {
            error("Command format error. The 3rd token should be \"from\".");
            return;
        }
        
        if (words.length > 4 && !words[4].equals(COMMENT_BEGIN_TEXT)) {
            error("The command does not immediately end with a comment.");
            return;
        }
        
        String tailNodeName = words[1];
        String headNodeName = words[3];
        
        if (!Utils.isValidIdentifier(tailNodeName)) {
            error("Bad tail node name: \"" + tailNodeName + "\".");
            return;
        }
        
        if (!Utils.isValidIdentifier(headNodeName)) {
            error("Bad head node name: \"" + headNodeName + "\".");
            return;
        }
        
        if (!nodeMap.containsKey(tailNodeName)) {
            error("No node with name \"" + tailNodeName + "\".");
            return;
        }
        
        if (!nodeMap.containsKey(headNodeName)) {
            error("No node with name \"" + headNodeName + "\".");
            return;
        }
        
        if (tailNodeName.equals(headNodeName)) {
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
