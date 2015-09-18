package net.coderodde.ai.bayesiannetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import static net.coderodde.ai.bayesiannetwork.BayesNetworkClassifier.classify;
import static net.coderodde.ai.bayesiannetwork.Utils.findEntireGraph;

/**
 * This class implements a console program for working on Bayes networks.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.61 (Sep 18, 2015)
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

    private void loop(String[] fileNames) {
        Scanner scanner;
        boolean turnOffPrompt;
        int fileNameIndex = 0;
        
        if (fileNames != null) {
            try {
                scanner = new Scanner(
                          new FileReader(
                          new File(fileNames[fileNameIndex])));
                
                fileNameIndex++;
            } catch (FileNotFoundException ex) {
                error("File \"" + fileNames[fileNameIndex] + "\" not found.");
                return;
            }
            
            turnOffPrompt = true;
        } else {
            scanner = new Scanner(System.in);
            turnOffPrompt = false;
        }
 
        for (;;) {
            if (!turnOffPrompt) {
                System.out.print("> ");
            }
            
            if (!scanner.hasNextLine()) {
                if (fileNames != null) {
                    if (fileNameIndex == fileNames.length) {
                        fileNames = null;
                        turnOffPrompt = false;
                        scanner = new Scanner(System.in);
                        System.out.print("> ");
                    } else {
                        try {
                            scanner = new Scanner(
                                      new FileReader(
                                      new File(fileNames[fileNameIndex])));
                            fileNameIndex++;
                        } catch (FileNotFoundException ex) {
                            error("File \"" + fileNames[fileNameIndex] + 
                                  "\" not found.");
                        }
                    }
                } else {
                    return;
                }
            }
            
            String command = scanner.nextLine().trim();

            if (command.isEmpty()) {
                // No text in the command line.
                continue;
            }

            if (command.equals("quit")) {
                if (fileNames != null) {
                    // Print no 'Bye!'
                    return;
                }
                
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
                    handleList(true);
                    continue;
                }
                
                case "is": {
                    handleIs(words);
                    continue;
                }
                
                case "echo": {
                    handleEcho(command);
                    continue;
                }
                
                case "help": {
                    handleHelp(words); 
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
            error("Cannot parse 'new' command.");
            return;
        }
        
        if (words.length >= 4 && !words[3].startsWith("#")) {
            error("Bad comment format.");
            return;
        }
        
        String nodeName = words[1];
        String probabilityString = words[2];
        
        if (!isValidIdentifier(nodeName)) {
            error("\"" + nodeName + "\" is a bad node identifier.");
            return;
        }
        
        double probability;
        
        try {
            probability = Double.parseDouble(probabilityString);
        } catch (NumberFormatException ex) {
            error("Cannot parse \"" + probabilityString + 
                  "\" as a probability value.");
            return;
        }
        
        if (Double.isNaN(probability)) {
            error("Input probability is NaN.");
            return;
        }
        
        if (probability < 0.0) {
            error("Probability is too small.");
            return;
        }
        
        if (probability > 1.0) {
            error("Probability is too large.");
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
            error("Missing the name of the node to delete.");
            return;
        }
        
        String nodeName = words[1];
        
        if (!isValidIdentifier(nodeName)) {
            error("\"" + nodeName + "\" is not a valid node name.");
            return;
        }
        
        DirectedGraphNode removed = nodeMap.remove(nodeName);
        
        if (removed != null) {
            removed.clear();
            probabilityMap.remove(removed);
            stateModified = true;
        }
    }
    
    private void handleConnect(String[] words) {
        if (words.length < 4) {
            error("Missing required tokens.");
            return;
        }
        
        if (!words[2].equals("to")) {
            error("Format error.");
            return;
        }
        
        String tailNodeName = words[1];
        String headNodeName = words[3];
        
        if (!isValidIdentifier(tailNodeName)) {
            error("Bad tail node name: \"" + tailNodeName + "\".");
            return;
        }
        
        if (!isValidIdentifier(headNodeName)) {
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
            error("Self-loops not allowed.");
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
            error("Missing required tokens.");
            return;
        }
        
        if (!words[2].equals("from")) {
            error("Format error.");
            return;
        }
        
        String tailNodeName = words[1];
        String headNodeName = words[3];
        
        if (!isValidIdentifier(tailNodeName)) {
            error("Bad tail node name: \"" + tailNodeName + "\".");
            return;
        }
        
        if (!isValidIdentifier(headNodeName)) {
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
            stateModified = true;
        }
    }
    
    private void handleIs(String[] words) {
        if (words.length < 5
                || !words[2].equals("connected")
                || !words[3].equals("to")) {
            error("Bad format.");
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
            error("No node \"" + tailNodeName + "\"");
            return;
        }
        
        if (!nodeMap.containsKey(headNodeName)) {
            error("No node \"" + headNodeName + "\"");
            return;
        }
        
        DirectedGraphNode tail = nodeMap.get(tailNodeName);
        DirectedGraphNode head = nodeMap.get(headNodeName);
        
        System.out.println(tail.hasChild(head));
    }
    
    private void handleList(boolean showList) {
        if (stateModified) {
            List<DirectedGraphNode> network = new ArrayList<>(nodeMap.values());
            
            if (network.isEmpty()) {
                error("You have no nodes.");
                return;
            }
            
            List<DirectedGraphNode> component = findEntireGraph(network.get(0));
            
            if (component.size() < network.size()) {
                error("The graph is not connected.");
                return; 
            }
            
            try {
                long startTime = System.currentTimeMillis();
                result = BayesNetworkClassifier.classify(network, 
                                                         probabilityMap);
                long endTime = System.currentTimeMillis();
                stateModified = false;
                
                System.out.println("Compiled the graph in " + 
                                  (endTime - startTime) + " milliseconds.");
                if (Math.abs(1.0 - result.getSumOfProbabilities()) > 0.0001) {
                    throw new IllegalStateException(
                    "The sum of probabilities over all possible states does " + 
                    "not sum to 1.0");
                }
                
                System.out.println("Number of possible states: " +
                                   result.getNumberOfStates());
            } catch (Exception ex) {
                error(ex.getMessage());
                return;
            }
        }

        if (result == null) {
            error("No network built yet.");
        } else if (showList) {
            System.out.print(result);
        }
    }
    
    private void handleEcho(String command) {
        String leftovers = command.substring(4).trim();
        System.out.println(leftovers);
    }
    
    private boolean handleQuery(String line) {
        if (!line.startsWith("p(")) {
            return false;
        }
        
        if (stateModified) {
            handleList(false);
            
            if (stateModified) {
                // If 'handleList' could not update the state, we have a problem
                // with the graph: it is either disconnected or contains cycles.
                return true;
            }
        }

        if (!line.endsWith(")")) {
            error("No trailing \")\".");
            return true;
        }

        String innerContent = line.substring(2, line.length() - 1).trim();
        String[] parts = innerContent.split("\\|");

        if (parts.length != 2) {
            error("No single delimeter bar |");
            return true;
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
                    error("No node \"" + varName + "\".");
                    return true;
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
                    error("No node \"" + varName + "\".");
                    return true;
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
                    error(ex.getMessage());
                    return true;
                }
            }
            
            System.out.println(result.query(posterioriVariables, 
                                            aprioriVariables));
        } catch (Exception ex) {
            error(ex.getMessage());
            return true;
        }
        
        return true;
    }

    private void handlePrintNode(String[] words) {
        if (words.length > 1 && !words[1].startsWith("#")) {
            error("Bad command.");
            return;
        }
        
        if (!nodeMap.containsKey(words[0])) {
            error("\"" + words[0] + "\": no such node.");
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
    
    private void handleHelp(String[] words) {
        if (words.length > 3) {
            error("The syntax for \"help\" command is \"help [keywords]\".");
            return;
        } else if (words.length == 3) {
            if (!words[1].equals("is") || !words[2].equals("connected")) {
                error("The syntax for \"help\" command is " +
                      "\"help [keywords]\".");
                return;
            }
        }
        
        if (words.length == 1) {
            System.out.println("  help new");
            System.out.println("  help del");
            System.out.println("  help connect");
            System.out.println("  help is connected");
            System.out.println("  help disconnect");
            System.out.println("  help list");
            System.out.println("  help echo");
            System.out.println("  help #");
            System.out.println("  help <nodename>");
            System.out.println("  help p");
            System.out.println("  help quit");
            return;
        }
        
        switch (words[1]) {
            case "new": {
                System.out.println("\"new <nodename> <probability>\"");
                System.out.println("Creates a new node with name <nodename> " +
                                   "and probability <probability>.");
                break;
            }
            
            case "del": {
                System.out.println("\"del <nodename>\"");
                System.out.println("Deletes the node with name <nodename>.");
                break;
            }
            
            case "connect": {
                System.out.println("\"connect <tailnode> to <headnode>\"");
                System.out.println("Creates an arc from <tailnode> to " +
                                   "<headnode>.");
                break;
            }
            
            case "is": {
                if (words.length != 3) {
                    System.out.println(
                            "ERROR: No help topic. Did you mean " +
                            "\"help is connected\"?");
                } else {
                    System.out.println(
                            "\"is <tailnode> connected to <headnode>\"");
                    System.out.println("Asks whether <tailnode> has a child " +
                                       "<headnode>.");
                }
                    
                break;
            }
            
            case "disconnect": {
                System.out.println("\"disconnect <tailnode> from <headnode>\"");
                System.out.println("Removes an arc from <tailnode> to " +
                                   "<headnode>.");
                break;
            }
            
            case "list": {
                System.out.println("\"list\"");
                System.out.println("Lists all the possible system states.");
                break;
            }
            
            case "echo": {
                System.out.println("\"echo [<text>]\"");
                System.out.println("Prints <text> to the console.");
                break;
            }
                
            case "#": {
                System.out.println("\"# [<text>]\"");
                System.out.println("Starts a line comment.");
                break;
            }
            
            case "p": {
                System.out.println(
                        "\"p(<posterioriVariables> | " +
                        "<aprioriVariables>)\"");
                System.out.println("Makes a query.");
                System.out.println("EXAMPLE 1: p(not var1 | var2, not var3)");
                System.out.println("EXAMPLE 2: p(var 1 | var2)");
                System.out.println(".");
                System.out.println(".");
                System.out.println(".");
                break;
            }
            
            case "<nodename>": {
                System.out.println("\"<nodename>\"");
                System.out.println("Print the node information.");
                break;
            }
            
            case "quit": {
                System.out.println("\"quit\"");
                System.out.println("Quits the program.");
                break;
            }
            
            default: {
                System.out.println(
                        "ERROR: Unknown topic: \"" + words[1] + "\"");
                break;
            }
        }
    }
    
    private static boolean hasHelpFlag(String[] args) {
        for (String argument : args) {
            if (argument.trim().equals("-h")) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void gen() {
        int width = 8;
        int depth = 9;
        
        long seed = System.currentTimeMillis();
        Random random = new Random(seed);
        List<List<DirectedGraphNode>> levels = new ArrayList<>();
        int id = 0;
        
        for (int d = 0; d < depth; ++d) {
            int w = Math.max(4, random.nextInt(width) + 1);
            List<DirectedGraphNode> level = new ArrayList<>(w);
            
            for (int i = 0; i < w; ++i) {
                level.add(new DirectedGraphNode("nde" + id));
                id++;
            }
            
            levels.add(level);
        }
        
        int edges = 200;
        
        while (edges > 0) {
            int levelA = random.nextInt(levels.size());
            int levelB = random.nextInt(levels.size());
            
            if (levelA == levelB) {
                continue;
            }
            
            if (levelA > levelB) {
                int tmp = levelA;
                levelA = levelB;
                levelB = tmp;
            }
            
            List<DirectedGraphNode> listA = levels.get(levelA);
            List<DirectedGraphNode> listB = levels.get(levelB);
            
            listA.get(random.nextInt(listA.size()))
                    .addChild(listB.get(random.nextInt(listB.size())));
            --edges;
        }
        
        for (List<DirectedGraphNode> level : levels) {
            for (DirectedGraphNode node : level) {
                System.out.println("new " + node.toString() + " 0.5");
            }
        }
        
        for (List<DirectedGraphNode> level : levels) {
            for (DirectedGraphNode node : level) {
                for (DirectedGraphNode child : node.children()) {
                    System.out.println("connect " + node + " to " + child);
                }
            }
        }
    }
    
    public static void main(String[] args) {
        if (hasHelpFlag(args)) {
            System.out.println("java -jar <PROGRAM.jar> [-h] [FILE_NAME]");
            return;
        }
        
        App app = new App();
        
        if (args.length == 0) {
            app.loop(null);
        } else {
            app.loop(args);
        }
    }
    
    private static void error(String message) {
        System.err.println("ERROR: " + message);
    }
}
