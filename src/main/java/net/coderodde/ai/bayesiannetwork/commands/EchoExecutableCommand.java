package net.coderodde.ai.bayesiannetwork.commands;

import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 ()
 * @since 1.6 ()
 */
public final class EchoExecutableCommand extends AbstractExecutableCommand {

    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command.trim());
        
        for (int i = 1; i < tokens.length; i++) {
            System.out.print(tokens[i] + " ");
        }
        
        System.out.println();
    }
}
