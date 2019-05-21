package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import org.mule.api.vcs.client.diff.ApplyResult;
import org.mule.api.vcs.client.diff.Diff;
import org.mule.api.vcs.client.diff.MergingStrategy;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Push changes to server",
        name = "push", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class PushCommand extends BaseCommand implements Callable<Integer> {

    @Option(names = {"--merge_strategy"}, description = "Strategy to be used for merging 'KEEP_THEIRS','KEEP_BOTH' or 'KEEP_OURS'", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    MergingStrategy mergingStrategy = MergingStrategy.KEEP_BOTH;


    @Override
    public Integer call() throws Exception {

        final ApiVCSClient apiVCSClient = createLocalApiVcsClient();
        final ValueResult master = apiVCSClient.push(mergingStrategy, new MergeListenerLogger() {
            @Override
            public void startApplying(List<Diff> diffs) {
                if (diffs.size() > 0) {
                    System.out.println(diffs.size() + " changes were found on the server. Start merging those changes");
                }
            }

            @Override
            public void endApplying(List<Diff> diffs, List<ApplyResult> result) {
                if (diffs.size() > 0) {
                    System.out.println("End of merge.");
                }
            }
        });
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            return 1;
        }

    }
}
