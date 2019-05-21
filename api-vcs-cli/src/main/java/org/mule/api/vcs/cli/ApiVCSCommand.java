package org.mule.api.vcs.cli;

import picocli.CommandLine;

@CommandLine.Command(name = "apivcs",
        subcommands = {
                CloneCommand.class,
                ListProjectsCommand.class,
                PullCommand.class,
                PushCommand.class,
                DiffCommand.class,
                CreateCommand.class,
                StatusCommand.class,
                RevertCommand.class,
                RevertAllCommand.class
        }
)
public class ApiVCSCommand implements Runnable {
    @Override
    public void run() {
        // print usage help message to STDOUT without ANSI escape codes
        CommandLine.usage(new ApiVCSCommand(), System.out, CommandLine.Help.Ansi.ON);
    }
}
