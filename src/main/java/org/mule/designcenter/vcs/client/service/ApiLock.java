package org.mule.designcenter.vcs.client.service;

public class ApiLock {

    private boolean success;
    private String owner;
    private BranchFileManager branchFileManager;

    public ApiLock(Boolean success, String owner, BranchFileManager branchFileManager) {
        this.success = success;
        this.owner = owner;
        this.branchFileManager = branchFileManager;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOwner() {
        return owner;
    }

    public BranchFileManager getBranchFileManager() {
        return branchFileManager;
    }
}
