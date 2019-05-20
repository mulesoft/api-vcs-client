package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import org.mule.api.vcs.client.diff.MergingStrategy;
import org.mule.api.vcs.client.service.impl.ApiRepositoryFileManager;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Pulls from the api server.",
        name = "pull", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class PullCommand extends BaseCommand implements Callable<Integer> {

    @Option(names = {"--merge_strategy"})
    MergingStrategy mergingStrategy = MergingStrategy.KEEP_BOTH;


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = new ApiVCSClient(new File("."), new ApiRepositoryFileManager(getAccessTokenProvider()));
        final ValueResult master = apiVCSClient.pull(mergingStrategy);
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            return 1;
        }

    }
}
