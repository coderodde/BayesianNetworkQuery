package net.coderodde.ai.bayesiannetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import static net.coderodde.ai.bayesiannetwork.BayesNetworkClassifier.classify;
import static net.coderodde.ai.bayesiannetwork.Utils.findEntireGraph;

/**
 * This class implements a console program for working on Bayes networks.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.618 (Sep 19, 2015)
 */
public class App {

    /**
     * This interface defines a command handler.
     */
    @FunctionalInterface
    private static interface CommandHandler {

        /**
         * Handles a command. This method requires two arguments which are 
         * basically the same, as some handlers are better implemented with a 
         * particular command representation.
         * 
         * @param command the actual line containing the entire command.
         * @param tokens  the whitespace delimited tokens of {@code command}.
         */
        void handle(String command, String[] tokens);
    }

    private static void error(String message) {
        System.err.println("ERROR: " + message);
    }

    private final CommandHandler connectHandler = 
            (String command, String[] tokens) -> {
        handleConnect(tokens);
    };

    private final CommandHandler delHandler = 
            (String command, String[] tokens) -> {
        handleDel(tokens);
    };

    private final CommandHandler disconnectHandler = 
            (String command, String[] tokens) -> {
        handleDisconnect(tokens);
    };

    private final CommandHandler echoHandler = 
            (String command, String[] tokens) -> {
        handleEcho(command);
    };

    private final CommandHandler helpHandler = 
            (String command, String[] tokens) -> {
        handleHelp(tokens);
    };

    private final CommandHandler isHandler = 
            (String command, String[] tokens) -> {
        handleIs(tokens);
    };

    private final CommandHandler newHandler = 
            (String command, String[] tokens) -> {
        handleNew(tokens);
    };

    /**
     * This map maps each node name to its representation.
     */
    private final Map<String, DirectedGraphNode> nodeMap = new HashMap<>();

    /**
     * Maps some command names to their respective handlers.
     */
    private final Map<String, CommandHandler> commandMap = new HashMap<>();

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

    /**
     * The scanner for reading the commands.
     */
    private Scanner scanner;

    /**
     * The array of file names to execute.
     */
    private String[] fileNameArray;

    /**
     * The index of the file currently executed.
     */
    private int fileNameIndex;

    /**
     * If set to {@code true} the command prompt "> " will be printed.
     */
    private boolean allowPrompt;

    /**
     * If set to {@code true}, we are reading from standard input.
     */
    private boolean readingFromStdin;

    private App(String[] fileNameArray) {
        this.fileNameArray = fileNameArray;

        commandMap.put("new",        newHandler);
        commandMap.put("del",        delHandler);
        commandMap.put("connect",    connectHandler);
        commandMap.put("disconnect", disconnectHandler);
        commandMap.put("is",         isHandler);
        commandMap.put("echo",       echoHandler);
        commandMap.put("help",       helpHandler);

        if (fileNameArray.length > 0) {
            String fileName = fileNameArray[0];

            try {
                scanner = new Scanner(new FileReader(new File(fileName)));
                fileNameIndex++;
                readingFromStdin = false;
            } catch (FileNotFoundException ex) {
                System.out.println(
                        "ERROR: File \"" + fileName + "\" not found.");
                System.exit(1);
            }

            allowPrompt = false;
        } else {
            scanner = new Scanner(System.in);
            allowPrompt = true;
            readingFromStdin = true;
        }
    }

    private boolean promptAllowed() {
        return allowPrompt;
    }

    private String read() {
        if (!scanner.hasNextLine()) {
            if (fileNameIndex == fileNameArray.length) {
                scanner = new Scanner(System.in);
                allowPrompt = true;
                readingFromStdin = true;
                System.out.print("> ");
            } else {
                try {
                    scanner = new Scanner(
                              new FileReader(
                              new File(fileNameArray[fileNameIndex])));
                    ++fileNameIndex;
                } catch (FileNotFoundException ex) {
                    error("File \"" + fileNameArray[fileNameIndex] + 
                          "\" is not found.");
                    System.exit(1);
                }
            }
        } 

        return scanner.nextLine();
    }

    /**
     * This method implements the actual REPL (Read, Evaluate, Print, Loop).
     */
    private void loop() {
        while (true) {
            if (promptAllowed()) {
                System.out.print("> ");
            }

            String command = read();

            if (command.isEmpty() || command.startsWith("#")) {
                // No text in the command or a line comment.
                continue;
            }

            if (command.equals("quit")) {
                if (readingFromStdin) {
                    // Exit the loop and print "Bye!".
                    break;
                }

                // Print no 'Bye!' whenever executing from files.
                return;
            }

            handleCommand(command);
        }

        System.out.println("Bye!");
    }

