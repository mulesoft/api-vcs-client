package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.SimpleResult;
import org.mule.api.vcs.client.diff.Diff;
import org.mule.api.vcs.client.diff.MergingStrategy;
import org.mule.api.vcs.client.service.impl.ApiManagerFileManager;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Show diffs",
        name = "diff", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class DiffCommand extends BaseCommand implements Callable<Integer> {


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = new ApiVCSClient(new File("."), new ApiManagerFileManager(getAccessTokenProvider()));
        final List<Diff> master = apiVCSClient.diff();
        if (master.isEmpty()) {
            System.out.println("No differences found.");
        } else {
            for (Diff diff : master) {
                diff.print(new PrintWriter(System.out));
            }
        }
        return 1;
    }
}
