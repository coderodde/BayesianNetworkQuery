package net.coderodde.ai.bayesiannetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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

    private boolean stateModified = true;
    private ClassificationResult result;

    private void loop() {
        Scanner scanner = new Scanner(System.in);

        for (;;) {
            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                // Most probably the input was redirected, print no "Bye!".
                return;
            }

            String commandLine = scanner.nextLine().trim();

            if (commandLine.isEmpty()) {
                // No text in the command line.
                continue;
            }

            if (commandLine.equals("quit")) {
                break;
            }

            String[] words = commandLine.split("\\s+");

            switch (words[0]) {
                case "node": {
                    handleNodeCommand(words);
                    break;
                }

                case "connect": {
                    handleConnectCommand(words);
                    break;
                }

                case "disconnect": {
                    handleDisconnectCommand(words);
                    break;
                }

                case "print": {
                    handlePrintCommand();
                    break;
                } 

                default: {
                    if (words[0].startsWith("p(")) {
                        tryQuery(commandLine);
                    } else {
                        System.out.println(
                                "Unknown command: \"" + commandLine + "\".");
                    }   
                }
            }
        }

        System.out.println("Bye!");
    }

    private void tryQuery(String line) {
        if (!line.startsWith("p(")) {
            return;
        }

        if (!line.endsWith(")")) {
            System.out.println("No trailing \")\".");
            return;
        }

        String innerContent = line.substring(2, line.length() - 1).trim();
        String[] parts = innerContent.split("\\|");

        if (parts.length != 2) {
            System.out.println("Error: No single delimeter bar |");
            return;
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
                    System.out.println("Error: no node \"" + varName + "\".");
                    return;
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
                    System.out.println("Error: no node \"" + varName + "\".");
                    return;
                }

                aprioriVariables.put(nodeMap.get(varName), !negate);
            }

            System.out.println(result.query(posterioriVariables, aprioriVariables));
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void handleNodeCommand(String[] words) {
        if (words.length == 1) {
            System.out.println("Error: no node name given.");
            return;
        } 

        String nodeName = words[1].trim();

        if (!nodeMap.containsKey(nodeName)) {
            if (words.length == 2) {
                System.out.println("Node \"" + nodeName + "\" is not defined.");
                return;
            }

            // Try to add a new node.
            double newProbability;

            try {
                newProbability = Double.parseDouble(words[2]);
            } catch (NumberFormatException ex) {
                System.out.println(
                        "Error: " + words[2] + " is an invalid probability.");
                return;
            }

            try {
                DirectedGraphNode newnode = new DirectedGraphNode(words[1]);
                nodeMap.put(nodeName, newnode);
                probabilityMap.put(newnode, newProbability);
                stateModified = true;
            } catch (IllegalArgumentException ex) {
                System.out.println("Error: " + ex.getMessage());
                nodeMap.remove(words[1]);
            }

            return;
        } else {
            if (words.length == 2) {
                // Print the current probability of a node.
                System.out.println(probabilityMap.get(nodeMap.get(nodeName)));
            } else if (words.length > 2) {
                // Update the probability.
                double newProbability;

                try {
                    newProbability = Double.parseDouble(words[2]);
                } catch (NumberFormatException ex) {
                    System.out.println(
                            "Error: misspelled probability: " + words[2]);
                    return;
                }

                try {
                    probabilityMap.put(nodeMap.get(nodeName), newProbability);
                } catch (IllegalArgumentException ex) {
                    System.out.println("Error: " + ex.getMessage());
                }
            }
        }
    }

    private void handleConnectCommand(String[] words) {
        if (words.length < 4 || !words[2].equals("to")) {
            System.out.println("Bad format. Use \"connect <tail> to <head>\"");
            return;
        }

        String tailNodeName = words[1];
        String headNodeName = words[3];

        if (!nodeMap.containsKey(tailNodeName)) {
            System.out.println("Error: \"" + words[1] + "\", no such node.");
            return;
        }

        if (!nodeMap.containsKey(headNodeName)) {
            System.out.println("Error: \"" + words[1] + "\", no such node.");
            return;
        }

        DirectedGraphNode tail = nodeMap.get(tailNodeName);
        DirectedGraphNode head = nodeMap.get(headNodeName);

        if (tail.hasChild(head)) {
            return;
        }

        tail.addChild(head);
        stateModified = true;
    }

    private void handleDisconnectCommand(String[] words) {
        if (words.length < 4 || !words[2].equals("from")) {
            System.out.println("Bad format. Use \"connect <tail> to <head>\"");
            return;
        }

        String tailNodeName = words[1];
        String headNodeName = words[3];

        if (!nodeMap.containsKey(tailNodeName)) {
            System.out.println("Error: \"" + words[1] + "\", no such node.");
            return;
        }

        if (!nodeMap.containsKey(headNodeName)) {
            System.out.println("Error: \"" + words[1] + "\", no such node.");
            return;
        }

        DirectedGraphNode tail = nodeMap.get(tailNodeName);
        DirectedGraphNode head = nodeMap.get(headNodeName);

        if (!tail.hasChild(head)) {
            return;
        }

        tail.removeChild(head);
        stateModified = true;
    }

    private void handlePrintCommand() {
        if (stateModified) {
            List<DirectedGraphNode> network = new ArrayList<>(nodeMap.values());

            try {
                result = BayesNetworkClassifier.classify(network, probabilityMap);
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

    public static void main(String[] args) {
        App app = new App();
        app.loop();
    }
}
