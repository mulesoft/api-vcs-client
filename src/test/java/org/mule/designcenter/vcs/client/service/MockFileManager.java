package org.mule.designcenter.vcs.client.service;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MockFileManager implements ApiFileManager {

    private File directory;

    public MockFileManager(File directory) {
        this.directory = directory;
    }

    @Override
    public ApiLock acquireLock(String projectId, String branchName) {
        return new ApiLock(true, "acme", new MockBranchFileManager(new File(directory, branchName)));
    }

    @Override
    public void releaseLock(String projectId, String branchName) {
        //Nothing to do here
    }

    @Override
    public List<ApiBranch> listBranches(String projectId) {
        final String[] list = directory.list();
        if (list == null) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(list).map((name) -> new ApiBranch(name)).collect(Collectors.toList());
        }
    }
}
