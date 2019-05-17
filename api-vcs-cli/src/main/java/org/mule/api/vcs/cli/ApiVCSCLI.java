package org.mule.api.vcs.cli;

import org.mule.api.vcs.cli.exceptions.ConfigurationException;
import picocli.CommandLine;

public class ApiVCSCLI {

    public static void main(String[] args) {

        final int commandLine = new CommandLine(new ApiVCSCommand()).setExecutionExceptionHandler((e, commandLine1, parseResult) -> {
            if (e instanceof ConfigurationException) {
                System.err.println(e.getMessage());
            } else {
                e.printStackTrace();
            }
            return -1;
        }).execute(args);
        System.exit(commandLine);
    }

}
