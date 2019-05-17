package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.SimpleResult;
import org.mule.api.vcs.client.diff.MergingStrategy;
import org.mule.api.vcs.client.service.UserInfoProvider;
import org.mule.api.vcs.client.service.impl.ApiManagerFileManager;
import org.mule.designcenter.model.Project;
import org.mule.designcenter.resource.projects.projectId.model.ProjectIdGETHeader;
import org.mule.designcenter.responses.ApiDesignerXapiResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Pulls from the api server.",
        name = "pull", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class PullCommand extends BaseCommand implements Callable<Integer> {

    @Option(names = {"--merge_strategy"})
    MergingStrategy mergingStrategy = MergingStrategy.KEEP_BOTH;


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = new ApiVCSClient(new File("."), new ApiManagerFileManager(getAccessTokenProvider()));
        final SimpleResult master = apiVCSClient.pull(mergingStrategy);
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            return 1;
        }

    }
}
