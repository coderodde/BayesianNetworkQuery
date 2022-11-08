package net.coderodde.ai.bayesiannetwork.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;
import net.coderodde.ai.bayesiannetwork.App;
import static net.coderodde.ai.bayesiannetwork.App.COMMENT_BEGIN_TEXT;
import static net.coderodde.ai.bayesiannetwork.Utils.error;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 ()
 * @since 1.6 ()
 */
public final class LoadFileExecutableCommand extends AbstractExecutableCommand {

    private final App app;
    
    public LoadFileExecutableCommand(App app) {
        this.app = app;
    }
    
    @Override
    public void execute(String command) {
        String[] tokens = splitToTokens(command.trim());
        
        if (tokens.length < 2) {
            error("No file specified.");
            return;
        } else if (tokens.length > 2 && !tokens[2].equals(COMMENT_BEGIN_TEXT)) {
            error("Too many tokens.");
            return;
        }
        
        String path = tokens[1];
        
        try {
            List<String> rows = Files.readAllLines(new File(path).toPath());
            
            for (String row : rows) {
                app.handleCommand(row);
            }
        } catch (IOException ex) {
            error("Cannot access file \"" + path + "\".");
        }
    }
}