    private void handleCommand(String command) {
        // Obtain whitespace delimited tokens.
        String[] words = command.split("\\s+");
        
        if (commandMap.containsKey(words[0])) {
            commandMap.get(words[0]).handle(command, words);
        } else if (words[0].equals("list")) {
            handleList(true);
        } else if (handleQuery(command)) {
            // Once here, the command was recognized as a query. Do not go
            // to 'handlePrintNode'.
        } else {
            // No match whatsoever, possibly the user wants to query a node 
            // information.
            handlePrintNode(words);
        }
    }
    
    /**
     * Checks that an identifier is a valid Java identifier.
     * 
     * @param identifier the identifier to check.
     * @return {@code true} only if the input identifier is valid.
     */
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

    /**
     * Handles the command starting with "new".
     * 
     * @param words the token array.
     */
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

        // Associate (or update probability) the node with the probability 
        // value.
        DirectedGraphNode node;

        if (nodeMap.containsKey(nodeName)) {
            node = nodeMap.get(nodeName);
            probabilityMap.put(node, probability);
        } else {
            node = new DirectedGraphNode(nodeName);
            nodeMap.put(nodeName, node);
        }

        probabilityMap.put(node, probability);
    }

    /**
     * Handles the command for deleting a node.
     * 
     * @param words the array of tokens.
     */
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

    /**
     * Handles the command for creating arcs between nodes.
     * 
     * @param words the array of tokens.
     */
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

    /**
     * Handles the command for removing arcs between nodes.
     * 
     * @param words the array of tokens.
     */
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

    /**
     * Handles the command for querying the existence of arcs between particular
     * nodes.
     * 
     * @param words the array of tokens.
     */
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

    /**
     * Handles the command for listing the system states.
     * 
     * @param showList whether to show the actual state list after successful
     *                 compilation.
     */
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

    /**
     * Handles the command for printing to the console.
     * 
     * @param command the command.
     */
    private void handleEcho(String command) {
        String leftovers = command.substring(4).trim();
        System.out.println(leftovers);
    }

    private Map<DirectedGraphNode, Boolean> 
        loadVariableMap(String command) {
        String[] variableStrings = command.split(",");
        Map<DirectedGraphNode, Boolean> map = new HashMap<>();
        
        for (int i = 0; i < variableStrings.length; ++i) {
            variableStrings[i] = variableStrings[i].trim();
            boolean negate = false;
            String varName;

            if (variableStrings[i].startsWith("not ")) {
                negate = true;
                varName = variableStrings[i].substring(4);
            } else {
                varName = variableStrings[i];
            }

            if (!nodeMap.containsKey(varName)) {
                error("No node \"" + varName + "\".");
                return null;
            } 

            map.put(nodeMap.get(varName), !negate);
        }
        
        return map;
    }
    
    /**
     * Handles the commands for making queries on the network.
     * 
     * @param command the command.
     * @return {@code true} if command prefix is that of query commands.
     */
    private boolean handleQuery(String command) {
        if (!command.startsWith("p(")) {
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

        if (!command.endsWith(")")) {
            error("No trailing \")\".");
            return true;
        }

        String innerContent = command.substring(2, command.length() - 1).trim();
        String[] parts = innerContent.split("\\|");

        if (parts.length != 2) {
            error("No single delimeter bar |");
            return true;
        }

        Map<DirectedGraphNode, Boolean> posterioriVariables =
                loadVariableMap(parts[0]);
        Map<DirectedGraphNode, Boolean> aprioriVariables = 
                loadVariableMap(parts[1]);

        try {
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

    /**
     * Handles the command for printing a node information.
     * 
     * @param words the array of tokens.
     */
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

        // Get parent node names.
        for (DirectedGraphNode parent : node.parents()) {
            sb.append(parent);

            if (i++ < node.parents().size() - 1) {
                sb.append(", ");
            }
        }

        String parentListString = sb.toString();

        sb.delete(0, sb.length());
        i = 0;

        // Get child node names.
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

    /**
     * Handles the command for printing the help information.
     * 
     * @param words the array of tokens.
     */
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

    /**
     * Returns {@code true} if at least one of the input strings is "-h".
     * 
     * @param args the strings to check.
     * @return {@code true} if at least one of the strings is "-h".
     */
    private static boolean hasHelpFlag(String[] args) {
        for (String argument : args) {
            if (argument.trim().equals("-h")) {
                return true;
            }
        }

        return false;
    }

    public static void main(String[] args) {
        if (hasHelpFlag(args)) {
            System.out.println(
                    "java -jar <PROGRAM.jar> [-h] [FILE1 FILE2 ... FILEN]");
            return;
        }

        new App(args).loop();
    }
}
