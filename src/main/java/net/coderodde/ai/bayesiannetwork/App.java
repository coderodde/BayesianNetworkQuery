package net.coderodde.ai.bayesiannetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import static net.coderodde.ai.bayesiannetwork.BayesNetworkClassifier.classify;

/**
 * This class implements a console program for working on Bayes networks.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Sep 15, 2015)
 */
public class App {

    /**
     * This map maps each node name to its internal representation.
     */
    private final Map<String, DirectedGraphNode> nodeMap = new HashMap<>();

    /**
     * This map maps each node to its probability.
     */
    private final ProbabilityMap<DirectedGraphNode> probabilityMap =
            new ProbabilityMap<>();

    /**
     * Indicates whether the state of the graph was changed after last
     * compilation.
     */
    private boolean stateModified = true;
    
    /**
     * Caches the last classification result for queries.
     */
    private ClassificationResult result;

    private void loop() {
        Scanner scanner = new Scanner(System.in);

        for (;;) {
            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                // Most probably the input was redirected, print no "Bye!".
                return;
            }

            String command = scanner.nextLine().trim();

            if (command.isEmpty()) {
                // No text in the command line.
                continue;
            }

            if (command.equals("quit")) {
                break;
            }

            if (command.startsWith("#")) {
                // A comment line.
                continue;
            }
            
            String[] words = command.split("\\s+");

            switch (words[0]) {
                case "new": {
                    handleNew(words);
                    continue;
                }
                
                case "del": {
                    handleDel(words);
                    continue;
                }
                
                case "connect": {
                    handleConnect(words);
                    continue;
                }
                
                case "disconnect": {
                    handleDisconnect(words);
                    continue;
                }
                
                case "list": {
                    handleList();
                    continue;
                }
                
                case "is": {
                    handleIs(words);
                    continue;
                }
            }
            
            if (handleQuery(command)) {
                continue;
            }
            
            handlePrintNode(words);
        }

