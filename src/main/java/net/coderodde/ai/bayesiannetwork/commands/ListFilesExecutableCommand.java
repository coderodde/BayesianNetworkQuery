package net.coderodde.ai.bayesiannetwork.commands;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.coderodde.ai.bayesiannetwork.AbstractExecutableCommand;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 ()
 * @since 1.6 ()
 */
public final class ListFilesExecutableCommand 
        extends AbstractExecutableCommand {

    @Override
    public void execute(String command) {
        File currentDirectory = new File(System.getProperty("user.dir"));
        
        for (File file : currentDirectory.listFiles()) {
            if (file.isDirectory()) {
                System.out.println("d - " + getFlags(file) + " " + file.getName());
            } else {
                System.out.println("f - " + getFlags(file) + " " + file.getName());
            }
        }
        
        List<String> fileNames = 
                Stream.of(
                        currentDirectory.list())
                        .collect(Collectors.toList());
        
        fileNames.forEach(System.out::println);
    }
    
    private String getFlags(File file) {
        return (file.canRead() ? "r" : "-") +
               (file.canWrite() ? "w" : "-") +
               (file.canExecute() ? "x" : "-");
    }
}
