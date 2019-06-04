package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.service.impl.ApiRepositoryFileManager;

import java.io.File;
import java.io.IOException;

public class BaseCommand {
    protected ApiVCSClient createLocalApiVcsClient() throws IOException {
        final File targetDirectory = getLocalWorkspaceDirectory();
        return new ApiVCSClient(targetDirectory, new ApiRepositoryFileManager());
    }

    protected File getLocalWorkspaceDirectory() throws IOException {
        return new File(".").getCanonicalFile();
    }
}
