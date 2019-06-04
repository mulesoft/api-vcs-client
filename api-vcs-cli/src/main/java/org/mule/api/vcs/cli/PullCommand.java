package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import org.mule.api.vcs.client.diff.MergingStrategy;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Pulls from the api server.",
        name = "pull", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class PullCommand extends BaseAuthorizedCommand implements Callable<Integer> {

    @Option(names = {"--merge_strategy"}, description = "Strategy to be used for merging 'KEEP_THEIRS','KEEP_BOTH' or 'KEEP_OURS'", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    MergingStrategy mergingStrategy = MergingStrategy.KEEP_BOTH;


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = createLocalApiVcsClient();
        final ValueResult<String> valueResult = apiVCSClient.currentBranch();
        if (valueResult.isFailure()) {
            System.err.println(valueResult.getMessage().get());
            return -1;
        }
        System.out.println();
        System.out.println("Start pulling from " + valueResult.doGetValue());
        final ValueResult master = apiVCSClient.pull(getAccessTokenProvider(), mergingStrategy, new MergeListenerLogger());
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] There where some conflicts while pulling.");
            return -1;
        } else {
            System.out.println();
            System.out.println("Pulled from `" + valueResult.doGetValue() + "` successfully.");
            System.out.println();
            return 1;
        }
    }

}
