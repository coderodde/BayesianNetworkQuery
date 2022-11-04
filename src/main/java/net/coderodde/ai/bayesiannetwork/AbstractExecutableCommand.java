package net.coderodde.ai.bayesiannetwork;

import java.util.Arrays;

public abstract class AbstractExecutableCommand {

    private static final String SPLIT_REGEX = "\\s+";
    
    /**
     * Executes a command.
     * 
     * @param command the text used to invoke this program.
     */
    public abstract void execute(String command);
    
    protected static String[] splitToTokens(String text) {
        String[] tokens = text.split(SPLIT_REGEX);
        
        if (tokens.length == 0) {
            System.out.println("tokens.length == 0");
        }
        
        if (tokens[0].isEmpty()) {
            return Arrays.copyOfRange(tokens, 1, tokens.length);
        }
        
        return tokens;
    }
}
