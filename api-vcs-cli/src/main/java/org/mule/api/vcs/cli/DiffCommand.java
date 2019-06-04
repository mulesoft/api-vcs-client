package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import org.mule.api.vcs.client.diff.Diff;
import picocli.CommandLine.Command;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

@Command(description = "Show diffs",
        name = "diff", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class DiffCommand extends BaseCommand implements Callable<Integer> {


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = createLocalApiVcsClient();
        final ValueResult<List<Diff>> mayBeDiffs = apiVCSClient.diff();
        if (mayBeDiffs.isFailure()) {
            System.err.println(mayBeDiffs.getMessage().get());
            return -1;
        }
        if (mayBeDiffs.doGetValue().isEmpty()) {
            System.out.println("No differences found.");
        } else {
            final PrintWriter printWriter = new PrintWriter(System.out);
            for (Diff diff : mayBeDiffs.doGetValue()) {
                diff.print(printWriter);
            }
            printWriter.close();
        }
        return 1;
    }
}
