package org.mule.designcenter.vcs.client;

import java.nio.charset.Charset;

public class ApiVCSConfig {

    public static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private String projectId;
    private String branch;

    public ApiVCSConfig() {
    }

    public ApiVCSConfig(String projectId, String branch) {
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
