package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import org.mule.api.vcs.client.diff.Diff;
import picocli.CommandLine.Command;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

@Command(description = "Show the status",
        name = "status", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class StatusCommand extends BaseAuthorizedCommand implements Callable<Integer> {


    @Override
    public Integer call() throws Exception {
        System.out.println();
        final ApiVCSClient apiVCSClient = createLocalApiVcsClient();
        final ValueResult<List<Diff>> mayBeDiffs = apiVCSClient.diff();
        if (mayBeDiffs.isFailure()) {
            System.err.println(mayBeDiffs.getMessage().get());
            return -1;
        }
        if (mayBeDiffs.doGetValue().isEmpty()) {
            System.out.println("No changes found.");
        } else {
            System.out.println();
            System.out.println("Changes to be committed:");
            System.out.println();
            final PrintWriter printWriter = new PrintWriter(System.out);
            for (Diff diff : mayBeDiffs.doGetValue()) {
                printWriter.println("\t" + diff.getOperationType().getLabel() + " " + diff.getRelativePath());
            }
            printWriter.flush();
        }
        System.out.println();
        return 1;
    }
}
