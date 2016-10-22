package net.coderodde.ai.bayesiannetwork.commands;

import java.util.Map;
import java.util.Objects;
import net.coderodde.ai.bayesiannetwork.App;
import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.ProbabilityMap;
import net.coderodde.ai.bayesiannetwork.Utils;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

public class DeleteNodeExecutableCommand extends AbstractExecutableCommand {

    private final App app;
    private final Map<String, DirectedGraphNode> nodeMap;
    private final ProbabilityMap<DirectedGraphNode> probabilityMap;
    
    public DeleteNodeExecutableCommand(
            App app,
            Map<String, DirectedGraphNode> nodeMap,
            ProbabilityMap<DirectedGraphNode> probabilityMap) {
        this.app = 
                Objects.requireNonNull(app,
                                       "The input application object is null.");
        this.nodeMap = 
                Objects.requireNonNull(nodeMap, "The input node map is null.");
        
        this.probabilityMap = 
                Objects.requireNonNull(probabilityMap, 
                                       "The input probability map is null.");
    }
    
    @Override
    public void execute(String command) {
        String[] words = splitToTokens(command);
        
        if (words.length < 2) {
            error("Invalid delete command.");
            return;
        }
        
        String nodeName = words[1];
        
        if (!Utils.isValidIdentifier(nodeName)) {
            error("\"" + nodeName + "\" is not a valid node name.");
            return;
        }
        
        if (words.length > 2 && !words[2].equals(COMMENT_BEGIN_TEXT)) {
            StringBuilder sb = new StringBuilder();
            sb.append(words[0])
              .append(" ")
              .append(words[1])
              .append(" #");
            
            for (int i = 2; i < words.length; ++i) {
                sb.append(" ").append(words[i]);
            }
            
            error("Bad command format. Did you mean \"" + 
                  sb.toString() + "\"?");
            return;
        }
        
        DirectedGraphNode removedNode = nodeMap.remove(nodeName);
        
        if (removedNode != null) {
            removedNode.clear(); // Unlink the removed node from its neighbors.
            probabilityMap.remove(removedNode);
            app.setModificationState(true);
        }
    }
}
