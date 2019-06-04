package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(description = "Reverts all changes.",
        name = "revert-all", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class RevertAllCommand extends BaseAuthorizedCommand implements Callable<Integer> {


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = createLocalApiVcsClient();
        final ValueResult<Void> master = apiVCSClient.revertAll();
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            final ValueResult<String> valueResult = apiVCSClient.currentBranch();
            System.out.println();
            System.out.println("All changes were reverted successfully.");
            System.out.println();
            return 1;
        }
    }
}
