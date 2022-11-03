package net.coderodde.ai.bayesiannetwork;

public abstract class AbstractExecutableCommand {

    private static final String SPLIT_REGEX = "\\s+";
    
    /**
     * Executes a command.
     * 
     * @param command the text used to invoke this program.
     */
    public abstract void execute(String command);
    
    protected static String[] splitToTokens(String text) {
        return text.split(SPLIT_REGEX);
    }
}
