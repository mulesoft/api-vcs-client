package org.mule.api.vcs.client;

import java.nio.charset.Charset;

public class BranchInfo {

    public static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private String projectId;
    private String branch;
    private String orgId;

    public BranchInfo() {
    }

    public BranchInfo(String projectId, String branch) {
        this(projectId,branch,null);
    }

    public BranchInfo(String projectId, String branch, String orgId) {
        this.projectId = projectId;
        this.branch = branch;
        this.orgId = orgId;
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

    public String getOrgId(){
        return orgId;
    }
}