        System.out.println("Bye!");
    }

    private static boolean isValidIdentifier(String identifier) {
        if (identifier.isEmpty()) {
            return false;
        }
        
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < identifier.length(); ++i) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    private void handleNew(String[] words) {
        if (words.length < 3) {
            System.out.println("ERROR: cannot parse 'new' command.");
            return;
        }
        
        if (words.length >= 4 && !words[3].startsWith("#")) {
            System.out.println("ERROR: Bad comment format.");
            return;
        }
        
        String nodeName = words[1];
        String probabilityString = words[2];
        
        if (!isValidIdentifier(nodeName)) {
            System.out.println("ERROR: \"" + nodeName + "\" is a bad node " +
                               "identifier.");
            return;
        }
        
        double probability;
        
        try {
            probability = Double.parseDouble(probabilityString);
        } catch (NumberFormatException ex) {
            System.out.println("ERROR: Cannot parse \"" + probabilityString + 
                               "\" as a probability value.");
            return;
        }
        
        if (Double.isNaN(probability)) {
            System.out.println("ERROR: Input probability is NaN.");
            return;
        }
        
        if (probability < 0.0) {
            System.out.println("ERROR: Probability is too small.");
            return;
        }
        
        if (probability > 1.0) {
            System.out.println("ERROR: Probability is too large.");
            return;
        }
        
        // Associate (or reassociate) the node with the probability value.
        DirectedGraphNode newnode = new DirectedGraphNode(nodeName);
        nodeMap.put(nodeName, newnode);
        probabilityMap.put(newnode, probability);
        stateModified = true;
    }
    
    private void handleDel(String[] words) {
        if (words.length < 2) {
            System.out.println(
                    "ERROR: Missing the name of the node to delete.");
            return;
        }
        
        String nodeName = words[1];
        
        if (!isValidIdentifier(nodeName)) {
            System.out.println("ERROR: \"" + nodeName + "\" is not a valid " +
                               "node name.");
            return;
        }
        
        DirectedGraphNode removed = nodeMap.remove(nodeName);
        
        if (removed != null) {
            probabilityMap.remove(removed);
            stateModified = true;
        }
    }
    
    private void handleConnect(String[] words) {
        if (words.length < 4) {
            System.out.println("ERROR: Missing required tokens.");
            return;
        }
        
        if (!words[2].equals("to")) {
            System.out.println("ERROR: Format error.");
            return;
        }
        
        String tailNodeName = words[1];
        String headNodeName = words[3];
        
        if (!isValidIdentifier(tailNodeName)) {
            System.out.println("ERROR: Bad tail node name: \"" + tailNodeName +
                               "\".");
            return;
        }
        
        if (!isValidIdentifier(headNodeName)) {
            System.out.println("ERROR: Bad head node name: \"" + headNodeName +
                               "\".");
            return;
        }
        
        if (!nodeMap.containsKey(tailNodeName)) {
            System.out.println("ERROR: No node with name \"" + tailNodeName + 
                               "\".");
            return;
        }
        
        if (!nodeMap.containsKey(headNodeName)) {
            System.out.println("ERROR: No node with name \"" + headNodeName + 
                               "\".");
            return;
        }
        
        if (tailNodeName.equals(headNodeName)) {
            System.out.println("ERROR: Self-loops not allowed.");
            return;
        }
        
        DirectedGraphNode tail = nodeMap.get(tailNodeName);
        DirectedGraphNode head = nodeMap.get(headNodeName);
        
        if (!tail.hasChild(head)) {
            tail.addChild(head);
            stateModified = true;
        }
    }
    
    private void handleDisconnect(String[] words) {
        if (words.length < 4) {
            System.out.println("ERROR: Missing required tokens.");
            return;
        }
        
        if (!words[2].equals("from")) {
            System.out.println("ERROR: Format error.");
            return;
        }
        
        String tailNodeName = words[1];
        String headNodeName = words[3];
        
        if (!isValidIdentifier(tailNodeName)) {
            System.out.println("ERROR: Bad tail node name: \"" + tailNodeName +
                               "\".");
            return;
        }
        
        if (!isValidIdentifier(headNodeName)) {
            System.out.println("ERROR: Bad head node name: \"" + headNodeName +
                               "\".");
            return;
        }
        
        if (!nodeMap.containsKey(tailNodeName)) {
            System.out.println("ERROR: No node with name \"" + tailNodeName + 
                               "\".");
            return;
        }
        
        if (!nodeMap.containsKey(headNodeName)) {
            System.out.println("ERROR: No node with name \"" + headNodeName + 
                               "\".");
            return;
        }
        
        if (tailNodeName.equals(headNodeName)) {
            return;
        }
        
        DirectedGraphNode tail = nodeMap.get(tailNodeName);
        DirectedGraphNode head = nodeMap.get(headNodeName);
        
        if (tail.hasChild(head)) {
            tail.removeChild(head);
            stateModified = true;
        }
    }
    
    private void handleIs(String[] words) {
        if (words.length < 5
                || !words[2].equals("connected")
                || !words[3].equals("to")) {
            System.out.println("ERROR: Bad format.");
            return;
        }
        
        String tailNodeName = words[1];
        String headNodeName = words[4];
        
        if (!isValidIdentifier(tailNodeName)) {
            System.out.println("ERROR: Bad tail node name \"" + tailNodeName +
                               "\".");
            return;
        }
        
        if (!isValidIdentifier(headNodeName)) {
            System.out.println("ERROR: Bad head node name \"" + headNodeName +
                               "\".");
            return;
        }
        
        if (!nodeMap.containsKey(tailNodeName)) {
            System.out.println("ERROR: No node \"" + tailNodeName + "\"");
            return;
        }
        
        if (!nodeMap.containsKey(headNodeName)) {
            System.out.println("ERROR: No node \"" + headNodeName + "\"");
            return;
        }
        
        DirectedGraphNode tail = nodeMap.get(tailNodeName);
        DirectedGraphNode head = nodeMap.get(headNodeName);
        
        System.out.println(tail.hasChild(head));
    }
    
    private void handleList() {
        if (stateModified) {
            List<DirectedGraphNode> network = new ArrayList<>(nodeMap.values());

            try {
                result = BayesNetworkClassifier.classify(network, 
                                                         probabilityMap);
                stateModified = false;
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                return;
            }
        }

        if (result == null) {
            System.out.println("Error: no network built yet.");
        } else {
            System.out.println(result);
        }
    }
    
    private boolean handleQuery(String line) {
        if (!line.startsWith("p(")) {
            return false;
        }

        if (!line.endsWith(")")) {
            System.out.println("ERROR: No trailing \")\".");
            return false;
        }

        String innerContent = line.substring(2, line.length() - 1).trim();
        String[] parts = innerContent.split("\\|");

        if (parts.length != 2) {
            System.out.println("ERROR: No single delimeter bar |");
            return false;
        }

        Map<DirectedGraphNode, Boolean> posterioriVariables = new HashMap<>();
        Map<DirectedGraphNode, Boolean> aprioriVariables = new HashMap<>();

        String[] posterioriVarStrings = parts[0].split(",");
        String[] aprioriVarStrings = parts[1].split(",");

        try {
            for (int i = 0; i < posterioriVarStrings.length; ++i) {
                posterioriVarStrings[i] = posterioriVarStrings[i].trim();
                boolean negate = false;
                String varName;

                if (posterioriVarStrings[i].startsWith("not ")) {
                    negate = true;
                    varName = posterioriVarStrings[i].substring(4);
                } else {
                    varName = posterioriVarStrings[i];
                }

                if (!nodeMap.containsKey(varName)) {
                    System.out.println("ERROR: No node \"" + varName + "\".");
                    return false;
                } 

                posterioriVariables.put(nodeMap.get(varName), !negate);
            }

            for (int i = 0; i < aprioriVarStrings.length; ++i) {
                aprioriVarStrings[i] = aprioriVarStrings[i].trim();
                boolean negate = false;
                String varName;

                if (aprioriVarStrings[i].startsWith("not ")) {
                    negate = true;
                    varName = aprioriVarStrings[i].substring(4);
                } else {
                    varName = aprioriVarStrings[i];
                }

                if (!nodeMap.containsKey(varName)) {
                    System.out.println("ERROR: No node \"" + varName + "\".");
                    return false;
                }

                aprioriVariables.put(nodeMap.get(varName), !negate);
            }

            if (stateModified) {
                try {
                    result = classify(new ArrayList<>(nodeMap.values()), 
                                      probabilityMap);
                    
                    if (result != null) {
                        stateModified = false;
                    }
                } catch (Exception ex) {
                    System.out.println("ERROR: " + ex.getMessage());
                    return false;
                }
            }
            
            System.out.println(result.query(posterioriVariables, 
                                            aprioriVariables));
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
        
        return true;
    }

    private void handlePrintNode(String[] words) {
        if (words.length > 1 && !words[1].startsWith("#")) {
            System.out.println("ERROR: Bad command.");
            return;
        }
        
        if (!nodeMap.containsKey(words[0])) {
            System.out.println("\"" + words[0] + "\": no such node.");
            return;
        }
        
        DirectedGraphNode node = nodeMap.get(words[0]);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        
        for (DirectedGraphNode parent : node.parents()) {
            sb.append(parent);
            
            if (i++ < node.parents().size() - 1) {
                sb.append(", ");
            }
        }
        
        String parentListString = sb.toString();
        
        sb.delete(0, sb.length());
        i = 0;
        
        for (DirectedGraphNode child : node.children()) {
            sb.append(child);
            
            if (i++ < node.children().size() - 1) {
                sb.append(", ");
            }
        }
        
        String childListString = sb.toString();
        
        System.out.println(
                "\"" + words[0] + "\", probability " + 
                probabilityMap.get(node) + ", parents: <" + parentListString +
                ">, children: <" + childListString + ">");
    }
    
    public static void main(String[] args) {
        App app = new App();
        app.loop();
    }
}
