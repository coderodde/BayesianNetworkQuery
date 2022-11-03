package net.coderodde.ai.bayesiannetwork.commands;

import java.util.Map;
import java.util.Objects;
import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import net.coderodde.ai.bayesiannetwork.DirectedGraphNode;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.ProbabilityMap;
import static net.coderodde.ai.bayesiannetwork.Utils.error;
import static net.coderodde.ai.bayesiannetwork.Utils.isValidIdentifier;
import static net.coderodde.ai.bayesiannetwork.Utils.parseProbability;

public final class CreateNewNodeExecutableCommand 
extends AbstractExecutableCommand {

    private final Map<String, DirectedGraphNode> nodeMap;
    private final ProbabilityMap<DirectedGraphNode> probabilityMap;
    
    public CreateNewNodeExecutableCommand(
            Map<String, DirectedGraphNode> nodeMap,
            ProbabilityMap<DirectedGraphNode> probabilityMap) {
        this.nodeMap = 
                Objects.requireNonNull(nodeMap,
                                       "The input node map is null.");
        this.probabilityMap = 
                Objects.requireNonNull(probabilityMap,
                                       "The input probability map is null.");
    }
    
    @Override
    public void execute(String command) {
        String[] words = splitToTokens(command);
        
        if (words.length < 3) {
            error("Cannot parse 'new' command.");
            return;
        }
        
        if (words.length >= 4 && !words[3].startsWith(COMMENT_BEGIN_TEXT)) {
            error("Bad comment format.");
            return;
        }
    
        String nodeName = words[1];
        
        if (!isValidIdentifier(nodeName)) {
            error("\"" + nodeName + "\" is a bad node identifier.");
            return;
        }
        
        String probabilityString = words[2];
        double probability;
        
        try {
            probability = parseProbability(probabilityString);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
            return;
        }
        
        DirectedGraphNode node;
        
        if (nodeMap.containsKey(nodeName)) {
            node = nodeMap.get(nodeName);
        } else {
            node = new DirectedGraphNode(nodeName, probability);
            nodeMap.put(nodeName, node);
        }
        
        probabilityMap.put(node, probability);
    }
}
