package org.mule.api.vcs.client.service;

public class BranchRepositoryLock {

    private boolean success;
    private String owner;
    private BranchRepositoryManager branchRepositoryManager;

    public BranchRepositoryLock(Boolean success, String owner, BranchRepositoryManager branchRepositoryManager) {
        this.success = success;
        this.owner = owner;
        this.branchRepositoryManager = branchRepositoryManager;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOwner() {
        return owner;
    }

    public BranchRepositoryManager getBranchRepositoryManager() {
        return branchRepositoryManager;
    }
}
