package org.mule.api.vcs.cli;

import picocli.CommandLine;

@CommandLine.Command(name = "apivcs",
        subcommands = {
                CloneCommand.class,
                ListProjectsCommand.class,
                PullCommand.class,
                PushCommand.class,
                DiffCommand.class
        }
)
public class ApiVCSCommand implements Runnable {
    @Override
    public void run() {

    }
}
