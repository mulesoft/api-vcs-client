package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(description = "Reverts a given file.",
        name = "revert", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class RevertCommand extends BaseAuthorizedCommand implements Callable<Integer> {

    @CommandLine.Parameters(description = "The file to revert.", arity = "1", index = "0")
    String relativePath;


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = createLocalApiVcsClient();
        final ValueResult<Void> master = apiVCSClient.revert(relativePath);
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            final ValueResult<String> valueResult = apiVCSClient.currentBranch();
            System.out.println();
            System.out.println("File reverted "+ relativePath + " successfully.");
            System.out.println();
            return 1;
        }
    }
}
