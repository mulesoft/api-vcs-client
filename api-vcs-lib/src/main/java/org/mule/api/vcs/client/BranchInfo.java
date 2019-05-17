package org.mule.api.vcs.client;

import java.nio.charset.Charset;

public class BranchInfo {

    public static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private String projectId;
    private String branch;

    public BranchInfo() {
    }

    public BranchInfo(String projectId, String branch) {
        this.projectId = projectId;
        this.branch = branch;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getBranch() {
        return branch;
    }
}
