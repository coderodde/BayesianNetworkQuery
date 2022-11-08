package net.coderodde.ai.bayesiannetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import static net.coderodde.ai.bayesiannetwork.BayesNetworkClassifier.classify;
import static net.coderodde.ai.bayesiannetwork.Utils.error;
import static net.coderodde.ai.bayesiannetwork.Utils.findEntireGraph;
import net.coderodde.ai.bayesiannetwork.commands.ArcQueryExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.ChangeDirectoryExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.ConnectNodePairExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.CreateNewNodeExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.DeleteNodeExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.DisconnectNodePairExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.EchoExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.HelpExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.ListFilesExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.LoadFileExecutableCommand;
import net.coderodde.ai.bayesiannetwork.commands.PrintNodesExecutableCommand;

/**
 * This class implements a console program for working on Bayes networks.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6180 (Nov 8, 2022)
 * @version 1.618 (Sep 19, 2015)
 */
public final class App {

    /**
     * Defines the token denoting a line comment.
     */
    public static final String COMMENT_BEGIN_TEXT = "#";
    
    /**
     * This map maps each node name to its representation.
     */
    private final Map<String, DirectedGraphNode> nodeMap = new TreeMap<>();

    /**
     * Maps command names to their respective handlers.
     */
    private final Map<String, AbstractExecutableCommand> commandMap =
            new HashMap<>();

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

        commandMap.put("new",        new CreateNewNodeExecutableCommand(this));
        commandMap.put("del",        new DeleteNodeExecutableCommand(this));
        commandMap.put("is",         new ArcQueryExecutableCommand(this));
        commandMap.put("disconnect", new DisconnectNodePairExecutableCommand(
                                         this));
        
        commandMap.put("connect",    new ConnectNodePairExecutableCommand(
                                         this));
        
        commandMap.put("echo",       new EchoExecutableCommand());
        commandMap.put("help",       new HelpExecutableCommand());
        commandMap.put("print",      new PrintNodesExecutableCommand(this));
        commandMap.put("ls",         new ListFilesExecutableCommand());
        commandMap.put("cd",         new ChangeDirectoryExecutableCommand());
        commandMap.put("load",       new LoadFileExecutableCommand(this));

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
    
    public Map<String, DirectedGraphNode> getNodeMap() {
        return nodeMap;
    }
    
    public ProbabilityMap<DirectedGraphNode> getProbabilityMap()  {
        return probabilityMap;
    }
    
    public void setModificationState(boolean stateModified) {
        this.stateModified = stateModified;
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
                System.out.println("Bye!");
                return;
            }

            handleCommand(command);
        }
    }

    public void handleCommand(String command) {
        // Obtain whitespace delimited tokens.
        String[] words = command.split("\\s+");

        if (commandMap.containsKey(words[0])) {
            commandMap.get(words[0]).execute(command);
        } else if (words[0].equals("list")) {
            handleList(true);
        } else if (words[0].equals(COMMENT_BEGIN_TEXT)) {
            // A comment. Do nothing.
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
            sb.append(parent.getName());

            if (i++ < node.parents().size() - 1) {
                sb.append(", ");
            }
        }

        String parentListString = sb.toString();

        sb.delete(0, sb.length());
        i = 0;

        // Get child node names.
        for (DirectedGraphNode child : node.children()) {
            sb.append(child.getName());

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
