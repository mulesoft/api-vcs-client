package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.SimpleResult;
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

@Command(description = "Clones a project in the given location",
        name = "clone", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class CloneCommand extends BaseCommand implements Callable<Integer> {
    @Parameters(description = "The project id to clone.", arity = "1")
    String projectId;

    @Parameters(description = "Target directory name", arity = "0..1")
    String targetDirectory;


    @Option(names = {"-b", "--branch"}, description = "The branch name.")
    String branch;


    @Override
    public Integer call() throws Exception {
        final UserInfoProvider accessTokenProvider = getAccessTokenProvider();
        final String folderName;
        if (targetDirectory == null) {
            final ApiDesignerXapiResponse<Project> response = ApiClientFactory.apiDesigner().projects.projectId(projectId).get(new ProjectIdGETHeader(accessTokenProvider.getOrgId(), accessTokenProvider.getUserId()), accessTokenProvider.getAccessToken());
            folderName = response.getBody().getName();
        } else {
            folderName = targetDirectory;
        }
        final File workingDirectory = new File(folderName);
        if (workingDirectory.exists()) {
            System.err.println("[Error] Target directory already exists.");
            return -1;
        }

        final boolean mkdirs = workingDirectory.mkdirs();
        if (!mkdirs) {
            System.err.println("[Error] Unable to create target directory.");
            return -1;
        }

        final ApiVCSClient apiVCSClient = new ApiVCSClient(workingDirectory, new ApiManagerFileManager(accessTokenProvider));
        final SimpleResult master = apiVCSClient.clone(new BranchInfo(projectId, Optional.ofNullable(branch).orElse("master")));
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            return 1;
        }

    }
}
