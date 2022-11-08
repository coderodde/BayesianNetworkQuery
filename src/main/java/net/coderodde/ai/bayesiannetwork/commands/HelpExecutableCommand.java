package net.coderodde.ai.bayesiannetwork.commands;

import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 ()
 * @since 1.6 ()
 */
public class HelpExecutableCommand extends AbstractExecutableCommand {

    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command.trim());
        
            if (tokens.length > 3) {
            error("The syntax for \"help\" command is \"help [keywords]\".");
            return;
        } else if (tokens.length == 3) {
            if (!tokens[1].equals("is") || !tokens[2].equals("connected")) {
                error("The syntax for \"help\" command is " +
                      "\"help [keywords]\".");
                return;
            }
        }

        if (tokens.length == 1) {
            System.out.println("  help new");
            System.out.println("  help del");
            System.out.println("  help connect");
            System.out.println("  help is connected");
            System.out.println("  help disconnect");
            System.out.println("  help list");
            System.out.println("  help ls");
            System.out.println("  help cd");
            System.out.println("  help load");
            System.out.println("  help echo");
            System.out.println("  help #");
            System.out.println("  help <nodename>");
            System.out.println("  help p");
            System.out.println("  help print");
            System.out.println("  help quit");
            return;
        }

        switch (tokens[1]) {
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
                if (tokens.length != 3) {
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

            case "load": {
                System.out.println("\"load <PATH>\"");
                System.out.println(
                        "Loads and executes commands from file <PATH>.");
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
                System.out.println("EXAMPLE 2: p(var1 | var2)");
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

            case "print": {
                System.out.println("Prints information of all the nodes.");
                break;
            }
            
            case "ls": {
                System.out.println(
                        "Lists the contents of the current working directory.");
                break;
            }
            
            case "cd": {
                System.out.println("cd <DIRECTORY>");
                System.out.println(
                        "Changes the current working directory to DIRECTORY.");
                
                System.out.println("EXAMPLE 1: cd ../../abc");
                System.out.println("EXAMPLE 2: cd def/xyz");
                System.out.println("EXAMPLE 3: cd /opt/abc");
                System.out.println("EXAMPLE 4: cd C:\\Users\\Bob");
                break;
            }
            
            case "quit": {
                System.out.println("\"quit\"");
                System.out.println("Quits the program.");
                break;
            }

            default: {
                System.out.println(
                        "ERROR: Unknown topic: \"" + tokens[1] + "\"");
                break;
            }
        } 
    }
}
