package org.mule.api.vcs.client.service;

import org.mule.api.vcs.client.BranchInfo;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MockFileManager implements RepositoryFileManager {

    private File directory;
    private Map<String, Integer> counters = new HashMap<>();

    public MockFileManager(File directory) {
        this.directory = directory;
    }

    @Override
    public BranchRepositoryLock acquireLock(String projectId, String branchName) {
        int counter = Optional.ofNullable(counters.get(branchName)).orElse(0);
        final File branchDirectory = getBranchDirectory(branchName, counter);
        if (getBranchDirectory(branchName, counter + 1).exists()) {
            counter = counter + 1;
        }
        counters.put(branchName, counter);
        return new BranchRepositoryLock(true, "acme", new MockBranchRepositoryManager(branchDirectory));
    }

    private File getBranchDirectory(String branchName, int counter) {
        return new File(directory, branchName + File.separator + "t" + counter);
    }

    @Override
    public void releaseLock(String projectId, String branchName) {
        //Nothing to do here
    }

    @Override
    public List<ApiBranch> branches(String projectId) {
        final String[] list = directory.list();
        if (list == null) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(list).map((name) -> new ApiBranch(name)).collect(Collectors.toList());
        }
    }

    @Override
    public List<ProjectInfo> projects() {
        return new ArrayList<>();
    }

    @Override
    public BranchInfo create(ApiType apiType, String name, String description) {
        return null;
    }

    @Override
    public String getGroupId() {
        return "";
    }
}
