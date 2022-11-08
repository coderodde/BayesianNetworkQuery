package net.coderodde.ai.bayesiannetwork.commands;

import java.io.File;
import java.nio.file.Path;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 ()
 * @since 1.6 ()
 */
public final class ChangeDirectoryExecutableCommand 
        extends AbstractExecutableCommand {

    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command.trim());
        
        File currentDirectory = new File(System.getProperty("user.dir"));
        String nextPathString = 
                currentDirectory.getAbsolutePath() 
                + File.separator
                + tokens[1];
        
        File nextDirectory = new File(nextPathString);
        
        if (!nextDirectory.exists()) {
            error("Directory " 
                    + nextDirectory.getAbsolutePath() 
                    + " does not exist.");
            return;
        }
        
        Path normalizedNextDirectoryPath = 
                Path.of(nextDirectory.getAbsolutePath()).normalize();
        
        nextPathString = normalizedNextDirectoryPath.toFile().getAbsolutePath();
        
        System.setProperty("user.dir", nextPathString);
        System.out.println("Switched to \"" + nextPathString + "\"");
    }
}
